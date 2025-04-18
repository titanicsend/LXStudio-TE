package titanicsend.model;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.model.LXView;
import heronarts.lx.structure.view.LXViewDefinition;
import heronarts.lx.transform.LXVector;
import heronarts.lx.utils.LXUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxModel.DmxCommonConfig;
import titanicsend.lasercontrol.MovingTarget;
import titanicsend.output.ChromatechSocket;
import titanicsend.output.GrandShlomoStation;
import titanicsend.pattern.jon.ModelBender;
import titanicsend.util.TE;

public class TEWholeModelStatic extends LXModel implements TEWholeModel {

  public String name;
  private final LXPoint gapPoint; // For pixel that shouldn't actually be lit
  private final int[] gapPointIndices; // Always 1 entry for static model

  private final List<TEVertex> mutableVertexes = new ArrayList<TEVertex>();
  public final List<TEVertex> vertexes = Collections.unmodifiableList(this.mutableVertexes);
  public HashMap<Integer, TEVertex> vertexesById;

  private final List<TEEdgeModel> mutableEdges = new ArrayList<TEEdgeModel>();
  public final List<TEEdgeModel> edges = Collections.unmodifiableList(this.mutableEdges);
  public HashMap<String, TEEdgeModel> edgesById;
  public HashMap<LXVector, List<TEEdgeModel>> edgesBySymmetryGroup;

  private final List<TEPanelModel> mutablePanels = new ArrayList<TEPanelModel>();
  public final List<TEPanelModel> panels = Collections.unmodifiableList(this.mutablePanels);
  public HashMap<String, TEPanelModel> panelsById;
  private final HashMap<TEPanelSection, Set<TEPanelModel>> panelsBySection;

  public HashMap<String, TELaserModel> lasersById;
  private final List<TELaserModel> mutableLasers = new ArrayList<TELaserModel>();
  public final List<TELaserModel> lasers = Collections.unmodifiableList(this.mutableLasers);

  public List<LXPoint> edgePoints; // Points belonging to edges
  public List<LXPoint> panelPoints; // Points belonging to panels
  public List<TEBox> boxes;
  public Boundaries boundaryPoints;

  private static final String RESOURCE_NAME_BEACONS = "beacons.txt";
  private static final String RESOURCE_NAME_DJLIGHTS = "djLights.txt";
  private static final String RESOURCE_NAME_VIEWS = "views.txt";

  // Beacons
  private final List<DmxModel> mutableBeacons = new ArrayList<DmxModel>();
  public final List<DmxModel> beacons = Collections.unmodifiableList(this.mutableBeacons);
  private final HashMap<String, DmxModel> beaconsById = new HashMap<String, DmxModel>();

  public List<DmxModel> getBeacons() {
    return this.beacons;
  }

  // DJ Lights
  private final List<DmxModel> mutableDjLights = new ArrayList<DmxModel>();
  public final List<DmxModel> djLights = Collections.unmodifiableList(this.mutableDjLights);
  private final HashMap<String, DmxModel> djLightsById = new HashMap<String, DmxModel>();

  public List<DmxModel> getDjLights() {
    return this.djLights;
  }

  // All DMX models
  private int sizeDmx = 0;
  private int nextDmxIndex = 0;
  private final List<DmxModel> mutableDmxModels = new ArrayList<DmxModel>();
  public final List<DmxModel> dmxModels = Collections.unmodifiableList(this.mutableDmxModels);

  // Boundaries are the points at the boundaries of our 3-dimensional grid. We retain
  // the `LXPoint` for convenience, but only the respective coordinate of each bound
  // point is important.
  public static class Boundaries {
    public final LXPoint minXBoundaryPoint;
    public final LXPoint maxXBoundaryPoint;
    public final LXPoint minYBoundaryPoint;
    public final LXPoint maxYBoundaryPoint;
    public final LXPoint minZBoundaryPoint;
    public final LXPoint maxZBoundaryPoint;

    public Boundaries(
        LXPoint minXBoundaryPoint,
        LXPoint maxXBoundaryPoint,
        LXPoint minYBoundaryPoint,
        LXPoint maxYBoundaryPoint,
        LXPoint minZBoundaryPoint,
        LXPoint maxZBoundaryPoint) {
      this.minXBoundaryPoint = minXBoundaryPoint;
      this.maxXBoundaryPoint = maxXBoundaryPoint;
      this.minYBoundaryPoint = minYBoundaryPoint;
      this.maxYBoundaryPoint = maxYBoundaryPoint;
      this.minZBoundaryPoint = minZBoundaryPoint;
      this.maxZBoundaryPoint = maxZBoundaryPoint;
    }
  }

