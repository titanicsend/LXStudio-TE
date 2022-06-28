require 'csv'

require './calculate_line_lengths'
require './junction_box'

# OutletBank is a bank of 110V AC power outlets coming from the generator.
# Each pair of outlets is on a shared 15A fuse.
class OutletBank
  NUMBER_OF_CIRCUITS = 10

  def initialize(vertex:)
    @vertex = vertex
    @id = vertex.id

    @circuits = (0..NUMBER_OF_CIRCUITS).map do |i|
      OutletBankCircuit.new(id: "C#{self.id}-#{i}", outlet_bank: self)
    end
  end

  def decorated_id
    "O#{@id}"
  end

  def has_any_available_circuits?
    circuits.any? { |c| c.outlets.none? { |outlet| outlet.assigned? } }
  end

  def assign_junction_box_to_open_circuit(junction_box:)
    if !has_any_available_circuits?
      raise "no available circuits; logic error"
    end

    circuits.select { |c| c.outlets.none? { |outlet| outlet.assigned? } }.first.outlets.first.assign_junction_box(junction_box: junction_box)
  end

  def self.load_outlet_banks(vertices:)
    bank = {}
    AC_POWER_OUTLET_BANK_VERTICES.map do |v|
      outlet_bank_vertex = vertices.find { |vertex_id, vertex| vertex_id == v }[1]

      bank[v] = OutletBank.new(vertex: outlet_bank_vertex)
    end
    bank
  end

  def self.assign_junction_boxes_to_outlet_banks(graph:, outlet_banks:, junction_boxes:)
    junction_boxes.each do |_, boxes|
      # Multiple boxes may be at each vertex depending upon nearby power needs.
      boxes.each do |box|
        shortest_eligible_distance_to_box_from_outlet_bank = 999999
        nearest_eligible_outlet_bank = nil

        outlet_banks.each do |_, outlet_bank|
          min_distance = min_distance_between_vertices_in_feet(graph, outlet_bank.vertex.id, box.vertex.id)

          if min_distance < shortest_eligible_distance_to_box_from_outlet_bank && outlet_bank.has_any_available_circuits?
            shortest_eligible_distance_to_box_from_outlet_bank = min_distance
            nearest_eligible_outlet_bank = outlet_bank
          end
        end

        if nearest_eligible_outlet_bank.nil?
          raise "no eligible outlet banks; logic error"
        end

        nearest_eligible_outlet_bank.assign_junction_box_to_open_circuit(junction_box: box)
      end
    end
  end
  
  # TODO: reflect when we need an extension cord too
  def cable_distance_to_junction_box()
  end

  attr_accessor :id, :vertex, :circuits

  private

end

class OutletBankCircuit
  MAX_CURRENT_AMPS = 15
  NUMBER_OF_OUTLETS = 2

  def initialize(id:, outlet_bank:)
    @id = id
    @outlet_bank = outlet_bank

    @outlets = (0..NUMBER_OF_OUTLETS).map do |i|
      Outlet.new(id: "OU#{id}-#{i}", outlet_bank_circuit: self)
    end
  end

  attr_accessor :id, :outlet_bank, :outlets

  def outlet_bank_id
    outlet_bank.id
  end
end

class Outlet
  def initialize(id:, outlet_bank_circuit:)
    @id = id
    @outlet_bank_circuit = outlet_bank_circuit
    @junction_box = nil
  end

  def assigned?
    junction_box != nil
  end

  def assign_junction_box(junction_box:)
    if assigned? || self.outlet_bank_circuit.outlets.any? { |outlet| outlet.assigned? }
      raise 'circuit/outlet already assigned'
    end

    self.junction_box = junction_box
    junction_box.outlet_bank = self.outlet_bank_circuit.outlet_bank
  end

  attr_accessor :id, :outlet_bank_circuit, :junction_box
end