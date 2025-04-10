require 'matrix'

require './edge.rb'
require './graph.rb'
require './panel.rb'
require './vertex.rb'

def microns_to_feet(distance_in_microns)
  distance_in_microns / 304_800
end

def min_distance_between_vertices_in_feet(graph, vertex1, vertex2)
  distance = graph.min_distance(vertex1, vertex2)
  microns_to_feet(distance)
end

# straight_line_distance calculates the straight line distance between two points.
# These points may be vertices, but they're not required to be.
def straight_line_distance(point1, point2)
  vector1 = Vector[point1[:x], point1[:y], point1[:z]]
  vector2 = Vector[point2.x, point2.y, point2.z]
  microns_to_feet((vector1 - vector2).magnitude)
  end

def power_cable_lengths(boxes:, graph:)
  cable_lengths = []
  num_gender_changers = 0
  num_y_connectors = 0

  n = boxes.map(&:circuits).flatten.select { |c| c.edge_strips.length > 1 }.length
  puts "number of circuits: #{n}"

  boxes.each do |box|
    box.circuits.each do |circuit|
      edges = circuit.edge_strips.map(&:edge)
      chained = 0
      circuit.edge_strips.each do |strip|
        if edges.include?(strip.edge.signal_from)
          chained += 1
          next
        end

        vertex = strip.edge.vertices.min_by do |v|
          min_distance_between_vertices_in_feet(graph, box.vertex.id, v.id)
        end

        length = min_distance_between_vertices_in_feet(graph, box.vertex.id, vertex.id)
        if vertex.id != strip.edge.signal_in_vertex.id
          num_gender_changers += 1
        end

        cable_lengths << length
      end

      num_y_connectors += [0, circuit.edge_strips.length - (1 + chained)].max

      circuit.panel_strips.each_with_index do |strip, i|
        next if circuit.panel_strips[0...i].any? { |other| other.panel == strip.panel }

        cable_lengths << straight_line_distance(strip.panel.centroid, box.vertex)
      end
    end
  end

  puts "Number of gender change adaptors required: #{num_gender_changers}"
  puts "Number of Y connectors required: #{num_y_connectors}"

  cable_lengths
end

def ethernet_cable_lengths(graph:, boxes:)
  cable_lengths = []

  boxes.each do |box|
    box.controllers.each do |controller|
      length = min_distance_between_vertices_in_feet(graph, box.vertex.id, controller.vertex.id)
      cable_lengths << length
    end
  end

  cable_lengths
end

def ac_power_cable_lengths(graph:, boxes:)
  cable_lengths = []

  boxes.each do |box|
    length = min_distance_between_vertices_in_feet(graph, box.vertex.id, box.outlet_bank.vertex.id)
    length += AC_POWER_WIGGLE_FEET

    cable_lengths << length
  end

  cable_lengths
end

def bucket_cable_lengths(cable_lengths)
  buckets = [5, 10, 15, 20, 25, 30, 35, 40, 45, 50]

  bucketed_cables = buckets.map { |i| [i, 0] }.to_h

  cable_lengths.each do |cable|
    bucket = buckets.find { |b| cable <= b }
    bucketed_cables[bucket] += 1
  end

  bucketed_cables
end
