#! /usr/bin/env python

# This script imports the TE v2022-3 config and generates Chromatik fixture files
# TODO: Generate a .LXM model file containing every fixture

import copy
import math
import os
import re
import sys
import textwrap

# Convert measurements to inches
UNIT_SCALE = 25400
# This needs to match panelRow.lxf
PANEL_POINT_SPACING = 1.9685039
EDGE_POINT_SPACING = 0.6562


class Vertex:
    def __init__(self, x, y, z, id=-1):
        self.x = x
        self.y = y
        self.z = z
        self.id = id

    def apply_axis_swap(self):
        # Swap X and Z to align show side of car with default Chromatik camera angle
        x_orig = self.x
        self.x = 0 - self.z
        self.z = x_orig

    def apply_scale(self):
        # Scale to new units to play nice with default Chromatik camera distance and pixel size
        self.x /= UNIT_SCALE
        self.y /= UNIT_SCALE
        self.z /= UNIT_SCALE

    def set_values(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z

    def copy_from(self, vertex_from):
        self.x = vertex_from.x
        self.y = vertex_from.y
        self.z = vertex_from.z

    def dist(self, v):
        return math.sqrt((self.x - v.x) ** 2 +
                         (self.y - v.y) ** 2 +
                         (self.z - v.z) ** 2)

    def __str__(self):
        if self.id is not None:
            return f'Vertex {self.id} (x={self.x} y={self.y} z={self.z})'
        else:
            return f'Vertex(x={self.x} y={self.y} z={self.z})'


class Output:
    def __init__(self, host, controller_output_number):
        self.host = host
        self.controller_output_number = controller_output_number

    def __str__(self):
        return self.host + " #" + str(self.controller_output_number)


class Edge:
    def __init__(self, edge_id, reverse, num_points, output_full):
        self.id = edge_id
        self.tag = "edge" + edge_id.replace('-', '_')
        self.fixture_name = "Edge" + edge_id
        parts = edge_id.split("-")
        self.v0name = parts[0]
        self.v1name = parts[1]
        self.v0 = vertexes[self.v0name]
        self.v1 = vertexes[self.v1name]
        self.yaw = 0
        self.roll = 0
        self.x_offset = 0
        self.reverse = reverse == "reversed"
        self.num_points = int(num_points)
        # Parse output
        parts = output_full.split("#")
        self.host = parts[0]
        parts = parts[1].split(":")
        self.controller_channel = parts[0]
        # Optional pixel offset
        self.led_offset = int(parts[1]) if len(parts) > 1 else 0

    def calc_transforms(self):
        # This is a trimmed down copy of the Panel.calc_transforms() method

        # Using the two vertices, calculate the transforms for locating the edge on the car.
        # These values will be written to the transform[] section of the LXF.
        # Think of these transforms as moving/pivoting the origin to bring it in line with the edge.

        # Track the locations of the two vertices relative to the last move
        prev = [copy.deepcopy(self.v0),
                copy.deepcopy(self.v1)]

        # Transpose to first vertex
        current = [Vertex(0, 0, 0),
                   Vertex(prev[1].x - prev[0].x, prev[1].y - prev[0].y, prev[1].z - prev[0].z)]

        # Use negative Z; Z-axis points away in LX
        prev[1].z = 0 - prev[1].z

        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])

        # 1. Yaw
        yaw = math.atan2(prev[1].z, prev[1].x)
        self.yaw = 0 - math.degrees(yaw)

        # calc new locations...
        current[1].x = (prev[1].x * math.cos(yaw)) + (prev[1].z * math.sin(yaw))
        current[1].z = 0 - (prev[1].x * math.sin(yaw)) + (prev[1].z * math.cos(yaw))
        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])

        # 2. Roll
        roll = math.atan2(0 - prev[1].y, prev[1].x)
        self.roll = 0 - math.degrees(roll)

        # calc new locations...
        current[1].x = (prev[1].x * math.cos(roll)) - (prev[1].y * math.sin(roll))
        current[1].y = (prev[1].x * math.sin(roll)) + (prev[1].y * math.cos(roll))
        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])

        # Default (X)-offset
        length_v01 = self.v1.dist(self.v0)
        length_edge = (self.num_points - 1) * EDGE_POINT_SPACING
        # Center the edge between the two vertices
        self.x_offset = (length_v01 - length_edge) / 2


