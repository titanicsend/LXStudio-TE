require 'csv'
require 'pp'
require 'set'

require './vertex'
require './edge'
require './graph'
require './panel'
require './junction_box'
require './calculate_line_lengths'
require './controller'

# Place junction boxes such that:
#   - Each edge and panel is assigned to circuits within a single box
#
#   - For the parts of the vehicle that are symmetrical, the wiring is also
#     symmetrical
#
#   - No boxes are placed anywhere they can't be visually obscured by panels
#     (or by the platform we intend to have at the crow's nest)
def place_junction_boxes(graph:)
  junction_boxes = {}

  # Start by just assigning boxes to the audience-facing, rear (i.e. stage
  # right) quadrant of the vehicle.
  edges = graph.edges.values.select do |edge|
    edge.vertices.all? { |v| v.x <= 0 && v.z >= 0 }
  end.sort_by(&:depth)

  panels = graph.panels.values.select do |panel|
    panel.vertices.all? { |v| v.x <= 0 && v.z >= 0 }
  end

  assign(edges: edges, panels: panels, junction_boxes: junction_boxes, graph: graph)

  # Now take the assignments for the quadrant and mirror them over to the other
  # 3 quadrants. There will be some unassigned panels and edges at this point
  # (those that cross quadrants), but we can deal with those separately.
  mirror_assignments(junction_boxes: junction_boxes, graph: graph)

  # Finally, assign everything that wasn't a mirrored version of something in the first quandrant.
  edges = graph.edges.values.select { |edge| edge.strips.all? { |strip| strip.circuit.nil? } }.sort_by(&:depth)
  panels = graph.panels.values.select { |panel| panel.strips.all? { |strip| strip.circuit.nil? } }

  assign(edges: edges, panels: panels, junction_boxes: junction_boxes, graph: graph)
  junction_boxes
end

def assign(edges:, panels:, junction_boxes:, graph:)
  edges.each do |edge|
    next if edge.assigned?

    # Find a box that can support the max current of the whole edge, or create
    # one if none exist, then assign each strip in the edge to a circuit within
    # that box.
    candidates = edge_assignment_candidates(
      edge: edge,
      graph: graph,
      junction_boxes: junction_boxes,
    )
    if candidates.empty?
      vertex_id = find_location_for_new_box(
        vertices: [edge.signal_in_vertex.id, edge.signal_out_vertex.id],
        graph: graph,
        allowed_vertices: allowed_vertices(panels: panels),
      )

      vertex = graph.vertices[vertex_id]
      candidates = [JunctionBox.new(vertex: vertex)]
      junction_boxes[vertex_id] ||= []
      junction_boxes[vertex_id] << candidates.first
    end

    box = candidates.max_by(&:utilization)
    already_selected_circuits = []
    edge.strips.each do |strip|
      circuit = box.circuits
        .select { |c| c.current + strip.max_current <= JunctionBoxCircuit::MAX_CURRENT }
        .max_by { |c| [already_selected_circuits.include?(circuit) ? 1 : 0, -1 * c.utilization] }
      circuit.edge_strips << strip
      strip.circuit = circuit
      already_selected_circuits << circuit
    end

    try_chaining_next_edge(edge: edge)
  end

  panels.each do |panel|
    panel.strips.each do |strip|
      candidates = panel_strip_assignment_candidates(panel: panel, strip: strip, graph: graph, junction_boxes: junction_boxes)
      if candidates.empty?
        vertex = panel.vertices[0]
        box = JunctionBox.new(vertex: vertex)
        box.circuits[0].panel_strips << strip
        junction_boxes[vertex.id] ||= []
        junction_boxes[vertex.id] << box
        next
      end

      circuit = candidates.max_by(&:utilization)
      circuit.panel_strips << strip
      strip.circuit = circuit
    end
  end
end

def try_chaining_next_edge(edge:)
  next_edge = edge.signal_to
  return if next_edge.nil?

  can_chain = edge.strips.all? do |strip|
    next_edge.strips.first.max_current+ strip.circuit.current <= JunctionBoxCircuit::MAX_CURRENT
  end
  return unless can_chain

  edge.strips.each_with_index do |strip, i|
    next_edge.strips[i].circuit = strip.circuit
    strip.circuit.edge_strips << next_edge.strips[i]
  end
