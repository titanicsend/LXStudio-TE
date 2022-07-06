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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPlugin;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.osc.LXOscListener;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.component.*;
import processing.core.PApplet;
import titanicsend.model.TEWholeModel;
import titanicsend.output.GPOutput;
import titanicsend.output.TEArtNetOutput;
import titanicsend.pattern.alex.*;
import titanicsend.pattern.cesar.*;
import titanicsend.pattern.jeff.*;
import titanicsend.pattern.mike.*;
import titanicsend.pattern.pixelblaze.*;
import titanicsend.pattern.tmc.*;
import titanicsend.pattern.tom.*;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.media.BasicImagePattern;
import titanicsend.pattern.yoffa.media.ReactiveHeartPattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;


public class TEApp extends PApplet implements LXPlugin  {
  private TEWholeModel model;

  private static int WIDTH = 1280;
  private static int HEIGHT = 800;
  private static boolean FULLSCREEN = false;
  private static String resourceSubdir;

  private static final int SHOWKONTROL_OSC_PORT_RX = 42069;

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

  public static class AutopilotComponent extends LXComponent implements LXOscComponent {
      public final BooleanParameter autopilotEnabledToggle =
              new BooleanParameter("Autopilot Enabled")
                      .setDescription("Toggle to turn on VJ autopilot mode")
                      .setValue(false);
      public AutopilotComponent(LX lx) {
          super(lx);
          addParameter("autopilotEnabledToggle", this.autopilotEnabledToggle);
      }
  }

  public AutopilotComponent autopilotComponent;

  @Override
  public void initialize(LX lx) {
    // Here is where you should register any custom components or make modifications
    // to the LX engine or hierarchy. This is also used in headless mode, so note that
    // you cannot assume you are working with an LXStudio class or that any UI will be
    // available.

    // Create an instance of our global autopilot component and register it with the LX engine
    // so that it can be saved and loaded in project files
    this.autopilotComponent = new AutopilotComponent(lx);
    lx.engine.registerComponent("autopilot", this.autopilotComponent);

    TEArtNetOutput.activateAll(lx, this.model.gapPoint.index);

    // Register custom pattern and effect types

    // Patterns/effects that currently conform to art direction standards
    lx.registry.addPattern(EdgeProgressions.class);
    lx.registry.addPattern(EdgeSymmetry.class);
    lx.registry.addPattern(Smoke.class);

    // Patterns that are in development towards meeting standards
    lx.registry.addPattern(AlternatingPattern.class);
    lx.registry.addPattern(BouncingDots.class);
    lx.registry.addPattern(BrightScreen.class);
    lx.registry.addPattern(ResizeableScreen.class);
    lx.registry.addPattern(Bubbles.class);
    lx.registry.addPattern(Checkers.class);
    lx.registry.addPattern(EdgeRunner.class);
    lx.registry.addPattern(ModelDebugger.class);
    lx.registry.addPattern(ModuleEditor.class);
    lx.registry.addPattern(PixelblazePattern.class);
    lx.registry.addPattern(PBAudio1.class);
    lx.registry.addPattern(PBXorcery.class);
    lx.registry.addPattern(PBFireworkNova.class);
    lx.registry.addPattern(PixelblazePatternParallel.class);
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



    int myGigglePixelID = 73;  // Looks like "TE"
    try {
      this.gpListener = new GigglePixelListener(lx, "0.0.0.0", myGigglePixelID);
      lx.engine.addLoopTask(this.gpListener);
      LX.log("GigglePixel listener created");
    } catch (IOException e) {
      LX.log("Failed to create GigglePixel listener: " + e.getMessage());
    }

    try {
      this.gpBroadcaster = new GigglePixelBroadcaster(
              lx, "255.255.255.255", this.model.name, myGigglePixelID);
      lx.engine.addLoopTask(this.gpBroadcaster);
      LX.log("GigglePixel broadcaster created");
    } catch (IOException e) {
      LX.log("Failed to create GigglePixel broadcaster: " + e.getMessage());
    }

    // create our Autopilot instance, run in general engine loop to
    // ensure performance under load
    autopilot = new TEAutopilot(lx);
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
    // includes a Autopilot ref to store (threadsafe) queue of unread OSC messages
    LX.log("Attaching the OSC message listener to port "
            + Integer.toString(SHOWKONTROL_OSC_PORT_RX) + " ...");
    try {
        lx.engine.osc.receiver(SHOWKONTROL_OSC_PORT_RX).addListener((message) -> {
            autopilot.onOscMessage(message);
      });
    } catch (SocketException sx) {
        sx.printStackTrace();
    }

    GPOutput gpOutput = new GPOutput(lx, this.gpBroadcaster);
    lx.addOutput(gpOutput);
  }

  public void initializeUI(LXStudio lx, LXStudio.UI ui) {
    // Here is where you may modify the initial settings of the UI before it is fully
    // built. Note that this will not be called in headless mode. Anything required
    // for headless mode should go in the raw initialize method above.
  }

  public static class AutopilotUIComponent extends UICollapsibleSection {
    public AutopilotUIComponent(LXStudio.UI ui, AutopilotComponent myComponent) {
      super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 80);
      setTitle("Autopilot: enable?");
      new UICheckbox(0, 0, myComponent.autopilotEnabledToggle).addToContainer(this);
    }
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
    new AutopilotUIComponent(ui, autopilotComponent).addToContainer(ui.leftPane.global);
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