class Panel:
    def __init__(self, panel_id, num_points, edge1, edge2, edge3, v01names, flipped, output_str):
        self.id = panel_id
        self.stripe = None
        self.num_points = int(num_points)
        self.edge1 = edge1
        self.edge2 = edge2
        self.edge3 = edge3
        self.leading_edge = v01names
        arrow = v01names.index('->')
        self.v0name = v01names[:arrow]
        self.v1name = v01names[arrow+2:]
        self.v2name = None
        self.v0 = None
        self.v1 = None
        self.v2 = None
        self.yaw = 0.0
        self.roll = 0.0
        self.pitch = 0.0
        self.x_offset = 0
        self.y_offset = 0
        self.flipped = flipped
        self.output_str = output_str

        # Parse output string into array of Output objects
        self.outputs = []
        num_outputs = math.ceil(self.num_points / 250)
        # Split on /
        o_entries = output_str.split('/')
        o_entries_current = 0
        o = 0
        while o < num_outputs:
            # Split on #: Host before, Controller channel numbers after
            o_parts = o_entries[o_entries_current].split('#')
            host = o_parts[0]
            # Extract min/max channel
            o_parts = o_parts[1].split('-')
            ch_min = int(o_parts[0])
            # Default max channel is 4 if not specified
            ch_max = int(o_parts[1]) if len(o_parts) > 1 else 4
            for ch in range(ch_min, ch_max+1):
                self.outputs.append(Output(host, ch))
                o += 1
                if o == num_outputs:
                    break
            o_entries_current += 1

    def find_vertices(self):
        # Pull vertices from dictionary by id
        self.v0 = vertexes[self.v0name]
        self.v1 = vertexes[self.v1name]
        # Look for the vertex that wasn't in the leading edge. hacky...
        known_vertices = [self.v0, self.v1]
        other_vertices = [
            edges[self.edge1].v0,
            edges[self.edge1].v1,
            edges[self.edge2].v0,
            edges[self.edge2].v1,
            edges[self.edge3].v0,
            edges[self.edge3].v1]

        for v in other_vertices:
            if v not in known_vertices:
                self.v2 = v
                self.v2name = v.id
                break

    def calc_transforms(self):
        # Using the three vertices, calculate the transforms for locating the panel on the car.
        # These values will be written to the transform[] section of the LXF.
        # Think of these transforms as moving/pivoting the origin to bring it in plane with the panel.

        # Fun fact: the Excel formula atan2(x,y) in Python is atan2(y,x)

        # Track the locations of the three vertices relative to the last move
        prev = [copy.deepcopy(self.v0),
                copy.deepcopy(self.v1),
                copy.deepcopy(self.v2)]

        # Transpose to first vertex
        current = [Vertex(0, 0, 0),
                   Vertex(prev[1].x - prev[0].x, prev[1].y - prev[0].y, prev[1].z - prev[0].z),
                   Vertex(prev[2].x - prev[0].x, prev[2].y - prev[0].y, prev[2].z - prev[0].z)]

        # Use negative Z; Z-axis points away in LX
        prev[1].z = 0 - prev[1].z
        prev[2].z = 0 - prev[2].z

        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])
        prev[2].copy_from(current[2])

        # 1. Yaw
        yaw = math.atan2(prev[1].z, prev[1].x)
        self.yaw = 0 - math.degrees(yaw)

        # calc new locations...
        current[1].x = (prev[1].x * math.cos(yaw)) + (prev[1].z * math.sin(yaw))
        current[1].z = 0 - (prev[1].x * math.sin(yaw)) + (prev[1].z * math.cos(yaw))
        current[2].x = (prev[2].x * math.cos(yaw)) + (prev[2].z * math.sin(yaw))
        current[2].z = 0 - (prev[2].x * math.sin(yaw)) + (prev[2].z * math.cos(yaw))
        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])
        prev[2].copy_from(current[2])

        # 2. Roll
        roll = math.atan2(0 - prev[1].y, prev[1].x)
        self.roll = 0 - math.degrees(roll)

        # calc new locations...
        current[1].x = (prev[1].x * math.cos(roll)) - (prev[1].y * math.sin(roll))
        current[1].y = (prev[1].x * math.sin(roll)) + (prev[1].y * math.cos(roll))
        current[2].x = (prev[2].x * math.cos(roll)) - (prev[2].y * math.sin(roll))
        current[2].y = (prev[2].x * math.sin(roll)) + (prev[2].y * math.cos(roll))
        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])
        prev[2].copy_from(current[2])

        # 3. Pitch
        pitch = math.atan2(0 - prev[2].z, prev[2].y)
        self.pitch = 0 - math.degrees(pitch)

        current[2].y = (prev[2].y * math.cos(pitch)) - (prev[2].z * math.sin(pitch))
        current[2].z = (prev[2].y * math.sin(pitch)) + (prev[2].z * math.cos(pitch))
        prev[0].copy_from(current[0])
        prev[1].copy_from(current[1])
        prev[2].copy_from(current[2])

        # Default X-offset
        # This would be better with trig.  Either way it will need fine-tuning by hand.
        length_v01 = math.sqrt((self.v1.x - self.v0.x) ** 2 +
                               (self.v1.y - self.v0.y) ** 2 +
                               (self.v1.z - self.v0.z) ** 2)
        row0 = self.stripe.rows[0]
        length_row0 = (row0.num_points + row0.offset - 1) * PANEL_POINT_SPACING
        # Center the first row against leading edge
        self.x_offset = (length_v01 - length_row0) / 2

        # Default Y-offset
        self.y_offset = 6.0

    def __str__(self):
        return f'{self.id} {self.leading_edge} V0: {self.v0}, V1: {self.v1}, V2: {self.v2}'
        # return f'Panel ID: {self.id}, Edge1: {self.edge1}, Edge2: {self.edge2}, Edge3: {self.edge3}, Leading Edge: {self.leading_edge}, V0: {self.v0}, V1: {self.v1}, V2: {self.v2}, Flipped: {self.flipped}, OutputStr: {self.output_str}'


