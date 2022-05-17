require 'matrix'

require './edge.rb'
require './graph.rb'
require './panel.rb'
require './place_junction_boxes.rb'
require './vertex.rb'

edge_assignments = CSV.read('edge_assignments.tsv', col_sep: "\t")
vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
boxes = place_junction_boxes(graph: graph)

def microns_to_feet(distance_in_microns)
  distance_in_microns / 304_800
end

def min_distance_between_vertices_in_feet(graph, vertex1, vertex2)
  distance = graph.min_distance(vertex1, vertex2)
  microns_to_feet(distance)
end

# straight_line_distance calculates the straight line distance between two points.
# These points may be vertices, but they're not required to be.
def straight_line_distance(point1, point2)
  vector1 = Vector[point1[:x], point1[:y], point1[:z]]
  vector2 = Vector[point2.x, point2.y, point2.z]
  microns_to_feet((vector1 - vector2).magnitude)
end

edge_lengths_count = {}

edge_assignments.each do |edge_assignment|
  vertex_id, *edge_ids = edge_assignment
  vertex_id = vertex_id.to_i
  edge_ids = edge_ids.compact

  edge_ids.each do |edge_id|
    edge = edges[edge_id]
    next if edge.nil?

    edge.vertices.each do |v|
      edge_power_length_feet = min_distance_between_vertices_in_feet(graph, vertex_id, v.id) * (1 + LINE_OVERAGE_BUFFER_PERCENT)
      if edge_lengths_count[edge_power_length_feet].nil?
        edge_lengths_count[edge_power_length_feet] = 1
      else
        edge_lengths_count[edge_power_length_feet] += 1
      end
    end
  end
end
edge_lengths_count = edge_lengths_count.sort

panel_lengths_count = {}

# TODO: finish calculating power lines to centroids of panels
graph.panels.each do |_, panel|
  centroid = panel.centroid

  # FIXME: why are there panels without closest junction boxes?
  assigned_junction_box = panel.closest_junction_box
  if assigned_junction_box.nil?
    next
  end

  panel_power_length_feet = straight_line_distance(centroid, assigned_junction_box.vertex) * (1 + LINE_OVERAGE_BUFFER_PERCENT)
  if panel_lengths_count[panel_power_length_feet].nil?
    panel_lengths_count[panel_power_length_feet] = 1
  else
    panel_lengths_count[panel_power_length_feet] += 1
  end
end
panel_lengths_count = panel_lengths_count.sort

puts "edge_lengths_count: #{edge_lengths_count}"
puts "panel_lengths_count: #{panel_lengths_count}"