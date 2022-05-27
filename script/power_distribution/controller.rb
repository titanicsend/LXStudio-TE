require 'csv'
require './calculate_line_lengths'
require './constants'
require './junction_box'

class Controller
  @@vertex_counter = {}

  def initialize(vertex:)
    @vertex = vertex
    @id = calculate_id
    @junction_box = nil
  end

  def decorated_id
    "C#{@id}"
  end

  def self.load_controllers(filename:, vertices:)
    rows = CSV.read(filename, col_sep: "\t")

    controllers = {}
    rows.drop(1).each do |row|
      _, signal_from, controller_vertex_id = row

      if signal_from != 'Controller'
        next
      end

      controller_vertex = vertices.find { |vertex_id, vertex| vertex_id.to_s == controller_vertex_id }[1]

      # Controllers are identified with `vertex-number_at_vertex`. e.g. the second controller
      # at vertex 100 will be 100-1.
      controller = Controller.new(vertex: controller_vertex)
      controllers[controller_vertex_id] = controller
    end
    controllers
  end

  # TODO: consider balancing to avoid uneven utilizations
  def self.assign_controllers_to_boxes(graph:, controllers:, junction_boxes:)
    num_assigned = 0
    controllers.each do |controller_vertex_id, controller|
      shortest_eligible_distance_to_box_from_controller = 999999
      nearest_eligible_box = nil

      junction_boxes.each do |_, boxes|
        # Multiple boxes may be at each vertex depending upon nearby power needs.
        boxes.each do |box|
          if box.controllers.length == MAX_CONTROLLERS_PER_JUNCTION_BOX
            next
          end

          min_distance = min_distance_between_vertices_in_feet(graph, controller.vertex.id, box.vertex.id)
          if min_distance < shortest_eligible_distance_to_box_from_controller
            shortest_eligible_distance_to_box_from_controller = min_distance
            nearest_eligible_box = box

            # No need to calculate for other boxes; they're at the same distance.
            # TODO: probably a good place to do a round-robin distribution among all boxes at the same vertex.
            next
          end
        end
      end

      nearest_eligible_box.assign_controller(controller)
      num_assigned += 1
    end
    if num_assigned != controllers.length
      raise "did not assign all controllers!"
    end
  end

  attr_accessor :junction_box, :vertex, :id

  private

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
end