class Row:
    def __init__(self, index, offset, num_points, gaps):
        self.index = index
        self.offset = offset
        self.num_points = num_points
        self.gaps = gaps


class Stripe:
    def __init__(self, id, row_length, strand_lengths, side, code):
        self.id = id
        self.row_length = row_length
        self.strand_lengths = strand_lengths
        self.side = side
        self.code = code
        # Decode the rows
        self.rows = []
        # Cumulative "extra" offset.  Doesn't include standard 1 per row (matches assumption in panelRow.lxf)
        offset = 0
        # Mildly redundant tracking of row index
        row_index = 0
        row_gaps = 0
        # Split on spaces
        for c in code.split():
            if "g" in c:
                # Gap pixel count
                # I see at least 1 instance of two g's with a space between them (SBB).  Accommodate.
                row_gaps += c.count("g")
            else:
                # Row w/ Nudges
                nudges = c.split(".")
                nudges_left = nudges[0].count("+") - nudges[0].count("-")
                nudges_right = nudges[1].count("+") - nudges[1].count("-")
                # We're only tracking the "extra" offset
                offset -= nudges_left if side == "L" else nudges_right
                row_length += nudges_left + nudges_right
                # Create a new Row
                self.rows.append(Row(row_index, offset, row_length, row_gaps))
                row_index += 1
                # Reset gap count for the next row
                row_gaps = 0
                # Next row is one less pixel by default
                row_length -= 1

    def get_orientation(self, from_back):
        if self.side == "L":
            return "LEFT" if from_back else "RIGHT"
        elif self.side == "R":
            return "RIGHT" if from_back else "LEFT"
        else:
            return "UNKNOWN"

    def __str__(self):
        print(f"Stripe ID: {id}")#, Row Length: {str(self.row_length)}, Strand Lengths: {self.strand_lengths}, Side: {self.side}, Code: {self.code}")


