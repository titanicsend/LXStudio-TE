require './calculate_line_lengths'
require 'csv'
require './constants'

class Edge
  # 60 / m LED strips
  LEDS_PER_MICRON = 0.00006

  STRIPS_PER_EDGE = 3

  def initialize(id:, vertices:)
    @id = id
    @vertices = vertices
    @signal_to = nil
    @signal_from = nil
    @build_priority = nil
    @strips = Array.new(STRIPS_PER_EDGE) do |i|
      EdgeStrip.new(id: "#{id}-#{i}", vertices: vertices, edge: self)
    end
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

  def self.load_edges(filename, vertices)
    rows = CSV.read(filename, col_sep: "\t")
    edges = {}
    rows.each do |row|
      vs = row[0].split('-').map { |v| vertices[v.to_i] }
      edges[row[0]] = Edge.new(id: row[0], vertices: vs)
    end
    edges
  end

  def self.load_signal_paths(filename:, edges:, vertices:)
    rows = CSV.read(filename, col_sep: "\t")

    # FYI: this should be `rows.length - 1`, but `edges.txt` has one more edge than
    # `edge_signal_paths.tsv`. I think? this is due to that weird "skip me" edge
    if rows.length != edges.length
      raise "mismatch between signal path edge number and edges.txt edge number"
    end

    rows.drop(1).each do |row|
      id, signal_from, controller_vertex, priority = row
      edge = edges[id]
      edge.build_priority = priority
      if signal_from == 'Controller'
        edge.signal_from = vertices[controller_vertex.to_i]
      else
        prev_edge = edges[signal_from]
        edge.signal_from = prev_edge
        prev_edge.signal_to = edge
      end
    end
  end

  def assigned?
    strips.none? { |strip| strip.circuit.nil? }
  end

  def signal_in_vertex
    if signal_from.is_a?(Vertex)
      # `signal_from` is actually the vertex of the controller. So, let's find the closest vertex
      # on the edge to the controller.
      vertices.min_by { |v| straight_line_distance({ :x => v.x, :y => v.y, :z => v.z }, signal_from) }
    elsif signal_from.is_a?(Edge)
      (signal_from.vertices & vertices).first
    else
      vertices.first
    end
  end

  def signal_out_vertex
    (vertices - [signal_in_vertex]).first
  end

  def depth
    @depth ||= if signal_from.is_a?(Edge)
                 signal_from.depth + 1
               else
                 0
               end
  end

  attr_accessor :id, :vertices, :signal_to, :signal_from, :strips, :build_priority
end

class EdgeStrip
  def initialize(id:, vertices:, edge:)
    @id = id
    @vertices = vertices
    @edge = edge
    @circuit = nil
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

  def edge_id
    edge.id
  end

  attr_accessor :id, :vertices, :edge, :circuit
end

