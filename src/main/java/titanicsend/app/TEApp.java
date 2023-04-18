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
import java.io.IOException;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Function;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.midi.surface.APC40Mk2;
import heronarts.lx.midi.surface.MidiFighterTwister;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.pattern.color.GradientPattern;
import heronarts.lx.pattern.texture.NoisePattern;
import heronarts.lx.pattern.texture.SparklePattern;
import heronarts.lx.studio.LXStudio;
import processing.core.PApplet;
import titanicsend.app.autopilot.*;
import titanicsend.model.TEWholeModel;
import titanicsend.output.GPOutput;
import titanicsend.output.GrandShlomoStation;
import titanicsend.pattern.TEEdgeTestPattern;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.TEPanelTestPattern;
import titanicsend.pattern.ben.*;
import titanicsend.pattern.cesar.*;
import titanicsend.pattern.jeff.*;
import titanicsend.pattern.jon.*;
import titanicsend.pattern.justin.TEGradientPattern;
import titanicsend.pattern.justin.TESolidPattern;
import titanicsend.pattern.mike.*;
import titanicsend.pattern.pixelblaze.*;
import titanicsend.pattern.tmc.*;
import titanicsend.pattern.tom.*;
import titanicsend.pattern.will.PowerDebugger;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.effect.BeaconEffect;
import titanicsend.pattern.yoffa.media.BasicImagePattern;
import titanicsend.pattern.yoffa.media.ReactiveHeartPattern;
import titanicsend.ui.UITEPerformancePattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.util.TE;

public class TEApp extends PApplet implements LXPlugin, LX.ProjectListener  {
  private TEWholeModel model;
  static public TEWholeModel wholeModel;

  private static int WIDTH = 1280;
  private static int HEIGHT = 800;
  private static boolean FULLSCREEN = false;
  private static String resourceSubdir;

  private GigglePixelListener gpListener;
  private GigglePixelBroadcaster gpBroadcaster;

  private TEAutopilot autopilot;
  private TEOscListener oscListener;
  private TEPatternLibrary library;

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

    new LXStudio(this, flags, this.model);
    this.surface.setTitle(this.model.name);
    