class Module:
    def __init__(self, id, definition):
        self.id = id
        self.fixture_name = "Module" + id.zfill(2)
        self.tag = "m" + id
        self.definition = definition
        # Edge chains from modules.txt
        self.chains = {}
        # Dictionaries of components within this module
        self.vertexes = {}
        self.edges = {}
        self.panels = {}

    def find_components(self):
        # Parse the definition
        # Split on space
        chains_str = self.definition.split()
        for chain_str in chains_str:
            chain = chain_str.split("-")
            vertex_prev = chain[0]
            for vertex_str in chain:
                # Keep a list of unique vertexes
                if vertex_str not in self.vertexes:
                    vertex = vertexes[vertex_str]
                    self.vertexes[vertex_str] = vertex
                # After the first vertex, look for edge
                if vertex_prev != vertex_str:
                    for edge_id, edge in edges.items():
                        if (edge.v0name == vertex_prev and edge.v1name == vertex_str) or (edge.v0name == vertex_str and edge.v1name == vertex_prev):
                            self.edges[edge_id] = edge
                            break

        # Find panels where two of the edges are within this module
        # TODO: Is this capturing all the panels?
        for panel_id, panel in panels.items():
            num_edges = 0
            if self.edges.get(panel.edge1) != None:
                num_edges += 1
            if self.edges.get(panel.edge2) != None:
                num_edges += 1
            if self.edges.get(panel.edge3) != None:
                num_edges += 1
            if num_edges >= 2:
                self.panels[panel_id] = panel


def load_vertexes(vertexes):
    file_path_vertexes = "../../resources/vehicle/vertexes.txt"
    try:
        with open(file_path_vertexes, "r") as file:
            for line in file:
                parts = line.strip().split("\t")

                # Check if there are at least 4 entries in the line
                if len(parts) >= 4:
                    vertex_id = parts[0]
                    vertex = Vertex(float(parts[1]), float(parts[2]), float(parts[3]), vertex_id)
                    vertex.apply_axis_swap()
                    vertex.apply_scale()
                    vertexes[vertex_id] = vertex

    # Handle file not found
    except FileNotFoundError:
        print(f"File '{file_path_vertexes}' not found.")


def load_edges(edges):
    file_path_edges = "../../resources/vehicle/edges.txt"
    try:
        with open(file_path_edges, "r") as file:
            for line in file:
                parts = line.strip().split("\t")

                # Check if there are enough entries in the line to create an Edge object
                if len(parts) >= 4:
                    edge_id = parts[0]
                    reverse = parts[1]
                    num_points = parts[2]
                    output_full = parts[3]

                    # Create an Edge object and add it to the dictionary
                    edge = Edge(edge_id, reverse, num_points, output_full)
                    edge.calc_transforms()
                    edges[edge_id] = edge
                else:
                    print(f"Invalid number of columns in edge: {line}")

    # Handle file not found
    except FileNotFoundError:
        print(f"File '{file_path_edges}' not found.")


def load_panels(panels):
    file_path_panels = "../../resources/vehicle/panels.txt"
    try:
        with open(file_path_panels, "r") as file:
            for line in file:
                parts = line.strip().split("\t")

                # Check if there are enough entries in the line to create a Panels object
                if len(parts) >= 8:
                    panel_id = parts[0]
                    num_points = parts[1]
                    edge1 = parts[2]
                    edge2 = parts[3]
                    edge3 = parts[4]
                    v0 = parts[5]
                    flipped = parts[6]
                    output_str = parts[7]

                    # Create a Panel object and add it to the dictionary
                    panel = Panel(panel_id, num_points, edge1, edge2, edge3, v0, flipped, output_str)
                    panel.find_vertices()
                    panel.stripe = stripes[panel.id]
                    panel.calc_transforms()
                    panels[panel_id] = panel
                else:
                    print(f"Invalid number of columns in panel: {line}")

    # Handle file not found
    except FileNotFoundError:
        print(f"File '{file_path_panels}' not found.")


def load_stripes(stripes):
    file_path_stripes = "../../resources/vehicle/striping-instructions.txt"
    try:
        with open(file_path_stripes, "r") as file:
            for line in file:
                # Remove parentheses-contained comments and trailing space, nod
                line = re.sub(r'\(.*?\)\s*', '', line)
                # Remove trailing newline
                line = re.sub(r'\n', '', line)
                # Panel ID
                space = line.find(" ")
                id = line[:space]
                line = line[space+1:]
                # First row length
                space = line.find(" ")
                row_length = int(line[:space])
                line = line[space+1:]
                # Custom strand lengths?
                strand_lengths = []
                if line.startswith("C"):
                    space = line.find(" ")
                    strand_lengths_str = line[1:space].split(",")
                    strand_lengths = [int(value) for value in strand_lengths_str]
                    line = line[space+1:]
                # R or L
                side = line[0]
                # Row codes
                code = line[2:]
                # Save to dict
                stripe = Stripe(id, row_length, strand_lengths, side, code)
                stripes[id] = stripe

    # Handle file not found
    except FileNotFoundError:
        print(f"File '{file_path_stripes}' not found.")