  private static class Geometry {
    public String name;
    public LXPoint gapPoint;
    public HashMap<Integer, TEVertex> vertexesById;
    public HashMap<String, TEEdgeModel> edgesById;
    public HashMap<String, TEPanelModel> panelsById;
    public HashMap<TEPanelSection, Set<TEPanelModel>> panelsBySection;
    public HashMap<String, List<TEPanelModel>> panelsByFlavor;
    public HashMap<String, TELaserModel> lasersById;
    public List<DmxModel> beacons;
    public List<DmxModel> djLights;
    public List<TEBox> boxes;
    public LXModel[] children;
    public Properties tags;
  }

  public TEWholeModelStatic() {
    this(loadGeometry());
  }

  private TEWholeModelStatic(Geometry geometry) {
    super(geometry.children);
    this.name = geometry.name;
    this.gapPoint = geometry.gapPoint;
    this.gapPointIndices = new int[] {geometry.gapPoint.index};
    this.vertexesById = geometry.vertexesById;
    this.edgePoints = new ArrayList<>();
    this.edgesById = geometry.edgesById;
    this.edgesBySymmetryGroup = new HashMap<>();
    buildEdgeRelations();

    this.panelsById = geometry.panelsById;
    this.panelsBySection = geometry.panelsBySection;

    this.panelPoints = new ArrayList<>();
    // filter gap points from panelPoints list
    int gaps = 0;
    for (TEPanelModel panel : this.panelsById.values()) {
      for (LXPoint pt : panel.points) {
        if (isGapPoint(pt)) {
          gaps++;
        } else {
          this.panelPoints.add(pt);
        }
      }
    }
    if (gaps > 0) {
      TE.log("Kicked out " + gaps + " gap points");
    }

    this.lasersById = geometry.lasersById;

    this.mutableVertexes.addAll(this.vertexesById.values());
    this.mutableEdges.addAll(this.edgesById.values());
    this.mutablePanels.addAll(this.panelsById.values());
    this.mutableLasers.addAll(this.lasersById.values());

    this.boxes = geometry.boxes;

    addBeacons(geometry.beacons);

    addDjLights(geometry.djLights);

    reindexPoints();
    this.boundaryPoints = initializeBoundaries();
    LX.log(String.format("Min X boundary: %f", boundaryPoints.minXBoundaryPoint.x));
    LX.log(String.format("Max X boundary: %f", boundaryPoints.maxXBoundaryPoint.x));

    LX.log(String.format("Min Y boundary: %f", boundaryPoints.minYBoundaryPoint.y));
    LX.log(String.format("Max Y boundary: %f", boundaryPoints.maxYBoundaryPoint.y));

    LX.log(String.format("Min Z boundary: %f", boundaryPoints.minZBoundaryPoint.z));
    LX.log(String.format("Max Z boundary: %f", boundaryPoints.maxZBoundaryPoint.z));

    LX.log(
        this.name
            + " loaded. "
            + this.vertexesById.size()
            + " vertexes, "
            + this.edgesById.size()
            + " edges, "
            + this.panelsById.size()
            + " panels, "
            + this.points.length
            + " pixels, "
            + this.beacons.size()
            + " beacons, "
            + this.djLights.size()
            + " DJ lights");

    if (this.beacons.size() == 0) {
      LX.warning("No active beacons were found in config file.");
    }
    if (this.djLights.size() == 0) {
      LX.warning("No active DJ lights were found in config file");
    }
  }

  /**
   * In Gap Pixel comparisons only the point index should be used, because a LXPoint instance may be
   * a copy of the original gap point.
   */
  public int getGapPointIndex() {
    return this.gapPoint.index;
  }

  public int[] getGapPointIndices() {
    return this.gapPointIndices;
  }

  public boolean isEdgePoint(int index) {
    return index >= edgePoints.get(0).index && index <= edgePoints.get(edgePoints.size() - 1).index;
  }

  public boolean isPanelPoint(int index) {
    return index >= panelPoints.get(0).index
        && index <= panelPoints.get(panelPoints.size() - 1).index;
  }

  public boolean isGapPoint(LXPoint p) {
    return p.index == this.gapPoint.index;
  }

  public List<LXPoint> getPanelPoints() {
    return this.panelPoints;
  }

  public List<LXPoint> getEdgePoints() {
    return this.edgePoints;
  }

  public List<TEVertex> getVertexes() {
    return this.vertexes;
  }

