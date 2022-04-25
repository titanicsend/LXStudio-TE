require 'csv'
require 'matrix'
require './constants.rb'
require './length.rb'
# require 'pry-byebug'

class Edge
  # 60 / m LED strips
  LEDS_PER_MICRON = 0.00006
  STRIPS_PER_EDGE = 3
  FIXTURE_WIDTH = 2.5 * Length::MICRONS_PER_IN # Width of the fixture channel 
  EXTRA_SHORTEN_BY = 1 * Length::MICRONS_PER_IN # Per side, in addition to angle-derived shortening

  @@edges = {}
  @@graph = nil

  def self.graph=(graph)
    @@graph = graph
  end

  def initialize(id:, vertices:)
    @id = id
    @vertices = vertices
    @signal_to = nil
    @signal_from = nil
    @strips = Array.new(STRIPS_PER_EDGE) { |i| EdgeStrip.new(id: "#{id}-#{i}",vertices: vertices, edge_id: id) }

    @@edges[id] = self
  end

  def length
    @length ||= vertices[1].distance(vertices[0])
  end

  def num_leds
    @num_leds ||= strips.sum(&:num_leds)
  end

  def max_current
    @max_current ||= strips.sum(&:max_current)
  end


  # For one of the two endpoints (vertex 0 or 1), return an edge hash
  # of all edges that share this specified vertex.
  def adjoining_edges(vertex_idx:)
    raise ArgumentError.new("Vertex index must be 0 or 1") unless [0,1].include? vertex_idx
    raise "Adjacency graph missing" if @@graph.nil?
    vertex = vertices[vertex_idx]
    adjacencies = @@graph.adjacency[vertex.id]
    adjacent_edge_ids = adjacencies.map do |neighbor_vertex_id|
      [vertex.id, neighbor_vertex_id].sort.join("-")
    end
    adj_edges = @@edges.select { |edge_id, edge| adjacent_edge_ids.include? edge_id }

    raise "Didn't find self in adjacencies" unless adj_edges.delete(id)
    adj_edges
  end

  # Given a vertex index 0 or 1 (for the two endpoints of the full line)
  # Return a hash of angles to neighbors like {edge.id: angle_in_radians}
  def angles_to_adjoining(vertex_idx:)
    adj_edges = adjoining_edges(vertex_idx: vertex_idx)
    adj_edges.map do |edge_id, edge|
      [edge_id,
      angle_to_adjoining(vertex: vertices[vertex_idx], other_edge: edge)]
    end.to_h
  end

  # Given one of this edge's vertices and a different edge that meets
  # at that vertex, retutn the angle in radians between them
  def angle_to_adjoining(vertex:, other_edge:)
    raise ArgumentError.new("Vertex not in this edge") unless vertices.include? vertex
    raise ArgumentError.new("Edge not adjacent") unless other_edge.id.split("-").map(&:to_i).include? vertex.id
    this_edge_other_vertex = vertices.find { |v| v != vertex }

    that_edge_other_vertex = other_edge.vertices.find { |v| v != vertex }

    vector_this = Vector[this_edge_other_vertex.x - vertex.x,
      this_edge_other_vertex.y - vertex.y,
      this_edge_other_vertex.z - vertex.z
    ]
    vector_that = Vector[that_edge_other_vertex.x - vertex.x,
      that_edge_other_vertex.y - vertex.y,
      that_edge_other_vertex.z - vertex.z
    ]
    vector_this.angle_with(vector_that)
  end

  # A descriptive hash for each vertex showing which connected edge forced
  # the shortening of that end of the edge.
  def endpoint_reductions
    vertices.map do |vertex|
      edge_angles = angles_to_adjoining(vertex_idx: vertices.index(vertex))
      critical_edge = [edge_angles.min_by { |k, v| v }].to_h
      reduction = self.class.shorten_end(critical_edge.first.last)

      [vertex,
        { angles_to_adjoining: edge_angles,
          critical_edge: critical_edge,
          reduction: reduction,
          reduction_s: Length.microns_to_ft_s(reduction)
        }
      ]
    end.to_h
  end

  # Given an angle, shorten this end to allow noninterference
  # and an extra tolerance for assembly error.
  def self.shorten_end(angle)
     FIXTURE_WIDTH / 2 / Math.tan(angle / 2) + EXTRA_SHORTEN_BY
  end

  # Total microns to shorten this edge due to neiboring edges (in microns)
  def length_reduction
    endpoint_reductions.map{ |v, h| h[:reduction] }.sum
  end

  # Length in microns of this edge fixture after shortening
  def reduced_length
    length - length_reduction
  end

  # Human-readable string explaining the length reductions
  def reduced_length_s
    per_end_reduction = endpoint_reductions.map{|v, h| "#{v.id}: #{Length.microns_to_ft_s(h[:reduction])}"}.join(", ")
    
    "#{id} \
    Reduced: #{Length.microns_to_ft_s(length_reduction)} \
    (#{per_end_reduction}) \
    Was: #{Length.microns_to_ft_s(length)} \
    Now: #{Length.microns_to_ft_s(reduced_length)}"
  end

  # [ vertices[0] endpoint inset in microns, 
  #   vertices[1] endpoint inset in microns ]
  def per_end_reduction
    endpoint_reductions.map{|v, h| h[:reduction] }
  end

  # Return a CSV row of id, v0 and v1 length reductions in ft, new v0 (x,y,z), new v1 (x,v,z)
  # This export is brought into the TE2 Modules, Panels, and Edges sheet 
  def new_endpoints_csv_row
    per_end_reduction_ft = per_end_reduction.map{|m| Length.microns_to_ft(m)}

    # New endpoint positions
    v0 = Vector[vertices[0].x, vertices[0].y, vertices[0].z]
    v1 = Vector[vertices[1].x, vertices[1].y, vertices[1].z]
    v0_v1_trim = (v1 - v0) * per_end_reduction[0] / length
    v1_v0_trim = (v0 - v1) * per_end_reduction[1] / length
    new_v0 = v0 + v0_v1_trim
    new_v1 = v1 + v1_v0_trim

    new_coords = [new_v0[0], new_v0[1], new_v0[2], new_v1[0], new_v1[1], new_v1[2]].map(&:to_i)
    
    [id].push(*per_end_reduction_ft).push(*new_coords).join(",")
  end


  def self.load_edges(filename, vertices)
    rows = CSV.read(filename, col_sep: "\t")
    edges = {}
    rows.each do |row|
      vs = row[0].split('-').map { |v| vertices[v.to_i] }
      edges[row[0]] = Edge.new(id: row[0], vertices: vs)
    end
    edges
  end

  def self.load_edge_paths(filename, edges, vertices)
    rows = CSV.read(filename, col_sep: "\t")
    edge_paths = [[]]
    rows.each do |row|
      edge = edges[row[0]]
      next if edge.nil?

      signal_from = row[16]
      if signal_from == 'Controller'
        edge.signal_from = vertices[row[17].to_i]
      else
        prev_edge = edges[row[16]]
        edge.signal_from = prev_edge
        prev_edge.signal_to = edge
      end

      edge_paths.last << edge
      if row[18] == 'Terminates'
        edge_paths << []
      end
    end
    edge_paths
  end

  attr_accessor :id, :vertices, :signal_to, :signal_from, :strips
end

class EdgeStrip
  def initialize(id:, vertices:, edge_id:)
    @id = id
    @vertices = vertices
    @edge_id = edge_id
  end

  def num_leds
    @num_leds ||= length * Edge::LEDS_PER_MICRON
  end

  def max_current
    @max_current ||= num_leds * MAX_CURRENT_PER_LED
  end

  def length
    @length ||= vertices[1].distance(vertices[0])
  end

  attr_accessor :id, :vertices, :edge_id
end

