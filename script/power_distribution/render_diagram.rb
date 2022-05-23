require 'victor'

class RenderDiagram
  WIDTH = 2400
  HEIGHT = 1200
  PADDING = 50

  VERTEX_RADIUS = 12

  def initialize(graph:, junction_boxes:, output:)
    @graph = graph
    @junction_boxes = junction_boxes
    @graph_bounds = calculate_graph_bounds
    @output = output
  end

  def render
    svg = Victor::SVG.new width: WIDTH, height: HEIGHT

    render_vertices(svg)
    render_edges(svg)
    render_vertex_connections(svg)

    @junction_boxes.each do |vertex_id, boxes|
      vertex = @graph.vertices[vertex_id.to_i]
      x, y = project(vertex)

      if x > WIDTH / 2
        x_offset = 20
      else
        x_offset = -30
      end

      svg.text(
        boxes.length,
        x: x + x_offset,
        y: y + 20,
        font_family: 'arial',
        font_weight: 'bold',
        font_size: 24,
        fill: 'red',
      )
    end

    svg.save(@output)
  end

  private

  def render_vertices(svg)
    @graph.vertices.values.select { |v| v.x <= 0 }.each do |vertex|
      x, y = project(vertex)
      svg.circle(
        cx: x,
        cy: y,
        r: VERTEX_RADIUS,
        fill: 'white',
        style: { stroke: 'black', stroke_width: 1 },
      )

      offset = vertex.id.to_s.length * 3
      svg.text(
        vertex.id,
        x: x - offset,
        y: y + 6,
        font_family: 'arial',
        font_weight: 'bold',
        font_size: 10,
        fill: 'black',
      )
    end
  end

  def calculate_vertex_connections
    vertex_connections = {}

    @junction_boxes.each do |vertex_id, boxes|
      vertex = @graph.vertices[vertex_id.to_i]
      vertex_connections[vertex_id.to_i] ||= {}
      boxes.each do |box|
        box.circuits.each do |circuit|
          edges = circuit.edge_strips.map(&:edge)
          circuit.edge_strips.each do |strip|
            next if edges.include?(strip.edge.signal_from)

            vertex_connections[vertex_id][strip.edge.signal_in_vertex.id] ||= 0
            vertex_connections[vertex_id][strip.edge.signal_in_vertex.id] += 1
          end
        end
      end
    end

    vertex_connections
  end

  def render_edges(svg)
    edges = @graph.edges.values.select do |edge|
      edge.vertices.all? { |v|  v.x <= 0 }
    end
    edges.each do |edge|
      p1, p2 = line_coordinates(*edge.vertices)
      svg.line(
        x1: p1[0],
        y1: p1[1],
        x2: p2[0],
        y2: p2[1],
        style: { stroke: 'black', stroke_width: 2 },
      )
    end
  end

  def render_vertex_connections(svg)
    vertex_connections = calculate_vertex_connections
    vertex_connections.each do |v_id, others|
      others.each do |w_id, count|
        v = @graph.vertices[v_id]
        w = @graph.vertices[w_id]

        p1, p2 = line_coordinates(v, w)
        cx1 = p1[0]
        cy1 = p1[1] - 50
        cx2 = p2[0]
        cy2 = p2[1] - 50

        svg.path(
          d: "M #{p1[0]} #{p1[1]} C #{cx1} #{cy1}, #{cx2} #{cy2}, #{p2[0]} #{p2[1]}",
          style: { stroke: 'blue', stroke_dasharray: '10,10', stroke_width: 2, fill_opacity: '0' },
        )
      end
    end
  end

  def line_coordinates(v1, v2)
    x1, y1 = project(v1)
    x2, y2 = project(v2)

    t = VERTEX_RADIUS / distance([x1, y1], [x2, y2])
    x1_p = (1 - t) * x1 + t * x2
    y1_p = (1 - t) * y1 + t * y2
    x2_p = (1 - t) * x2 + t * x1
    y2_p = (1 - t) * y2 + t * y1

    [[x1_p, y1_p], [x2_p, y2_p]]
  end

  def distance(p1, p2)
    Math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)
  end

  def calculate_graph_bounds
    bounds = {
      x: [Float::INFINITY, -Float::INFINITY],
      y: [Float::INFINITY, -Float::INFINITY],
      z: [Float::INFINITY, -Float::INFINITY],
    }
    @graph.vertices.each_value do |vertex|
      [:x, :y, :z].each do |axis|
        if vertex.send(axis) < bounds[axis][0]
          bounds[axis][0] = vertex.send(axis)
        end

        if vertex.send(axis) > bounds[axis][1]
          bounds[axis][1] = vertex.send(axis)
        end
      end
    end

    bounds
  end

  # In our 3D coordinate system, the Y axis is vertical and the Z axis is
  # horizontal from the front to the back of the car.
  def project(vertex)
    normalized_y = (vertex.y - @graph_bounds[:y][0]).to_f / (@graph_bounds[:y][1] - @graph_bounds[:y][0])
    normalized_z = (@graph_bounds[:z][1] - vertex.z).to_f / (@graph_bounds[:z][1] - @graph_bounds[:z][0])

    result = [
      normalized_z * (WIDTH - (PADDING * 2)) + PADDING,
      (HEIGHT - PADDING) - (normalized_y * (HEIGHT - (PADDING * 2))),
    ]
    result
  end
end
