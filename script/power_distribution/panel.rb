require 'csv'
require './constants.rb'
require './junction_box.rb'

class Panel
  LEDS_PER_SQUARE_MICRON = 462.0 / (1_000_000**2)

  def initialize(id:, vertices:)
    @id = id
    @vertices = vertices
    @controller_vertex = nil
    @signal_start_vertex
    @strips = Array.new((max_current / SCALED_MAX_CURRENT_PER_CIRCUIT).floor) do |i|
      PanelStrip.new(
        id: "#{id}-#{i}",
        panel: self,
        current: SCALED_MAX_CURRENT_PER_CIRCUIT,
        vertices: vertices,
      )
    end
    @strips << PanelStrip.new(
      id: "#{id}-#{@strips.length}",
      panel: self,
      current: max_current - @strips.sum(&:current),
      vertices: vertices,
    )
    @panel_type = PANEL_TYPE_LIT
    @channels_required = 1
    @priority = nil
  end

  attr_accessor :id, :vertices, :strips, :panel_type, :channels_required, :controller_vertex, :signal_start_vertex, :priority

  def area
    side_lengths = vertices.combination(2).map do |v1, v2|
      v1.distance(v2)
    end
    s = side_lengths.sum / 2
    Math.sqrt(s * (s - side_lengths[0]) * (s - side_lengths[1]) * (s - side_lengths[2]))
  end

  def num_leds
    @num_leds ||= area * LEDS_PER_SQUARE_MICRON
  end

  def max_current
    @max_current ||= num_leds * MAX_CURRENT_PER_LED
  end

  def centroid
    v1 = vertices[0]
    v2 = vertices[1]
    v3 = vertices[2]

    centroid_x = (v1.x + v2.x + v3.x) / 3
    centroid_y = (v1.y + v2.y + v3.y) / 3
    centroid_z = (v1.z + v2.z + v3.z) / 3

    {
      :x => centroid_x,
      :y => centroid_y,
      :z => centroid_z,
    }
  end

  # The long edge vertices SHOULD be where signal comes in. This is not 100% of the time true. (see SED)
  def valid_signal_in_vertices
    long_edge_vertices = vertices.permutation(2)
      .map{ |v1,v2| [v1, v2, v1.distance(v2)] }
      .sort_by(&:last).last.slice(0,2)
  end

  def self.load_panels(filename, vertices)
    rows = CSV.read(filename, col_sep: "\t")
    panels = {}
    rows.each do |row|
      vs = [row[1], row[2], row[3]].map do |edge|
        edge.split('-').map(&:to_i)
      end
      vs = vs.flatten.uniq.map { |v| vertices[v] }
      panels[row[0]] = Panel.new(id: row[0], vertices: vs)
    end
    panels
  end

  # assigned_junction_box_vertices are just the vertices of the box(es) assigned to
  # power this panel. They are not necessarily the same as the vertices of the panel;
  # wouldn't that be nice?
  def assigned_junction_box_vertices
    if strips.any? { |strip| strip.circuit.nil? || strip.circuit.junction_box.nil? }
      raise "strip not yet assigned"
    end

    junction_box_vertex_ids_set = Set.new
    strips.each do |strip|
      circuit = strip.circuit
      if junction_box_vertex_ids_set.include?(circuit.junction_box.vertex.id)
        next
      end

      junction_box_vertex_ids_set.add(circuit.junction_box.vertex.id)
    end

    junction_box_vertex_ids_set.to_a
  end
end

class PanelStrip
  def initialize(id:, panel:, vertices:, current:)
    @id = id
    @panel = panel
    @current = current
    @vertices = vertices
    @circuit = nil
  end

  def panel_id
    panel.id
  end

  attr_accessor :vertices, :current, :panel, :id, :circuit
end

