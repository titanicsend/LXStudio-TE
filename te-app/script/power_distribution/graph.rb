class Graph
  def initialize(edges:, vertices:, panels:)
    @edges = edges
    @vertices = vertices
    @panels = panels
    @adjacency = make_adjacency_list
  end

  attr_accessor :adjacency, :edges, :vertices, :panels

  def make_adjacency_list
    adjacency = {}
    edges.each_value do |edge|
      edge.vertices.permutation.each do |vertices|
        adjacency[vertices[0].id] ||= []
        adjacency[vertices[0].id] << vertices[1].id
      end
    end
    adjacency
  end

  def min_distance(vertex1, vertex2)
    distance_from_vertex1 = {vertex1 => 0}
    distance_from_vertex1.default = Float::INFINITY
    queue = [vertex1]

    until queue.empty?
      vertex = queue.shift
      return distance_from_vertex1[vertex] if vertex == vertex2

      adjacency[vertex].each do |neighbor|
        edge_length = edges[[vertex, neighbor].sort.join('-')].length
        distance = distance_from_vertex1[vertex] + edge_length
        next if distance >= distance_from_vertex1[neighbor]

        distance_from_vertex1[neighbor] = distance
        unless queue.include?(neighbor)
          insert_index = queue.bsearch_index do |pos|
            distance_from_vertex1[pos] > distance_from_vertex1[neighbor]
          end
          if insert_index.nil?
            queue.push(neighbor)
          else
            queue.insert(insert_index, neighbor)
          end
        end
      end
    end

    Float::INFINITY
  end
end
