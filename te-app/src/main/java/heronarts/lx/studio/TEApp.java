/**
 * Copyright 2020- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package heronarts.lx.studio;

import heronarts.glx.GLXWindow;
import heronarts.glx.event.GamepadEvent;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.pattern.texture.NoisePattern;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;
import org.lwjgl.system.Platform;
import studio.jkb.beyond.BeyondPlugin;
import studio.jkb.supermod.SuperMod;
import studio.jkb.supermod.UISuperMod;
import titanicsend.app.TEAutopilot;
import titanicsend.app.TEGlobalPatternControls;
import titanicsend.app.TEOscListener;
import titanicsend.app.TEUIControls;
import titanicsend.app.TEVirtualOverlays;
import titanicsend.app.autopilot.TEHistorian;
import titanicsend.app.autopilot.TEPatternLibrary;
import titanicsend.app.autopilot.TEPhrase;
import titanicsend.app.autopilot.TEShowKontrol;
import titanicsend.app.autopilot.TEUserInterface;
import titanicsend.app.autopilot.justin.AutoParameter;
import titanicsend.app.autopilot.justin.AutoParameter.Scale;
import titanicsend.app.autopilot.justin.Autopilot;
import titanicsend.app.autopilot.justin.AutopilotLibrary;
import titanicsend.app.dev.DevSwitch;
import titanicsend.app.dev.UIDevSwitch;
import titanicsend.app.director.Director;
import titanicsend.app.director.DirectorEffect;
import titanicsend.app.director.UIDirector;
import titanicsend.app.effectmgr.GlobalEffectManager;
import titanicsend.app.effectmgr.TEGlobalEffects;
import titanicsend.audio.AudioStemModulator;
import titanicsend.audio.AudioStems;
import titanicsend.audio.AudioStemsPlugin;
import titanicsend.color.ColorPaletteManager;
import titanicsend.color.TEGradientSource;
import titanicsend.dmx.DmxEngine;
import titanicsend.dmx.effect.BeaconStrobeEffect;
import titanicsend.dmx.pattern.BeaconDirectPattern;
import titanicsend.dmx.pattern.BeaconEasyPattern;
import titanicsend.dmx.pattern.BeaconEverythingPattern;
import titanicsend.dmx.pattern.BeaconGamePattern;
import titanicsend.dmx.pattern.BeaconStraightUpPattern;
import titanicsend.dmx.pattern.DjLightsDirectPattern;
import titanicsend.dmx.pattern.DjLightsEasyPattern;
import titanicsend.dmx.pattern.ExampleDmxTEPerformancePattern;
import titanicsend.effect.BasicShaderEffect;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.EdgeSieveEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.GlobalPatternControlEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;
import titanicsend.gamepad.GamepadEngine;
import titanicsend.lx.APC40Mk2;
import titanicsend.lx.APC40Mk2.UserButton;
import titanicsend.lx.DirectorAPCminiMk2;
import titanicsend.lx.EffectsMiniLab3;
import titanicsend.model.TEWholeModel;
import titanicsend.model.TEWholeModelDynamic;
import titanicsend.modulator.dmx.Dmx16bitModulator;
import titanicsend.modulator.dmx.DmxDirectorColorModulator;
import titanicsend.modulator.dmx.DmxDualRangeModulator;
import titanicsend.modulator.dmx.DmxGridModulator;
import titanicsend.modulator.dmx.DmxRangeModulator;
import titanicsend.modulator.justin.MultiplierModulator;
import titanicsend.modulator.justin.UIMultiplierModulator;
import titanicsend.modulator.outputOsc.OutputOscColorModulator;
import titanicsend.modulator.outputOsc.OutputOscFloatModulator;
import titanicsend.modulator.outputOsc.OutputOscTempoModulator;
import titanicsend.ndi.NDIEngine;
import titanicsend.ndi.NDIOutFixture;
import titanicsend.ndi.NDIOutRawEffect;
import titanicsend.ndi.NDIOutShaderEffect;
import titanicsend.ndi.NDIReceiverPattern;
import titanicsend.osc.CrutchOSC;
import titanicsend.oscremapper.OscRemapperPlugin;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.ben.Audio1;
import titanicsend.pattern.ben.BassLightning;
import titanicsend.pattern.ben.Xorcery;
import titanicsend.pattern.ben.XorceryDiamonds;
import titanicsend.pattern.cesar.HandTracker;
import titanicsend.pattern.cnk.DotPolka;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.ShaderPatternClassFactory;
import titanicsend.pattern.glengine.ShaderPrecompiler;
import titanicsend.pattern.jeff.ArtStandards;
import titanicsend.pattern.jeff.BasicRainbowPattern;
import titanicsend.pattern.jeff.BassReactive;
import titanicsend.pattern.jeff.BassReactiveEdge;
import titanicsend.pattern.jeff.EdgeProgressions;
import titanicsend.pattern.jeff.EdgeSymmetry;
import titanicsend.pattern.jeff.SignalDebugger;
import titanicsend.pattern.jeff.Smoke;
import titanicsend.pattern.jeff.TempoReactiveEdge;
import titanicsend.pattern.jon.ArcEdges;
import titanicsend.pattern.jon.EdgeFall;
import titanicsend.pattern.jon.EdgeKITT;
import titanicsend.pattern.jon.Electric;
import titanicsend.pattern.jon.ElectricEdges;
import titanicsend.pattern.jon.FireEdges;
import titanicsend.pattern.jon.Fireflies;
import titanicsend.pattern.jon.FollowThatStar;
import titanicsend.pattern.jon.FourStar;
import titanicsend.pattern.jon.FxDualWave;
import titanicsend.pattern.jon.FxLaserCharge;
import titanicsend.pattern.jon.Kaleidosonic;
import titanicsend.pattern.jon.ModelFileWriter;
import titanicsend.pattern.jon.MultipassDemo;
import titanicsend.pattern.jon.Phasers;
import titanicsend.pattern.jon.RadialSimplex;
import titanicsend.pattern.jon.RainBands;
import titanicsend.pattern.jon.SimplexPosterized;
import titanicsend.pattern.jon.SpaceExplosionFX;
import titanicsend.pattern.jon.SpiralDiamonds;
import titanicsend.pattern.jon.StarSwarm;
import titanicsend.pattern.jon.TESparklePattern;
import titanicsend.pattern.jon.TriangleNoise;
import titanicsend.pattern.jon.TurbulenceLines;
import titanicsend.pattern.justin.DmxGridPattern;
import titanicsend.pattern.justin.GammaTestPattern;
import titanicsend.pattern.justin.MothershipDrivingPattern;
import titanicsend.pattern.justin.TEGradientPattern;
import titanicsend.pattern.justin.TESolidPattern;
import titanicsend.pattern.justin.TwoColorPattern;
import titanicsend.pattern.look.PolySpiral;
import titanicsend.pattern.look.SigmoidDanceAudioLevels;
import titanicsend.pattern.look.SigmoidDanceAudioWaveform;
import titanicsend.pattern.look.SketchDemo;
import titanicsend.pattern.look.SketchStem;
import titanicsend.pattern.look.TriangleCrossAudioWaveform;
import titanicsend.pattern.look.TriangleInfinityRadialWaveform;
import titanicsend.pattern.mike.Checkers;
import titanicsend.pattern.mike.EdgeRunner;
import titanicsend.pattern.mike.ModelDebugger;
import titanicsend.pattern.piemonte.Afterglow;
import titanicsend.pattern.piemonte.CandyFlip;
import titanicsend.pattern.piemonte.EdgeGlitch;
import titanicsend.pattern.piemonte.FaceMelt;
import titanicsend.pattern.piemonte.IceGlint;
import titanicsend.pattern.piemonte.SpecialKube;
import titanicsend.pattern.pixelblaze.PBAudio1;
import titanicsend.pattern.pixelblaze.PBFireworkNova;
import titanicsend.pattern.pixelblaze.PBXorcery;
import titanicsend.pattern.pixelblaze.PixelblazeParallel;
import titanicsend.pattern.pixelblaze.PixelblazeSandbox;
import titanicsend.pattern.selina.HappyChibi;
import titanicsend.pattern.sinas.LightBeamsAudioReactivePattern;
import titanicsend.pattern.sinas.TdNdiPattern;
import titanicsend.pattern.sinas.TdStableDiffusionPattern;
import titanicsend.pattern.tom.BouncingDots;
import titanicsend.pattern.tom.Fire;
import titanicsend.pattern.tom.PulsingTriangles;
import titanicsend.pattern.util.PanelDebugPattern;
import titanicsend.pattern.util.TargetPixelStamper;
import titanicsend.pattern.yoffa.config.OrganicPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderEdgesPatternConfig;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.pattern.yoffa.effect.BeaconEffect;
import titanicsend.preset.PresetEngine;
import titanicsend.preset.UIUserPresetManager;
import titanicsend.preset.UIUserPresetSelector;
import titanicsend.ui.UI3DManager;
import titanicsend.ui.UITEColorControl;
import titanicsend.ui.UITEPerformancePattern;
import titanicsend.ui.color.UIColorPaletteManagerSection;
import titanicsend.ui.effect.UIRandomStrobeEffect;
import titanicsend.ui.modulator.UIDmx16bitModulator;
import titanicsend.ui.modulator.UIDmxDualRangeModulator;
import titanicsend.ui.modulator.UIDmxGridModulator;
import titanicsend.ui.modulator.UIDmxRangeModulator;
import titanicsend.util.MissingControlsManager;
import titanicsend.util.TE;

public class TEApp extends LXStudio {

  static {
    // Ensure that AWT is only used in headless mode
    System.setProperty("java.awt.headless", "true");
  }

  public static TEWholeModel wholeModel;

  private static int WINDOW_WIDTH = 1280;
  private static int WINDOW_HEIGHT = 800;

  private static final String FLAG_HEADLESS = "--headless";
  private static final String FLAG_OPENGL = "--opengl";
  private static final String FLAG_WARNINGS = "--warnings";
  private static final String FLAG_DEBUG = "--debug";
  private static final String FLAG_DISABLE_ZEROCONF = "--disable-zeroconf";
  private static final String FLAG_FORCE_OUTPUT = "--force-output";
  private static final String FLAG_DISABLE_OUTPUT = "--disable-output";

  // Default shader system rendering canvas
  // resolution.  Determines the maximum number of
  // model points allowed. May be changed via the startup
  // command line argument --resolution=WIDTHxHEIGHT
  // (Default allows roughly 102,000 points.)
  public static int glRenderWidth = 480;
  public static int glRenderHeight = 480;

  public static GamepadEngine gamepadEngine;

  @LXPlugin.Name("Titanic's End")
  public static class Plugin implements LXStudio.Plugin, LX.Listener, LX.ProjectListener {

    private final TEVirtualOverlays virtualOverlays;

    //    private GigglePixelListener gpListener;
    //    private GigglePixelBroadcaster gpBroadcaster;

    private TEAutopilot autopilot;
    private TEOscListener oscListener;
    private Autopilot autopilotJKB;

    private final DmxEngine dmxEngine;
    private final NDIEngine ndiEngine;
    private final GLEngine glEngine;
    private final SuperMod superMod;
    private final OscRemapperPlugin oscRemapperPlugin;

    private final ColorPaletteManager paletteManagerA;
    private final ColorPaletteManager paletteManagerB;
    private final CrutchOSC crutchOSC;
    private DevSwitch devSwitch;
    private final Director director;
    private final PresetEngine presetEngine;
    private final GlobalEffectManager effectManager;

    // objects that manage UI displayed in 3D views
    private UI3DManager ui3dManager;

    private LX lx;

    public Plugin(LX lx) {
      log("TEApp.Plugin(LX)");
      this.lx = lx;
      lx.addListener(this);
      lx.addProjectListener(this);

      wholeModel = new TEWholeModelDynamic(lx);

      // Saved options for UI overlays
      lx.engine.registerComponent(
          "virtualOverlays", this.virtualOverlays = new TEVirtualOverlays(lx));

      // set up global control values object so all patterns can potentially be controlled
      // by a single external input (e.g. DMX controller )
      lx.engine.registerComponent("globalPatternControls", new TEGlobalPatternControls(lx));

      this.dmxEngine = new DmxEngine(lx);
      this.ndiEngine = new NDIEngine(lx);
      this.glEngine = new GLEngine(lx, glRenderWidth, glRenderHeight);
      gamepadEngine = new GamepadEngine(lx);
      this.presetEngine = new PresetEngine(lx);
      this.presetEngine.openFile(lx.getMediaFile("Presets/UserPresets/BM24.userPresets"));
      this.effectManager = new GlobalEffectManager(lx);
      lx.engine.registerComponent("effectManager", this.effectManager);
      TEGlobalEffects.allocateSlots();

      // Super Modulator midi controller
      this.superMod = new SuperMod(lx);

      // OscRemapper plugin
      this.oscRemapperPlugin = new OscRemapperPlugin(lx, Path.of("resources", "osc_remapper.yaml"));

      lx.engine.registerComponent(
          "paletteManagerA", this.paletteManagerA = new ColorPaletteManager(lx));
      if (UIColorPaletteManagerSection.DISPLAY_TWO_MANAGED_SWATCHES) {
        lx.engine.registerComponent(
            "paletteManagerB", this.paletteManagerB = new ColorPaletteManager(lx, "SWATCH B", 1));
      } else {
        this.paletteManagerB = null;
      }

      new TEGradientSource(lx);

      // Initialize Resolume gradient publisher (logs palette color changes for now)
      lx.engine.registerComponent(
          "resolumePalette", new titanicsend.osc.TEResolumeGradientPublisher(lx));

      // JKB Autopilot
      // lx.engine.registerComponent("autopilot", this.autopilotJKB = new AutopilotExample(lx));
      // initializeAutopilotLibraryJKB();

      // Load metadata about unused controls per-pattern into a singleton that patterns will
      // reference later
      MissingControlsManager.get();

      // CrutchOSC is an LXOscEngine supplement for TouchOSC clients
      lx.engine.registerComponent("focus", this.crutchOSC = new CrutchOSC(lx));

      this.director = new Director(lx);
    }

    public void initialize(LX lx) {
      // Here is where you should register any custom components or make modifications
      // to the LX engine or hierarchy. This is also used in headless mode, so note that
      // you cannot assume you are working with an LXStudio class or that any UI will be
      // available.

      log("TEApp.Plugin.initialize()");

      // Register child plugin components
      AudioStemsPlugin.registerComponents(lx);
      BeyondPlugin.registerComponents(lx);

      // Patterns/effects that currently conform to art direction standards
      lx.registry.addPattern(EdgeProgressions.class);
      lx.registry.addPattern(EdgeSymmetry.class);
      lx.registry.addPattern(Smoke.class);

      // Patterns that are in development towards meeting standards
      lx.registry.addPattern(ArcEdges.class);
      lx.registry.addPattern(BassLightning.class);
      lx.registry.addPattern(BouncingDots.class);
      lx.registry.addPattern(FxLaserCharge.class);
      lx.registry.addPattern(Checkers.class);
      lx.registry.addPattern(EdgeFall.class);
      lx.registry.addPattern(EdgeKITT.class);
      lx.registry.addPattern(EdgeRunner.class);
      lx.registry.addPattern(Electric.class);
      lx.registry.addPattern(ElectricEdges.class);
      lx.registry.addPattern(FireEdges.class);
      lx.registry.addPattern(Fireflies.class);
      lx.registry.addPattern(FollowThatStar.class);
      lx.registry.addPattern(FourStar.class);
      lx.registry.addPattern(FxDualWave.class);
      lx.registry.addPattern(Kaleidosonic.class);
      lx.registry.addPattern(MultipassDemo.class);
      lx.registry.addPattern(NDIReceiverPattern.class);
      lx.registry.addPattern(StarSwarm.class);
      lx.registry.addPattern(TdNdiPattern.class);
      lx.registry.addPattern(TdStableDiffusionPattern.class);
      lx.registry.addPattern(ModelFileWriter.class);
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
      lx.registry.addPattern(RainBands.class);
      lx.registry.addPattern(SimplexPosterized.class);
      lx.registry.addPattern(SpaceExplosionFX.class);
      lx.registry.addPattern(SpiralDiamonds.class);
      lx.registry.addPattern(TEMidiFighter64DriverPattern.class);
      lx.registry.addPattern(TESparklePattern.class);
      lx.registry.addPattern(TurbulenceLines.class);
      lx.registry.addPattern(TriangleNoise.class);
      lx.registry.addPattern(PulsingTriangles.class);
      lx.registry.addPattern(Fire.class);
      lx.registry.addPattern(TESolidPattern.class);
      lx.registry.addPattern(TEGradientPattern.class);
      lx.registry.addPattern(IceGlint.class);
      lx.registry.addPattern(FaceMelt.class);
      lx.registry.addPattern(CandyFlip.class);
      lx.registry.addPattern(Afterglow.class);
      lx.registry.addPattern(EdgeGlitch.class);
      lx.registry.addPattern(SpecialKube.class);
      lx.registry.addPattern(HappyChibi.class);
      lx.registry.addPattern(DotPolka.class);
      lx.registry.addPattern(PolySpiral.class);
      lx.registry.addPattern(SigmoidDanceAudioWaveform.class);
      lx.registry.addPattern(SigmoidDanceAudioLevels.class);
      lx.registry.addPattern(TriangleCrossAudioWaveform.class);
      lx.registry.addPattern(TriangleInfinityRadialWaveform.class);
      lx.registry.addPattern(SketchDemo.class);
      lx.registry.addPattern(SketchStem.class);

      // Examples for teaching and on-boarding developers
      lx.registry.addPattern(BasicRainbowPattern.class);
      lx.registry.addPattern(BassReactive.class);
      lx.registry.addPattern(BassReactiveEdge.class);
      lx.registry.addPattern(TempoReactiveEdge.class);
      lx.registry.addPattern(ArtStandards.class);

      // DMX patterns
      lx.registry.addPattern(BeaconDirectPattern.class);
      lx.registry.addPattern(BeaconEasyPattern.class);
      lx.registry.addPattern(BeaconEverythingPattern.class);
      lx.registry.addPattern(BeaconGamePattern.class);
      lx.registry.addPattern(BeaconStraightUpPattern.class);
      lx.registry.addPattern(DjLightsDirectPattern.class);
      lx.registry.addPattern(DjLightsEasyPattern.class);
      lx.registry.addPattern(ExampleDmxTEPerformancePattern.class);

      // Effects
      lx.registry.addEffect(BasicShaderEffect.class);
      lx.registry.addEffect(DirectorEffect.class);
      lx.registry.addEffect(DistortEffect.class);
      lx.registry.addEffect(EdgeSieveEffect.class);
      lx.registry.addEffect(ExplodeEffect.class);
      lx.registry.addEffect(SimplifyEffect.class);
      lx.registry.addEffect(SustainEffect.class);
      lx.registry.addEffect(RandomStrobeEffect.class);

      // Utility Effects
      lx.registry.addEffect(GlobalPatternControlEffect.class);
      lx.registry.addEffect(NDIOutRawEffect.class);
      lx.registry.addEffect(NDIOutShaderEffect.class);

      // DMX effects
      lx.registry.addEffect(BeaconStrobeEffect.class);
      lx.registry.addEffect(BeaconEffect.class);

      // Patterns for DMX input
      lx.registry.addPattern(DmxGridPattern.class);

      // Automatic shader pattern wrapper
      ShaderPatternClassFactory spf = new ShaderPatternClassFactory();
      spf.registerShaders(lx);

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
      // "ShaderToyPattern" in ShaderPanelsPatternConfig.java

      // Useful for test, but might turn the car black in performance
      // lx.registry.removePattern(PlanesPattern.class); // remove pattern added automatically by
      // LX.

      // Frame Rate Killers
      // lx.registry.addEffect(titanicsend.effect.Kaleidoscope.class);
      // "StarryOutrun" in OrganicPatternConfig.java

      @SuppressWarnings("unchecked")
      Function<Class<?>, Class<LXPattern>[]> patternGetter =
          (Class<?> patternConfigClass) ->
              (Class<LXPattern>[])
                  Arrays.stream(patternConfigClass.getDeclaredClasses())
                      .filter(LXPattern.class::isAssignableFrom)
                      .toArray(Class[]::new);

      // Patterns generated via ConstructedShaderPattern
      lx.registry.addPatterns(patternGetter.apply(OrganicPatternConfig.class));
      lx.registry.addPatterns(patternGetter.apply(ShaderPanelsPatternConfig.class));
      lx.registry.addPatterns(patternGetter.apply(ShaderEdgesPatternConfig.class));

      // Test/debug/utility patterns
      lx.registry.addPattern(ModelDebugger.class);
      // lx.registry.addPattern(ModuleEditor.class);
      lx.registry.addPattern(PanelDebugPattern.class);
      lx.registry.addPattern(SignalDebugger.class);
      lx.registry.addPattern(HandTracker.class);
      lx.registry.addPattern(TargetPixelStamper.class);
      // lx.registry.addPattern(ModelFileWriter.class);
      lx.registry.addPattern(TwoColorPattern.class);
      lx.registry.addPattern(MothershipDrivingPattern.class);
      lx.registry.addPattern(GammaTestPattern.class);

      // Midi surface names for use with BomeBox
      lx.engine.midi.registerSurface(APC40Mk2.class);
      // The Director midi surface must be registered *after* the Director and ColorPaletteManager
      lx.engine.midi.registerSurface(DirectorAPCminiMk2.class);
      lx.engine.midi.registerSurface(EffectsMiniLab3.class);
      // lx.engine.midi.registerSurface(MidiNames.BOMEBOX_MIDIFIGHTERTWISTER1,
      // MidiFighterTwister.class);
      // lx.engine.midi.registerSurface(MidiNames.BOMEBOX_MIDIFIGHTERTWISTER2,
      // MidiFighterTwister.class);
      // lx.engine.midi.registerSurface(MidiNames.BOMEBOX_MIDIFIGHTERTWISTER3,
      // MidiFighterTwister.class);
      // lx.engine.midi.registerSurface(MidiNames.BOMEBOX_MIDIFIGHTERTWISTER4,
      // MidiFighterTwister.class);

      // Fast edit: direct chain to SuperMod plugin
      this.superMod.initialize(lx);
      this.superMod.addModulatorSource(this.superModSource);

      // Initialize OscRemapper plugin
      this.oscRemapperPlugin.initialize(lx);

      // Custom modulators
      lx.registry.addModulator(Dmx16bitModulator.class);
      lx.registry.addModulator(DmxGridModulator.class);
      // Replaced by Chromatik version:
      // lx.registry.addModulator(DmxColorModulator.class);
      lx.registry.addModulator(DmxDirectorColorModulator.class);
      lx.registry.addModulator(DmxDualRangeModulator.class);
      lx.registry.addModulator(DmxRangeModulator.class);
      lx.registry.addModulator(MultiplierModulator.class);

      // Output modulators
      lx.registry.addModulator(OutputOscFloatModulator.class);
      lx.registry.addModulator(OutputOscTempoModulator.class);
      lx.registry.addModulator(OutputOscColorModulator.class);

      // NDI Fixture
      lx.registry.addFixture(NDIOutFixture.class);

      // Custom UI components
      if (lx instanceof LXStudio) {
        // UI: Effects
        ((LXStudio.Registry) lx.registry).addUIDeviceControls(UIRandomStrobeEffect.class);

        // UI: Modulators
        ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIDmx16bitModulator.class);
        ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIDmxGridModulator.class);
        // Replaced by Chromatik version:
        // ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIDmxColorModulator.class);
        ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIDmxDualRangeModulator.class);
        ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIDmxRangeModulator.class);
        ((LXStudio.Registry) lx.registry).addUIModulatorControls(UIMultiplierModulator.class);
      }

      //      int myGigglePixelID = 73; // Looks like "TE"
      //      try {
      //        this.gpListener = new GigglePixelListener(lx, "0.0.0.0", myGigglePixelID);
      //        lx.engine.addLoopTask(this.gpListener);
      //        TE.log("GigglePixel listener created");
      //      } catch (IOException e) {
      //        TE.log("Failed to create GigglePixel listener: " + e.getMessage());
      //      }

      //      // This should of course be in the config, but we leave for the playa in like a week
      //      String destIP = "192.168.42.255";
      //      try {
      //        this.gpBroadcaster =
      //            new GigglePixelBroadcaster(lx, destIP, wholeModel.getName(), myGigglePixelID);
      //        lx.engine.addLoopTask(this.gpBroadcaster);
      //        TE.log("GigglePixel broadcaster created");
      //      } catch (IOException e) {
      //        TE.log("Failed to create GigglePixel broadcaster: " + e.getMessage());
      //      }

      // create our library for autopilot
      TEPatternLibrary library = initializePatternLibrary(lx);

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
      TE.log("Attaching the OSC message listener to port " + TEShowKontrol.OSC_PORT + " ...");
      try {
        lx.engine
            .osc
            .receiver(TEShowKontrol.OSC_PORT)
            .addListener(
                (message) -> {
                  this.oscListener.onOscMessage(message);
                  lx.engine.osc.receiveActive.setValue(true);
                });
        lx.engine.osc.receiveActive.setValue(true);
      } catch (SocketException sx) {
        sx.printStackTrace();
      }

      //      GPOutput gpOutput = new GPOutput(lx, this.gpBroadcaster);
      //      lx.addOutput(gpOutput);

      // TEOscMessage.applyTEOscOutputSettings(lx);

      // Developer/Production Switch
      // Must be after everything else has initialized.
      this.devSwitch = new DevSwitch(lx);
    }

    private TEPatternLibrary initializePatternLibrary(LX lx) {
      // library that will hold metadata about TE patterns for autopilot
      // will not be used if autopilot is disabled
      TEPatternLibrary l = new TEPatternLibrary(lx);

      // aliases to reduce line count below...
      TEPatternLibrary.TEPatternCoverageType covEdges =
          TEPatternLibrary.TEPatternCoverageType.EDGES;
      TEPatternLibrary.TEPatternCoverageType covPanels =
          TEPatternLibrary.TEPatternCoverageType.PANELS;
      TEPatternLibrary.TEPatternCoverageType covPanelPartial =
          TEPatternLibrary.TEPatternCoverageType.PANELS_PARTIAL;
      TEPatternLibrary.TEPatternCoverageType covBoth = TEPatternLibrary.TEPatternCoverageType.BOTH;

      TEPatternLibrary.TEPatternColorCategoryType cPalette =
          TEPatternLibrary.TEPatternColorCategoryType.PALETTE;
      TEPatternLibrary.TEPatternColorCategoryType cWhite =
          TEPatternLibrary.TEPatternColorCategoryType.WHITE;
      TEPatternLibrary.TEPatternColorCategoryType cNonConforming =
          TEPatternLibrary.TEPatternColorCategoryType.NONCONFORMING;

      TEPhrase chorus = TEPhrase.CHORUS;
      TEPhrase down = TEPhrase.DOWN;
      TEPhrase up = TEPhrase.UP;

      // CHORUS patterns
      l.addPattern(NoisePattern.class, covBoth, cPalette, chorus);
      l.addPattern(PBXorcery.class, covPanelPartial, cPalette, chorus);
      l.addPattern(
          ShaderPanelsPatternConfig.NeonBlocks.class, covPanelPartial, cNonConforming, chorus);
      l.addPattern(Audio1.class, covPanelPartial, cPalette, chorus);
      l.addPattern(ShaderPanelsPatternConfig.OutrunGrid.class, covPanels, cNonConforming, chorus);
      l.addPattern(OrganicPatternConfig.OldMatrixScroller.class, covPanels, cNonConforming, chorus);
      l.addPattern(StarSwarm.class, covBoth, cPalette, chorus);
      l.addPattern(ShaderPanelsPatternConfig.Marbling.class, covPanels, cNonConforming, chorus);
      l.addPattern(OrganicPatternConfig.NeonCellsLegacy.class, covPanelPartial, cPalette, chorus);
      l.addPattern(
          ShaderPanelsPatternConfig.NeonTriangles.class, covPanels, cNonConforming, chorus);
      l.addPattern(Phasers.class, covPanels, cPalette, chorus);
      l.addPattern(
          ShaderPanelsPatternConfig.PulsingPetriDish.class, covPanels, cNonConforming, chorus);
      l.addPattern(Electric.class, covPanelPartial, cPalette, chorus);
      l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covBoth, cNonConforming, chorus);
      l.addPattern(EdgeRunner.class, covEdges, cPalette, chorus);
      l.addPattern(ShaderPanelsPatternConfig.AudioTest2.class, covPanelPartial, cPalette, chorus);
      l.addPattern(
          ShaderPanelsPatternConfig.MetallicWaves.class, covPanelPartial, cPalette, chorus);
      l.addPattern(
          ShaderEdgesPatternConfig.NeonRipplesEdges.class, covPanelPartial, cPalette, chorus);
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
      l.addPattern(LightBeamsAudioReactivePattern.class, covPanelPartial, cPalette, up);

      return l;
    }

    public static final double SLOWMIN = 30;
    public static final double SLOWMAX = 90;

    public void initializeAutopilotLibraryJKB() {
      AutopilotLibrary library = this.autopilotJKB.library;

      // Tell autopilot which parameters to animate

      // Gradient
      library
          .addPattern(heronarts.lx.pattern.color.GradientPattern.class)
          .addParameter(new AutoParameter("gradient", AutoParameter.Scale.ABSOLUTE, .75, 1))
          .addParameter(
              new AutoParameter("xAmount", AutoParameter.Scale.ABSOLUTE, .7, 1, SLOWMIN, SLOWMAX))
          .addParameter(
              new AutoParameter("rotate", AutoParameter.Scale.ABSOLUTE, 1, 1, 0)) // force rotate on
          .addParameter(new AutoParameter("yaw", AutoParameter.Scale.ABSOLUTE, 0, 360, 120));

      // Noise is both a color and pattern
      library
          .addPattern(NoisePattern.class)
          .addParameter(new AutoParameter("scale", Scale.ABSOLUTE, 22, 80, 0)) // Randomize 22-80
          .addParameter(new AutoParameter("midpoint", Scale.ABSOLUTE, 20, 80, 40))
          .addParameter(new AutoParameter("xScale", Scale.NORMALIZED, .25, .75, .1))
          .addParameter(new AutoParameter("yScale", Scale.NORMALIZED, 0, 1, .2))
          .addParameter(new AutoParameter("contrast", Scale.ABSOLUTE, 100, 400, 100))
          .addParameter(new AutoParameter("motionSpeed", Scale.ABSOLUTE, .6, .9, .1))
          .addParameter(new AutoParameter("xMotion", Scale.NORMALIZED, 0, 1, .5))
          .addParameter(new AutoParameter("yMotion", Scale.NORMALIZED, 0, 1, .5));

      // Chase
      library
          .addPattern(heronarts.lx.pattern.strip.ChasePattern.class)
          .addParameter(new AutoParameter("speed", Scale.ABSOLUTE, -20, 40, 20))
          .addParameter(new AutoParameter("size", Scale.ABSOLUTE, 20, 70, 40))
          .addParameter(new AutoParameter("fade", Scale.ABSOLUTE, 20, 60, 20))
          .addParameter(new AutoParameter("chunkSize", Scale.ABSOLUTE, 20, 80, 0))
          .addParameter(new AutoParameter("shift", Scale.ABSOLUTE, 0, 70, 14));

      // Chevron
      library
          .addPattern(heronarts.lx.pattern.form.ChevronPattern.class)
          .addParameter(new AutoParameter("speed", Scale.ABSOLUTE, 45, 75, 20))
          .addParameter(new AutoParameter("xAmt", Scale.ABSOLUTE, 0, 1, .2))
          .addParameter(new AutoParameter("zAmt", Scale.ABSOLUTE, 0, 1, SLOWMIN, SLOWMAX, .5))
          .addParameter(new AutoParameter("sharp", Scale.ABSOLUTE, 1.7, 30, SLOWMIN, SLOWMAX, 20))
          .addParameter(new AutoParameter("stripes", Scale.ABSOLUTE, 1, 4, SLOWMIN, SLOWMAX, 1.75))
          .addParameter(new AutoParameter("yaw", Scale.ABSOLUTE, 0, 360, SLOWMIN, SLOWMAX, 120))
          .addParameter(new AutoParameter("pitch", Scale.ABSOLUTE, 0, 180, SLOWMIN, SLOWMAX));

      // Life (deprecated)
      library
          .addPattern(heronarts.lx.pattern.texture.LifePattern.class)
          .addParameter(new AutoParameter("translateX", Scale.ABSOLUTE, -.7, .7, .9))
          .addParameter(new AutoParameter("yaw", Scale.ABSOLUTE, 0, 360, 180))
          .addParameter(
              new AutoParameter("translateY", Scale.ABSOLUTE, -.4, .7, SLOWMIN, SLOWMAX, .2))
          .addParameter(new AutoParameter("expand", Scale.ABSOLUTE, 1, 3, 1))
          .addParameter(new AutoParameter("pitch", Scale.ABSOLUTE, 175, 211, SLOWMIN, SLOWMAX, 20));

      // Orbox
      library
          .addPattern(heronarts.lx.pattern.form.OrboxPattern.class)
          .addParameter(new AutoParameter("shapeLerp", Scale.NORMALIZED, 0, 1, .5))
          .addParameter(new AutoParameter("fill", Scale.NORMALIZED, 0, .5, .3))
          .addParameter(new AutoParameter("radius", Scale.ABSOLUTE, 0, 100, 50))
          .addParameter(new AutoParameter("width", Scale.ABSOLUTE, 0, 8, 4))
          .addParameter(new AutoParameter("fade", Scale.ABSOLUTE, 5, 100, 30))
          .addParameter(new AutoParameter("xAmt", Scale.ABSOLUTE, 40, 90, 25))
          .addParameter(new AutoParameter("zAmt", Scale.ABSOLUTE, 40, 90, 25))
          .addParameter(new AutoParameter("yaw", Scale.ABSOLUTE, 0, 360, SLOWMIN, SLOWMAX, 180))
          .addParameter(new AutoParameter("shearY", Scale.NORMALIZED, 0, 1, SLOWMIN, SLOWMAX, .7));

      // Planes
      library
          .addPattern(heronarts.lx.pattern.form.PlanesPattern.class)
          .addParameter(
              new AutoParameter(
                  "layer/1/position",
                  Scale.NORMALIZED,
                  0,
                  1,
                  .4)) // Check to see if layered patterns work
      ;

      // Solid
      library
          .addPattern(heronarts.lx.pattern.color.SolidPattern.class)
          .addParameter(new AutoParameter("hue", Scale.NORMALIZED, 0, 1, .2))
          .addParameter(new AutoParameter("saturation", Scale.NORMALIZED, 0, 1, .3));

      // Sparkle
      library
          .addPattern(heronarts.lx.pattern.texture.SparklePattern.class)
          .addParameter(new AutoParameter("maxLevel", Scale.ABSOLUTE, 50, 100, 30))
          .addParameter(new AutoParameter("minLevel", Scale.ABSOLUTE, 42, 70, 20))
          .addParameter(new AutoParameter("density", Scale.ABSOLUTE, 10, 180, SLOWMIN, SLOWMAX, 50))
          .addParameter(new AutoParameter("sharp", Scale.ABSOLUTE, -.5, .5, .3));
    }

    private static final int SUPERMOD_STEM_COLUMN_START = 0;
    private static final int SUPERMOD_STEM_COLUMNS = 4;
    private static final int SUPERMOD_STEM_ROW_ENERGY = 0;
    private static final int SUPERMOD_STEM_ROW_WAVE = 1;

    /** Redirect some of the SuperMod buttons to Audio Stem modulators */
    private final studio.jkb.supermod.SuperMod.ModulatorSource superModSource =
        (label, col, row) -> {
          if (col >= SUPERMOD_STEM_COLUMN_START
              && col < (SUPERMOD_STEM_COLUMN_START + SUPERMOD_STEM_COLUMNS)
              && (row == SUPERMOD_STEM_ROW_ENERGY || row == SUPERMOD_STEM_ROW_WAVE)) {

            AudioStems.Stem stem = AudioStems.get().stems.get(col);
            AudioStemModulator m = new AudioStemModulator(label + " " + stem.label);
            m.stem.setValue(stem);
            if (row == SUPERMOD_STEM_ROW_ENERGY) {
              m.outputMode.setValue(AudioStemModulator.OutputMode.ENERGY);
            } else {
              m.outputMode.setValue(AudioStemModulator.OutputMode.WAVE);
            }
            return m;
          }

          // If button was not in target range, decline opportunity to build custom modulator
          return null;
        };

    /**
     * Here is where you may modify the initial settings of the UI before it is fully built. Note
     * that this will not be called in headless mode. Anything required for headless mode should go
     * in the raw initialize method above.
     */
    public void initializeUI(LXStudio lx, LXStudio.UI ui) {
      log("TEApp.Plugin.initializeUI()");

      LXStudio.Registry registry = (LXStudio.Registry) lx.registry;
      registry.addUIDeviceControls(UITEPerformancePattern.class);
      registry.addUIParameterControl(UIUserPresetSelector.class);
      registry.addUIParameterControl(UITEColorControl.class);

      this.superMod.initializeUI(lx, ui);
      this.oscRemapperPlugin.initializeUI(lx, ui);
    }

    /**
     * At this point, the LX Studio application UI has been built. You may now add additional views
     * and components to the UI hierarchy.
     */
    public void onUIReady(LXStudio lx, LXStudio.UI ui) {
      log("TEApp.Plugin.onUIReady()");

      // =======================================================================================
      // Custom UI - Design Mode
      // =======================================================================================

      // ------------
      // Content Pane
      // ------------

      UI2dContainer contentPane = ui.leftPane.content;
      float wContent = contentPane.getContentWidth();

      // Add UI section for User Presets
      new UIUserPresetManager(ui, lx, wContent).addToContainer(contentPane, 2);

      // Add SuperMod to the Content Pane
      UISuperMod uiSuperMod = new UISuperMod(ui, this.superMod, wContent);
      uiSuperMod.addToContainer(contentPane, 3);
      uiSuperMod.setExpanded(false);

      // Add UI section for autopilot
      TEUserInterface.AutopilotUISection uiAutopilot =
          new TEUserInterface.AutopilotUISection(ui, this.autopilot);
      uiAutopilot.addToContainer(contentPane, 4);
      uiAutopilot.setExpanded(false);

      // ------------
      // Model pane
      // ------------

      UI2dContainer modelPane = ui.leftPane.model;
      float modelPaneWidth = modelPane.getContentWidth();

      new UIDevSwitch(ui, this.devSwitch, modelPaneWidth).addToContainer(modelPane, 0);

      new TEUIControls(ui, this.virtualOverlays, modelPaneWidth).addToContainer(modelPane, 1);

      // ------------
      // Global pane
      // ------------

      UI2dContainer globalPane = ui.leftPane.global;
      float wGlobal = globalPane.getContentWidth();

      // 0. Add UI section for director
      new UIDirector(ui, this.director, wGlobal).addToContainer(globalPane, 0);

      // 1. Add Palette manager right below director (since they share a MIDI controller)
      UIColorPaletteManagerSection.addToLeftGlobalPane(
          ui, this.paletteManagerA, this.paletteManagerB);

      // 2. Chromatik Audio (default)

      // 3. AudioStems Plugin (added automatically when plugin initialized)
      // NOTE(look): somewhat inconvenient to reason about the order that plugins add UI. Maybe
      // easier for us
      //             to initialize AudioStems plugin directly in this file (this.audioStems = new
      // AudioStems())
      //             and handle UI setup, registerComponents, dispose() here.

      // =======================================================================================
      // Custom UI - Performance Mode
      // =======================================================================================

      // ------------
      // Right Tools
      // ------------

      UIColorPaletteManagerSection.addToRightPerformancePane(
          ui, this.paletteManagerA, this.paletteManagerB);

      // =======================================================================================
      // Custom UI - Preview
      // =======================================================================================

      // Add 3D Ui components
      this.ui3dManager = new UI3DManager(lx, ui, this.virtualOverlays);

      // Set camera zoom and point size to match current model
      applyTECameraPosition();

      // =======================================================================================
      // Non-UI Initialization Hooks
      // =======================================================================================

      // precompile binaries for any new or changed shaders
      ShaderPrecompiler.rebuildCache();

      // Import latest gamepad controllers db
      gamepadEngine.updateGamepadMappings();

      // Initialize OscRemapper plugin UI
      this.oscRemapperPlugin.onUIReady(lx, ui);
    }

    @Override
    public void projectChanged(File file, Change change) {
      if (change == Change.OPEN) {
        applyTECameraPosition();
      }
    }

    /**
     * Sets camera position and point size for the appropriate model. Static model requires manual
     * placement of the camera due to very large scale (1"=50000). Subsequently project files saved
     * with one model type and opened with the other need their camera position updated.
     */
    public void applyTECameraPosition() {
      if (this.lx instanceof LXStudio) {
        LXStudio.UI ui = ((LXStudio) this.lx).ui;
        double pointSize = ui.preview.pointCloud.pointSize.getValue();
        if (pointSize > 500) {
          // Camera position and point size for dynamic model (2024+)
          ui.preview.pointCloud.pointSize.reset();
          ui.preview.camera.radius.reset();
          ui.previewAux.camera.radius.reset();
        }
      }
    }

    @Override
    public void dispose() {
      log("TEApp.Plugin.dispose()");
      this.superMod.removeModulatorSource(this.superModSource);
      this.superMod.dispose();

      this.lx.removeListener(this);
      this.lx.removeProjectListener(this);

      this.devSwitch.dispose();
      this.director.dispose();
      this.dmxEngine.dispose();
      this.crutchOSC.dispose();
      this.glEngine.dispose();
      this.effectManager.dispose();
      gamepadEngine.dispose();

      ((TEWholeModelDynamic) wholeModel).dispose();
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    // Keyboard shortcut for debugging: Add all patterns to current channel
    // (Ctrl or Meta) + Alt + Shift + A
    if ((keyEvent.isControlDown() || keyEvent.isMetaDown())
        && keyEvent.isAltDown()
        && keyEvent.isShiftDown()
        && keyEvent.getKeyCode() == 65) {
      this.engine.addTask(this::addAllPatterns);
    } else {
      super.onKeyPressed(keyEvent, keyChar, keyCode);
    }
  }

  /** Dev tool: add all patterns in registry to current channel. */
  private void addAllPatterns() {
    LXBus channel = this.engine.mixer.getFocusedChannel();
    if (channel instanceof LXChannel) {
      TE.log(
          "*** Instantiating all %d patterns in registry to channel %s ***",
          this.registry.patterns.size(), channel.getLabel());
      TE.log("Here we gOOOOOOOOOOOO....");
      List<LXPattern> patterns = new ArrayList<LXPattern>();
      for (Class<? extends LXPattern> clazz : this.registry.patterns) {
        try {
          if (TEPerformancePattern.class.isAssignableFrom(clazz)) {
            patterns.add(instantiatePattern(clazz));
          }
        } catch (Exception ex) {
          TE.error(ex, "Broken pattern! Could not instantiate " + clazz);
        }
      }
      patterns.sort((p1, p2) -> p1.getLabel().compareTo(p2.getLabel()));
      for (LXPattern pattern : patterns) {
        try {
          ((LXChannel) channel).addPattern(pattern);
        } catch (Exception ex) {
          TE.error(ex, "Failure adding pattern to channel! ");
        }
      }
    } else {
      TE.error("Selected channel must be a channel and not a group before adding all patterns.");
    }
  }

  @Override
  protected void onGamepadButtonPressed(GamepadEvent gamepadEvent, int button) {
    gamepadEngine.lxGamepadButtonPressed(gamepadEvent, button);
  }

  @Override
  protected void onGamepadButtonReleased(GamepadEvent gamepadEvent, int button) {
    gamepadEngine.lxGamepadButtonReleased(gamepadEvent, button);
  }

  @Override
  protected void onGamepadAxisChanged(GamepadEvent gamepadEvent, int axis, float value) {
    gamepadEngine.lxGamepadAxisChanged(gamepadEvent, axis, value);
  }

  private TEApp(GLXWindow window, Flags flags) throws IOException {
    super(window, flags);
  }

  private static final DateFormat LOG_FILENAME_FORMAT =
      new SimpleDateFormat("'LXStudio-TE-'yyyy.MM.dd-HH.mm.ss'.log'");

  private static String projectFileName = null;

  /**
   * Main interface into the program.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    try {
      String logFileName = LOG_FILENAME_FORMAT.format(Calendar.getInstance().getTime());
      File logs = new File(LX.Media.LOGS.getDirName());
      if (!logs.exists()) {
        logs.mkdir();
      }
      setLogFile(new File(LX.Media.LOGS.getDirName(), logFileName));

      LX.log("Starting Chromatik version " + LXStudio.VERSION);
      LX.log(
          "Running java "
              + System.getProperty("java.version")
              + " "
              + System.getProperty("java.vendor")
              + " "
              + System.getProperty("os.name")
              + " "
              + System.getProperty("os.version")
              + " "
              + System.getProperty("os.arch"));
      LX.LOG_WARNINGS = true;

      Flags flags = new Flags();
      flags.windowTitle = "Chromatik — Titanic's End";
      flags.windowWidth = WINDOW_WIDTH;
      flags.windowHeight = WINDOW_HEIGHT;
      flags.zeroconf = false;
      flags.classpathPlugins.add(titanicsend.audio.AudioStemsPlugin.class.getTypeName());
      flags.classpathPlugins.add("heronarts.lx.studio.TEApp$Plugin");

      if (args.length > 0) {
        final StringBuilder argString = new StringBuilder(256);
        for (String arg : args) {
          argString.append(arg);
          argString.append(" ");
        }
        log("CLI args: " + argString.toString());
      }

      // Always use OpenGL back-end for BGFX on Linux'
      // NOTE: Consider changing this if Chromatik ever implements
      // full Vulkan support.
      if (Platform.get() == Platform.LINUX) {
        log("Forcing use of OpenGL on Linux");
        flags.useOpenGL = true;
      }

      boolean headless = false;
      File projectFile = null;
      for (int i = 0; i < args.length; ++i) {
        final String arg = args[i];
        if (FLAG_HEADLESS.equals(arg)) {
          headless = true;
        } else if (FLAG_OPENGL.equals(arg)) {
          flags.useOpenGL = true;
        } else if (FLAG_WARNINGS.equals(arg)) {
          LX.LOG_WARNINGS = true;
        } else if (FLAG_DEBUG.equals(arg)) {
          LX.LOG_DEBUG = true;
        } else if (FLAG_DISABLE_ZEROCONF.equals(arg)) {
          flags.zeroconf = false;
        } else if (FLAG_FORCE_OUTPUT.equals(arg)) {
          flags.outputMode = Flags.OutputMode.ACTIVE;
        } else if (FLAG_DISABLE_OUTPUT.equals(arg)) {
          flags.outputMode = Flags.OutputMode.INACTIVE;
        } else if (arg.endsWith(".lxp") || arg.endsWith(".lxs")) {
          try {
            projectFileName = arg;
            projectFile = new File(arg);
            LX.log("Received command-line project file name: " + projectFileName);
          } catch (Exception x) {
            LX.error(x, "Command-line project file path invalid: " + arg);
          }
        } else if (arg.equals("--resolution")) {
          // Parse shader rendering resolution from command line. Resolution is specified as a
          // string
          // in the format "WIDTHxHEIGHT" where WIDTH and HEIGHT are integers. (e.g. "640x480")
          if (i + 1 < args.length) {
            String[] resolution = args[i + 1].split("x");
            if (resolution.length == 2) {
              try {
                glRenderWidth = Integer.parseInt(resolution[0]);
                glRenderHeight = Integer.parseInt(resolution[1]);
                i++; // let the rest of the parser skip the resolution argument
              } catch (NumberFormatException nfx) {
                error("Invalid render resolution: " + args[i + 1]);
              }
              // test for out-of-bounds resolutions, just in case
              if (glRenderWidth < 64
                  || glRenderHeight < 64
                  || glRenderWidth > 4096
                  || glRenderHeight > 4096) {
                error("Invalid render resolution: " + args[i + 1]);
              }
            } else {
              error("Invalid render resolution: " + args[i + 1]);
            }
          } else {
            error("Missing render resolution");
          }
        } else {
          error("Unrecognized CLI argument, ignoring: " + arg);
        }
      }

      if (headless) {
        log("Headless CLI flag set, running without UI...");
        headless(flags, projectFile);
        return;
      }

      // Run the full windowed application
      final GLXWindow window = new GLXWindow(flags);
      final File finalProjectFile = projectFile;

      // Start the GLX Chromatik application on another thread
      new Thread(
              () -> {
                try {
                  applicationThread(window, flags, finalProjectFile);
                } catch (Throwable x) {
                  error(
                      x,
                      "Unhandled exception in Chromatik application thread: "
                          + x.getLocalizedMessage());
                }
              })
          .start();

      // Run the main event loop
      window.main();

    } catch (Throwable x) {
      error(x, "Unhandled exception in Chromatik.main: " + x.getLocalizedMessage());
    }
  }

  private static void applicationThread(GLXWindow window, Chromatik.Flags flags, File projectFile)
      throws IOException {
    final TEApp lx = new TEApp(window, flags);

    // Schedule a task to load the initial project file at launch
    final File finalProjectFile = projectFile;
    final boolean isSchedule = projectFile != null && projectFile.getName().endsWith(".lxs");
    lx.engine.addTask(
        () -> {
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
        });

    // Run the application UI!
    lx.run();
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
      }
    } else {
      error("No project or schedule file specified in headless mode, this will be uneventful...");
    }
    lx.engine.start();
  }
}
