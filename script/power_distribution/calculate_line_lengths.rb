require './edge.rb'
require './graph.rb'
require './panel.rb'
require './vertex.rb'

edge_assignments = CSV.read('edge_assignments.tsv', col_sep: "\t")
vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
boxes = place_junction_boxes(graph: graph)

# These are just for one lead; we'll need this amount in red and black.
edge_power_length_feet = 0
panel_power_length_feet = 0

def min_distance_between_vertices_in_feet(graph, vertex1, vertex2)
  distance = graph.min_distance(vertex1, vertex2)
  distance / 304_800
end

edge_assignments.each do |edge_assignment|
  vertex_id, *edge_ids = edge_assignment
  vertex_id = vertex_id.to_i
  edge_ids = edge_ids.compact

  edge_ids.each do |edge_id|
    edge = edges[edge_id]
    next if edge.nil?

    edge.vertices.each do |v|
      edge_power_length_feet += min_distance_between_vertices_in_feet(vertex_id, v.id)
    end
  end
end

# TODO: calculate power lines to centroids of panels
# TODO: calculate ethernet runs along edges to controllers


total_red_wire_feet = (edge_power_length_feet + panel_power_length_feet) * (1 + LINE_OVERAGE_BUFFER_PERCENT)
total_black_wire_feet = total_red_wire_feet