package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.TEPerformancePattern;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.LXUtils;

/**
 * Copyright 2020- Mark C. Slee, Heron Arts LLC
 * <p>
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 * <p>
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 * <p>
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 * <p>
 * This version:
 * adapted for use with Titanic's End live performance controls - 2023
 */

@LXCategory(LXCategory.TEXTURE)
public class TESparklePattern extends TEPerformancePattern {

    public class Engine {

        protected final static int MAX_SPARKLES = 1024;
        protected final static double MAX_DENSITY = 4;

        private class Sparkle {

            private boolean isOn;

            // Moves through 0-1 for sparkle lifecycle
            private double basis;

            // Random 0-1 constant used to control how much timescale variation is applied
            private double randomVar;

            // Random 0-1 constant used to control brightness of sparkle
            private double randomLevel;

            // How many pixels to output to
            private int activePixels;

            // Each individual sparkle maintains a list of output indices that it is applied to
            private int[] indexBuffer;

            private Sparkle() {
                this.isOn = false;
                this.basis = Math.random();
                this.randomVar = Math.random();
                this.randomLevel = Math.random();
            }

            private void rebuffer(LXModel model) {
                this.indexBuffer = new int[maxPixelsPerSparkle];
                reindex(model);
            }

            private void reindex(LXModel model) {
                // Choose a set of LED indices at random for this sparkle to point to
                for (int i = 0; i < this.indexBuffer.length; ++i) {
                    this.indexBuffer[i] = LXUtils.constrain((int) (Math.random() * model.size), 0, model.size - 1);
                }
            }
        }

        public final ObjectParameter<LXWaveshape> waveshape = new ObjectParameter<>("Wave", new LXWaveshape[]{
                LXWaveshape.TRI,
                LXWaveshape.SIN,
                LXWaveshape.UP,
                LXWaveshape.DOWN,
                LXWaveshape.SQUARE,
        });

        private final Sparkle[] sparkles = new Sparkle[MAX_SPARKLES];

        /**
         * Array of raw value output levels, matching the size of the model
         */
        public double[] outputLevels;

        private int numSparkles;
        private int maxPixelsPerSparkle;

        public final CompoundParameter minLevel = (CompoundParameter)
                new CompoundParameter("Min", 75, 0, 100)
                        .setUnits(CompoundParameter.Units.PERCENT)
                        .setDescription("Minimum brightness level, as a percentage of the maximum");

        public final CompoundParameter maxLevel = (CompoundParameter)
                new CompoundParameter("Max", 100, 0, 100)
                        .setUnits(CompoundParameter.Units.PERCENT)
                        .setDescription("Peak sparkle brightness level");

        public Engine(LXModel model) {
            setModel(model);
        }

        public void setModel(LXModel model) {
            // An output level for every pixel in the model
            this.outputLevels = new double[model.size];

            // Set a cap on the maximum number of sparkle generators
            this.numSparkles = LXUtils.min(model.size, MAX_SPARKLES);
            // There can be up to MAX_DENSITY times the size of the model sparkle destinations,
            // so each generator will address up to that many pixels
            this.maxPixelsPerSparkle = (int) Math.ceil(MAX_DENSITY * model.size / this.numSparkles);

            // Make sure we have enough sparkles allocated, and reindex them all against this model
            for (int i = 0; i < this.numSparkles; ++i) {
                if (this.sparkles[i] == null) {
                    this.sparkles[i] = new Sparkle();
                }
                this.sparkles[i].rebuffer(model);
            }
        }