end

def allowed_vertices(panels:)
  crows_nest_vertices = [31, 42]
  crows_nest_vertices + panels.map(&:vertices).flatten.map(&:id).uniq
end

def edge_assignment_candidates(edge:, graph:, junction_boxes:)
  # Consider junction boxes that already exist at each of the vertices of the
  # edge as well as junction boxes placed at immediate neigbors of those
  # vertices if the distance from that neighbor to each end of the edge is <=
  # 17 ft (1V drop for 12 AWG). Pick the candidate that leads to the best
  # average utilization, or create a new box if one doesn't already exist.
  candidates = []

  edge.vertices.each do |vertex|
    unless junction_boxes[vertex.id].nil?
      candidates += junction_boxes[vertex.id].select do |box|
        box.can_fit_edge?(edge)
      end
    end
  end

  if candidates.empty?
    candidate_vertices = edge.vertices
      .map { |v| v.adjacent(graph: graph, max_level: 2) }
      .flatten
      .select { |v| edge.vertices.any? { |w| graph.min_distance(v, w.id) < 17 * 304_800 } }

    candidate_vertices.each do |v|
      unless junction_boxes[v].nil?
        candidates += junction_boxes[v].select do |box|
          box.can_fit_edge?(edge)
        end
      end
    end
  end

  candidates
end

def find_location_for_new_box(vertices:, graph:, allowed_vertices:)
  queue = vertices
  visited = Set.new
  until queue.empty?
    v = queue.shift
    visited.add(v)
    if allowed_vertices.include?(v)
      return v
    end
    queue += graph.adjacency[v].reject { |w| visited.include?(w) }
  end

  nil
end

def panel_strip_assignment_candidates(panel:, strip:, graph:, junction_boxes:)
  candidate_circuits = []

  strip.vertices.each do |vertex|
    unless junction_boxes[vertex.id].nil?
      candidate_circuits += junction_boxes[vertex.id]
        .map(&:circuits)
        .flatten
        .select { |c| c.current + strip.current <= JunctionBoxCircuit::MAX_CURRENT }
    end
  end

  if candidate_circuits.empty?
    vertices = strip.vertices
      .map { |v| graph.adjacency[v.id] }
      .flatten
      .uniq
      .select { |v| strip.vertices.all? { |w| graph.min_distance(v, w.id) < (17 * 304_800) } }
    vertices.each do |v|
      if !junction_boxes[v].nil?
        candidate_circuits += junction_boxes[v]
          .map(&:circuits)
          .flatten
          .select { |c| c.current + strip.current <= JunctionBoxCircuit::MAX_CURRENT }
      end
    end
  end

  candidate_circuits
end

def mirror_assignments(junction_boxes:, graph:)
  transforms = [[1, -1], [-1, 1], [-1, -1]]
  new_junction_boxes = {}
  transforms.each do |transform|
    junction_boxes.values.flatten.each do |box|
      mirrored_vertex = graph.vertices.values.find do |v|
        (v.x - box.vertex.x * transform[0]).abs < 100_000 && (v.y - box.vertex.y).abs < 100_000 && (v.z - box.vertex.z * transform[1]).abs < 100_000
      end
      next if mirrored_vertex.nil? || mirrored_vertex == box.vertex

      new_box = JunctionBox.new(vertex: mirrored_vertex)
      box.circuits.each_with_index do |circuit, i|
        circuit.edge_strips.each do |strip|
          mirrored_strip = find_mirror_edge_strip(edge_strip: strip, transform: transform, graph: graph)
          next unless !mirrored_strip.nil? && mirrored_strip.circuit.nil?

          puts "#{strip.id}: #{mirrored_strip.id}"
          new_box.circuits[i].edge_strips << mirrored_strip
          mirrored_strip.circuit = new_box.circuits[i]
        end

        circuit.panel_strips do |strip|
          mirrored_strip = find_mirror_panel_strip(panel_strip: strip, transform: transform, graph: graph)
          next if mirrored_strip.nil?
          next unless mirrored_strip.circuit.nil?
          new_box.circuits[i].panel_strips << mirrored_strip
          mirrored_strip.circuit = new_box.circuits[i]
        end
      end
      new_junction_boxes[mirrored_vertex.id] ||= []
      new_junction_boxes[mirrored_vertex.id] << new_box
    end
  end

  junction_boxes.merge!(new_junction_boxes)
