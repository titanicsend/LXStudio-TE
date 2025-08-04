#pragma name "Wormhole"
#pragma TEControl.WOW1.Range(0.4,0.0,1.0)
#pragma TEControl.SPEED.Value(0.75)
#pragma TEControl.QUANTITY.Range(33.0,1.0,50.0)

// 2D rotation matrix function
mat2 rotationMatrix(float angle) {
    float cosAngle = cos(angle);
    float sinAngle = sin(angle);
    return mat2(cosAngle, sinAngle, -sinAngle, cosAngle);
}

vec2 rotate(vec2 point,float angle){
    mat2 rotationMatrix=mat2(cos(angle),-sin(angle),sin(angle),cos(angle));
    return rotationMatrix*point;
}

// adapted from https://www.shadertoy.com/view/fdVSRc
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 st=fragCoord.xy/iResolution.xy;
    st.x*=iResolution.x/iResolution.y;

    st=st-vec2(.5);
    st=rotate(st,iRotationAngle)/iScale;
    st-=iTranslate;

    vec3 rayDirection = vec3(st, 0.4);

    // Current position along the ray
    vec3 rayPosition = vec3(0.0);

    // Apply rotation to the ray direction based on time
    // Rotate around Y axis (affecting YZ plane)
    rayDirection.yz *= rotationMatrix(iTime * 0.2);
    // Rotate around Y axis again (affecting XZ plane)
    rayDirection.xz *= rotationMatrix(iTime * 0.2);

    // Raymarching loop - march through 33 steps
    for (float i = 0.0; i < iQuantity; i++) {
        // Calculate distance field using sine waves for animation
        vec2 sineWave = sin(rayPosition.yz - vec2(-1.0, iTime * 5.0));

        // Distance field calculation
        // Creates a shape based on cosine of XY coordinates plus the sine wave
        float distanceToSurface = 0.2 - length(cos(rayPosition.xy) + sineWave);

        // Apply some distortion to the ray direction based on the sine wave
        rayDirection.yx += sineWave / (32.0 + distanceToSurface);

        // March the ray forward
        // The step size is based on the distance to surface and some time-based animation
        float timeAnimation = (sin(iTime) + 1.0) * 0.3;
        float stepSize = distanceToSurface - 0.1 - timeAnimation;
        rayPosition += stepSize * rayDirection;

        // Accumulate color based on how close we are to the surface
        // Closer to surface = brighter color
        vec3 surfaceColor = vec3(0.2, 0.8, 0.9); // Cyan-ish color
        float brightness = 0.005 / clamp(abs(distanceToSurface), 0.01, 9.0);
        fragColor.xyz += surfaceColor * brightness;
    }
}