        public void run(double deltaMs, LXModel model) {

            // Differs from original - speed control scales all speed-related parameters
            final double speed = Math.abs(getSpeed());
            final double minIntervalMs = 1000;
            final double maxIntervalMs = 1000;

            final double variation = 0.25;
            final double durationInv = 1;
            final double density = .01 * getQuantity();
            final double baseLevel = getWow1();

            LXWaveshape waveshape = this.waveshape.getObject();

            double maxLevel = this.maxLevel.getValue();
            double minLevel = maxLevel * .01 * this.minLevel.getValue();

            // Compute how much brightness sparkles can add to reach top level
            final double maxDelta = maxLevel - baseLevel;
            final double minDelta = minLevel - baseLevel;

            double shape = getWow2();
            if (shape >= 0) {
                shape = LXUtils.lerp(1, 3, shape);
            } else {
                shape = 1 / LXUtils.lerp(1, 3, -shape);
            }

            // Initialize all output levels to base level
            for (int i = 0; i < this.outputLevels.length; ++i) {
                this.outputLevels[i] = baseLevel;
            }

            // Run all the sparkles
            for (int i = 0; i < this.numSparkles; ++i) {
                final Sparkle sparkle = this.sparkles[i];
                double sparkleInterval = LXUtils.lerp(maxIntervalMs, minIntervalMs, LXUtils.constrain(speed + variation * (sparkle.randomVar - .5), 0, 1));
                sparkle.basis += (Math.abs(getDeltaMs())) / sparkleInterval;

                // Check if the sparkle has looped
                if (sparkle.basis > 1) {
                    sparkle.basis = sparkle.basis % 1.;

                    int desiredPixels = (int) (model.size * density);
                    float desiredPixelsPerSparkle = desiredPixels / (float) this.numSparkles;

                    if (desiredPixels < this.numSparkles) {
                        sparkle.activePixels = 1;
                        sparkle.isOn = Math.random() < desiredPixelsPerSparkle;
                    } else {
                        sparkle.isOn = true;
                        sparkle.activePixels = Math.round(desiredPixelsPerSparkle);
                    }

                    // Re-randomize this sparkle
                    if (sparkle.isOn) {
                        sparkle.randomVar = Math.random();
                        sparkle.randomLevel = Math.random();
                        sparkle.reindex(model);
                    }
                }

                // Process active sparkles
                if (sparkle.isOn) {
                    // The duration is a percentage 0-100% of the total period time for which the
                    // sparkle is active. Here we scale the sparkle's raw 0-1 basis onto this portion
                    // of duration, and only process the sparkle if it's still in the 0-1 range, e.g.
                    // if duration is 50% then durationInv = 2 and we'll be done after 0-0.5
                    double sBasis = sparkle.basis * durationInv;
                    if (sBasis < 1) {
                        // Compute and scale the sparkle's waveshape
                        double g = waveshape.compute(sBasis);
                        if (shape != 1) {
                            g = Math.pow(g, shape);
                        }

                        // Determine how much brightness to add for this sparkle
                        double maxSparkleDelta = LXUtils.lerp(minDelta, maxDelta, sparkle.randomLevel);
                        double sparkleAdd = maxSparkleDelta * g;

                        // Add the sparkle's brightness level to all output pixels
                        // Note that we cap the number of active pixels to the size of the
                        // index buffer to allow the running frame to finish successfully
                        // on view changes that greatly reduce the number of active pixels
                        int maxActive = Math.min(sparkle.activePixels, sparkle.indexBuffer.length);
                        for (int c = 0; c < maxActive; ++c) {
                            this.outputLevels[sparkle.indexBuffer[c]] += sparkleAdd;
                        }
                    }
                }
            }
        }
    }

    public final Engine engine = new Engine(model);

    public TESparklePattern(LX lx) {
        super(lx);

        // Sparkle rate - Note default is zero, but we start out
        // with speed at 0.5 because at zero nothing sparkles
        // and you get an all black car.  Yay!
        controls.setRange(TEControlTag.SPEED, 0, -2, 2)
                        .setValue(TEControlTag.SPEED, 0.5);


        // Sparkle density
        controls.setRange(TEControlTag.QUANTITY, 50, 0, 100 * Engine.MAX_DENSITY)
                .setExponent(TEControlTag.QUANTITY,2);

        // Base brightness level
        controls.setRange(TEControlTag.WOW1, 0, 0, 100);

        // Sharpness of sparkle curve
        controls.setRange(TEControlTag.WOW2, 0, -1, 1);

        // Waveshape (in Size control position)
        controls.setControl(TEControlTag.SIZE,engine.waveshape);

        // minlevel (in XPos control position)
        controls.setControl(TEControlTag.XPOS,engine.minLevel);

        // maxlevel (in YPos control position)
        controls.setControl(TEControlTag.YPOS,engine.maxLevel);

        addCommonControls();
    }

    @Override
    protected void onModelChanged(LXModel model) {
        engine.setModel(model);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        engine.run(deltaMs, model);
        int i = 0;
        int color = calcColor();
        for (LXPoint p : model.points) {
            colors[p.index] =
                    LXColor.multiply(color, LXColor.gray(LXUtils.clamp(engine.outputLevels[i++], 0, 100)));
        }
    }
}
