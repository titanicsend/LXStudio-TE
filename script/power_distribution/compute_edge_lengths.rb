require 'pp'

require './vertex'
require './edge'
require './graph'
require './panel'
require './junction_box'
require 'csv'

# Edge geopmetry assumes points in space connected by edges.
# In reality the LEDs cannot go all the way to each vertex due
# to physical interference. For each edge, find it's most 
# acute-angled neighbor at each end, and reduce the edge length
# to not interefere (plus shorten an additional length on each 
# end as an additional fitting tolerance.)


def print_edge_reductions(edges)
  edges.values.each{|e| puts e.reduced_length_s}
end

def print_shortened_endpoints_csv(edges)
  edges.values.each{|e| puts e.new_endpoints_csv_row}
end


vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
Edge.graph = graph


print_edge_reductions(edges)

# print_shortened_endpoints_csv(edges)

# pp edges["79-97"].endpoint_reductions
# puts edges["79-97"].reduced_length_s


