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

package heronarts.lx.studio;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.pattern.form.PlanesPattern;
import heronarts.lx.pattern.texture.NoisePattern;
import titanicsend.app.GigglePixelBroadcaster;
import titanicsend.app.GigglePixelListener;
import titanicsend.app.GigglePixelUI;
import titanicsend.app.TEAutopilot;
import titanicsend.app.TEOscListener;
import titanicsend.app.TEUIControls;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.app.autopilot.*;
import titanicsend.dmx.DmxEngine;
import titanicsend.dmx.pattern.ExampleDmxTEPerformancePattern;
import titanicsend.dmx.pattern.BeaconDirectPattern;
import titanicsend.dmx.pattern.BeaconEverythingPattern;
import titanicsend.dmx.pattern.DjLightsDirectPattern;
import titanicsend.dmx.pattern.DjLightsEasyPattern;
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
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.ben.*;
import titanicsend.pattern.cesar.HandTracker;
import titanicsend.pattern.jeff.*;
import titanicsend.pattern.jon.*;
import titanicsend.pattern.justin.*;
import titanicsend.pattern.look.*;
import titanicsend.pattern.mike.*;
import titanicsend.pattern.pixelblaze.*;
import titanicsend.pattern.tom.*;
import titanicsend.pattern.util.TargetPixelStamper;
import titanicsend.pattern.will.PowerDebugger;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.effect.BeaconEffect;
import titanicsend.ui.UIBackings;
import titanicsend.ui.UILasers;
import titanicsend.ui.UITEPerformancePattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.util.MissingControlsManager;
import titanicsend.util.TE;

public class TEApp extends LXStudio {

  static public TEWholeModel wholeModel;

  private static int WINDOW_WIDTH = 1280;
  private static int WINDOW_HEIGHT = 800;
  private static String resourceSubdir;

  // Global feature on/off switches for troubleshooting
  public static final boolean ENABLE_COLOR_CENTRAL = true;
  public static final boolean ENABLE_VIEW_CENTRAL = true;

  @LXPlugin.Name("Titanic's End")
  public static class Plugin implements LXStudio.Plugin, LX.Listener, LX.ProjectListener {

    private final TEVirtualOverlays virtualOverlays;

    private GigglePixelListener gpListener;
    private GigglePixelBroadcaster gpBroadcaster;

    private TEAutopilot autopilot;
    private TEOscListener oscListener;
    private TEPatternLibrary library;

    private final DmxEngine dmxEngine;
    private final TELaserTask laserTask;
    private final ColorCentral colorCentral;
    private final ViewCentral viewCentral;
    private final CrutchOSC crutchOSC;

    private LX lx;

    public Plugin(LX lx) {
      log("TEApp.Plugin(LX)");
      this.lx = lx;
      lx.addListener(this);
      lx.addProjectListener(this);

      // Saved options for UI overlays
      lx.engine.registerComponent("virtualOverlays", this.virtualOverlays = new TEVirtualOverlays(lx));

//      lx.ui.preview.addComponent(visual);
//      new TEUIControls(ui, visual, ui.leftPane.global.getContentWidth()).addToContainer(ui.leftPane.global);
      this.dmxEngine = new DmxEngine(lx);

      // create our loop task for outputting data to lasers
      this.laserTask = new TELaserTask(lx);
      lx.engine.addLoopTask(this.laserTask);

      // Add special per-channel swatch control. *Post-EDC note: this will get revised.
      this.colorCentral = new ColorCentral(lx);

      // Load metadata about unused controls per-pattern into a singleton that patterns will reference later
      MissingControlsManager.get();

      // Add special view controller
      this.viewCentral = new ViewCentral(lx);

      // CrutchOSC is an LXOscEngine supplement for TouchOSC clients
      lx.engine.registerComponent("focus", this.crutchOSC = new CrutchOSC(lx));
    }

