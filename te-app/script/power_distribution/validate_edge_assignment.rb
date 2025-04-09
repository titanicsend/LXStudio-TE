require 'pp'

require './edge.rb'
require './vertex.rb'
require './graph.rb'

assignments = CSV.read('edge_assignments.tsv', col_sep: "\t")
vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
graph = Graph.new(edges: edges)

errors = {}

assignments.each do |assignment|
  vertex_id, *edge_ids = assignment
  vertex_id = vertex_id.to_i
  edge_ids = edge_ids.compact
  errors[vertex_id] = []
  edge_ids.each do |edge_id|
    edge = edges[edge_id]
    next if edge.nil?

    edge.vertices.each do |v|
      distance = graph.min_distance(vertex_id, v.id)
      distance_feet = distance / 304_800
      if distance_feet > 17
        errors[vertex_id] << "vertex #{v.id} of edge #{edge_id} is #{distance_feet} feet away from #{vertex_id}"
      end
    end
  end
end

pp errors