    String logFileName = LOG_FILENAME_FORMAT.format(Calendar.getInstance().getTime());
    LX.setLogFile(new File(flags.mediaPath, LX.Media.LOGS.getDirName() + File.separator + logFileName));
  }

  public void loadCLfile(LX lx) {
    // Hack to load CLI filename in PApplet environment
    if (projectFileName != null) {
      final File finalProjectFile =lx.getMediaFile(LX.Media.PROJECTS, projectFileName);
    
      if (finalProjectFile.getName().endsWith(".lxs")) {
        lx.preferences.schedulerEnabled.setValue(true);
        LX.log("Opening schedule file: " + finalProjectFile);
        lx.scheduler.openSchedule(finalProjectFile, true);
      } else {
        try {
          if (finalProjectFile.exists()) {
            LX.log("Opening project file passed as argument: " + projectFileName);
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

  public TEUserInterface.AutopilotComponent autopilotComponent;

  @Override
  public void initialize(LX lx) {
    // Here is where you should register any custom components or make modifications
    // to the LX engine or hierarchy. This is also used in headless mode, so note that
    // you cannot assume you are working with an LXStudio class or that any UI will be
    // available.

    // Create autopilot component and register it with the LX engine
    // so that it can be saved and loaded in project files
    this.autopilotComponent = new TEUserInterface.AutopilotComponent(lx);
    lx.engine.registerComponent("autopilot", this.autopilotComponent);

    GrandShlomoStation.activateAll(lx, this.model.gapPoint.index);

    // Patterns/effects that currently conform to art direction standards
    lx.registry.addPattern(EdgeProgressions.class);
    lx.registry.addPattern(EdgeSymmetry.class);
    lx.registry.addPattern(Smoke.class);

    // Patterns that are in development towards meeting standards
    lx.registry.addPattern(ArcEdges.class);
    lx.registry.addPattern(BassLightning.class);
    lx.registry.addPattern(BouncingDots.class);
    lx.registry.addPattern(Bubbles.class);
    lx.registry.addPattern(Checkers.class);
    lx.registry.addPattern(EdgeFall.class);
    lx.registry.addPattern(EdgeKITT.class);
    lx.registry.addPattern(EdgeRunner.class);
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
    lx.registry.addPattern(SolidEdge.class);
    lx.registry.addPattern(SolidPanel.class);
    lx.registry.addPattern(TESparklePattern.class);
    lx.registry.addPattern(SpiralDiamonds.class);
    lx.registry.addPattern(PulsingTriangles.class);
    lx.registry.addPattern(HandTracker.class);
    lx.registry.addPattern(Fire.class);
    lx.registry.addPattern(TEMidiFighter64DriverPattern.class);
    lx.registry.addPattern(TESolidPattern.class);
    lx.registry.addPattern(TEGradientPattern.class);

    // Patterns that will not aspire to art direction standards
    lx.registry.addPattern(BasicImagePattern.class);
    lx.registry.addPattern(ReactiveHeartPattern.class);

    // Examples for teaching and on-boarding developers
    lx.registry.addPattern(BasicRainbowPattern.class);
    lx.registry.addPattern(BassReactive.class);
    lx.registry.addPattern(BassReactiveEdge.class);
    lx.registry.addPattern(TempoReactiveEdge.class);
    lx.registry.addPattern(ArtStandards.class);
    lx.registry.addEffect(titanicsend.effect.EdgeSieve.class);
    lx.registry.addEffect(titanicsend.effect.Kaleidoscope.class);
    lx.registry.addEffect(titanicsend.effect.NoGapEffect.class);
    lx.registry.addEffect(BeaconEffect.class);

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
    autopilot = new TEAutopilot(lx, library, history);
    lx.engine.addLoopTask(autopilot);

    // listener to toggle on the autopilot instance's enabled flag
    LXParameterListener autopilotEnableListener = (p) -> {
      // only toggle if different!
      if (autopilot.isEnabled()
              != this.autopilotComponent.autopilotEnabledToggle.getValueb()) {
        autopilot.setEnabled(this.autopilotComponent.autopilotEnabledToggle.getValueb());
      }
    };
    this.autopilotComponent.autopilotEnabledToggle.addListener(autopilotEnableListener);

    // create our listener for OSC messages
    this.oscListener = new TEOscListener(lx, autopilot);

    // add custom OSC listener to handle OSC messages from ShowKontrol
    // includes an Autopilot ref to store (threadsafe) queue of unread OSC messages
    TE.log("Attaching the OSC message listener to port "
            + TEShowKontrol.OSC_PORT + " ...");
    try {
        lx.engine.osc.receiver(TEShowKontrol.OSC_PORT).addListener((message) -> {
          this.oscListener.onOscMessage(message);
      });
    } catch (SocketException sx) {
        sx.printStackTrace();
    }

    GPOutput gpOutput = new GPOutput(lx, this.gpBroadcaster);
    lx.addOutput(gpOutput);
    
    lx.addProjectListener(this);
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
    l.addPattern(PBAudio1.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.OutrunGrid.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.RainbowSwirlPanels.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.MatrixScroller.class, covPanels, cNonConforming, chorus);
    l.addPattern(FollowThatStar.class, covBoth, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.NeonCellsLegacy.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.RainbowSwirlEdges.class, covEdges, cNonConforming, chorus);
    l.addPattern(ShaderPanelsPatternConfig.NeonTriangles.class, covPanels, cNonConforming, chorus);
    l.addPattern(ShaderPanelsPatternConfig.Mondelbrot.class, covPanels, cNonConforming, chorus);
    l.addPattern(Phasers.class, covPanels, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.PulsingPetriDish.class, covPanels, cNonConforming, chorus);
    l.addPattern(ShaderPanelsPatternConfig.Electric.class, covPanelPartial, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covBoth, cNonConforming, chorus);
    l.addPattern(EdgeRunner.class, covEdges, cPalette, chorus);

    // DOWN patterns
    l.addPattern(GradientPattern.class, covPanelPartial, cPalette, down);
    l.addPattern(OrganicPatternConfig.WaterEdges.class, covEdges, cPalette, down);
    l.addPattern(OrganicPatternConfig.WaterPanels.class, covPanelPartial, cPalette, down);
    l.addPattern(OrganicPatternConfig.WavyEdges.class, covEdges, cPalette, down);
    l.addPattern(NoisePattern.class, covBoth, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.Galaxy.class, covPanelPartial, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.StormScanner.class, covPanels, cPalette, down);

    // UP patterns
    l.addPattern(NoisePattern.class, covPanelPartial, cNonConforming, up);
    l.addPattern(OrganicPatternConfig.WavyEdges.class, covPanelPartial, cNonConforming, up);
    l.addPattern(SparklePattern.class, covBoth, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.Electric.class, covPanelPartial, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.SlitheringSnake.class, covPanelPartial, cPalette, up);
    l.addPattern(PBAudio1.class, covPanelPartial, cPalette, up);

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

    // add autopilot settings UI section
    new TEUserInterface.AutopilotUISection(ui, autopilotComponent).addToContainer(ui.leftPane.global);

    // precompile binaries for any new or changed shaders
    ShaderPrecompiler.rebuildCache();
       
    lx.engine.addTask(() -> {
      loadCLfile(lx);
    });
  }

  @Override
  public void draw() {
    // All handled by core LX engine, do not modify, method exists only so that Processing
    // will run a draw-loop.
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
      PApplet.main("titanicsend.app.TEApp", args);
    }
  }
  
	public void projectChanged(File file, Change change) {
		if (change == Change.TRY || change == Change.NEW) {
			// Clear for file open
			this.autopilotComponent.autopilotEnabledToggle.setValue(false);
		} 
	}

}
