/**
 * Copyright 2020- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.app;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.pattern.color.GradientPattern;
import heronarts.lx.pattern.form.PlanesPattern;
import heronarts.lx.pattern.texture.NoisePattern;
import heronarts.lx.pattern.texture.SparklePattern;
import heronarts.lx.studio.LXStudio;
import processing.core.PApplet;
import titanicsend.app.autopilot.*;
import titanicsend.lasercontrol.PangolinHost;
import titanicsend.lasercontrol.TELaserTask;
import titanicsend.lx.APC40Mk2;
import titanicsend.lx.MidiFighterTwister;
import titanicsend.lx.APC40Mk2.UserButton;
import titanicsend.model.TEWholeModel;
import titanicsend.model.justin.ColorCentral;
import titanicsend.model.justin.ViewCentral;
import titanicsend.modulator.justin.MultiplierModulator;
import titanicsend.modulator.justin.UIMultiplierModulator;
import titanicsend.osc.CrutchOSC;
import titanicsend.output.GPOutput;
import titanicsend.output.GrandShlomoStation;
import titanicsend.pattern.TEEdgeTestPattern;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.TEPanelTestPattern;
import titanicsend.pattern.ben.*;
import titanicsend.pattern.cesar.HandTracker;
import titanicsend.pattern.jeff.*;
import titanicsend.pattern.jon.*;
import titanicsend.pattern.justin.TEGradientPattern;
import titanicsend.pattern.justin.TESolidPattern;
import titanicsend.pattern.mike.*;
import titanicsend.pattern.pixelblaze.*;
import titanicsend.pattern.tom.*;
import titanicsend.pattern.will.PowerDebugger;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.effect.BeaconEffect;
import titanicsend.ui.UITEPerformancePattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.util.TE;

public class TEApp extends PApplet implements LXPlugin {
  private TEWholeModel model;
  static public TEWholeModel wholeModel;

  private LXStudio lx;

  private static int WIDTH = 1280;
  private static int HEIGHT = 800;
  private static boolean FULLSCREEN = false;
  private static String resourceSubdir;

  private GigglePixelListener gpListener;
  private GigglePixelBroadcaster gpBroadcaster;

  private TEAutopilot autopilot;
  private TEOscListener oscListener;
  private TEPatternLibrary library;

  private TELaserTask laserTask;
  private ColorCentral colorCentral;
  private ViewCentral viewCentral;
  private CrutchOSC crutchOSC;

  // Global feature on/off switches for troubleshooting
  public static final boolean ENABLE_COLOR_CENTRAL = true;
  static public final boolean ENABLE_TOUCHOSC_IPADS = true;
  public static final boolean ENABLE_VIEW_CENTRAL = true;
  public static final boolean DELAY_FILE_OPEN_TO_FIRST_ENGINE_LOOP = true;

  @Override
  public void settings() {
    if (FULLSCREEN) {
      fullScreen(PApplet.P3D);
    } else {
      size(WIDTH, HEIGHT, PApplet.P3D);
    }
    pixelDensity(displayDensity());
  }

  @Override
  public void setup() {
    LXStudio.Flags flags = new LXStudio.Flags(this);
    flags.resizable = true;
    flags.useGLPointCloud = false;
    flags.startMultiThreaded = true;

    this.model = new TEWholeModel(resourceSubdir);
    TEApp.wholeModel = this.model;

    this.lx = new LXStudio(this, flags, this.model);
    this.surface.setTitle(this.model.name);

    String logFileName = LOG_FILENAME_FORMAT.format(Calendar.getInstance().getTime());
    LX.setLogFile(new File(flags.mediaPath, LX.Media.LOGS.getDirName() + File.separator + logFileName));
  }

  public void openDelayedFile(LX lx) {
    // Hack to load CLI filename in PApplet environment
    // Also now used for delayed opening of recent file to improve startup time.
    if (projectFileName != null) {
      final File finalProjectFile =lx.getMediaFile(LX.Media.PROJECTS, projectFileName);

      if (finalProjectFile.getName().endsWith(".lxs")) {
        lx.preferences.schedulerEnabled.setValue(true);
        LX.log("Opening schedule file: " + finalProjectFile);
        lx.scheduler.openSchedule(finalProjectFile, true);
      } else {
        try {
          if (finalProjectFile.exists()) {
            LX.log("Now starting delayed open of project file: " + projectFileName);
            lx.openProject(finalProjectFile);
          } else {
            LX.error("Project filename not found: " + projectFileName);
          }
        } catch (Exception x) {
          LX.error(x, "Exception loading project: " + x.getLocalizedMessage());
        }
      }
    }
  }

  public void setOscDestinationForIpads() {
    try {
      lx.engine.osc.transmitHost.setValue(PangolinHost.HOSTNAME);
      lx.engine.osc.transmitPort.setValue(PangolinHost.PORT);
    } catch (Exception ex) {
      TE.err(ex, "Failed to set destination OSC address to ShowKontrol IP for iPads relay");
    }
  }

  @Override
  public void initialize(LX lx) {
    // Here is where you should register any custom components or make modifications
    // to the LX engine or hierarchy. This is also used in headless mode, so note that
    // you cannot assume you are working with an LXStudio class or that any UI will be
    // available.

    GrandShlomoStation.activateAll(lx, this.model.getGapPointIndex());

    // Patterns/effects that currently conform to art direction standards
    lx.registry.addPattern(EdgeProgressions.class);
    lx.registry.addPattern(EdgeSymmetry.class);
    lx.registry.addPattern(Smoke.class);

    // Patterns that are in development towards meeting standards
    lx.registry.addPattern(ArcEdges.class);
    lx.registry.addPattern(BassLightning.class);
    lx.registry.addPattern(BouncingDots.class);
    lx.registry.addPattern(Checkers.class);
    lx.registry.addPattern(EdgeFall.class);
    lx.registry.addPattern(EdgeKITT.class);
    lx.registry.addPattern(EdgeRunner.class);
    lx.registry.addPattern(Electric.class);
    lx.registry.addPattern(ElectricEdges.class);
    lx.registry.addPattern(FollowThatStar.class);
    lx.registry.addPattern(FrameBrights.class);
    lx.registry.addPattern(FourStar.class);
    lx.registry.addPattern(Iceflow.class);
    lx.registry.addPattern(Phasers.class);
    lx.registry.addPattern(PixelblazeSandbox.class);
    lx.registry.addPattern(PBAudio1.class);
    lx.registry.addPattern(Audio1.class);
    lx.registry.addPattern(PBXorcery.class);
    lx.registry.addPattern(Xorcery.class);
    lx.registry.addPattern(XorceryDiamonds.class);
    lx.registry.addPattern(PBFireworkNova.class);
    lx.registry.addPattern(PixelblazeParallel.class);
    lx.registry.addPattern(RadialSimplex.class);
    lx.registry.addPattern(SimplexPosterized.class);
    lx.registry.addPattern(TEMidiFighter64DriverPattern.class);
    lx.registry.addPattern(TESparklePattern.class);
    lx.registry.addPattern(TurbulenceLines.class);
    lx.registry.addPattern(TriangleNoise.class);
    lx.registry.addPattern(SpiralDiamonds.class);
    lx.registry.addPattern(PulsingTriangles.class);
    lx.registry.addPattern(Fire.class);
    lx.registry.addPattern(TESolidPattern.class);
    lx.registry.addPattern(TEGradientPattern.class);

    // Patterns that will not aspire to art direction standards

    // Examples for teaching and on-boarding developers
    lx.registry.addPattern(BasicRainbowPattern.class);
    lx.registry.addPattern(BassReactive.class);
    lx.registry.addPattern(BassReactiveEdge.class);
    lx.registry.addPattern(TempoReactiveEdge.class);
    lx.registry.addPattern(ArtStandards.class);
    lx.registry.addEffect(titanicsend.effect.EdgeSieve.class);
    lx.registry.addEffect(titanicsend.effect.NoGapEffect.class);
    lx.registry.addEffect(BeaconEffect.class);


    // TODO - The following patterns were removed from the UI prior to EDC 2023 to keep
    // TODO - them from being accidentally activated during a performance.
    // TODO - update/fix as needed!

    // Nonfunctional - throws exception on load
    // lx.registry.addPattern(ReactiveHeartPattern.class);
    // lx.registry.addPattern(SolidPanel.class);
    // lx.registry.addPattern(SolidEdge.class);
    // lx.registry.addPattern(BasicImagePattern.class);
    // lx.registry.addPattern(Bubbles.class);

    // Nonfunctional - need work or additional hardware that will not be at EDC
    lx.registry.addPattern(HandTracker.class);

    // "ShaderToyPattern" in ShaderPanelsPatternConfig.java

    // Useful for test, but might turn the car black in performance
    lx.registry.removePattern(PlanesPattern.class);  // remove pattern added automatically by LX.

    // Frame Rate Killers
    // lx.registry.addEffect(titanicsend.effect.Kaleidoscope.class);
    // "StarryOutrun" in OrganicPatternConfig.java


    @SuppressWarnings("unchecked")
    Function<Class<?>, Class<LXPattern>[]> patternGetter =
            (Class<?> patternConfigClass) ->
                    (Class<LXPattern>[]) Arrays.stream(patternConfigClass.getDeclaredClasses())
                            .filter(LXPattern.class::isAssignableFrom)
                            .toArray(Class[]::new);

    lx.registry.addPatterns(patternGetter.apply(OrganicPatternConfig.class));
    lx.registry.addPatterns(patternGetter.apply(ShaderPanelsPatternConfig.class));
    lx.registry.addPatterns(patternGetter.apply(ShaderEdgesPatternConfig.class));

    // Test/debug patterns
    lx.registry.addPattern(ModelDebugger.class);
    lx.registry.addPattern(PowerDebugger.class);
    // lx.registry.addPattern(ModuleEditor.class);
    lx.registry.addPattern(SignalDebugger.class);
    lx.registry.addPattern(TEEdgeTestPattern.class);
    lx.registry.addPattern(TEPanelTestPattern.class);

    // Midi surface names for use with BomeBox
    lx.engine.midi.registerSurface("FoH: APC40 mkII", APC40Mk2.class);
    lx.engine.midi.registerSurface("FoH: Midi Fighter Twister", MidiFighterTwister.class);
    lx.engine.midi.registerSurface("FoH: Midi Fighter Twister (2)", MidiFighterTwister.class);
    lx.engine.midi.registerSurface("FoH: Midi Fighter Twister (3)", MidiFighterTwister.class);
    lx.engine.midi.registerSurface("FoH: Midi Fighter Twister (4)", MidiFighterTwister.class);

    // Custom modulator type
    lx.registry.addModulator(MultiplierModulator.class);
    if (lx instanceof LXStudio) {
      ((LXStudio.Registry)lx.registry).addUIModulatorControls(UIMultiplierModulator.class);
    }

    // create our library for autopilot
    this.library = initializePatternLibrary(lx);

    int myGigglePixelID = 73;  // Looks like "TE"
    try {
      this.gpListener = new GigglePixelListener(lx, "0.0.0.0", myGigglePixelID);
      lx.engine.addLoopTask(this.gpListener);
      TE.log("GigglePixel listener created");
    } catch (IOException e) {
      TE.log("Failed to create GigglePixel listener: " + e.getMessage());
    }

    // This should of course be in the config, but we leave for the playa in like a week
    String destIP = "192.168.42.255";
    try {
      this.gpBroadcaster = new GigglePixelBroadcaster(
              lx, destIP, this.model.name, myGigglePixelID);
      lx.engine.addLoopTask(this.gpBroadcaster);
      TE.log("GigglePixel broadcaster created");
    } catch (IOException e) {
      TE.log("Failed to create GigglePixel broadcaster: " + e.getMessage());
    }

    // create our historian instance
    TEHistorian history = new TEHistorian();

    // create our Autopilot instance, run in general engine loop to
    // ensure performance under load
    this.autopilot = new TEAutopilot(lx, library, history);
    lx.engine.addLoopTask(this.autopilot);
    APC40Mk2.setUserButton(UserButton.USER, this.autopilot.enabled);

    // create our listener for OSC messages
    this.oscListener = new TEOscListener(lx, autopilot);

    // add custom OSC listener to handle OSC messages from ShowKontrol
    // includes an Autopilot ref to store (threadsafe) queue of unread OSC messages
    TE.log("Attaching the OSC message listener to port "
            + TEShowKontrol.OSC_PORT + " ...");
    try {
      lx.engine.osc.receiver(TEShowKontrol.OSC_PORT).addListener((message) -> {
        this.oscListener.onOscMessage(message);
        lx.engine.osc.receiveActive.setValue(true);
      });
      lx.engine.osc.receiveActive.setValue(true);
    } catch (SocketException sx) {
      sx.printStackTrace();
    }

    // create our loop task for outputting data to lasers
    this.laserTask = new TELaserTask(lx);
    lx.engine.addLoopTask(this.laserTask);

    GPOutput gpOutput = new GPOutput(lx, this.gpBroadcaster);
    lx.addOutput(gpOutput);
    
    // Add special per-channel swatch control.  Do not try this at home.
    this.colorCentral = new ColorCentral(lx);

    // Add special view controller
    this.viewCentral = new ViewCentral(lx);

    // CrutchOSC is an LXOscEngine supplement for TouchOSC clients
    this.crutchOSC = new CrutchOSC(lx);
    
  }

  private TEPatternLibrary initializePatternLibrary(LX lx) {
    // library that will hold metadata about TE patterns for autopilot
    // will not be used if autopilot is disabled
    TEPatternLibrary l = new TEPatternLibrary(lx);

    // aliases to reduce line count below...
    TEPatternLibrary.TEPatternCoverageType covEdges = TEPatternLibrary.TEPatternCoverageType.EDGES;
    TEPatternLibrary.TEPatternCoverageType covPanels = TEPatternLibrary.TEPatternCoverageType.PANELS;
    TEPatternLibrary.TEPatternCoverageType covPanelPartial = TEPatternLibrary.TEPatternCoverageType.PANELS_PARTIAL;
    TEPatternLibrary.TEPatternCoverageType covBoth = TEPatternLibrary.TEPatternCoverageType.BOTH;

    TEPatternLibrary.TEPatternColorCategoryType cPalette = TEPatternLibrary.TEPatternColorCategoryType.PALETTE;
    TEPatternLibrary.TEPatternColorCategoryType cWhite = TEPatternLibrary.TEPatternColorCategoryType.WHITE;
    TEPatternLibrary.TEPatternColorCategoryType cNonConforming = TEPatternLibrary.TEPatternColorCategoryType.NONCONFORMING;

    TEPhrase chorus = TEPhrase.CHORUS;
    TEPhrase down = TEPhrase.DOWN;
    TEPhrase up = TEPhrase.UP;

    // CHORUS patterns
    l.addPattern(NoisePattern.class, covBoth, cPalette, chorus);
    l.addPattern(PBXorcery.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.NeonBlocks.class, covPanelPartial, cNonConforming, chorus);
    l.addPattern(Audio1.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.OutrunGrid.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.MatrixScroller.class, covPanels, cNonConforming, chorus);
    l.addPattern(FollowThatStar.class, covBoth, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.NeonCellsLegacy.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.NeonTriangles.class, covPanels, cNonConforming, chorus);
    l.addPattern(Phasers.class, covPanels, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.PulsingPetriDish.class, covPanels, cNonConforming, chorus);
    l.addPattern(Electric.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covBoth, cNonConforming, chorus);
    l.addPattern(EdgeRunner.class, covEdges, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.MetallicWaves.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderEdgesPatternConfig.NeonRipplesEdges.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.WaterEdges.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.PulseCenter.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.WavyPanels.class, covPanelPartial, cPalette, chorus);

    // DOWN patterns
    l.addPattern(ShaderPanelsPatternConfig.Galaxy.class, covPanelPartial, cPalette, down);
    l.addPattern(TEGradientPattern.class, covPanelPartial, cPalette, down);
    l.addPattern(OrganicPatternConfig.WaterPanels.class, covPanelPartial, cPalette, down);
    l.addPattern(SimplexPosterized.class, covBoth, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.Galaxy.class, covPanelPartial, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.SmokeShader.class, covPanelPartial, cPalette, down);
    l.addPattern(TriangleNoise.class, covPanels, cPalette, down);
    l.addPattern(TurbulenceLines.class, covPanels, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.StormScanner.class, covPanelPartial, cPalette, down);
    l.addPattern(Phasers.class, covPanelPartial, cPalette, down);

    // UP patterns
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covBoth, cPalette, up);
    l.addPattern(TESparklePattern.class, covBoth, cPalette, up);
    l.addPattern(Electric.class, covPanelPartial, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.SlitheringSnake.class, covPanelPartial, cPalette, up);
    l.addPattern(Audio1.class, covPanelPartial, cPalette, up);
    l.addPattern(SimplexPosterized.class, covPanelPartial, cPalette, up);
    l.addPattern(ShaderPanelsPatternConfig.SmokeShader.class, covPanelPartial, cPalette, up);
    l.addPattern(TriangleNoise.class, covPanelPartial, cPalette, up);
    l.addPattern(TurbulenceLines.class, covPanelPartial, cPalette, up);
    l.addPattern(ShaderEdgesPatternConfig.NeonRipplesEdges.class, covPanelPartial, cPalette, up);
    l.addPattern(ArcEdges.class, covPanelPartial, cPalette, up);

    return l;
  }

  public void initializeUI(LXStudio lx, LXStudio.UI ui) {
    // Here is where you may modify the initial settings of the UI before it is fully
    // built. Note that this will not be called in headless mode. Anything required
    // for headless mode should go in the raw initialize method above.

    ((LXStudio.Registry)lx.registry).addUIDeviceControls(UITEPerformancePattern.class);
  }

  public void onUIReady(LXStudio lx, LXStudio.UI ui) {
    // At this point, the LX Studio application UI has been built. You may now add
    // additional views and components to the Ui heirarchy.

    TEVirtualOverlays visual = new TEVirtualOverlays(this.model);
    lx.ui.preview.addComponent(visual);
    new TEUIControls(ui, visual, ui.leftPane.global.getContentWidth()).addToContainer(ui.leftPane.global);

    GigglePixelUI gpui = new GigglePixelUI(ui, ui.leftPane.global.getContentWidth(),
            this.gpListener, this.gpBroadcaster);
    gpui.addToContainer(ui.leftPane.global);

    // Add UI section for autopilot
    new TEUserInterface.AutopilotUISection(ui, this.autopilot).addToContainer(ui.leftPane.global);
    // Add UI section for all other general settings
    new TEUserInterface.TEUISection(ui, laserTask).addToContainer(ui.leftPane.global);

    // precompile binaries for any new or changed shaders
    ShaderPrecompiler.rebuildCache();

    lx.engine.addTask(() -> {
      setOscDestinationForIpads();
      openDelayedFile(lx);
      // Replace old saved destination IPs from project files
      setOscDestinationForIpads();
    });
  }

  @Override
  public void draw() {
    // All handled by core LX engine, do not modify, method exists only so that Processing
    // will run a draw-loop.
  }

  @Override
  public void keyPressed(processing.event.KeyEvent keyEvent) {
    // Keyboard shortcut for debugging: Add all patterns to current channel
    // (Ctrl or Meta) + Alt + Shift + A
    if ((keyEvent.isControlDown() || keyEvent.isMetaDown()) && keyEvent.isAltDown() && keyEvent.isShiftDown() && keyEvent.getKeyCode() == 65) {
      this.lx.engine.addTask(() -> {
        addAllPatterns();
      });
    } else {
      super.keyPressed(keyEvent);
    }
  }

  /**
   * Dev tool: add all patterns in registry to current channel.
   */
  private void addAllPatterns() {
    LXBus channel = this.lx.engine.mixer.getFocusedChannel();
    if (channel instanceof LXChannel) {
      TE.log("*** Instantiating all " + this.lx.registry.patterns.size() + " patterns in registry to channel " + channel.getLabel() + " ***");
      TE.log("Here we gOOOOOOOOOOOO....");
      for (Class<? extends LXPattern> clazz : this.lx.registry.patterns) {
        try {
          ((LXChannel)channel).addPattern(this.lx.instantiatePattern(clazz));
        } catch (Exception ex) {
          TE.err(ex, "Broken pattern! Could not instantiate " + clazz);
        }
      }
    } else {
      TE.err("Selected channel must be a channel and not a group before adding all patterns.");
    }
  }

  @Override
  public void dispose() {
    this.crutchOSC.dispose();
    super.dispose();
  }

  private static final DateFormat LOG_FILENAME_FORMAT = new SimpleDateFormat("'LXStudio-TE-'yyyy.MM.dd-HH.mm.ss'.log'");

  private static String projectFileName = null;

  /**
   * Main interface into the program. Two modes are supported, if the --headless
   * flag is supplied then a raw CLI version of LX is used. If not, then we embed
   * in a Processing 4 applet and run as such.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    LX.log("Initializing LX version " + LXStudio.VERSION);

    // NOTE(mcslee): Hack for macOS Ventura!!
    // https://github.com/processing/processing4/issues/544
    // Hopefully to be removed in a future version
    com.jogamp.opengl.GLProfile.initSingleton();

    boolean headless = false;
    boolean loadTestahedron = false;
    boolean loadVehicle = false;
    File projectFile = null;
    for (int i = 0; i < args.length; ++i) {
      if ("--headless".equals(args[i])) {
        headless = true;
      } else if ("--fullscreen".equals(args[i]) || "-f".equals(args[i])) {
        FULLSCREEN = true;
      } else if ("--width".equals(args[i]) || "-w".equals(args[i])) {
        try {
          WIDTH = Integer.parseInt(args[++i]);
        } catch (Exception x ) {
          LX.error("Width command-line argument must be followed by integer");
        }
      } else if ("--height".equals(args[i]) || "-h".equals(args[i])) {
        try {
          HEIGHT = Integer.parseInt(args[++i]);
        } catch (Exception x ) {
          LX.error("Height command-line argument must be followed by integer");
        }
      } else if (args[i].endsWith(".lxp")) {
        try {
          projectFileName = args[i];
          projectFile = new File(args[i]);
          LX.log("Received command-line project file name: " + projectFileName);
        } catch (Exception x) {
          LX.error(x, "Command-line project file path invalid: " + args[i]);
        }
      } else if (args[i].equals("testahedron")) {
        loadTestahedron = true;
      } else if (args[i].equals("vehicle")) {
        loadVehicle = true;
      } else {
        throw new IllegalArgumentException("Unknown arg: " + args[i]);
      }
    }
    if (loadTestahedron && !loadVehicle) {
      resourceSubdir = "testahedron";
    } else if (loadVehicle && !loadTestahedron) {
      resourceSubdir = "vehicle";
    } else {
      throw new IllegalArgumentException("You must specify either testahedron or vehicle");
    }
    if (headless) {
      // We're not actually going to run this as a PApplet, but we need to explicitly
      // construct and set the initialize callback so that any custom components
      // will be run
      LX.Flags flags = new LX.Flags();
      flags.initialize = new TEApp();
      if (projectFile == null) {
        LX.log("WARNING: No project filename was specified for headless mode!");
      }
      LX.headless(flags, projectFile);
    } else {
      if (DELAY_FILE_OPEN_TO_FIRST_ENGINE_LOOP) {
        /* JKB note: Special trickery.  To preserve the 5 second loading time, defer
         * all file opening until the first engine run loop.  We'll check the .lxpreferences
         * file for a recent filename, set it aside, then remove the filename from .lxpreferences.
         * That will speed up the load, and then we'll use the after-load file open trick
         * from last summer to open the most recent file OR command line file.
         *
         * This clever preferences file manipulation was suggested by mcslee over email
         * as a solution to the CLI arg problem on... Aug 24, 2022.  Good times!
         */
        File preferences = new File(".lxpreferences");
        if (preferences.exists()) {
          LX.log("Checking preferences for recent project file...");
          JsonObject obj = new JsonObject();
          boolean removedFileName = false;
          try (FileReader fr = new FileReader(preferences)) {
            // Load parameters and settings from file
            obj = new Gson().fromJson(fr, JsonObject.class);
            if (obj.has("projectFileName")) {
              // Remember recent file name but only if CLI arg was not passed
              if (projectFileName == null) {
                projectFileName = obj.get("projectFileName").getAsString();
                LX.log("Setting aside recent file name " + projectFileName + " to open on first engine loop.");
              }
              // Remove the recent file name from preferences so it won't load.
              obj.remove("projectFileName");
              removedFileName = true;
            }
          } catch (Exception x) {
            LX.error("Error hack-reading .lxpreferences: " + x.getMessage());
          }
          if (removedFileName) {
            LX.log("Removing file name from preferences...");
            try (FileWriter fw = new FileWriter(preferences);
                 JsonWriter writer = new JsonWriter(fw)) {
              writer.setIndent("  ");
              new GsonBuilder().create().toJson(obj, writer);
            } catch (IOException iox) {
              LX.error(iox, "Exception hacking the .lxpreferences file: " + iox.getMessage());
            }
          } else {
            LX.log("No recent project file found. Continuing with load...");
          }
        }
      }
      PApplet.main("titanicsend.app.TEApp", args);
    }
  }

}
