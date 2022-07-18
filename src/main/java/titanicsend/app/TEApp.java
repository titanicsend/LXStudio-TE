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

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPlugin;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.color.GradientPattern;
import heronarts.lx.pattern.texture.NoisePattern;
import heronarts.lx.pattern.texture.SparklePattern;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.component.*;
import processing.core.PApplet;
import titanicsend.app.autopilot.TEPatternLibrary;
import titanicsend.app.autopilot.TEPhrase;
import titanicsend.app.autopilot.TEUserInterface;
import titanicsend.model.TEWholeModel;
import titanicsend.output.GPOutput;
import titanicsend.output.TEArtNetOutput;
import titanicsend.pattern.alex.*;
import titanicsend.pattern.ben.BassLightning;
import titanicsend.pattern.cesar.*;
import titanicsend.pattern.jeff.*;
import titanicsend.pattern.jon.*;
import titanicsend.pattern.mike.*;
import titanicsend.pattern.pixelblaze.*;
import titanicsend.pattern.tmc.*;
import titanicsend.pattern.tom.*;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.media.BasicImagePattern;
import titanicsend.pattern.yoffa.media.ReactiveHeartPattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.app.autopilot.TEShowKontrol;
import titanicsend.util.TE;

public class TEApp extends PApplet implements LXPlugin  {
  private TEWholeModel model;

  private static int WIDTH = 1280;
  private static int HEIGHT = 800;
  private static boolean FULLSCREEN = false;
  private static String resourceSubdir;

  private GigglePixelListener gpListener;
  private GigglePixelBroadcaster gpBroadcaster;

  private TEAutopilot autopilot;

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

    new LXStudio(this, flags, this.model);
    this.surface.setTitle(this.model.name);
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

    TEArtNetOutput.activateAll(lx, this.model.gapPoint.index);

    // Patterns/effects that currently conform to art direction standards
    lx.registry.addPattern(EdgeProgressions.class);
    lx.registry.addPattern(EdgeSymmetry.class);
    lx.registry.addPattern(Smoke.class);

    // Patterns that are in development towards meeting standards
    lx.registry.addPattern(BassLightning.class);
    lx.registry.addPattern(BouncingDots.class);
    lx.registry.addPattern(Bubbles.class);
    lx.registry.addPattern(Checkers.class);
    lx.registry.addPattern(EdgeKITT.class);
    lx.registry.addPattern(EdgeRunner.class);
    lx.registry.addPattern(FollowThatStar.class);
    lx.registry.addPattern(Iceflow.class);
    lx.registry.addPattern(PixelblazeSandbox.class);
    lx.registry.addPattern(PBAudio1.class);
    lx.registry.addPattern(PBXorcery.class);
    lx.registry.addPattern(PBFireworkNova.class);
    lx.registry.addPattern(PixelblazeParallel.class);
    lx.registry.addPattern(SolidEdge.class);
    lx.registry.addPattern(SolidPanel.class);
    lx.registry.addPattern(PulsingTriangles.class);
    lx.registry.addPattern(HandTracker.class);
    lx.registry.addPattern(Fireflies.class);
    lx.registry.addPattern(Fire.class);

    // Patterns that will not aspire to art direction standards
    lx.registry.addPattern(BasicImagePattern.class);
    lx.registry.addPattern(ReactiveHeartPattern.class);

    // Examples for teaching and on-boarding developers
    lx.registry.addPattern(BasicRainbowPattern.class);
    lx.registry.addPattern(BassReactive.class);
    lx.registry.addPattern(BassReactiveEdge.class);
    lx.registry.addPattern(TempoReactiveEdge.class);
    lx.registry.addPattern(ArtStandards.class);
    lx.registry.addEffect(titanicsend.effect.BasicEffect.class);
    lx.registry.addEffect(titanicsend.effect.Kaleidoscope.class);
    lx.registry.addPatterns(OrganicPatternConfig.getPatterns());
    lx.registry.addPatterns(ShaderPanelsPatternConfig.getPatterns());
    lx.registry.addPatterns(ShaderEdgesPatternConfig.getPatterns());

    // create our library for autopilot
    TEPatternLibrary library = initializePatternLibrary(lx);

    int myGigglePixelID = 73;  // Looks like "TE"
    try {
      this.gpListener = new GigglePixelListener(lx, "0.0.0.0", myGigglePixelID);
      lx.engine.addLoopTask(this.gpListener);
      TE.log("GigglePixel listener created");
    } catch (IOException e) {
      TE.log("Failed to create GigglePixel listener: " + e.getMessage());
    }

    try {
      this.gpBroadcaster = new GigglePixelBroadcaster(
              lx, "255.255.255.255", this.model.name, myGigglePixelID);
      lx.engine.addLoopTask(this.gpBroadcaster);
      TE.log("GigglePixel broadcaster created");
    } catch (IOException e) {
      TE.log("Failed to create GigglePixel broadcaster: " + e.getMessage());
    }

    // create our Autopilot instance, run in general engine loop to
    // ensure performance under load
    autopilot = new TEAutopilot(lx, library);
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

    // TODO(will) go back to using built-in OSC listener for setBPM messages once:
    // 1. Mark merges his commit for utilizing the main OSC listener
    // 2. Mark adds protection on input checking for setBPM = 0.0 messages
    //    (https://github.com/heronarts/LX/blob/e3d0d11a7d61c73cd8dde0c877f50ea4a58a14ff/src/main/java/heronarts/lx/Tempo.java#L201)