end

def find_mirror_edge_strip(edge_strip:, transform:, graph:)
  mirrored_vertices = edge_strip.vertices.map do |v|
    graph.vertices.values.find do |w|
      (v.x - w.x * transform[0]).abs < 100_000 && (v.y - w.y).abs < 100_000 && (v.z - w.z * transform[1]).abs < 100_000
    end
  end

  return nil if mirrored_vertices.any?(&:nil?)
  edge = graph.edges[mirrored_vertices.map(&:id).sort.join('-')]
  _, _, index = edge_strip.id.split('-')
  edge.strips[index.to_i]
end

def find_mirror_panel_strip(panel_strip:, transform:, graph:)
  mirrored_vertices = panel_strip.vertices.map do |v|
    graph.vertices.values.find do |w|
      (v.x - w.x * transform[0]).abs < 100_000 && (v.y - w.y).abs < 100_000 && (v.z - w.z * transform[1]).abs < 100_000
    end
  end
  return nil if mirrored_vertices.any?(&:nil?)

  panel = graph.panels.values.find do |p|
    (p.vertices.map(&:id) & mirrored_vertices.map(&:id)).length == 3
  end

  _, index = panel_strip.id.split('-')
  panel.strips[index.to_i]
end

def print_boxes(junction_boxes)
  junction_boxes.each do |vertex, boxes|
    next if boxes.empty?
    puts "#{vertex} - #{boxes.sum(&:current)} A - #{100 * (boxes.sum(&:utilization) / boxes.size).truncate(4)}% utilized"
  end

  boxes = junction_boxes.values.flatten
  puts junction_boxes.keys.length
  puts "#{boxes.size} total junction boxes"
  puts "#{boxes.sum(&:current)} Amps"
  puts "#{boxes.sum(&:utilization) / boxes.count} average utilization"
end

vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
Edge.load_signal_paths(filename: '../../resources/vehicle/edge_signal_paths.tsv', edges: edges, vertices: vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
controllers = Controller.load_controllers(edge_signal_filename: '../../resources/vehicle/edge_signal_paths.tsv', panel_signal_filename: '../../resources/vehicle/panel_signal_paths.tsv', graph: graph, vertices: vertices)
boxes = place_junction_boxes(graph: graph)
print_boxes(boxes)

Controller.assign_controllers_to_boxes(graph: graph, controllers: controllers, junction_boxes: boxes)

boxes.each do |_, box_grouping|
  box_grouping.each do |box|
    puts "Junction box #{box.id} has #{box.controllers.length} controller(s) assigned"
  end
end

edge_to_box = {}
boxes.values.flatten.each do |box|
  box.circuits.each do |circuit|
    # if circuit.utilization > 1
    #   puts "#{circuit.id}: #{circuit.utilization}"
    # end
    circuit.edge_strips.each do |strip|
      edge_to_box[strip.edge_id] ||= Set.new
      edge_to_box[strip.edge_id].add(box.id)
    end
  end
end

graph.panels.each_value do |panel|
  panel.strips.each do |strip|
    if strip.circuit.nil?
      puts "#{strip.id} unassigned"
    end
  end
end

graph.edges.each_value do |edge|
  edge.strips.each do |strip|
    if strip.circuit.nil?
      puts "#{strip.id} unassigned"
    end
  end
end

lengths = bucket_cable_lengths(power_cable_lengths(boxes: boxes.values.flatten, graph: graph))
pp lengths
total_length = lengths.sum { |k, v| k * v }
pp total_length
