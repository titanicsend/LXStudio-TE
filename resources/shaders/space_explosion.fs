//fork of https://www.shadertoy.com/view/7ddBzr

/*
 * "Hyperspace explosion" by Ben Wheatley - 2018
 * License MIT License
 * Contact: github.com/BenWheatley
 */

// constants
const int MIN_OCTAVE = 3;
const int MAX_OCTAVE = 8;
const float PI = 3.14159265359;
const float centerToCorner = sqrt((0.5*0.5) + (0.5*0.5));
const float tangentScale = PI / (2.0*centerToCorner);
const float thetaToPerlinScale = 2.0 / PI;

float cosineInterpolate(float a, float b, float x) {
	float ft = x * PI;
	float f = (1.0 - cos(ft)) * 0.5;

	return a*(1.0-f) + b*f;
}

float seededRandom(float seed) {
    int x = int(seed);
    x = x << 13 ^ x;
    x = (x * (x * x * 15731 + 789221) + 1376312589);
    x = x & 0x7fffffff;
    return float(x)/1073741824.0;
}

// The magic constants are essentially arbitary:
// they define the scale of the largest component of the Perlin noise
float perlinNoise(float perlinTheta, float r, float time) {
    float sum = 0.0;
    for (int octave=MIN_OCTAVE; octave<MAX_OCTAVE; ++octave) {
        float sf = pow(2.0, float(octave));
        float sf8 = sf*64.0; // I can't remember where this variable name came from

		float new_theta = sf*perlinTheta;
        float new_r = sf*r + time; // Add current time to this to get an animated effect

        float new_theta_floor = floor(new_theta);
		float new_r_floor = floor(new_r);
		float fraction_r = new_r - new_r_floor;
		float fraction_theta = new_theta - new_theta_floor;

        float t1 = seededRandom( new_theta_floor	+	sf8 *  new_r_floor      );
		float t2 = seededRandom( new_theta_floor	+	sf8 * (new_r_floor+1.0) );

        new_theta_floor += 1.0;
        float maxVal = sf*2.0;
        if (new_theta_floor >= maxVal) {
            new_theta_floor -= maxVal; // So that interpolation with angle 0-360° doesn't do weird things with angles > 360°
        }

        float t3 = seededRandom( new_theta_floor	+	sf8 *  new_r_floor      );
		float t4 = seededRandom( new_theta_floor	+	sf8 * (new_r_floor+1.0) );

		float i1 = cosineInterpolate(t1, t2, fraction_r);
		float i2 = cosineInterpolate(t3, t4, fraction_r);

        sum += cosineInterpolate(i1, i2, fraction_theta)/sf;
    }
    return sum;
}

// main
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = fragCoord.xy / iResolution.xy;

    float dx = 0.5 - uv.x;
    float dy = 0.5 - uv.y;
    dy *= iResolution.y / iResolution.x;

    float perlinTheta = (PI+atan(dy, -dx))/PI;
    float r = sqrt((dx*dx) + (dy*dy));
    r = centerToCorner - r;

    float perlin = perlinNoise(perlinTheta, r, iTime);

    float timeMod = mod(-iTime, 1.5);
    float scale = 2.0*(timeMod-r);
    float glowRing = cos(pow(1.0-scale, 0.1));
    glowRing -= 0.5;
    glowRing *= 2.0;
    if (scale>1.0) {
        scale = 0.0;
    }
    float c = scale*perlin*2.0;

    fragColor = vec4(glowRing+c, glowRing, glowRing*0.5+(c*2.0), 0);
    // alpha, derived from brightness, for LX blending.
    fragColor.a = max(fragColor.r,max(fragColor.g,fragColor.b));	
}