    public void initialize(LX lx) {
      // Here is where you should register any custom components or make modifications
      // to the LX engine or hierarchy. This is also used in headless mode, so note that
      // you cannot assume you are working with an LXStudio class or that any UI will be
      // available.

      log("TEApp.Plugin.initialize()");

      GrandShlomoStation.activateAll(lx, wholeModel.getGapPointIndex());

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
      lx.registry.addPattern(SpaceExplosionFX.class);
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
      lx.registry.addPattern(SigmoidDanceAudioWaveform.class);
      lx.registry.addPattern(SigmoidDanceAudioLevels.class);
      lx.registry.addPattern(TriangleCrossAudioLevels.class);
      lx.registry.addPattern(TriangleCrossAudioWaveform.class);
      lx.registry.addPattern(TriangleInfinityLevels.class);
      lx.registry.addPattern(TriangleInfinityWaveform.class);
      lx.registry.addPattern(TriangleInfinityRadialWaveform.class);

      // Examples for teaching and on-boarding developers
      lx.registry.addPattern(BasicRainbowPattern.class);
      lx.registry.addPattern(BassReactive.class);
      lx.registry.addPattern(BassReactiveEdge.class);
      lx.registry.addPattern(TempoReactiveEdge.class);
      lx.registry.addPattern(ArtStandards.class);
      lx.registry.addEffect(titanicsend.effect.EdgeSieve.class);
      lx.registry.addEffect(titanicsend.effect.NoGapEffect.class);
      lx.registry.addEffect(titanicsend.effect.PanelAdjustEffect.class);
      lx.registry.addEffect(BeaconEffect.class);

      // DMX patterns
      lx.registry.addPattern(BeaconDirectPattern.class);
      lx.registry.addPattern(BeaconEverythingPattern.class);
      lx.registry.addPattern(DjLightsDirectPattern.class);
      lx.registry.addPattern(DjLightsEasyPattern.class);
      lx.registry.addPattern(ExampleDmxTEPerformancePattern.class);
 
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
      lx.registry.addPattern(TargetPixelStamper.class);

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
            lx, destIP, wholeModel.name, myGigglePixelID);
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
      log("TEApp.Plugin.initializeUI()");

      ((LXStudio.Registry)lx.registry).addUIDeviceControls(UITEPerformancePattern.class);
    }

    public void onUIReady(LXStudio lx, LXStudio.UI ui) {
      // At this point, the LX Studio application UI has been built. You may now add
      // additional views and components to the UI heirarchy.
      log("TEApp.Plugin.onUIReady()");

      // Model pane

      new TEUIControls(ui, this.virtualOverlays, ui.leftPane.model.getContentWidth()).addToContainer(ui.leftPane.model, 0);

      new GigglePixelUI(ui, ui.leftPane.model.getContentWidth(),
          this.gpListener, this.gpBroadcaster).addToContainer(ui.leftPane.model, 1);

      // Add UI section for all other general settings
      new TEUserInterface.TEUISection(ui, laserTask).addToContainer(ui.leftPane.model, 2);

      // Global pane

      // Add UI section for autopilot
      new TEUserInterface.AutopilotUISection(ui, this.autopilot).addToContainer(ui.leftPane.global, 0);

      applyTECameraPosition();

      // 3D components
      ui.preview.addComponent(new UIBackings(lx, this.virtualOverlays));
      ui.previewAux.addComponent(new UIBackings(lx, this.virtualOverlays));

      ui.preview.addComponent(new UILasers(lx, this.virtualOverlays));
      ui.previewAux.addComponent(new UILasers(lx, this.virtualOverlays));

      // precompile binaries for any new or changed shaders
      ShaderPrecompiler.rebuildCache();

      lx.engine.addTask(() -> {
        setOscDestinationForIpads();
        //openDelayedFile(lx);
        // Replace old saved destination IPs from project files
        //setOscDestinationForIpads();
      });
    }

    public void setOscDestinationForIpads() {
      try {
        this.lx.engine.osc.transmitHost.setValue(PangolinHost.HOSTNAME);
        this.lx.engine.osc.transmitPort.setValue(PangolinHost.PORT);
      } catch (Exception ex) {
        TE.err(ex, "Failed to set destination OSC address to ShowKontrol IP for iPads relay");
      }
    }

    @Override
    public void projectChanged(File file, Change change) {
      if (change == Change.OPEN) {
        applyTECameraPosition();
      }
    }

    public void applyTECameraPosition() {
      if (this.lx instanceof LXStudio) {
        LXStudio.UI ui = ((LXStudio)this.lx).ui;
        ui.preview.pointCloud.pointSize.setValue(80000);
        ui.preview.camera.theta.setValue(270);
        ui.preview.camera.phi.setValue(-6);
        ui.preview.camera.radius.setValue(17000000);
        ui.previewAux.camera.theta.setValue(270);
        ui.previewAux.camera.phi.setValue(-6);
        ui.previewAux.camera.radius.setValue(17000000);
      }
    }