  public LXModel[] getChildren() {
    return this.children;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  /**
   * Builds structures that compute spacial relationships for edges, such as edges that are mirrored
   * fore-aft and port-starboard.
   */
  private void buildEdgeRelations() {
    for (TEEdgeModel edge : this.edgesById.values()) {
      // In decimeters to better group
      int absY = Math.round(Math.abs(edge.model.center.y) / 100_000);
      int absZ = Math.round(Math.abs(edge.model.center.z) / 100_000);
      LXVector symmetryKey = new LXVector(0, absY * 100_000, absZ * 100_000);
      List<TEEdgeModel> symmetryGroup =
          this.edgesBySymmetryGroup.computeIfAbsent(symmetryKey, k -> new ArrayList<>());
      symmetryGroup.add(edge);
      edge.symmetryGroup.clear();
      edge.symmetryGroup.addAll(symmetryGroup);
      this.edgePoints.addAll(Arrays.asList(edge.points));
    }
  }

  private static void loadVertexes(Geometry geometry) {
    geometry.vertexesById = new HashMap<Integer, TEVertex>();
    Scanner s = TE.loadFile("vertexes.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 4 : "Found " + tokens.length + " tokens";
      int id = Integer.parseInt(tokens[0]);
      int x = Integer.parseInt(tokens[1]);
      int y = Integer.parseInt(tokens[2]);
      int z = Integer.parseInt(tokens[3]);
      LXVector vector = new LXVector(x, y, z);
      TEVertex v = new TEVertex(vector, id);
      geometry.vertexesById.put(id, v);
    }
    s.close();
  }

  private static void loadTags(Geometry geometry) {
    geometry.tags = new Properties();
    try (InputStream is = new FileInputStream("resources/vehicle/tags.properties")) {
      geometry.tags.load(is);
    } catch (IOException e) {
      LX.log("Error loading tags: " + e.getMessage());
    }
  }

  private static void loadEdges(Geometry geometry) {
    geometry.edgesById = new HashMap<String, TEEdgeModel>();
    Scanner s = TE.loadFile("edges.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 4 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      String edgeKind = tokens[1];
      int numPixels = Integer.parseInt(tokens[2]);
      String socketCfg = tokens[3];

      if (socketCfg.startsWith("x10") || socketCfg.contains("?")) socketCfg = "uncontrolled";

      boolean dark;
      boolean fwd = true;
      switch (edgeKind) {
        case "default":
          dark = false;
          break;
        case "reversed":
          dark = false;
          fwd = false;
          break;
        case "dark":
          dark = true;
          assert socketCfg.equals("uncontrolled");
          break;
        default:
          throw new Error("Weird edge config: " + line);
      }

      tokens = id.split("-");
      if (tokens.length != 2) {
        throw new Error("Found " + tokens.length + " ID tokens");
      }
      int v0Id = Integer.parseInt(tokens[0]);
      int v1Id = Integer.parseInt(tokens[1]);
      assert v0Id < v1Id;
      TEVertex v0 = geometry.vertexesById.get(v0Id);
      TEVertex v1 = geometry.vertexesById.get(v1Id);
      String[] tags = new String[] {tokens[0] + tokens[1], id};

      for (String view : geometry.tags.stringPropertyNames()) {
        List<String> ids = Arrays.asList(geometry.tags.getProperty(view).split(","));
        if (ids.contains(id)) {
          String[] newTags = new String[tags.length + 1]; // Resize the tags array to fit all IDs
          System.arraycopy(
              tags, 0, newTags, 0, tags.length); // Copy the old tags into the new array
          newTags[tags.length] = view;
          tags = newTags;
        }
      }
      TEEdgeModel e = new TEEdgeModel(v0, v1, numPixels, dark, tags);
      v0.addEdge(e);
      v1.addEdge(e);

      if (!socketCfg.equals("uncontrolled")) {
        tokens = socketCfg.split(":");
        String socketStr;
        int strandOffset;
        if (tokens.length == 1) {
          socketStr = socketCfg;
          strandOffset = 0;
        } else if (tokens.length == 2) {
          socketStr = tokens[0];
          strandOffset = Integer.parseInt(tokens[1]);
        } else {
          throw new IllegalArgumentException("Bad edge config " + socketCfg);
        }

        tokens = socketStr.split("#");
        assert tokens.length == 2;
        String ip = tokens[0];
        int channelNum = Integer.parseInt(tokens[1]);
        ChromatechSocket socket = GrandShlomoStation.getOrMake(ip, channelNum);
        socket.addEdge(e, strandOffset, fwd);
      }

      geometry.edgesById.put(id, e);
    }
    s.close();
  }

  private static int calcNudge(String s) {
    int rv = 0;
    for (char c : s.toCharArray()) {
      if (c == '-') rv--;
      else if (c == '+') rv++;
      else throw new IllegalArgumentException("Bad nudge char " + c);
    }
    return rv;
  }

  private static Map<String, TEStripingInstructions> loadStripingInstructions(Geometry geometry) {
    Scanner s = TE.loadFile("striping-instructions.txt");

    Map<String, TEStripingInstructions> rv = new HashMap<>();
    while (s.hasNextLine()) {
      String line = s.nextLine().replaceAll("\s*\\(.+?\\)\s*", " ");
      String[] tokens = line.split(" ");
      if (tokens[0].contains(".")) {
        LX.log("Ignoring leftover Striping IP " + tokens[0]);
        continue;
      }
      String id = tokens[0];
      if (tokens.length < 3) continue;
      int rowLength = Integer.parseInt(tokens[1]);

      int[] channelLengths = null;
      int next_index = 2;
      if (tokens[next_index].startsWith("C")) {
        channelLengths =
            Arrays.stream(tokens[next_index].substring(1).split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
        next_index++;
      }
      boolean isLeft;
      if (tokens[next_index].equals("L")) {
        isLeft = true;
      } else if (tokens[next_index].equals("R")) {
        isLeft = false;
      } else {
        throw new Error("Invalid left/right token " + tokens[next_index]);
      }
      next_index++;
      List<Integer> rowLengths = new ArrayList<>();
      List<Integer> beforeNudges = new ArrayList<>();
      List<Integer> gaps = new ArrayList<>();
      int currentGap = 0;
      int phase = 0;
      for (int i = next_index; i < tokens.length; i++) {
        String token = tokens[i];
        if (token.matches("^g+$")) {
          currentGap += token.length();
        } else {
          String[] subTokens = token.split("\\.", -1);
          if (subTokens.length != 2) {
            throw new IllegalArgumentException("Bad subtokens for [" + line + "]: " + token);
          }
          int leftNudge = calcNudge(subTokens[0]);
          int rightNudge = calcNudge(subTokens[1]);
          if (isLeft == (phase == 0)) {
            beforeNudges.add(leftNudge);
          } else {
            beforeNudges.add(rightNudge);
          }
          rowLength += leftNudge + rightNudge;
          rowLengths.add(rowLength);
          rowLength--;
          gaps.add(currentGap);
          currentGap = 0;
          phase = 1 - phase;
        }
      }
      TEStripingInstructions tesi =
          new TEStripingInstructions(
              channelLengths,
              rowLengths.stream().mapToInt(i -> i).toArray(),
              beforeNudges.stream().mapToInt(i -> i).toArray(),
              gaps.stream().mapToInt(i -> i).toArray());
      rv.put(id, tesi);
    }
    return rv;
  }

  private static int getChannelLengthForPanel(TEStripingInstructions tesi, int n) {
    if (tesi.channelLengths == null) return TEStripingInstructions.DEFAULT_PANEL_CHANNEL_LENGTH;
    else return tesi.channelLengths[n];
  }

  private static void setPanelOutputs(
      TEPanelModel p, TEStripingInstructions tesi, String outputConfig) {
    String[] outputs = outputConfig.split("/");

    // The index into the panel that maps to the current channel's pixel 0
    int firstChannelPixel = 0;

    // How many channels into the panel we currently are
    int channelsIntoPanel = 0;

    // If the panel has a group of outputs joined by slashes, this keeps track of
    // which one we're currently on.
    int outputIndex;

    // Loop until every pixel in the panel has been mapped to an output channel.
    for (outputIndex = 0; firstChannelPixel < p.size; outputIndex++) {
      if (outputs.length <= outputIndex) {
        TE.err("Ran out of configured channels before assigning all pixels in " + p.getId());
        return;
      }
      String[] tokens = outputs[outputIndex].split("#");
      assert tokens.length == 2 : "Bad panel output config: " + outputConfig;
      String ip = tokens[0];
      tokens = tokens[1].split("-");
      int firstChannel = Integer.parseInt(tokens[0]);
      int lastChannel;
      if (tokens.length == 1) {
        lastChannel = ChromatechSocket.CHANNELS_PER_IP;
      } else {
        assert tokens.length == 2;
        lastChannel = Integer.parseInt(tokens[1]);
      }

      // How many channels into the current output we are. For instance, if the config is
      // 10.7.x.x#3, and we're assigning to channel 3, this is 0. When we're assigning to
      // channel 4, it becomes 1.
      int channelsIntoOutput;

      for (channelsIntoOutput = 0;
          // Keep going on the current controller as long as there remain panel pixels not assigned
          // to outputs...
          firstChannelPixel < p.size
              &&
              // ...and channels available to assign them to
              firstChannel + channelsIntoOutput <= lastChannel;
          // Increment both of these when we progress to the next channel
          channelsIntoOutput++, channelsIntoPanel++) {

        // The index into the panel that maps to the current channel's final pixel; this is more
        // complicated than it seems because some channels have 251 pixels. The striping
        // instructions
        // keep track of that.
        int lastChannelPixel =
            firstChannelPixel + getChannelLengthForPanel(tesi, channelsIntoPanel) - 1;

        // If the channel has room for more pixels than the panel has available, use the latter.
        if (lastChannelPixel > p.size - 1) lastChannelPixel = p.size - 1;

        // Create a socket to a particular controller channel
        ChromatechSocket socket =
            GrandShlomoStation.getOrMake(ip, firstChannel + channelsIntoOutput);

        // And assign these pixels of the panel to it
        socket.addPanel(p, firstChannelPixel, lastChannelPixel);

        // The next channel will begin where we left off.
        firstChannelPixel = lastChannelPixel + 1;
      }
    }
  }

  private static void loadPanels(Geometry geometry) {
    geometry.panelsById = new HashMap<>();
    geometry.panelsBySection = new HashMap<>();
    geometry.panelsByFlavor = new HashMap<>();

    Map<String, TEStripingInstructions> stripingInstructions = loadStripingInstructions(geometry);

    for (String id : stripingInstructions.keySet()) {
      TEStripingInstructions tesi = stripingInstructions.get(id);
      assert tesi != null;
      StringBuilder out = new StringBuilder("Panel " + id + " has row lengths ");
      for (int i : tesi.rowLengths) out.append(i).append(" ");
      // LX.log(out.toString());
    }

    Scanner s = TE.loadFile("panels.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 8 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      int declaredNumPixels = Integer.parseInt(tokens[1]);
      String e0Id = tokens[2];
      String e1Id = tokens[3];
      String e2Id = tokens[4];
      String startingPath = tokens[5];
      String flipStr = tokens[6];
      String panelType = tokens[7];

      TEEdgeModel e0 = geometry.edgesById.get(e0Id);
      TEEdgeModel e1 = geometry.edgesById.get(e1Id);
      TEEdgeModel e2 = geometry.edgesById.get(e2Id);

      HashSet<TEVertex> vh = new HashSet<>();
      vh.add(e0.v0);
      vh.add(e0.v1);
      vh.add(e1.v0);
      vh.add(e1.v1);
      vh.add(e2.v0);
      vh.add(e2.v1);
      TEVertex[] vertexes = vh.toArray(new TEVertex[0]);
      assert vertexes.length == 3;

      if (panelType.startsWith("x10")) panelType = "lit";
      if (panelType.contains("?")) panelType = "lit";

      boolean lit = panelType.contains(".");
      String outputConfig = panelType;

      if (lit) panelType = "lit";

      tokens = startingPath.split("->");
      assert tokens.length == 2 : "Starting path has " + tokens.length + " tokens";
      int startVertexId = Integer.parseInt(tokens[0]);
      int midVertexId = Integer.parseInt(tokens[1]);

      TEStripingInstructions tesi = stripingInstructions.get(id);
      TEPanelModel p =
          TEPanelFactory.build(
              id,
              vertexes[0],
              vertexes[1],
              vertexes[2],
              startVertexId,
              midVertexId,
              e0,
              e1,
              e2,
              panelType,
              tesi,
              geometry.gapPoint,
              geometry.tags);
      if (p.points.length != declaredNumPixels) {
        TE.err(
            "Panel "
                + id
                + " was declared to have "
                + declaredNumPixels
                + "px but it actually has "
                + p.points.length);
      }
      if (flipStr.equals("flipped")) {
        p.offsetTriangles.flip();
      } else if (!flipStr.equals("unflipped")) {
        throw new Error("Panel " + id + " is neither flipped nor unflipped");
      }

      e0.connectedPanels.add(p);
      e1.connectedPanels.add(p);
      e2.connectedPanels.add(p);

      geometry.panelsById.put(id, p);

      if (!geometry.panelsBySection.containsKey(p.getSection()))
        geometry.panelsBySection.put(p.getSection(), new HashSet<>());
      geometry.panelsBySection.get(p.getSection()).add(p);

      String flavor = p.flavor;
      if (!geometry.panelsByFlavor.containsKey(flavor))
        geometry.panelsByFlavor.put(flavor, new ArrayList<>());
      geometry.panelsByFlavor.get(flavor).add(p);

      if (lit) setPanelOutputs(p, tesi, outputConfig);
    }
    s.close();

    for (String flavor : geometry.panelsByFlavor.keySet()) {
      StringBuilder flavorStr = new StringBuilder("Panels of flavor ");
      flavorStr.append(flavor);
      flavorStr.append(": ");
      for (TEPanelModel panel : geometry.panelsByFlavor.get(flavor)) {
        flavorStr.append(panel.getId());
        flavorStr.append(" ");
      }
      // LX.log(flavorStr.toString());
    }
  }

  private static void loadLasers(Geometry geometry) {
    geometry.lasersById = new HashMap<>();

    Scanner s = TE.loadFile("lasers.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split("\t");
      assert tokens.length == 4 : "Found " + tokens.length + " tokens";

      String id = tokens[0];
      int x = Integer.parseInt(tokens[1]);
      int y = Integer.parseInt(tokens[2]);
      int z = Integer.parseInt(tokens[3]);
      String[] tags = new String[] {id};

      TELaserModel laser = new TELaserModel(id, x, y, z, tags);
      // laser.control = new Cone(laser);
      laser.control = new MovingTarget(laser);
      geometry.lasersById.put(id, laser);
    }
  }

  private static void loadBeacons(Geometry geometry) {
    geometry.beacons = new ArrayList<DmxModel>();

    try (Scanner s = TE.loadFile(RESOURCE_NAME_BEACONS)) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (line.startsWith("#") || LXUtils.isEmpty(line)) {
          continue;
        }
        String[] tokens = line.split("\t");
        assert tokens.length == 13 : "Found " + tokens.length + " tokens";

        DmxCommonConfig config = new DmxCommonConfig();
        config.id = tokens[0];
        config.x = Integer.parseInt(tokens[1]);
        config.y = Integer.parseInt(tokens[2]);
        config.z = Integer.parseInt(tokens[3]);
        config.yaw = Double.parseDouble(tokens[4]);
        config.pitch = Double.parseDouble(tokens[5]);
        config.roll = Double.parseDouble(tokens[6]);
        config.host = tokens[7];
        config.sequenceEnabled = Boolean.parseBoolean(tokens[8]);
        config.fps = Float.parseFloat(tokens[9]);
        config.universe = Integer.parseInt(tokens[10]);
        config.channel = Integer.parseInt(tokens[11]);
        float tiltLimit = Float.parseFloat(tokens[12]);
        String[] tags = new String[] {config.id};

        BeaconModel beacon = new BeaconModel(config, tiltLimit, tags);
        geometry.beacons.add(beacon);
      }
    } catch (Throwable e) {
      LX.error(e, "Error loading Beacons from resource: " + e.getMessage());
    }
  }

