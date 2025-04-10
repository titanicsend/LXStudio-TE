//fork of https://www.shadertoy.com/view/3scGDX
// Simplex3D noise is forked from https://www.shadertoy.com/view/XsX3zB

#define PI 3.1415926

vec3 random3(vec3 c) {
	float j = 4096.0*sin(dot(c,vec3(17.0, 59.4, 15.0)));
	vec3 r;
	r.z = fract(512.0*j);
	j *= .125;
	r.x = fract(512.0*j);
	j *= .125;
	r.y = fract(512.0*j);
	return r-0.5;
}

const float F3 =  0.3333333;
const float G3 =  0.1666667;

float simplex3d(vec3 p) {
	 vec3 s = floor(p + dot(p, vec3(F3)));
	 vec3 x = p - s + dot(s, vec3(G3));

	 vec3 e = step(vec3(0.0), x - x.yzx);
	 vec3 i1 = e*(1.0 - e.zxy);
	 vec3 i2 = 1.0 - e.zxy*(1.0 - e);

	 vec3 x1 = x - i1 + G3;
	 vec3 x2 = x - i2 + 2.0*G3;
	 vec3 x3 = x - 1.0 + 3.0*G3;

	 vec4 w, d;

	 w.x = dot(x, x);
	 w.y = dot(x1, x1);
	 w.z = dot(x2, x2);
	 w.w = dot(x3, x3);

	 w = max(0.6 - w, 0.0);

	 d.x = dot(random3(s), x);
	 d.y = dot(random3(s + i1), x1);
	 d.z = dot(random3(s + i2), x2);
	 d.w = dot(random3(s + 1.0), x3);

	 w *= w;
	 w *= w;
	 d *= w;

	 return dot(d, vec4(52.0));
}

// Note: Use uv_multiplier == 32 for a fine grained noise.
float getSimplex3DNoise(vec2 uv, float uv_multiplier, float time_multiplier){
    return 0.5 + 0.5 * simplex3d(vec3(uv * uv_multiplier, iTime * time_multiplier)) * smoothstep(0.0, 0.005, abs(0.6-uv.x));
}

float pat(vec2 uv, float p, float q, float s, float glow) {
	q += (0.5 + sin(iTime * s) * {%width[.5, .5, 1.5]});
    float z = cos(q * PI * uv.x) * cos(p * PI * uv.y) + cos(q * PI * uv.y) * cos(p * PI * uv.x);
    float dist = abs(z) * (1.0 / glow);
    return dist;
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Normalized UV coordinates
    vec2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;

    // Glow logic
    float glow_audio = bassLevel * trebleLevel * frequencyReact;
    float glow = 0.1 + min(0.6, glow_audio); // glow levels that look good: min=0.1, avg=0.2, max=0.5

    // Associate noise to the p and q parameters of "pat" function with the iWow1.
    float noise_uv_multiplier = 3. * (1. + bassRatio);
    float noise_p_1 = getSimplex3DNoise(uv, noise_uv_multiplier, 0.1) * iWow1;
    float noise_q_1 = getSimplex3DNoise(uv, noise_uv_multiplier, 0.1) * iWow1;
    float noise_p_2 = getSimplex3DNoise(uv, noise_uv_multiplier, 0.1) * iWow1;
    float noise_q_2 = getSimplex3DNoise(uv, noise_uv_multiplier, 0.1) * iWow1;

   	// Perturb the UV coordinates
   	uv = uv + 0.2 * sin(uv.yx * 1.2 + 0.25 * iTime);
    uv = rotate(uv, iRotationAngle) * iScale;

    // layer1
    float p1 = 5.0 + noise_p_1;
    float q1 = 2.0 + noise_q_1;
    float d = pat(uv, p1, q1, 1.0, glow);

    // Wow2 mixes in second layer of cells
    float p2 = 3.0 + noise_p_2;
    float q2 = 7.0 + noise_q_2;
    d = mix(d, d * pat(uv, 3.0, 7.0, 0.5, glow), iWow2);
    fragColor = vec4(iColorRGB, 0.5 / d);
}