def load_modules(modules):
    file_path_modules = "../../resources/vehicle/modules.txt"
    try:
        with open(file_path_modules, "r") as file:
            for line in file:
                parts = line.strip().split(":")

                # Check if there are enough entries in the line to create a Module
                if len(parts) == 2:
                    module_id = parts[0]
                    definition = parts[1].strip()

                    # Create a Module object and add it to the dictionary
                    module = Module(module_id, definition)
                    module.find_components()
                    modules[module_id] = module
                else:
                    print(f"Invalid number of columns in module: {line}")

        # Handle file not found
    except FileNotFoundError:
        print(f"File '{file_path_modules}' not found.")


def create_directories():
    if not os.path.exists("../../Fixtures"):
        os.makedirs("../../Fixtures")
    if not os.path.exists("../../Fixtures/TE"):
        os.makedirs("../../Fixtures/TE")
    if not os.path.exists("../../Models"):
        os.makedirs("../../Models")


def create_panels():
    # Create "panel" folder if it does not exist
    if not os.path.exists("../../Fixtures/TE/panel"):
        os.makedirs("../../Fixtures/TE/panel")

    # Loop over each panel and create a new file
    for panel_id, panel in panels.items():
        filename = f"../../Fixtures/TE/panel/{panel_id}.lxf"
        with (open(filename, "w") as panel_file):
            panel_file.write('''
{
  /* Titanic's End Fixture File */
  label: "''' + panel_id + '''",
  tags: [ "''' + panel_id + '''", "panel" ],

  parameters: {
    "xOffset": { default: ''' + str(panel.x_offset) + ''', type: float, description: "Adjust X position within the plane of the panel. Use to fine-tune position after installation" },
    "yOffset": { default: ''' + str(panel.y_offset) + ''', type: float, description: "Adjust Y position within the plane of the panel. Use to fine-tune position after installation" },
  
    /* Modify HOST and CONTROLLER OUTPUT default values HERE to assign one of this panel's strands to a
       new controller output. Then click [Reload Fixture File] at the bottom of the Inspector section in Chromatik.
       CAUTION: LXM model files containing this fixture might not update to the new defaults. */
''')
            # Output parameters. Matches the number of physical strands on this panel.
            output_num = 0
            for output in panel.outputs:
                 output_num += 1
                 panel_file.write('''
    "strand''' + str(output_num) + '''host": { default: "''' + output.host + '''", type: "string", label: "S''' + str(output_num) + ''' Host", description: "Strand ''' + str(output_num) + ''' Controller IP address or hostname" },
    "strand''' + str(output_num) + '''output": { default: ''' + str(output.controller_output_number) + ''', type: "int", min: 1, max: 4, label: "S''' + str(output_num) + ''' Output", description: "Strand ''' + str(output_num) + ''' Controller Output Number 1-4" },''')

            # Continue with parameters section...
            panel_file.write('''

    "artnetSequence": { default: false, type: "boolean", label: "ArtNet Sequence", description: "Enable ArtNet sequence packets" },
    
    # Debug tools
    "onCar": { default: true, type: "boolean", label: "On Car", description: "True = Locate panel to its position on the car, False = Locate to origin" }
  },
  
  transforms: [
    /* Transpose starting corner to Vertex0 */
    { x: "''' + str_coord(panel.v0.x) + '''", enabled: "$onCar" },
    { y: "''' + str_coord(panel.v0.y) + '''", enabled: "$onCar" },
    { z: "''' + str_coord(panel.v0.z) + '''", enabled: "$onCar" },

    /* Rotate to plane of panel */
    { yaw: "''' + str_coord(panel.yaw) + '''", enabled: "$onCar" },
    { roll: "''' + str_coord(panel.roll) + '''", enabled: "$onCar" },
    { pitch: "''' + str_coord(panel.pitch) + '''", enabled: "$onCar" },

    /* X-Y adjustments within plane of panel */
    { x: "$xOffset" },
    { y: "$yOffset" }    
  ],

  meta: {
    "edge1": "''' + panel.edge1 + '''",
    "edge2": "''' + panel.edge2 + '''",
    "edge3": "''' + panel.edge3 + '''",
    "leadingEdge": "''' + panel.leading_edge + '''"
  },

  components: [
    /* Looking at the back of panel, starts from ''' + panel.stripe.get_orientation(True) + '''
       Looking at the front of panel, starts from ''' + panel.stripe.get_orientation(False) + ''' */
''')

            # Panel rows!  Each row is a component.
            for row in panel.stripe.rows:
                panel_file.write('''
    { type: "panelRow", row: "''' + str(row.index) + '''", offset: "''' + str(row.offset) + '''", numPoints: "''' + str(row.num_points) + '''" },''')

            # Continue with components section...
            panel_file.write('''

    /* Temporary method of handling gap pixels, will be removed soon */
    { type: "point", id: "gap" }
  ],

  outputs: [''')

            # Outputs section
            output_index = 0
            row_index = 0
            next_row_start = 0
            reverse = False
            for output in panel.outputs:
                panel_file.write('''
                  
    { host: "$strand''' + str(output_index + 1) + '''host", universe: "$strand''' + str(output_index + 1) + '''output*10", protocol: "artnet", sequenceEnabled: "$artnetSequence",
      segments: [''')

                # Is this output a custom strand length?
                strand_length = 250
                if len(panel.stripe.strand_lengths) > output_index:
                    strand_length = panel.stripe.strand_lengths[output_index]

                # Track number of pixels in this output.  Break to new output on exact strand length.
                remaining_length = strand_length

                # Add segments for this output
                while remaining_length > 0:
                    # print(f"{panel_id}  output:{output_index}  row:{row_index}  remaining:{remaining_length}")
                    row = panel.stripe.rows[row_index]

                    start_str = ""
                    length_str = ""
                    length = row.num_points - next_row_start

                    # Continuing a row from the previous output?
                    if next_row_start > 0:
                        start_str = ", start: " + str(next_row_start)
                        next_row_start = 0
                    else:
                        # Check for gap pixels
                        remaining_length -= row.gaps
                        # Temporary gap pixel method
                        for i in range(row.gaps):
                            panel_file.write('''
        { componentId: "gap" },''')

                    # If row doesn't fit in current output, split it.
                    if length > remaining_length:
                        next_row_start = remaining_length
                        length = remaining_length
                        length_str = ", length: " + str(length)

                    # Track remaining length in this output (TE spec 250 pixels/output except for hardware odd ones)
                    remaining_length -= length

                    # Create an output segment for this row
                    panel_file.write('''
        { componentIndex: ''' + str(row_index) + ''', reverse: ''' + str(reverse).lower() + length_str + start_str + ''' },''')

                    # If row was not split across outputs, go to next row
                    if next_row_start == 0:
                        row_index += 1
                        # Zig-zag, but only for new rows
                        reverse = not reverse

                    if row_index >= len(panel.stripe.rows):
                        # All finished, no more rows in the panel
                        break
                        #remaining_length = 0

                # Next output!
                output_index += 1

                # Close segments and output
                panel_file.write('''
      ]
    }''')
                # If it's not the last output, write a comma
                if output_index < len(panel.outputs):
                    panel_file.write(",")

            # Finished with the outputs section
            panel_file.write('''
  ]
}
''')


