require './edge.rb'
require './graph.rb'
require './panel.rb'
require './vertex.rb'

assignments = CSV.read('edge_assignments.tsv', col_sep: "\t")
vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
boxes = place_junction_boxes(graph: graph)

# These are just for one lead; we'll need this amount in red and black.
edge_power_length_feet = 0
panel_power_length_feet = 0

assignments.each do |assignment|
  vertex_id, *edge_ids = assignment
  vertex_id = vertex_id.to_i
  edge_ids = edge_ids.compact

  edge_ids.each do |edge_id|
    edge = edges[edge_id]
    next if edge.nil?

    edge.vertices.each do |v|
      distance = graph.min_distance(vertex_id, v.id)
      distance_feet = distance / 304_800

      edge_power_length_feet += distance_feet
    end
  end
end

# TODO: calculate power lines to centroids
# TODO: calculate ethernet runs along edges to controllers

total_red_wire_feet = (edge_power_length_feet + panel_power_length_feet) * (1 + LINE_OVERAGE_BUFFER_PERCENT)
total_black_wire_feet = total_red_wire_feet