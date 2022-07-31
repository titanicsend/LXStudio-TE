uniform vec3 color;
uniform float stars;
uniform float starSize;
uniform float rotate;
uniform float glow;
uniform float energy;
uniform float vTime;

const float step = 0.5;  // star spacing

// Minkowski distance at fractional exponents makes the nice 4-pointed star!
float minkowskiDistance(vec2 uv, float p) {
   return pow(pow(abs(uv.x), p) + pow(abs(uv.y), p), 1.0 / p);
}

// normalized HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {

    // normalize and shift origin to center (-0.5-0.5 coordinate range)
    vec2 uPos = -0.5+(fragCoord.xy / iResolution.xy );
    
    // star size pulses with the music!
    float pulse = (0.3 * energy * (-0.5+beat)) + 1.618 * starSize;
    float dance = 0.2 + (0.5 * bassLevel * energy);
    float brightness = 0.;

    // set up matrix for optional rotation
    float theta = mod(vTime * 4,6.28);
    mat2 rot = mat2(cos(theta), -sin(theta),
            sin(theta), cos(theta));

    // for each star
    for (float i = 1.; i <= stars; i++) {
        // time offset for each star.
        float t = vTime + step*i;

        // calculate point offset and rescale for our normalized coord range
        // TODO - add a little bump with the beat.
        vec2 point = vec2(0.92 * sin(t) + 0.08 * cos(t * 6.0),
                         -0.3 + (0.65 * sin(t * 0.85) + dance * sin(t * 2.0)));
        point = uPos - point/2.0;

        // if we're rotating, give the individual stars slightly different rates
        // by playing with the matrix scale a little
        if (rotate > 0.) {
          float dir = 0.5*beat/i;
          dir = (mod(i,2.) == 1.) ? 0.5+dir : 1.0-dir;
          point = point * (dir * rot);
        }
        
        // add brightness contribution of each star
        brightness += pulse / minkowskiDistance(point,0.375) / (i + 1.);
    }
    
    // weird brightness-for-alpha multiply is to replicate a bug in the original java
    // version of this pattern. (I didn't see that LX wanted alpha in a different
    // range from everything else when setting hsba colors.)
    vec3 col = vec3(color.x,
                    color.y * clamp((pulse+(2.25f-brightness)),0,1),
                    brightness = color.z * clamp(brightness * brightness,0,1));

    col = hsv2rgb(col);
    fragColor = vec4(col, brightness * glow);
}