def str_coord(coordinate):
    return "{:.4f}".format(coordinate)


def create_edges():
    # Create "edge" folder if it does not exist
    if not os.path.exists("../../Fixtures/TE/edge"):
        os.makedirs("../../Fixtures/TE/edge")

    # Loop over each edge and create a new file
    for edge_id, edge in edges.items():
        filename = f"../../Fixtures/TE/edge/{edge.fixture_name}.lxf"
        with (open(filename, "w") as edge_file):
            edge_file.write('''
{
  /* Titanic's End Fixture File */
  label: "Edge ''' + edge.id + '''",
  tags: [ "''' + edge.tag + '''", "edge" ],

  parameters: {
    "points": { default: ''' + str(edge.num_points) + ''', type: "int", min: 1, label: "Points", description: "Number of points in the edge" },
    "host": { default: "''' + edge.host + '''", type: "string", label: "Host", description: "Controller IP address or hostname" },    
    "output": { default: ''' + str(edge.controller_channel) + ''', type: "int", min: 1, max: 4, label: "Output", description: "Controller output 1-4" },
    "ledOffset": { default: ''' + str(edge.led_offset) + ''', type: "int", min: 0, label: "LED Offset", description: "0-based starting position, in pixels, of this edge within the controller output" },
    "artnetSequence": { default: false, type: "boolean", label: "ArtNet Sequence", description: "Whether ArtNet sequence packets are enabled" },
    "reverse": { default: ''' + str(edge.reverse).lower() + ''', type: "boolean", label: "Reverse", description: "Reverse the output direction" },
    
    "xOffset": { default: ''' + str(edge.x_offset) + ''', type: "float", label: "X offset", description: "Location offset of edge from starting vertex" },
  
    # Debug tools
    "onCar": { default: true, type: "boolean", label: "On Car", description: "True = Locate panel to its position on the car, False = Locate to origin" },

    # Option to disable output, such as when outputs are configured in a parent fixture
    "hasOutput": { default: true, type: "boolean", label: "Has Output", description: "Whether built-in output is enabled" }
  },
  
  transforms: [
    /* Transpose starting corner to Vertex0 */
    { x: "''' + str_coord(edge.v0.x) + '''", enabled: "$onCar" },
    { y: "''' + str_coord(edge.v0.y) + '''", enabled: "$onCar" },
    { z: "''' + str_coord(edge.v0.z) + '''", enabled: "$onCar" },

    /* Rotate to in line with Vertex0 and Vertex1 */
    { yaw: "''' + str_coord(edge.yaw) + '''", enabled: "$onCar" },
    { roll: "''' + str_coord(edge.roll) + '''", enabled: "$onCar" },

    /* X offset from starting vertex */
    { x: "$xOffset" }    
  ],

  meta: {
    "edgeId": "''' + edge.id + '''",
    "v0": "''' + edge.v0name + '''",
    "v1": "''' + edge.v1name + '''"
  },

  components: [
    { type: "strip", 
      numPoints: "$points",
      spacing: "''' + str(EDGE_POINT_SPACING) + '''",
      reverse: "$reverse"
    }
  ],
  
  outputs: [
    { enabled: "$hasOutput",
      host: "$host",
      universe: "$output*10+(($ledOffset*3)/512)",
      channel: "($ledOffset*3)%512",
      protocol: "artnet", 
      sequenceEnabled: "$artnetSequence"
    }
  ]
}
''')


