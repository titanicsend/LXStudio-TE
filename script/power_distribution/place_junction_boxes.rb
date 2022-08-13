require 'csv'
require 'pp'
require 'set'

require './vertex'
require './edge'
require './graph'
require './panel'
require './junction_box'
require './calculate_line_lengths'
require './controller'
require './outlet_bank'

def print_boxes(junction_boxes)
  groups = junction_boxes.values.group_by { |j| j.vertex.id }
  groups.each do |vertex, boxes|
    next if boxes.empty?
    puts "combined at vertex #{vertex} - #{boxes.sum(&:current)} A - #{100 * (boxes.sum(&:utilization) / boxes.size).truncate(4)}% utilized"
    boxes.each do |box|
      puts "  -- box #{box.id}, #{100 * box.utilization.truncate(4)}% utilized, needs ethernet switch? #{box.needs_ethernet_switch? ? 'yes' : 'no'}"
    end
  end

  boxes = junction_boxes.values.flatten
  puts junction_boxes.keys.length
  puts "#{boxes.size} total junction boxes"
  puts "#{boxes.sum(&:current)} Amps"
  puts "#{boxes.sum(&:utilization) / boxes.count} average utilization"
  puts "#{boxes.select { |box| box.needs_ethernet_switch? }.count} boxes need an ethernet switch"
end

vertices = Vertex.load_vertices('../../resources/vehicle/vertexes.txt')
edges = Edge.load_edges('../../resources/vehicle/edges.txt', vertices)
Edge.load_signal_paths(filename: '../../resources/vehicle/edge_signal_paths.tsv', edges: edges, vertices: vertices)
panels = Panel.load_panels('../../resources/vehicle/panels.txt', vertices)
graph = Graph.new(edges: edges, vertices: vertices, panels: panels)
controllers = Controller.load_controllers(edge_signal_filename: '../../resources/vehicle/edge_signal_paths.tsv', panel_signal_filename: '../../resources/vehicle/panel_signal_paths.tsv', graph: graph, vertices: vertices)
boxes = JunctionBox.load_assignments(filename: '../../resources/vehicle/power_assignments.tsv', edges: edges, panels: panels, vertices: vertices)

Controller.assign_controllers_to_boxes(graph: graph, controllers: controllers, junction_boxes: boxes)
print_boxes(boxes)

boxes.values.each do |box|
  box.controllers.each do |controller|
    puts "#{controller.id},#{box.id}"
  end
end

total_controllers = 0
controllers.each do |_, assigned_controllers_at_vertex|
  total_controllers += assigned_controllers_at_vertex.length
end

puts "Total assigned channels, from the vertices:"
total_channels_from_vertices = 0
vertices.each do |_, vertex|
  total_channels_from_vertex = vertex.controllers.map(&:channels_assigned).sum
  if total_channels_from_vertex == 0
    next
  end
  puts "  - vertex #{vertex.id} has #{vertex.controllers.map(&:id)}, #{total_channels_from_vertex} total assigned channel(s)"
  total_channels_from_vertices += total_channels_from_vertex
end
puts "  total assigned channels: #total_channels_from_vertices}"

puts "Total assigned channels, from the controllers:"
total_channels_from_controllers = 0
# This sort_by is ugly, but it just sorts by ascending vertex ID.
controllers.sort_by { |_, c| c.first.id.split('-')[0].to_i }.each do |_, controller_group|
  controller_group.each do |controller|
    puts "  - controller #{controller.id} has #{controller.channels_assigned} assigned channels: #{controller.edges.count} edge(s) (#{controller.edges.map(&:id).join(', ')}), #{controller.panels.count} panel(s) (#{controller.panels.map(&:id).join(', ')})"
    if controller.channels_assigned <= 4
      puts "  - --- Good candidate for 4-channel controller!"
    end
    total_channels_from_controllers += controller.channels_assigned
  end
end
puts "  total assigned channels: #{total_channels_from_controllers}"

if total_channels_from_vertices != total_channels_from_controllers
  raise "expected total number of vertices to be the same!"
end

puts "Total assigned controllers: #{total_controllers}"

edge_to_box = {}
boxes.values.each do |box|
  box.circuits.each do |circuit|
    circuit.edge_strips.each do |strip|
      edge_to_box[strip.edge_id] ||= Set.new
      edge_to_box[strip.edge_id].add(box.id)
    end
  end
end

graph.panels.each_value do |panel|
  panel.strips.each do |strip|
    if strip.circuit.nil?
      puts "#{strip.id} unassigned"
    end
  end
end

graph.edges.each_value do |edge|
  edge.strips.each do |strip|
    if strip.circuit.nil?
      puts "#{strip.id} unassigned"
    end
  end
end

puts "Power boxes responsible for this panel: (by panel ID -- box vertex ID(s))"
graph.panels.values.flatten.sort_by(&:priority).each do |panel|
  puts "#{panel.id} -- #{panel.assigned_junction_box_vertices.map(&:id).join(', ')}"
end

outlet_banks = OutletBank.load_outlet_banks(vertices: vertices)
#OutletBank.assign_junction_boxes_to_outlet_banks(graph: graph, outlet_banks: outlet_banks, junction_boxes: boxes)

power_cable_lengths = bucket_cable_lengths(power_cable_lengths(boxes: boxes.values.flatten, graph: graph))
power_cable_lengths.delete_if { |_, v| v == 0 }
puts "5V Power cable lengths:"
pp power_cable_lengths
puts "---------"

total_power_length = power_cable_lengths.sum { |k, v| k * v }
puts "Total 5V power cable lengths:"
pp total_power_length
puts "---------"

ethernet_cable_lengths = bucket_cable_lengths(ethernet_cable_lengths(boxes: boxes.values.flatten, graph: graph))
ethernet_cable_lengths.delete_if { |_, v| v == 0 }
puts "Ethernet cable lengths:"
pp ethernet_cable_lengths
puts "---------"

total_ethernet_length = ethernet_cable_lengths.sum { |k, v| k * v }
puts "Total ethernet cable lengths:"
pp total_ethernet_length
puts "---------"

# ac_power_cable_lengths = bucket_cable_lengths(ac_power_cable_lengths(boxes: boxes.values.flatten, graph: graph))
# ac_power_cable_lengths.delete_if { |_, v| v == 0 }
# puts "AC power cable lengths:"
# pp ac_power_cable_lengths
# puts "---------"

# Uncomment me, run the script, copy-paste, split to columns to regenerate `panel_signal_paths_scratch` (first 4 columns)
# graph.panels.each_value do |panel|
#  puts "#{panel.id},#{panel.controller_vertex.id},#{panel.signal_start_vertex_id},#{panel.valid_signal_in_vertices.map(&:id).join('-')}"
# end