    @Override
    public void dispose() {
      log("TEApp.Plugin.dispose()");
      this.lx.removeListener(this);
      this.lx.removeProjectListener(this);

      this.dmxEngine.dispose();
      this.colorCentral.dispose();
      this.crutchOSC.dispose();
      this.viewCentral.dispose();
    }
  }

  /* TODO: Move this from P4LX version to Chromatik version
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
  }*/

  /**
   * Dev tool: add all patterns in registry to current channel.
   */
  private void addAllPatterns() {
    LXBus channel = this.engine.mixer.getFocusedChannel();
    if (channel instanceof LXChannel) {
      TE.log("*** Instantiating all " + this.registry.patterns.size() + " patterns in registry to channel " + channel.getLabel() + " ***");
      TE.log("Here we gOOOOOOOOOOOO....");
      List<LXPattern> patterns = new ArrayList<LXPattern>();
      for (Class<? extends LXPattern> clazz : this.registry.patterns) {
        try {
          if (TEPerformancePattern.class.isAssignableFrom(clazz)) {
            patterns.add(instantiatePattern(clazz));
          }
        } catch (Exception ex) {
          TE.err(ex, "Broken pattern! Could not instantiate " + clazz);
        }
      }
      patterns.sort((p1, p2) -> p1.getLabel().compareTo(p2.getLabel()));
      for (LXPattern pattern : patterns) {
        try {
          ((LXChannel)channel).addPattern(pattern);
        } catch (Exception ex) {
          TE.err(ex, "Failure adding pattern to channel! ");
        }
      }
    } else {
      TE.err("Selected channel must be a channel and not a group before adding all patterns.");
    }
  }

  private TEApp(Flags flags, TEWholeModel model) throws IOException {
    super(flags, model);
  }

  private TEApp(Flags flags) throws IOException {
    super(flags);
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
    LX.log("Running java " +
        System.getProperty("java.version") + " " +
        System.getProperty("java.vendor") + " " +
        System.getProperty("os.name") + " " +
        System.getProperty("os.version") + " " +
        System.getProperty("os.arch")
        );

    LX.LOG_WARNINGS = true;

    Flags flags = new Flags();
    flags.windowTitle = "Chromatik — Titanic's End";
    flags.windowWidth = WINDOW_WIDTH;
    flags.windowHeight = WINDOW_HEIGHT;
    flags.zeroconf = false;
    flags.classpathPlugins.add("heronarts.lx.studio.TEApp$Plugin");
    //flags.useOpenGL = true;

    String logFileName = LOG_FILENAME_FORMAT.format(Calendar.getInstance().getTime());
    File logs = new File(LX.Media.LOGS.getDirName());
    if (!logs.exists()) {
      logs.mkdir();
    }
    setLogFile(new File(LX.Media.LOGS.getDirName(), logFileName));

    boolean headless = false;
    boolean loadTestahedron = false;
    boolean loadVehicle = true;
    File projectFile = null;
    for (int i = 0; i < args.length; ++i) {
      final String arg = args[i];
      if ("--headless".equals(arg)) {
        headless = true;
      } else if (arg.endsWith(".lxp") || arg.endsWith(".lxs")) {
        try {
          projectFileName = arg;
          projectFile = new File(arg);
          LX.log("Received command-line project file name: " + projectFileName);
        } catch (Exception x) {
          LX.error(x, "Command-line project file path invalid: " + arg);
        }
      } else if (arg.equals("testahedron")) {
        loadTestahedron = true;
        loadVehicle = false;
      } else if (arg.equals("vehicle")) {
        loadTestahedron = false;
        loadVehicle = true;
      } else {
        error("Unrecognized CLI argument, ignoring: " + arg);
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
      log("Headless CLI flag set, running without UI...");
      headless(flags, projectFile);
    } else {
      try {
        TEWholeModel model = new TEWholeModel(resourceSubdir);
        TEApp.wholeModel = model;

        TEApp lx = new TEApp(flags, model);

        // Schedule a task to load the initial project file at launch
        final File finalProjectFile = projectFile;
        final boolean isSchedule = (projectFile != null) ? projectFile.getName().endsWith(".lxs") : false;
        lx.engine.addTask(() -> {
          if (isSchedule) {
            lx.preferences.schedulerEnabled.setValue(true);
            LX.log("Opening schedule file: " + finalProjectFile);
            lx.scheduler.openSchedule(finalProjectFile, true);
          } else {
            try {
              lx.preferences.loadInitialProject(finalProjectFile);
            } catch (Exception x) {
              error(x, "Exception loading initial project: " + x.getLocalizedMessage());
            }
            lx.preferences.loadInitialSchedule();
          }
          if (flags.forceOutput) {
            lx.engine.output.enabled.setValue(true);
          }
        });

        lx.run();
      } catch (Exception x) {
        throw new RuntimeException(x);
      }
    }
  }

  public static void headless(Flags flags, File projectFile) {
    final LX lx = new LX(flags);
    if (projectFile != null) {
      boolean isSchedule = projectFile.getName().endsWith(".lxs");
      if (!projectFile.exists()) {
        error((isSchedule ? "Schedule" : "Project") + " file does not exist: " + projectFile);
      } else {
        if (isSchedule) {
          lx.preferences.schedulerEnabled.setValue(true);
          log("Opening schedule file: " + projectFile);
          lx.scheduler.openSchedule(projectFile, true);
        } else {
          log("Opening project file: " + projectFile);
          lx.openProject(projectFile);
        }
        if (flags.forceOutput) {
          lx.engine.output.enabled.setValue(true);
        }
      }
    } else {
      error("No project or schedule file specified in headless mode, this will be uneventful...");
    }
    lx.engine.start();
  }
}