def create_modules():
    # Create "module" folder if it does not exist
    if not os.path.exists("../../Fixtures/TE/module"):
        os.makedirs("../../Fixtures/TE/module")

    # Loop over each module and create a new file
    for module_id, module in modules.items():
        filename = f"../../Fixtures/TE/module/{module.fixture_name}.lxf"
        with (open(filename, "w") as module_file):
            module_file.write('''{
  /* Titanic's End Fixture File */
  label: "''' + module.fixture_name + '''",
  tags: [ "''' + module.tag + '''", "module" ],

  parameters: {
    "placeholder": { default: "''' + module.id + '''", type: "string", description: "Placeholder parameter, does nothing" }''')

            # Pass-through parameters for edges and panels
            # JKB note: It might be better not to list these out in the module fixture, as the defaults
            # will get out of sync with the defaults in the child fixture files.  For the case of module fixtures
            # which is expected to be "for emergencies" use,
            # it might be better to always be inheriting the defaults from the edge/panel files.
#            for edge_id, edge in module.edges.items():
#                module_file.write(''',
#    "e''' + edge.id + '''host": { default: "''' + edge.host + '''", type: "string", description: "Edge ''' + edge.id + ''' ip or hostname" }''')

            # Finish parameters section
            module_file.write('''
  },

  meta: {
    "moduleId": "''' + module.id + '''",
    "definition": "''' + module.definition + '''"
  },

  components: [
    /* Edges */''')

            is_first = True
            for edge_id, edge in module.edges.items():
                if is_first:
                    is_first = False
                else:
                    module_file.write(''',''')
                module_file.write('''
    { type: "TE/edge/''' + edge.fixture_name + '''" }''')

            # Comma between edges and panels
            if len(module.edges) > 0 and len(module.panels) > 0:
                module_file.write(''',''')

            module_file.write('''

    /* Panels */''')

            is_first = True
            for panel_id, panel in module.panels.items():
                if is_first:
                    is_first = False
                else:
                    module_file.write(''',''')
                module_file.write('''
    { type: "TE/panel/''' + panel.id + '''" }''')

            module_file.write('''
  ]
}
''')


