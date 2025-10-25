package titanicsend.app.effectmgr;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.EdgeGlowShaderEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.HueOrbitShaderEffect;
import titanicsend.effect.KaleidoscopeShaderEffect;
import titanicsend.effect.PixelizationShaderEffect;
import titanicsend.effect.StrobeShaderEffect;
import titanicsend.effect.SustainEffect;
import titanicsend.pattern.ben.BassLightning;
import titanicsend.pattern.jon.FxLaserCharge;
import titanicsend.pattern.jon.TEControlTag;

/**
 * Register TE-specific slots for Global Effects. These provide static locations for effects
 * regardless of the order they are added to a project file, to keep the MIDI controller
 * knobs/buttons [on the MiniLab3] in consistent locations.
 */
public abstract class TEGlobalEffects {

  public static void allocateSlots() {
    final GlobalEffectManager manager = GlobalEffectManager.get();

    // Effect slots (pad + knobs)

    // 0 - StrobeShader
    manager.allocateSlot(
        new Slot<StrobeShaderEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.speed;
          }
        });

    // 1 - Explode
    manager.allocateSlot(
        new Slot<ExplodeEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.speed;
          }

          @Override
          protected TriggerParameter _getTriggerParameter() {
            return device.trigger;
          }
        });

    // 2 - Sustain
    manager.allocateSlot(
        new Slot<SustainEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.sustain;
          }
        });

    // 3 - Distort
    manager.allocateSlot(
        new Slot<DistortEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.size;
          }
        });

    // 4 - HueOrbit
    manager.allocateSlot(
        new Slot<HueOrbitShaderEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.angle;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.depth;
          }
        });

    // 5 - Kaleidoscope
    manager.allocateSlot(
        new Slot<KaleidoscopeShaderEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.mix;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.segments;
          }
        });

    // 6 - Pixelization
    manager.allocateSlot(
        new Slot<PixelizationShaderEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.size;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.separation;
          }
        });

    // 7 - EdgeGlow
    manager.allocateSlot(
        new Slot<EdgeGlowShaderEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.mix;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.glowStrength;
          }

          @Override
          protected BooleanParameter _getTriggerParameter() {
            return device.edgeOnly;
          }
        });

    // Trigger slots

    // 16 (9th key from right) - Explode trigger
    manager.allocateTriggerSlot(
        16,
        new Slot<ExplodeEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            // Optional visualization source; not required for trigger to work
            return device.depth;
          }

          @Override
          protected BooleanParameter _getTriggerParameter() {
            return device.trigger;
          }
        });

  }
}
