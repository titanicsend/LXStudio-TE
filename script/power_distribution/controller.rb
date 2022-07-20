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
    @panels = []
    # Note: only covers the "injection" edge of the signal run, not any edges chained from there.
    @edges = []
  end

  def decorated_id
    "C#{@id}"
  end

  def channels_assigned
    edges.length + panels.sum(&:channels_required)
  end

  def self.load_controllers(edge_signal_filename:, panel_signal_filename:, graph:, vertices:)
    controllers = {}
    populate_panel_controllers(filename: panel_signal_filename, controllers: controllers, graph: graph, vertices: vertices)
    populate_edge_controllers(filename: edge_signal_filename, controllers: controllers, graph: graph, vertices: vertices)

    controllers
  end

  def self.assign_controllers_to_boxes(graph:, controllers:, junction_boxes:)
    controllers.each do |controller_vertex_id, vertex_controllers|
      vertex_controllers.each do |controller|
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
              next
            end
          end
        end
        nearest_eligible_box.assign_controller(controller)
      end
    end
  end

  def assign_signal_to_edge(edge:)
    if channels_assigned >= MAX_CHANNELS_PER_CONTROLLER
      raise 'assigned too many signal runs already'
    end
    self.edges.push(edge)
  end


  def assign_signal_to_panel(panel:)
    if channels_assigned >= MAX_CHANNELS_PER_CONTROLLER || channels_assigned + panel.channels_required > MAX_CHANNELS_PER_CONTROLLER
      raise 'assigned too many signal runs already'
    end
    self.panels.push(panel)
  end

  attr_accessor :edges, :panels, :junction_box, :vertex, :id

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

  def self.assign_new_controller_at_vertex(vertex:, edge:, panel:, controllers:)
      # Controllers are identified with `vertex-number_at_vertex`. e.g. the second controller
      # at vertex 100 will be 100-1.
      controller = Controller.new(vertex: vertex)
      if edge != nil
        controller.edges.push(edge)
      end
      if panel != nil
        controller.panels.push(panel)
      end

      if controllers[vertex.id] != nil
        controllers[vertex.id].push(controller)
      else
        controllers[vertex.id] = [controller]
      end
      vertex.controllers.push(controller)
      controller
  end

  def self.populate_edge_controllers(filename:, controllers:, graph:, vertices:)
    rows = CSV.read(filename, col_sep: "\t")

    rows.drop(1).each do |row|
      edge_id, signal_from, controller_vertex_id = row

      if signal_from != 'Controller'
        next
      end

      edge = graph.edges.values.flatten.find { |edge| edge.id == edge_id }
      controller_vertex = vertices.find { |vertex_id, vertex| vertex_id.to_s == controller_vertex_id }[1]

      assigned_controller = nil
      if controllers[controller_vertex.id] != nil
        # Left to right for exhausting signal channels. But, we might have non-exhausted controllers due to the needs
        # of assigning panels.
        least_addressed_controller = controllers[controller_vertex.id].min_by(&:channels_assigned)

        if least_addressed_controller.channels_assigned >= MAX_CHANNELS_PER_CONTROLLER
          assigned_controller = assign_new_controller_at_vertex(vertex: controller_vertex, edge: edge, panel: nil, controllers: controllers)
        else
          least_addressed_controller.assign_signal_to_edge(edge: edge)
          assigned_controller = least_addressed_controller
        end
      else
        assigned_controller = assign_new_controller_at_vertex(vertex: controller_vertex, edge: edge, panel: nil, controllers: controllers)
      end

      validate_controller_distance_to_first_pixel_edge(controller: assigned_controller, edge: edge, graph: graph)
    end
    controllers
  end

  def self.populate_panel_controllers(filename:, controllers:, graph:, vertices:)
    rows = CSV.read(filename, col_sep: "\t")

    rows.drop(1).each do |row|
      panel_id, _, _, _, panel_type, channels_required, controller_vertex_id = row

      panel = graph.panels.values.flatten.find { |panel| panel.id == panel_id }
      panel.panel_type = panel_type
      panel.channels_required = channels_required.to_i

      controller_vertex = vertices.find { |vertex_id, vertex| vertex_id.to_s == controller_vertex_id }[1]

      assigned_controller = nil
      if controllers[controller_vertex.id] != nil
        # Left to right for exhausting signal channels. But, we might have non-exhausted controllers due to the needs
        # of assigning panels.
        least_addressed_controller = controllers[controller_vertex.id].min_by(&:channels_assigned)

        if least_addressed_controller.channels_assigned < MAX_CHANNELS_PER_CONTROLLER && least_addressed_controller.channels_assigned + panel.channels_required <= MAX_CHANNELS_PER_CONTROLLER
          least_addressed_controller.assign_signal_to_panel(panel: panel)
          assigned_controller = least_addressed_controller
        else
          assigned_controller = assign_new_controller_at_vertex(vertex: controller_vertex, edge: nil, panel: panel, controllers: controllers)
        end
      else
        assigned_controller = assign_new_controller_at_vertex(vertex: controller_vertex, edge: nil, panel: panel, controllers: controllers)
      end

      validate_controller_distance_to_first_pixel_panel(controller: assigned_controller, panel: panel)
    end
    controllers
  end
end

def validate_controller_distance_to_first_pixel_edge(controller:, edge:, graph:)
  min_distance_from_controller_to_signal_injection_feet = min_distance_between_vertices_in_feet(graph, controller.vertex.id, edge.signal_in_vertex.id)

  if min_distance_from_controller_to_signal_injection_feet > MAX_CONTROLLER_DISTANCE_SIGNAL_TO_FIRST_PIXEL_FEET
    raise"error: edge #{edge.id} is too far from controller at #{controller.vertex}"
  end
end

def validate_controller_distance_to_first_pixel_panel(controller:, panel:)
  min_distance_from_controller_to_signal_injection_feet = straight_line_distance(panel.centroid, controller.vertex)

  if min_distance_from_controller_to_signal_injection_feet > MAX_CONTROLLER_DISTANCE_SIGNAL_TO_FIRST_PIXEL_FEET
    raise "error: panel #{panel.id} is too far from controller at #{controller.vertex}"
  end
end