  private static void loadDjLights(Geometry geometry) {
    geometry.djLights = new ArrayList<DmxModel>();

    try (Scanner s = TE.loadFile(RESOURCE_NAME_DJLIGHTS)) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (line.startsWith("#") || LXUtils.isEmpty(line)) {
          continue;
        }
        String[] tokens = line.split("\t");
        assert tokens.length == 12 : "Found " + tokens.length + " tokens";

        DmxCommonConfig config = new DmxCommonConfig();
        config.id = tokens[0];
        config.x = Integer.parseInt(tokens[1]);
        config.y = Integer.parseInt(tokens[2]);
        config.z = Integer.parseInt(tokens[3]);
        config.yaw = Double.parseDouble(tokens[4]);
        config.pitch = Double.parseDouble(tokens[5]);
        config.roll = Double.parseDouble(tokens[6]);
        config.host = tokens[7];
        config.sequenceEnabled = Boolean.parseBoolean(tokens[8]);
        config.fps = Float.parseFloat(tokens[9]);
        config.universe = Integer.parseInt(tokens[10]);
        config.channel = Integer.parseInt(tokens[11]);
        String[] tags = new String[] {config.id};

        // Chauvet light for JKB testing
        // ChauvetSpot160Model m = new ChauvetSpot160Model(config, tags);
        AdjStealthModel m = new AdjStealthModel(config, tags);

        geometry.djLights.add(m);
      }
    } catch (Throwable e) {
      LX.error(e, "Error loading DJ Lights from resource: " + e.getMessage());
    }
  }

  private static void loadBoxes(Geometry geometry) {
    geometry.boxes = new ArrayList<>();

    Scanner s = TE.loadFile("boxes.txt");

    List<LXVector> vectors = new ArrayList<>();
    while (s.hasNextLine()) {
      String line = s.nextLine();
      if (line.isBlank()) continue;
      String[] tokens = line.split("\\s+");
      assert tokens.length == 3 : "Found " + tokens.length + " tokens";

      int x = Integer.parseInt(tokens[0]);
      int y = Integer.parseInt(tokens[1]);
      int z = Integer.parseInt(tokens[2]);

      vectors.add(new LXVector(x, y, z));

      if (vectors.size() == 8) {
        geometry.boxes.add(new TEBox(vectors));
        List<LXVector> mirrored = new ArrayList<>();
        for (LXVector v : vectors) {
          mirrored.add(new LXVector(v.x, v.y, -v.z));
        }
        geometry.boxes.add(new TEBox(mirrored));
        vectors.clear();
      }
    }
    assert vectors.size() == 0 : "Leftover lines in boxes.txt";
  }

  private static void loadGeneral(Geometry geometry) {
    Scanner s = TE.loadFile("general.txt");

    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split(":");
      assert tokens.length == 2 : "Found " + tokens.length + " tokens";
      switch (tokens[0].trim()) {
        case "name":
          geometry.name = tokens[1].trim();
          break;
        default:
          throw new Error("Weird line: " + line);
      }
    }
    s.close();
    assert geometry.name != null : "Model has no name";
  }

  // initializeBoundaries finds the boundaries of the grid we've drawn to contain our shape
  // and instantiates a helper class called Boundaries to keep track of the edge values
  // represented by that outermost LXPoint for each axis.
  private Boundaries initializeBoundaries() {
    ArrayList<LXPoint> pointsList = new ArrayList<>(Arrays.asList(this.points));

    LXPoint minXValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.x)).get();
    LXPoint maxXValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.x)).get();

    LXPoint minYValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.y)).get();
    LXPoint maxYValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.y)).get();

    LXPoint minZValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.z)).get();
    LXPoint maxZValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.z)).get();
    return new Boundaries(
        minXValuePoint,
        maxXValuePoint,
        minYValuePoint,
        maxYValuePoint,
        minZValuePoint,
        maxZValuePoint);
  }

  private static Geometry loadGeometry() {
    Geometry geometry = new Geometry();
    List<LXModel> childList = new ArrayList<LXModel>();

    loadGeneral(geometry);

    loadBoxes(geometry);

    loadVertexes(geometry);

    loadTags(geometry);

    // Vertexes aren't LXPoints (and thus, not LXModels) so they're not children

    loadLasers(geometry);

    for (TEModel child : geometry.lasersById.values()) {
      childList.add(child.model);
    }

    loadBeacons(geometry);

    loadDjLights(geometry);

    loadEdges(geometry);

    for (TEModel child : geometry.edgesById.values()) {
      childList.add(child.model);
    }

    geometry.gapPoint = new LXPoint();
    List<LXPoint> gapList = new ArrayList<>();
    gapList.add(geometry.gapPoint);
    childList.add(new LXModel(gapList));

    loadPanels(geometry);

    for (TEModel child : geometry.panelsById.values()) {
      childList.add(child.model);
    }

    geometry.children = childList.toArray(new LXModel[0]);

    return geometry;
  }

  public float minX() {
    return this.boundaryPoints.minXBoundaryPoint.x;
  }

  public float maxX() {
    return this.boundaryPoints.maxXBoundaryPoint.x;
  }

  public float minY() {
    return this.boundaryPoints.minYBoundaryPoint.y;
  }

  public float maxY() {
    return this.boundaryPoints.maxYBoundaryPoint.y;
  }

  public float minZ() {
    return this.boundaryPoints.minZBoundaryPoint.z;
  }

  public float maxZ() {
    return this.boundaryPoints.maxZBoundaryPoint.z;
  }

  public List<LXPoint> getEdgePointsBySection(TEEdgeSection section) {
    return edgePoints.stream()
        .filter(point -> section == TEEdgeSection.PORT ? point.x > 0 : point.x < 0)
        .collect(Collectors.toList());
  }

  public List<LXPoint> getPointsBySection(TEPanelSection section) {
    return panelsBySection.get(section).stream()
        .map(panel -> panel.model.getPoints())
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @Override
  public TEVertex getVertex(int vertexId) {
    return this.vertexesById.get(vertexId);
  }

  @Override
  public boolean hasPanel(String panelId) {
    return this.panelsById.containsKey(panelId);
  }

  @Override
  public TEPanelModel getPanel(String panelId) {
    return this.panelsById.get(panelId);
  }

  @Override
  public List<TEPanelModel> getPanels() {
    return this.panels;
  }

  @Override
  public boolean hasEdge(String edgeId) {
    return this.edgesById.containsKey(edgeId);
  }

  @Override
  public TEEdgeModel getEdge(String edgeId) {
    return this.edgesById.get(edgeId);
  }

  @Override
  public List<TEEdgeModel> getEdges() {
    return this.edges;
  }

  @Override
  public Map<LXVector, List<TEEdgeModel>> getEdgesBySymmetryGroup() {
    return this.edgesBySymmetryGroup;
  }

  @Override
  public List<TELaserModel> getLasers() {
    return this.lasers;
  }

  // Beacons

  private void addBeacons(List<DmxModel> beacons) {
    this.mutableBeacons.addAll(beacons);
    for (DmxModel beacon : beacons) {
      this.beaconsById.put(beacon.id, beacon);
    }

    addDmxModels(beacons);
  }

  public DmxModel getBeacon(String id) {
    return this.beaconsById.get(id);
  }

  // DJ Lights

  private void addDjLights(List<DmxModel> djLights) {
    this.mutableDjLights.addAll(djLights);
    for (DmxModel djLight : djLights) {
      this.djLightsById.put(djLight.id, djLight);
    }

    addDmxModels(djLights);
  }

  public DmxModel getDjLight(String id) {
    return this.djLightsById.get(id);
  }

  // All DMX Models

  private void addDmxModels(List<DmxModel> models) {
    for (DmxModel model : models) {
      if (!this.mutableDmxModels.contains(model)) {
        model.index = this.nextDmxIndex++;
        this.mutableDmxModels.add(model);
      } else {
        TE.err("Can not add dmx model twice.");
      }
    }
    updateSizeDmx();
  }

  protected final void updateSizeDmx() {
    this.sizeDmx = this.mutableDmxModels.size();
  }

  @Override
  public int sizeDmx() {
    return this.sizeDmx;
  }

  @Override
  public List<DmxModel> getDmxModels() {
    return this.dmxModels;
  }

  public void loadViews(LX lx) {
    List<ViewDefinition> views = new ArrayList<ViewDefinition>();

    // Load view definitions from file
    try (Scanner s = new Scanner(new File("resources/vehicle/" + RESOURCE_NAME_VIEWS))) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (line.startsWith("#")) {
          continue;
        }
        String[] tokens = line.split("\t");
        // Asserts default to off in Java...
        if (tokens.length == 3) {
          String label = tokens[0];
          // Normalization: true = relative, false = absolute
          LXView.Normalization n =
              Boolean.parseBoolean(tokens[1])
                  ? LXView.Normalization.RELATIVE
                  : LXView.Normalization.ABSOLUTE;
          String selector = tokens[2];
          views.add(new ViewDefinition(label, selector, n));
        } else {
          LX.error(
              "Invalid number of columns in "
                  + RESOURCE_NAME_VIEWS
                  + " config file, found "
                  + tokens.length
                  + ": "
                  + line);
        }
      }
    } catch (IOException e) {
      LX.error(e, "Error loading views from file:");
      return;
    }

    // adjust model geometry for easy texture mapping in views
    ModelBender mb = new ModelBender();
    mb.adjustEndGeometry(this);

    // Create LXViews once
    for (ViewDefinition v : views) {
      if (v.viewEnabled && (v.viewSelector != null) && !v.viewSelector.isEmpty()) {
        addGlobalView(lx, v);
      } else {
        LX.warning("View Definition is not a filter.");
      }
    }

    // restore original model geometry
    mb.restoreModel(this);
  }

  private void addGlobalView(LX lx, ViewDefinition viewDefinition) {
    LXViewDefinition view = lx.structure.views.addView();
    view.label.setValue(viewDefinition.label);
    view.selector.setValue(viewDefinition.viewSelector);
    view.normalization.setValue(viewDefinition.viewNormalization);
  }

  private final List<TEModelListener> listeners = new ArrayList<TEModelListener>();

  public TEWholeModel addListener(TEModelListener listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate TEListener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public TEWholeModel removeListener(TEModelListener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-registered TEListener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }
}
