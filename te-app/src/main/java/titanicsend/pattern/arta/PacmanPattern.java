package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.transform.LXVector;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class PacmanPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("PSize", 1.0f, 0.1f, 1.0f)
                    .setDescription("Size of the circle");
    
    public final CompoundParameter mouthSize =
            new CompoundParameter("MSize", 0.48f, 0.1f, 0.8f)
                    .setDescription("Size of Pacman's mouth");
    
    public final CompoundParameter mouthSpeed =
            new CompoundParameter("MSpeed", 1.55f, 0.1f, 10.0f)
                    .setDescription("Speed of mouth opening/closing animation");
    
    public final CompoundParameter mouthAnimation =
            new CompoundParameter("MAnimation", 1.0f, 0.0f, 1.0f)
                    .setDescription("Mouth animation amount (0=closed, 1=open)");
    
    public final BooleanParameter mouthMove =
            new BooleanParameter("MouthMove", true)
                    .setDescription("Enable mouth movement (off = mouth stays open)");
    
    public final BooleanParameter showEyes =
            new BooleanParameter("Eyes", true)
                    .setDescription("Show Pacman's eyes");
    
    public final CompoundParameter twist =
            new CompoundParameter("Twist", 0.0f, 0.0f, 360.0f)
                    .setDescription("Rotate the entire Pacman character");
    
    public final DiscreteParameter colorChoice =
            new DiscreteParameter("Color", 0, 0, 6)
                    .setDescription("Pacman color (0=Yellow, 1=Pink, 2=Purple, 3=Green, 4=Blue, 5=Red, 6=Orange)");
    
    public final BooleanParameter colorShift =
            new BooleanParameter("ColorShift", false)
                    .setDescription("Automatically cycle through colors");
    
    public final BooleanParameter flipFace =
            new BooleanParameter("FlipFace", false)
                    .setDescription("Flip Pacman's face to the opposite side");
    
    public final CompoundParameter colorShiftSpeed =
            new CompoundParameter("ColorSpeed", 2.0f, 0.1f, 2.0f)
                    .setDescription("Speed of color cycling (seconds per color)");
    
    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    // Animation variables
    private double animationTime = 0.0;
    
    // Panic listener
    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };
    
    // Method to get color based on choice
    private int getPacmanColor() {
        switch (colorChoice.getValuei()) {
            case 0: return TEColor.YELLOW;
            case 1: return LXColor.hsb(330, 100, 100); // Pink
            case 2: return LXColor.hsb(270, 100, 100); // Purple
            case 3: return LXColor.hsb(120, 100, 100); // Green
            case 4: return LXColor.hsb(240, 100, 100); // Blue
            case 5: return LXColor.hsb(0, 100, 100);   // Red
            case 6: return TEColor.ORANGE;
            default: return TEColor.YELLOW;
        }
    }

    public PacmanPattern(LX lx) {
        super(lx);
        addParameter("PSize", size);
        addParameter("MSize", mouthSize);
        addParameter("MSpeed", mouthSpeed);
        addParameter("MAnimation", mouthAnimation);
        addParameter("MouthMove", mouthMove);
        addParameter("Eyes", showEyes);
        addParameter("Twist", twist);
        addParameter("Color", colorChoice);
        addParameter("ColorShift", colorShift);
        addParameter("ColorSpeed", colorShiftSpeed);
        addParameter("FlipFace", flipFace);
        addParameter("PANIC", panic);
        
        // Add panic listener
        panic.addListener(panicListener);

    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * mouthSpeed.getValuef() * 0.001; // Convert to seconds and apply speed
        
        // Update color shift if enabled
        if (colorShift.isOn()) {
            // Cycle through colors based on speed parameter
            float colorTime = (float) (animationTime / colorShiftSpeed.getValuef());
            int colorIndex = (int) (colorTime % 7); // 7 colors total
            colorChoice.setValue(colorIndex);
        }
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate the radius based on model size and size parameter
        float maxDimension = Math.max(model.xMax - model.xMin, model.yMax - model.yMin);
        float radius = (maxDimension / 2.0f) * size.getValuef();
        
        // Get twist angle in radians
        float twistAngle = (float) Math.toRadians(twist.getValuef());
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Draw the yellow circle
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Calculate distance from center (using rotated coordinates)
            float distance = (float) Math.sqrt(
                rotatedX * rotatedX + rotatedY * rotatedY
            );
            
            // If point is within the circle radius, make it the selected color
            if (distance <= radius) {
                colors[point.index] = getPacmanColor();
            }
        }
        
        // Add the mouth cutout (black triangle on the left side)
        // Calculate animated mouth angle
        float baseMouthAngle = mouthSize.getValuef() * (float) Math.PI; // Convert to radians
        
        float animationAmount;
        if (mouthMove.isOn()) {
            // Create a sine wave animation (0 to 1 to 0)
            float animationWave = (float) Math.sin(animationTime * 2 * Math.PI);
            animationAmount = (animationWave + 1.0f) / 2.0f; // Convert to 0-1 range
            
            // Apply manual animation control if needed
            animationAmount = animationAmount * mouthAnimation.getValuef();
        } else {
            // Mouth stays open (full animation amount)
            animationAmount = 1.0f;
        }
        
        // Final mouth angle with animation
        float mouthAngle = baseMouthAngle * animationAmount;
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Calculate angle from center (using rotated coordinates)
            float angle = (float) Math.atan2(rotatedY, rotatedX);
            
            // Normalize angle to 0-2π range
            if (angle < 0) {
                angle += 2 * Math.PI;
            }
            
            // Check if point is in the mouth area (left or right side based on flipFace)
            boolean isLeftSide = !flipFace.getValueb(); // true for left side, false for right side
            if (isLeftSide) {
                // Mouth opens to the left (around 0° or 2π)
                if ((angle >= 0 - mouthAngle/2 && angle <= 0 + mouthAngle/2) || 
                    (angle >= 2*Math.PI - mouthAngle/2 && angle <= 2*Math.PI)) {
                    // Calculate distance from center (using rotated coordinates)
                    float distance = (float) Math.sqrt(
                        rotatedX * rotatedX + rotatedY * rotatedY
                    );
                    
                    // If point is within the circle radius, make it black (mouth cutout)
                    if (distance <= radius) {
                        colors[point.index] = LXColor.BLACK;
                    }
                }
            } else {
                // Mouth opens to the right (around π)
                if (angle >= Math.PI - mouthAngle/2 && angle <= Math.PI + mouthAngle/2) {
                    // Calculate distance from center (using rotated coordinates)
                    float distance = (float) Math.sqrt(
                        rotatedX * rotatedX + rotatedY * rotatedY
                    );
                    
                    // If point is within the circle radius, make it black (mouth cutout)
                    if (distance <= radius) {
                        colors[point.index] = LXColor.BLACK;
                    }
                }
            }
        }
        
        // Add eye if enabled
        if (showEyes.isOn()) {
            // Single eye position (above and to the left/right of center based on flipFace)
            float eyeRadius = radius * 0.15f; // Eye size relative to Pacman size
            float eyeX = flipFace.getValueb() ? centerX + radius * 0.1f : centerX - radius * 0.1f; // Right or left of center
            float eyeY = centerY + radius * 0.4f; // Above center but not too high
            
            // Draw the single eye
            for (int i = 0; i < model.points.length; i++) {
                LXVector point = new LXVector(model.points[i]);
                
                // Apply twist rotation to the point
                float relX = point.x - centerX;
                float relY = point.y - centerY;
                float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
                float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
                
                // Apply the opposite rotation to the eye position
                float eyeRelX = eyeX - centerX;
                float eyeRelY = eyeY - centerY;
                float rotatedEyeX = centerX + (float) (eyeRelX * Math.cos(-twistAngle) - eyeRelY * Math.sin(-twistAngle));
                float rotatedEyeY = centerY + (float) (eyeRelX * Math.sin(-twistAngle) + eyeRelY * Math.cos(-twistAngle));
                
                // Check if point is within the eye
                float eyeDistance = (float) Math.sqrt(
                    (point.x - rotatedEyeX) * (point.x - rotatedEyeX) + 
                    (point.y - rotatedEyeY) * (point.y - rotatedEyeY)
                );
                
                // If point is within the eye and within Pacman's body, make it black
                if (eyeDistance <= eyeRadius && 
                    (float) Math.sqrt(rotatedX * rotatedX + rotatedY * rotatedY) <= radius) {
                    colors[point.index] = LXColor.BLACK;
                }
            }
        }
    }
    
    /**
     * Called when the momentary PANIC button is pressed. Resets all parameters to defaults.
     */
    protected void onPanic() {
        size.reset();
                        mouthSize.reset();
                mouthSpeed.reset();
                mouthAnimation.reset();
                mouthMove.reset();
                showEyes.reset();
        twist.reset();
        colorChoice.reset();
        colorShift.reset();
        colorShiftSpeed.reset();
        flipFace.reset();
    }
}
