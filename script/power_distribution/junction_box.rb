class JunctionBox
  @@vertex_counter = {}

  def initialize(vertex:, id: nil)
    @vertex = vertex
    @id = id || calculate_id
    @circuits = (0..15).map do |i|
      JunctionBoxCircuit.new(id: "#{id}-#{i}", junction_box: self)
    end
    @controllers = []
    @outlet_bank = nil
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

  # A box needs an ethernet switch if it is powering any controllers. Controllers should always
  # be powered and signal driven from the same box.
  def needs_ethernet_switch?
    controllers.any?
  end

  def decorated_id
    "J#{@id}"
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
      # TODO: does this need to change to SCALED_MAX_CURRENT_PER_CIRCUIT too?
      ((JunctionBoxCircuit::MAX_CURRENT - circuit.current) / current_per_strip).floor
    end

    slots >= edge.strips.length
  end

  def assign_controller(controller)
    if controllers.length >= MAX_CONTROLLERS_PER_JUNCTION_BOX
      raise 'too many controllers assigned'
    end

    controller.junction_box = self
    controllers.push(controller)
  end

  def self.load_assignments(filename:, panels:, edges:, vertices:)
    junction_boxes = {}
    assignments = CSV.read(filename, col_sep: "\t", headers: true)

    assignments.each do |assignment|
      box_id = assignment['Powerbox ID']
      box = junction_boxes[box_id]
      if box.nil?
        vertex = vertices[box_id.split('-').first.to_i]
        box = JunctionBox.new(vertex: vertex, id: box_id)
        junction_boxes[box_id] = box
      end

      if assignment['Fixture ID'].include?('-')
        type = 'Edge'
      else
        type = 'Panel'
      end

      case type
      when 'Edge'
        edge = edges[assignment['Fixture ID']]
        pp edge.id
        edge.strips.each do |strip|
          circuit = box.circuits.select { |c| c.remaining_current >= strip.max_current }.max_by(&:utilization)
          next if circuit.nil?
          circuit.edge_strips << strip
          strip.circuit = circuit
        end
      when 'Panel'
        panel = panels[assignment['Fixture ID']]
        pp panel.id
        panel.strips.each do |strip|
          circuit = box.circuits.select { |c| c.remaining_current >= strip.current }.max_by(&:utilization)
          next if circuit.nil?
          circuit.panel_strips << strip
          strip.circuit = circuit
        end
      end
    end

    junction_boxes
  end

  attr_accessor :outlet_bank, :controllers, :circuits, :vertex, :id
end

class JunctionBoxCircuit
  MAX_CURRENT = 15
  def initialize(id:, junction_box:)
    @id = id
    @junction_box = junction_box
    @panel_strips = []
    @edge_strips = []
  end

  attr_accessor :panel_strips, :edge_strips, :id, :junction_box

  def dup
    c = JunctionBoxCircuit.new(id: id, junction_box_id: junction_box_id)
    c.panel_strips = panel_strips.dup
    c.edge_strips = edge_strips.dup
    c
  end

  def current
    panel_strips.sum(&:current) + edge_strips.sum(&:max_current)
  end

  def remaining_current
    MAX_CURRENT - current
  end

  def utilization
    current / MAX_CURRENT
  end

  def junction_box_id
    junction_box.id
  end
end
