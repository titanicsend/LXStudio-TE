class JunctionBox
  @@vertex_counter = {}

  def initialize(vertex:)
    @vertex = vertex
    @id = calculate_id
    @circuits = (0..15).map { |i| JunctionBoxCircuit.new(id: "#{id}-#{i}", junction_box_id: id) }
  end

  def calculate_id
    counter = @@vertex_counter[vertex.id]
    if counter.nil?
      @@vertex_counter[vertex.id] = 0
      counter = 0
    end
    id = "#{vertex.id}-#{counter}"
    @@vertex_counter[vertex.id] += 1

    id
  end

  def current
    circuits.sum(&:current)
  end

  def utilization
    circuits.sum(&:utilization) / circuits.size
  end

  def dup
    b = JunctionBox.new(vertex: @vertex)
    b.circuits = circuits.map(&:dup)
    b
  end

  def can_fit_edge?(edge)
    current_per_strip = edge.strips.first.max_current
    slots = circuits.sum do |circuit|
      ((JunctionBoxCircuit::MAX_CURRENT - circuit.current) / current_per_strip).floor
    end

    slots >= edge.strips.length
  end

  attr_accessor :circuits, :vertex, :id
end

class JunctionBoxCircuit
  MAX_CURRENT = 15
  def initialize(id:, junction_box_id:)
    @id = id
    @junction_box_id = junction_box_id
    @panel_strips = []
    @edge_strips = []
  end

  attr_accessor :panel_strips, :edge_strips, :id, :junction_box_id

  def dup
    c = JunctionBoxCircuit.new(id: id, junction_box_id: junction_box_id)
    c.panel_strips = panel_strips.dup
    c.edge_strips = edge_strips.dup
    c
  end

  def current
    panel_strips.sum(&:current) + edge_strips.sum(&:max_current)
  end

  def utilization
    current / MAX_CURRENT
  end
end