    // add custom OSC listener to handle OSC messages from ShowKontrol
    // includes an Autopilot ref to store (threadsafe) queue of unread OSC messages
    TE.log("Attaching the OSC message listener to port "
            + Integer.toString(TEShowKontrol.OSC_PORT) + " ...");
    try {
        lx.engine.osc.receiver(TEShowKontrol.OSC_PORT).addListener((message) -> {
            autopilot.onOscMessage(message);
      });
    } catch (SocketException sx) {
        sx.printStackTrace();
    }

    GPOutput gpOutput = new GPOutput(lx, this.gpBroadcaster);
    lx.addOutput(gpOutput);
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
    l.addPattern(OrganicPatternConfig.RhythmicFlashStatic.class, covPanels, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.NeonBlocks.class, covPanelPartial, cNonConforming, chorus);
    l.addPattern(PBAudio1.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.Outrun.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.RainbowSwirlPanels.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.MatrixScroller.class, covPanels, cNonConforming, chorus);
    l.addPattern(FollowThatStar.class, covBoth, cPalette, chorus);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, chorus);
    l.addPattern(OrganicPatternConfig.NeonCellsLegacy.class, covPanelPartial, cPalette, chorus);
    l.addPattern(OrganicPatternConfig.RainbowSwirlEdges.class, covEdges, cNonConforming, chorus);
    l.addPattern(ShaderPanelsPatternConfig.NeonTriangles.class, covPanels, cNonConforming, chorus);
    l.addPattern(ShaderPanelsPatternConfig.PulsingHeart.class, covPanels, cNonConforming, chorus);
    //l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covPanels, cNonConforming, chorus); // only works with audio

    // DOWN patterns
    l.addPattern(GradientPattern.class, covBoth, cPalette, down);
    l.addPattern(Smoke.class, covBoth, cPalette, down);
    l.addPattern(OrganicPatternConfig.NeonSnake.class, covPanelPartial, cPalette, down);
    l.addPattern(OrganicPatternConfig.RainbowSwirlPanels.class, covPanels, cPalette, down);
    l.addPattern(OrganicPatternConfig.WaterEdges.class, covEdges, cPalette, down);
    l.addPattern(OrganicPatternConfig.WaterPanels.class, covPanelPartial, cPalette, down);
    l.addPattern(OrganicPatternConfig.WavyEdges.class, covEdges, cPalette, down);
    l.addPattern(NoisePattern.class, covBoth, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.Galaxy.class, covPanelPartial, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.SynthWaves.class, covPanelPartial, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.LightBeamsPattern.class, covPanelPartial, cPalette, down);
    l.addPattern(ShaderPanelsPatternConfig.NeonRipples.class, covPanels, cPalette, down);
    l.addPattern(ShaderEdgesPatternConfig.SynthWavesEdges.class, covEdges, cPalette, down);

    // UP patterns
    l.addPattern(NoisePattern.class, covPanelPartial, cNonConforming, down);
    l.addPattern(OrganicPatternConfig.WavyEdges.class, covPanelPartial, cNonConforming, down);
    l.addPattern(PBXorcery.class, covPanelPartial, cNonConforming, up);
    l.addPattern(OrganicPatternConfig.AlternatingDots.class, covPanelPartial, cNonConforming, up);
    l.addPattern(OrganicPatternConfig.BreathingDots.class, covPanelPartial, cNonConforming, up);
    l.addPattern(OrganicPatternConfig.NeonBarsEdges.class, covEdges, cPalette, up);
    l.addPattern(OrganicPatternConfig.BasicElectricEdges.class, covEdges, cNonConforming, up);
    l.addPattern(OrganicPatternConfig.PowerGrid.class, covEdges, cPalette, up);
    l.addPattern(SparklePattern.class, covBoth, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.Electric.class, covPanelPartial, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.JetStream.class, covPanels, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, up);
    l.addPattern(ShaderPanelsPatternConfig.SpaceExplosion.class, covPanels, cNonConforming, up);

    // misc patterns
    //l.addPattern(EdgeProgressions.class, covEdges, colorWhite, chorus);
    //l.addPattern(EdgeSymmetry.class, covEdges, colorWhite, chorus);
    //lx.registry.addPattern(PBFireworkNova.class); // would make great strobe / trigger...
    //lx.registry.addPattern(PulsingTriangles.class); // would make great strobe...
    //lx.registry.addPattern(Fireflies.class); // OK but kills FPS...
    //l.addPattern(ReactiveHeartPattern.class, covPanels, colorNonConforming, TEPhrase.CHORUS); // needs to not be on all panels, reactive
    //l.addPattern(BassLightning.class, covEdges, cWhite, chorus); // would be amazing right before a drop, on sound!!

    return l;
  }

  public void initializeUI(LXStudio lx, LXStudio.UI ui) {
    // Here is where you may modify the initial settings of the UI before it is fully
    // built. Note that this will not be called in headless mode. Anything required
    // for headless mode should go in the raw initialize method above.
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
  }

  @Override
  public void draw() {
    // All handled by core LX engine, do not modify, method exists only so that Processing
    // will run a draw-loop.
  }

  /**
   * Main interface into the program. Two modes are supported, if the --headless
   * flag is supplied then a raw CLI version of LX is used. If not, then we embed
   * in a Processing 4 applet and run as such.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    LX.log("Initializing LX version " + LXStudio.VERSION);
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

}
