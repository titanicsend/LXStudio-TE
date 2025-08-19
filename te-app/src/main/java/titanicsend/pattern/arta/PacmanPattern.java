package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
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
    
    public final BooleanParameter showEyes =
            new BooleanParameter("Eyes", true)
                    .setDescription("Show Pacman's eyes");

    // Animation variables
    private double animationTime = 0.0;

    public PacmanPattern(LX lx) {
        super(lx);
        addParameter("PSize", size);
        addParameter("MSize", mouthSize);
        addParameter("MSpeed", mouthSpeed);
        addParameter("MAnimation", mouthAnimation);
        addParameter("Eyes", showEyes);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * mouthSpeed.getValuef() * 0.001; // Convert to seconds and apply speed
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate the radius based on model size and size parameter
        float maxDimension = Math.max(model.xMax - model.xMin, model.yMax - model.yMin);
        float radius = (maxDimension / 2.0f) * size.getValuef();
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Draw the yellow circle
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Calculate distance from center
            float distance = (float) Math.sqrt(
                (point.x - centerX) * (point.x - centerX) + 
                (point.y - centerY) * (point.y - centerY)
            );
            
            // If point is within the circle radius, make it yellow
            if (distance <= radius) {
                colors[point.index] = TEColor.YELLOW;
            }
        }
        
        // Add the mouth cutout (black triangle on the left side)
        // Calculate animated mouth angle
        float baseMouthAngle = mouthSize.getValuef() * (float) Math.PI; // Convert to radians
        
        // Create a sine wave animation (0 to 1 to 0)
        float animationWave = (float) Math.sin(animationTime * 2 * Math.PI);
        float animationAmount = (animationWave + 1.0f) / 2.0f; // Convert to 0-1 range
        
        // Apply manual animation control if needed
        animationAmount = animationAmount * mouthAnimation.getValuef();
        
        // Final mouth angle with animation
        float mouthAngle = baseMouthAngle * animationAmount;
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Calculate angle from center to this point
            float angle = (float) Math.atan2(point.y - centerY, point.x - centerX);
            
            // Normalize angle to 0-2π range
            if (angle < 0) {
                angle += 2 * Math.PI;
            }
            
            // Check if point is in the mouth area (left side, within mouth angle)
            // Mouth opens to the left (π to 2π range, which is left side)
            if (angle >= Math.PI - mouthAngle/2 && angle <= Math.PI + mouthAngle/2) {
                // Calculate distance from center
                float distance = (float) Math.sqrt(
                    (point.x - centerX) * (point.x - centerX) + 
                    (point.y - centerY) * (point.y - centerY)
                );
                
                // If point is within the circle radius, make it black (mouth cutout)
                if (distance <= radius) {
                    colors[point.index] = LXColor.BLACK;
                }
            }
        }
        
        // Add eye if enabled
        if (showEyes.isOn()) {
            // Single eye position (above and to the right of the mouth, since Pacman faces left)
            float eyeRadius = radius * 0.15f; // Eye size relative to Pacman size
            float eyeX = centerX + radius * 0.1f; // Closer to center (more to the left)
            float eyeY = centerY + radius * 0.4f; // Above center but not too high
            
            // Draw the single eye
            for (int i = 0; i < model.points.length; i++) {
                LXVector point = new LXVector(model.points[i]);
                
                // Check if point is within the eye
                float eyeDistance = (float) Math.sqrt(
                    (point.x - eyeX) * (point.x - eyeX) + 
                    (point.y - eyeY) * (point.y - eyeY)
                );
                
                // If point is within the eye and within Pacman's body, make it black
                if (eyeDistance <= eyeRadius && 
                    (float) Math.sqrt((point.x - centerX) * (point.x - centerX) + 
                                     (point.y - centerY) * (point.y - centerY)) <= radius) {
                    colors[point.index] = LXColor.BLACK;
                }
            }
        }
    }
}
