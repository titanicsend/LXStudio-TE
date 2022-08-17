require 'matrix'

require './calculate_line_lengths.rb'
require './edge.rb'
require './graph.rb'
require './junction_box.rb'
require './panel.rb'
require './vertex.rb'

# Add 10% to length for safety
LINE_LENGTH_FUDGE_FACTOR = 1.10

vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)

def controller_power_cable_lengths(graph:, junction_box_ids:, controller_ids:)
  cable_lengths = []

  junction_box_ids.each_with_index do |junction_box_id, index|
    # Order matters!
    controller_id = controller_ids[index]

    # These IDs contain a second element after the `-` that indicates the sequence number.
    # For these purposes, we don't care about that part.
    controller_vertex_id = controller_id.split('-')[0].to_i
    junction_box_vertex_id = junction_box_id.split('-')[0].to_i

    is_edge_case = false
    if junction_box_vertex_id == '36.5'
      is_edge_case = true
      junction_box_vertex_id = '36'
    end

    length = min_distance_between_vertices_in_feet(graph, junction_box_vertex_id, controller_vertex_id)
    if length == 0
      # Colocated at the same vertex; just make it a foot, and we'll round up to 2 feet later.
      length = 1
    end

    # Add 3 feet to account for weird half-vertex scheme.
    if is_edge_case
      length += 3
    end

    puts "The distance between vertex #{junction_box_vertex_id} and vertex #{controller_vertex_id} is #{length} feet"
    adjusted_length = length * LINE_LENGTH_FUDGE_FACTOR

    cable_lengths << adjusted_length
  end

  cable_lengths
end

rows = CSV.read('../../resources/vehicle/read-only-controller-to-power-box-assignment.tsv', col_sep: "\t")

controller_ids = []
junction_box_ids = []

# Drop header row.
rows.drop(1).each do |row|
  controller_id, junction_box_id, _ = row
  controller_ids.push(controller_id)
  junction_box_ids.push(junction_box_id)
end

# Should be 1:1. And because I'm lazy, ordering matters.
if controller_ids.length != junction_box_ids.length
  raise "mismatch between controller_ids and junction_box_ids"
end

controller_power_cable_lengths = controller_power_cable_lengths(graph: graph, junction_box_ids: junction_box_ids, controller_ids: controller_ids)
bucketed_controller_power_cables = bucket_cable_lengths(controller_power_cable_lengths)

bucketed_controller_power_cables.delete_if { |_, v| v == 0 }
puts "5V Controller Power from Junction Box length:"
pp bucketed_controller_power_cables
puts "---------"