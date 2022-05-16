require 'csv'
require 'pp'
require 'set'

require './vertex'
require './edge'
require './graph'
require './panel'
require './junction_box'

def get_assigned_box_from_circuit(junction_boxes, circuit)
  stripped_box_id = circuit.junction_box_id.split('-')[0].to_i
  junction_boxes[stripped_box_id]&.first
end

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
    edge.vertices.all? { |v| v.x < 0 && v.z > 0 }
  end

  panels = graph.panels.values.select do |panel|
    panel.vertices.all? { |v| v.x < 0 && v.z > 0 }
  end

  edges.each do |edge|
    # Find a box that can support the max current of the whole edge, or create
    # one if none exist, then assign each strip in the edge to a circuit within
    # that box.
    candidates = edge_assignment_candidates(
      edge: edge,
      graph: graph,
      junction_boxes: junction_boxes,
    )
    if candidates.empty?
      vertex_id = find_location_for_new_box(vertices: edge.vertices.map(&:id), graph: graph, allowed_vertices: allowed_vertices(panels: panels))
      vertex = graph.vertices[vertex_id]
      candidates = [JunctionBox.new(vertex: vertex)]
      junction_boxes[vertex_id] ||= []
      junction_boxes[vertex_id] << candidates.first
    end

    box = candidates.max_by(&:utilization)
    edge.strips.each do |strip|
      circuit = box.circuits
        .select { |c| c.current + strip.max_current <= JunctionBoxCircuit::MAX_CURRENT }
        .max_by(&:utilization)
      circuit.edge_strips << strip
    end
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
        panel.closest_junction_box = box
        next
      end

      circuit = candidates.min_by(&:utilization)
      circuit.panel_strips << strip

      # Multiple boxes can be at one vertex. We just need the stripped ID here.
      box = get_assigned_box_from_circuit(junction_boxes, circuit)
      panel.closest_junction_box = box
    end
  end

  # Now take the assignments for the quadrant and mirror them over to the other
  # 3 quadrants. There will be some unassigned panels and edges at this point
  # (those that cross quadrants), but we can deal with those separately.
  mirror_assignments(junction_boxes: junction_boxes, graph: graph)

  junction_boxes
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
  transforms.each do |transform|
    junction_boxes.values.flatten.each do |box|
      mirrored_vertex = graph.vertices.values.find do |v|
        v.x == box.vertex.x * transform[0] && v.z == box.vertex.z * transform[1]
      end
      next if mirrored_vertex.nil?
      new_box = JunctionBox.new(vertex: mirrored_vertex)
      box.circuits.each_with_index do |circuit|
        circuit.edge_strips.each_with_index do |strip, i|
          mirrored_strip = find_mirror_edge_strip(edge_strip: strip, transform: transform, graph: graph)
          next if mirrored_strip.nil?
          new_box.circuits[i].edge_strips << mirrored_strip
        end

        circuit.panel_strips.each_with_index do |strip, i|
          mirrored_strip = find_mirror_panel_strip(panel_strip: strip, transform: transform, graph: graph)
          next if mirrored_strip.nil?
          new_box.circuits[i].panel_strips << mirrored_strip

          panel = graph.panels[strip.panel_id]
          next if panel.closest_junction_box.present?

          panel.closest_junction_box = box
        end
      end
      junction_boxes[mirrored_vertex.id] ||= []
      junction_boxes[mirrored_vertex.id] << new_box
    end
  end
end

def find_mirror_edge_strip(edge_strip:, transform:, graph:)
  mirrored_vertices = edge_strip.vertices.map do |v|
    graph.vertices.values.find do |w|
      v.x * transform[0] == w.x && v.z * transform[1] == w.z
    end
  end
  return nil if mirrored_vertices.any?(&:nil?)
  pp mirrored_vertices
  edge = graph.edges[mirrored_vertices.map(&:id).sort.join('-')]
  _, _, index = edge_strip.id.split('-')
  edge.strips[index.to_i]
end

def find_mirror_panel_strip(panel_strip:, transform:, graph:)
  mirrored_vertices = panel_strip.vertices.map do |v|
    graph.vertices.values.find do |w|
      v.x * transform[0] == w.x && v.z * transform[1] == w.z
    end
  end
  return nil if mirrored_vertices.any?(&:nil?)

  panel = graph.panels.values.find do |p|
    (p.vertices.map(&:id) & mirrored_vertices).length == 3
  end
  return nil if panel.nil?

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
  puts "#{junction_boxes.values.flatten.size} total junction boxes"
  puts "#{boxes.sum(&:current)} Amps"
  puts "#{boxes.sum(&:utilization) / boxes.count} average utilization"
end

vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
boxes = place_junction_boxes(graph: graph)
print_boxes(boxes)

edge_to_box = {}
boxes.values.flatten.each do |box|
  box.circuits.each do |circuit|
    circuit.edge_strips.each do |strip|
      edge_to_box[strip.edge_id] ||= Set.new
      edge_to_box[strip.edge_id].add(box.id)
    end
  end
end