def create_top_level_fixture():
    filename = f"../../Fixtures/TE/TE.lxf"
    with (open(filename, "w") as te_file):
        te_file.write('''{
  /* Titanic's End Fixture File */
  label: "TE",
  tags: [ "te", "car" ],
  
  parameters: {
    "artnetSequence": { default: false, type: "boolean", label: "ArtNet Sequence", description: "Whether ArtNet sequence packets are enabled" }
  },
  
  components: [
    /* Edges */''')

        # Add each edge as a fixture
        for edge_id, edge in edges.items():
            te_file.write('''
    { type: "edge/''' + edge.fixture_name + '''", artnetSequence: "artnetSequence" },''')

        te_file.write('''

    /* Panels */''')

        # Add each panel as a fixture
        for panel_id, panel in panels.items():
            te_file.write('''
    { type: "panel/''' + panel_id + '''", artnetSequence: "artnetSequence" },''')

        # Finish components sections and file
        te_file.write('''
  ]
}''')


def create_top_level_model():
    filename = f"../../Models/TE_car.lxm"
    with (open(filename, "w") as te_file):
        te_file.write('''{
  "version": "0.4.2.TE.7-SNAPSHOT",
  "timestamp": 1706055285413,
  "fixtures": [''')

        is_first = True

        # Insert each panel fixture
        for panel_id, panel in panels.items():
            if is_first:
                is_first = False
            else:
                te_file.write(''',''')
            te_file.write('''
    {
      "jsonFixtureType": "TE/panel/''' + panel_id + '''",
      "jsonParameters": {
        "onCar": true
      },
      "class": "heronarts.lx.structure.JsonFixture",
      "internal": {
        "modulationColor": 0,
        "modulationControlsExpanded": true,
        "modulationsExpanded": true
      },
      "parameters": {
        "label": "''' + panel_id + '''",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0,
        "yaw": 0.0,
        "pitch": 0.0,
        "roll": 0.0,
        "scale": 1.0,
        "selected": false,
        "deactivate": false,
        "enabled": true,
        "brightness": 1.0,
        "identify": false,
        "mute": false,
        "solo": false,
        "tags": "",
        "fixtureType": "TE/panel/''' + panel_id + '''"
      },
      "children": {}
    }''')

        # Insert each edge fixture
        for edge_id, edge in edges.items():
            if is_first:
                is_first = False
            else:
                te_file.write(''',''')
            te_file.write('''
    {
      "jsonFixtureType": "TE/edge/''' + edge.fixture_name + '''",
      "jsonParameters": {
        "onCar": true
      },
      "class": "heronarts.lx.structure.JsonFixture",
      "internal": {
        "modulationColor": 0,
        "modulationControlsExpanded": true,
        "modulationsExpanded": true
      },
      "parameters": {
        "label": "Edge ''' + edge_id + '''",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0,
        "yaw": 0.0,
        "pitch": 0.0,
        "roll": 0.0,
        "scale": 1.0,
        "selected": false,
        "deactivate": false,
        "enabled": true,
        "brightness": 1.0,
        "identify": false,
        "mute": false,
        "solo": false,
        "tags": "",
        "fixtureType": "TE/edge/''' + edge.fixture_name + '''"
      },
      "children": {}
    }''')

        # Finish fixtures section and file
        te_file.write('''
  ]
}''')


# Create empty dictionaries for vertexes, edges, panels, and stripes
vertexes = {}
edges = {}
stripes = {}
panels = {}
modules = {}

# Load data from old config files into dictionaries
load_vertexes(vertexes)
load_edges(edges)
load_stripes(stripes)
load_panels(panels)
load_modules(modules)

# Write new json config files
# Fixture and Model directories
create_directories()
# LXF Fixture Files
create_panels()
create_edges()
create_modules()
create_top_level_fixture()
# LXM Model Files
create_top_level_model()
