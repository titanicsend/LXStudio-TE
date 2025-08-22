package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.transform.LXVector;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class GhostPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("GSize", 0.6f, 0.1f, 2.0f)
                    .setDescription("Size of the ghost");
    
    public final CompoundParameter floatSpeed =
            new CompoundParameter("FloatSpeed", 1.0f, 0.1f, 5.0f)
                    .setDescription("Speed of floating animation");
    
    public final CompoundParameter floatAmount =
            new CompoundParameter("FloatAmount", 0.3f, 0.0f, 1.0f)
                    .setDescription("Amount of floating movement");
    
    public final BooleanParameter enableFloat =
            new BooleanParameter("Float", true)
                    .setDescription("Enable floating animation");
    
    public final BooleanParameter showEyes =
            new BooleanParameter("Eyes", true)
                    .setDescription("Show ghost's eyes");
    
    public final CompoundParameter twist =
            new CompoundParameter("Twist", 0.0f, 0.0f, 360.0f)
                    .setDescription("Rotate the entire ghost");
    
    public final DiscreteParameter colorChoice =
            new DiscreteParameter("Color", 0, 0, 7)
                    .setDescription("Ghost color (0=Classic White, 1=Red/Blinky, 2=Pink/Pinky, 3=Cyan/Inky, 4=Orange/Clyde, 5=Blue, 6=Purple, 7=Green)");
    
    public final BooleanParameter colorShift =
            new BooleanParameter("ColorShift", false)
                    .setDescription("Automatically cycle through colors");
    
    public final CompoundParameter colorShiftSpeed =
            new CompoundParameter("ColorSpeed", 2.0f, 0.1f, 5.0f)
                    .setDescription("Speed of color cycling (seconds per color)");
    
    public final BooleanParameter scared =
            new BooleanParameter("Scared", false)
                    .setDescription("Turn ghost blue (scared mode)");
    
    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    // Animation variables
    private double animationTime = 0.0;
    
    // Ghost shape coordinates (correctly parsed from your polygon coords)
    // Original coords: 19,484,453,119,486,220,486,484,452,484,452,454,420,452,418,420,387,420,386,453,354,455,353,487,287,485,285,421,220,419,220,484,153,486,152,451,119,451,117,419,87,419,86,449,55,455,54,485,18,221,55,220,51,121,85,119,88,87,122,85,117,54,186,53,187,19,317,19,319,51,383,54,385,81,421,85
    // Correctly parsed as (x,y) pairs from your polygon coords
    // Your string: 19,484,453,119,486,220,486,484,452,484,452,454,420,452,418,420,387,420,386,453,354,455,353,487,287,485,285,421,220,419,220,484,153,486,152,451,119,451,117,419,87,419,86,449,55,455,54,485,18,221,55,220,51,121,85,119,88,87,122,85,117,54,186,53,187,19,317,19,319,51,383,54,385,81,421,85
    // Properly ordered ghost coordinates following the outline clockwise from top-left
    // Looking at your coordinate pairs and analyzing the ghost shape more carefully
    // Your coordinates: (19,484), (453,119), (486,220), (486,484), (452,484), (452,454), (420,452), (418,420), (387,420), (386,453), (354,455), (353,487), (287,485), (285,421), (220,419), (220,484), (153,486), (152,451), (119,451), (117,419), (87,419), (86,449), (55,455), (54,485), (18,221), (55,220), (51,121), (85,119), (88,87), (122,85), (117,54), (186,53), (187,19), (317,19), (319,51), (383,54), (385,81), (421,85), (453,219)
    // Reordered to create proper ghost shape: start from bottom-left, go up and around head, then down right side, then wavy bottom
    private final int[] ghostPolyX = {51,85,88,122,117,186,187,317,319,383,385,421,453,486,486,452,452,420,418,387,386,354,353,287,285,220,220,153,152,119,117,87,86,55,54,19,18,55};
    private final int[] ghostPolyY = {121,119,87,85,54,53,19,19,51,54,81,85,119,220,484,484,454,452,420,420,453,455,487,485,421,419,484,486,451,451,419,419,449,455,485,484,221,220};
    
    // Panic listener
    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };
    
    // Method to get ghost color based on choice
    private int getGhostColor() {
        if (scared.isOn()) {
            return LXColor.hsb(240, 100, 80); // Scared blue
        }
        
        switch (colorChoice.getValuei()) {
            case 0: return LXColor.hsb(0, 0, 95);     // Classic White
            case 1: return LXColor.hsb(0, 100, 100);   // Red (Blinky)
            case 2: return LXColor.hsb(330, 100, 100); // Pink (Pinky)
            case 3: return LXColor.hsb(180, 100, 100); // Cyan (Inky)
            case 4: return TEColor.ORANGE;             // Orange (Clyde)
            case 5: return LXColor.hsb(240, 100, 100); // Blue
            case 6: return LXColor.hsb(270, 100, 100); // Purple
            case 7: return LXColor.hsb(120, 100, 100); // Green
            default: return LXColor.hsb(0, 0, 95);     // Default white
        }
    }

    public GhostPattern(LX lx) {
        super(lx);
        addParameter("GSize", size);
        addParameter("FloatSpeed", floatSpeed);
        addParameter("FloatAmount", floatAmount);
        addParameter("Float", enableFloat);
        addParameter("Eyes", showEyes);
        addParameter("Twist", twist);
        addParameter("Color", colorChoice);
        addParameter("ColorShift", colorShift);
        addParameter("ColorSpeed", colorShiftSpeed);
        addParameter("Scared", scared);
        addParameter("PANIC", panic);
        
        // Add panic listener
        panic.addListener(panicListener);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * floatSpeed.getValuef() * 0.001; // Convert to seconds and apply speed
        
        // Update color shift if enabled
        if (colorShift.isOn()) {
            // Cycle through colors based on speed parameter
            float colorTime = (float) (animationTime / colorShiftSpeed.getValuef());
            int colorIndex = (int) (colorTime % 8); // 8 colors total
            colorChoice.setValue(colorIndex);
        }
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate floating offset
        float floatOffset = 0.0f;
        if (enableFloat.isOn()) {
            floatOffset = (float) Math.sin(animationTime * 2 * Math.PI) * 
                         floatAmount.getValuef() * 
                         Math.max(model.xMax - model.xMin, model.yMax - model.yMin) * 0.1f;
        }
        
        // Apply floating to center
        centerY += floatOffset;
        
        // Get twist angle in radians
        float twistAngle = (float) Math.toRadians(twist.getValuef());
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Normalize ghost coordinates to fit the model
        float[] normalizedPolyX = new float[ghostPolyX.length];
        float[] normalizedPolyY = new float[ghostPolyY.length];
        
        // Find bounds of original polygon
        int minPolyX = Integer.MAX_VALUE, maxPolyX = Integer.MIN_VALUE;
        int minPolyY = Integer.MAX_VALUE, maxPolyY = Integer.MIN_VALUE;
        
        for (int i = 0; i < ghostPolyX.length; i++) {
            minPolyX = Math.min(minPolyX, ghostPolyX[i]);
            maxPolyX = Math.max(maxPolyX, ghostPolyX[i]);
            minPolyY = Math.min(minPolyY, ghostPolyY[i]);
            maxPolyY = Math.max(maxPolyY, ghostPolyY[i]);
        }
        
        // Calculate scaling factor
        float polyWidth = maxPolyX - minPolyX;
        float polyHeight = maxPolyY - minPolyY;
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        
        float scale = Math.min(modelWidth / polyWidth, modelHeight / polyHeight) * size.getValuef();
        
        // Normalize and scale coordinates (flip Y to fix upside-down orientation)
        for (int i = 0; i < ghostPolyX.length; i++) {
            normalizedPolyX[i] = centerX + (ghostPolyX[i] - minPolyX - polyWidth/2) * scale;
            // Flip Y coordinate by subtracting from max instead of min
            normalizedPolyY[i] = centerY - (ghostPolyY[i] - minPolyY - polyHeight/2) * scale;
        }
        
        // Draw the ghost shape (exactly like PacmanPattern draws the body)
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Check if point is inside the ghost polygon using ray casting algorithm
            if (isPointInPolygon(centerX + rotatedX, centerY + rotatedY, normalizedPolyX, normalizedPolyY)) {
                colors[point.index] = getGhostColor();
            }
        }
        
        // Add eyes AFTER the ghost body is complete (exactly like PacmanPattern)
        if (showEyes.isOn()) {
            addProperGhostEyes(centerX, centerY, scale, twistAngle, normalizedPolyX, normalizedPolyY);
        }
    }
    

    /**
     * Ray casting algorithm to determine if a point is inside a polygon
     * Fixed version to handle edge cases better
     */
    private boolean isPointInPolygon(float x, float y, float[] polyX, float[] polyY) {
        int nvert = polyX.length;
        boolean inside = false;
        
        for (int i = 0, j = nvert - 1; i < nvert; j = i++) {
            float xi = polyX[i];
            float yi = polyY[i];
            float xj = polyX[j];
            float yj = polyY[j];
            
            // Check if ray crosses this edge
            if (((yi > y) != (yj > y)) && 
                (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    /**
     * Classic Pac-Man ghost eyes - white rectangular bases with blue square pupils
     */
    private void addProperGhostEyes(float centerX, float centerY, float scale, float twistAngle, float[] normalizedPolyX, float[] normalizedPolyY) {
        // Eye parameters - EVEN BIGGER for better visibility!
        float eyeWidth = Math.max(scale * 45.0f, 12.0f);   // Width of white eye base - even bigger!
        float eyeHeight = Math.max(scale * 60.0f, 18.0f);  // Height of white eye base - even bigger!
        float pupilSize = Math.max(scale * 20.0f, 7.0f);   // Size of blue pupil - even bigger!
        float eyeOffsetX = Math.max(scale * 35.0f, 15.0f); // Closer together - distance from center horizontally 
        float eyeOffsetY = Math.max(scale * 40.0f, 20.0f); // Distance from center (UP from center)
        
        // Left and right eye positions
        float leftEyeX = centerX - eyeOffsetX;
        float leftEyeY = centerY + eyeOffsetY;  // UP from center
        float rightEyeX = centerX + eyeOffsetX;
        float rightEyeY = centerY + eyeOffsetY; // UP from center
        
        // Draw classic Pac-Man style eyes
        drawClassicGhostEye(leftEyeX, leftEyeY, eyeWidth, eyeHeight, pupilSize, centerX, centerY, twistAngle, normalizedPolyX, normalizedPolyY);
        drawClassicGhostEye(rightEyeX, rightEyeY, eyeWidth, eyeHeight, pupilSize, centerX, centerY, twistAngle, normalizedPolyX, normalizedPolyY);
    }
    
    private void drawClassicGhostEye(float eyeX, float eyeY, float eyeWidth, float eyeHeight, float pupilSize, float centerX, float centerY, float twistAngle, float[] normalizedPolyX, float[] normalizedPolyY) {
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point (same as ghost body)
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Apply the opposite rotation to the eye position (same as Pacman)
            float eyeRelX = eyeX - centerX;
            float eyeRelY = eyeY - centerY;
            float rotatedEyeX = centerX + (float) (eyeRelX * Math.cos(-twistAngle) - eyeRelY * Math.sin(-twistAngle));
            float rotatedEyeY = centerY + (float) (eyeRelX * Math.sin(-twistAngle) + eyeRelY * Math.cos(-twistAngle));
            
            // Only draw if within ghost body
            if (isPointInPolygon(centerX + rotatedX, centerY + rotatedY, normalizedPolyX, normalizedPolyY)) {
                
                // Define the two white rectangles to form a FAT + sign
                float bottomRectWidth = eyeWidth/1.5f;  // Bottom rectangle: much thicker vertical bar (was /3)
                float bottomRectHeight = eyeHeight;     // Bottom rectangle: taller in Y (vertical bar of +)
                float topRectWidth = eyeWidth;          // Top rectangle: wider in X (horizontal bar of +)  
                float topRectHeight = eyeHeight/1.5f;   // Top rectangle: much thicker horizontal bar (was /3)
                
                // Bottom rectangle position (vertical bar of the + sign)
                float deltaXBottom = Math.abs(point.x - rotatedEyeX);
                float deltaYBottom = Math.abs(point.y - rotatedEyeY);
                
                // Top rectangle position (horizontal bar of the + sign)
                float deltaXTop = Math.abs(point.x - rotatedEyeX);
                float deltaYTop = Math.abs(point.y - rotatedEyeY);
                
                // Draw bottom white rectangle (vertical bar of +)
                if (deltaXBottom <= bottomRectWidth/2 && deltaYBottom <= bottomRectHeight/2) {
                    colors[point.index] = LXColor.WHITE;
                }
                
                // Draw top white rectangle (horizontal bar of +) 
                if (deltaXTop <= topRectWidth/2 && deltaYTop <= topRectHeight/2) {
                    colors[point.index] = LXColor.WHITE;
                }
                
                // Draw blue pupil on the RIGHT side of the horizontal bar (replaces right arm of +)
                float pupilX = rotatedEyeX + topRectWidth/4; // Offset to the right side
                float pupilDeltaX = Math.abs(point.x - pupilX);
                float pupilDeltaY = Math.abs(point.y - rotatedEyeY);
                
                if (pupilDeltaX <= pupilSize/2 && pupilDeltaY <= pupilSize/2) {
                    colors[point.index] = LXColor.BLUE;
                }
            }
        }
    }

    /**
     * Simple test eyes - just red circles to verify positioning
     */
    private void addSimpleTestEyes(float centerX, float centerY, float scale, float twistAngle) {
        // Eye parameters - much larger for visibility!
        float eyeRadius = Math.max(scale * 10.0f, 3.0f); // Make eyes much bigger, minimum 3 units
        float eyeOffsetX = Math.max(scale * 15.0f, 8.0f); // Distance from center horizontally 
        float eyeOffsetY = Math.max(scale * 20.0f, 10.0f); // Distance from center (UP from center)
        
        // Left and right eye positions
        float leftEyeX = centerX - eyeOffsetX;
        float leftEyeY = centerY + eyeOffsetY;  // UP from center
        float rightEyeX = centerX + eyeOffsetX;
        float rightEyeY = centerY + eyeOffsetY; // UP from center
        
        
        // Draw simple red circles - no constraints, just to see if they appear
        drawSimpleEye(leftEyeX, leftEyeY, eyeRadius, LXColor.RED);
        drawSimpleEye(rightEyeX, rightEyeY, eyeRadius, LXColor.RED);
    }
    
    private void drawSimpleEye(float eyeX, float eyeY, float eyeRadius, int color) {
        int pixelsSet = 0;
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Simple distance check - no rotation for now
            float distance = (float) Math.sqrt(
                (point.x - eyeX) * (point.x - eyeX) + 
                (point.y - eyeY) * (point.y - eyeY)
            );
            
            // If point is within the eye radius, make it the specified color
            if (distance <= eyeRadius) {
                colors[point.index] = color;
                pixelsSet++;
            }
        }
    }

    /**
     * Add ghost eyes - using the same approach as PacmanPattern
     */
    private void addGhostEyes(float centerX, float centerY, float scale, float twistAngle, float[] normalizedPolyX, float[] normalizedPolyY) {
        // Eye parameters - copying Pacman's approach exactly
        float eyeRadius = scale * 0.15f; // Eye size relative to ghost size (same as Pacman)
        float eyeOffsetX = scale * 0.2f; // Distance from center horizontally (slightly wider for ghost)
        float eyeOffsetY = scale * 0.4f; // Distance from center (same as Pacman - UP from center)
        
        // Left and right eye positions (copying Pacman's Y positioning EXACTLY)
        float leftEyeX = centerX - eyeOffsetX;
        float leftEyeY = centerY + eyeOffsetY;  // + like in Pacman (UP from center)
        float rightEyeX = centerX + eyeOffsetX;
        float rightEyeY = centerY + eyeOffsetY; // + like in Pacman (UP from center)
        
        // Draw eyes using Pacman's exact approach
        drawEyeLikePacman(leftEyeX, leftEyeY, eyeRadius, centerX, centerY, scale, twistAngle, normalizedPolyX, normalizedPolyY);
        drawEyeLikePacman(rightEyeX, rightEyeY, eyeRadius, centerX, centerY, scale, twistAngle, normalizedPolyX, normalizedPolyY);
    }
    
    /**
     * Draw eye using Pacman's exact approach
     */
    private void drawEyeLikePacman(float eyeX, float eyeY, float eyeRadius, float centerX, float centerY, float scale, float twistAngle, float[] normalizedPolyX, float[] normalizedPolyY) {
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point (same as Pacman)
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Apply the opposite rotation to the eye position (same as Pacman)
            float eyeRelX = eyeX - centerX;
            float eyeRelY = eyeY - centerY;
            float rotatedEyeX = centerX + (float) (eyeRelX * Math.cos(-twistAngle) - eyeRelY * Math.sin(-twistAngle));
            float rotatedEyeY = centerY + (float) (eyeRelX * Math.sin(-twistAngle) + eyeRelY * Math.cos(-twistAngle));
            
            // Check if point is within the eye
            float eyeDistance = (float) Math.sqrt(
                (point.x - rotatedEyeX) * (point.x - rotatedEyeX) + 
                (point.y - rotatedEyeY) * (point.y - rotatedEyeY)
            );
            
            // If point is within the eye AND within ghost body, make it black (copying Pacman's logic)
            // Check if point is within the ghost polygon using the same transform as the main body
            if (eyeDistance <= eyeRadius && 
                isPointInPolygon(centerX + rotatedX, centerY + rotatedY, normalizedPolyX, normalizedPolyY)) {
                colors[point.index] = LXColor.BLACK;
            }
        }
    }
    
    /**
     * Called when the momentary PANIC button is pressed. Resets all parameters to defaults.
     */
    protected void onPanic() {
        size.reset();
        floatSpeed.reset();
        floatAmount.reset();
        enableFloat.reset();
        showEyes.reset();
        twist.reset();
        colorChoice.reset();
        colorShift.reset();
        colorShiftSpeed.reset();
        scared.reset();
    }
    
    @Override
    public void dispose() {
        // Remove the panic listener to prevent memory leaks
        panic.removeListener(panicListener);
        super.dispose();
    }
}
