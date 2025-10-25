// taste of noise 7 by leon denise 2021/10/14
// adapted for TE pattern pipeline (fireflies style backbuffer + signatures)
// original credits: Inigo Quilez, David Hoskins, NuSan; reviews by Fabrice Neyret
#define TE_NOPOSTPROCESSING

float hash13(vec3 p3) {
	p3  = fract(p3 * .1031);
	p3 += dot(p3, p3.zyx + 31.32);
	return fract((p3.x + p3.y) * p3.z);
}

// Inigo Quilez smooth min and smoothing helper
float smin(float d1, float d2, float k) {
	float h = clamp(0.5 + 0.5*(d2-d1)/k, 0.0, 1.0);
	return mix(d2, d1, h) - k*h*(1.0-h);
}
float smoothing(float d1, float d2, float k) {
	return clamp(0.5 + 0.5*(d2-d1)/k, 0.0, 1.0);
}

// rotation matrix
mat2 rot(float a) { return mat2(cos(a), -sin(a), sin(a), cos(a)); }

#define repeat(p,r) (mod(p,r)-r/2.)

// global variables used by map()
float material;
float rng;

// signed distance field for scene
float map(vec3 p) {
	// time with speed and mild time-warp driven by WOW1 for unpredictability
	float speed = mix(0.6, 1.8, iWow1);
	float t = iTime * speed + rng * 0.9;

	// domain repetition
	float grid = 5.;
	vec3 cell = floor(p/grid);
	p = repeat(p,grid);

	// distance from origin
	float dp = length(p);

	// rotation parameter
	vec3 angle = vec3(.1,-.5,.1) + dp*.5 + p*.1 + cell;
	// WOW1 increases unpredictable, cell-variant rotation jitter over time
	vec3 jitter = vec3(
		sin(t*1.1 + hash13(cell + vec3(1.0,0.0,0.0)) * 6.28318),
		sin(t*1.3 + hash13(cell + vec3(0.0,1.0,0.0)) * 6.28318),
		sin(t*1.7 + hash13(cell + vec3(0.0,0.0,1.0)) * 6.28318)
	);
	angle += iWow1 * 1.2 * jitter;

	// shrink sphere size with WOW1-driven micro-variation in time
	float size = sin(rng*3.14 + iWow1 * 0.5 * sin(t + hash13(cell) * 6.28318));

	// stretch sphere
	float wave = sin(-dp*1. + t + hash13(cell)*6.28)*.5;
	// add non-linear time-warp to make motion less predictable as WOW1 increases
	t += iWow1 * 0.5 * sin(iTime * 1.37 + hash13(cell) * 6.28318);
	// WOW1: add a gentle, cell-based noisy wobble to the fold offset to evolve patterns
	float wowNoise = sin(t + hash13(cell) * 6.28318);
	// Stronger WOW1 influence on structural wobble
	wave += 0.6 * iWow1 * wowNoise;

	// kaleidoscopic iterated function
	const int count = 4;
	float a = 1.0;
	float scene = 1000.;
	float shape = 1000.;
	for (int index = 0; index < count; ++index) {
		// fold and translate
		p.xz = abs(p.xz) - (.5 + wave)*a;

		// rotate
		p.xz *= rot(angle.y/a);
		p.yz *= rot(angle.x/a);
		p.yx *= rot(angle.z/a);

		// sphere
		shape = length(p) - 0.2*a*size;

	// material blending; WOW1 increases smoothing to change structural character
	float k = mix(0.25, 0.6, iWow1) * a;
	material = mix(material, float(index), smoothing(shape, scene, k));

		// add with a blend
		scene = smin(scene, shape, 1.*a);

		// falloff transformations
		a /= 1.9;
	}
	return scene;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	// initialize
	fragColor = vec4(0.0, 0.0, 0.0, 1.0);
	material = 0.0;

	// normalized screen coordinates
	vec2 uv = (fragCoord.xy - iResolution.xy * 0.5) / iResolution.y;

	// Apply rotation (Angle + Spin combined into iRotationAngle by the host)
	uv *= rot(-iRotationAngle);

	// simple camera
	vec3 eye = vec3(1.0, 1.0, 1.0);
	vec3 at  = vec3(0.0, 0.0, 0.0);
	vec3 z   = normalize(at - eye);
	vec3 x   = normalize(cross(z, vec3(0.0, 1.0, 0.0)));
	vec3 y   = cross(x, z);
	vec3 ray = normalize(vec3(z + uv.x * x + uv.y * y));
	vec3 pos = eye;

	// no mouse in TE fireflies style – keep camera static
	// white noise seed
	vec3 seed = vec3(fragCoord.xy, iTime);
	rng = hash13(seed);

	// raymarch
	const float steps = 30.0;
	float index;
	for (index = steps; index > 0.0; --index) {
		// volume estimation
		float dist = map(pos);
		if (dist < 0.01) {
			break;
		}
		// dithering
		dist *= 0.9 + .1 * rng;
		// ray step
		pos += ray * dist;
	}

	// ambient occlusion-ish shading from steps count
	float shade = index / steps;

	// normal estimate (NuSan)
	vec2 off = vec2(.001, 0.0);
	vec3 normal = normalize(
		map(pos) - vec3(
			map(pos - off.xyy),
			map(pos - off.yxy),
			map(pos - off.yyx)
		)
	);

	// palette (IQ)
	vec3 tint = .5 + .5 * cos(vec3(3.0, 2.0, 1.0) + material*.5 + length(pos)*.5);

	// lighting
	float ld = dot(reflect(ray, normal), vec3(0,1,0))*0.5 + 0.5;
	vec3 light = vec3(1.000, 0.502, 0.502) * sqrt(ld);
	ld = dot(reflect(ray, normal), vec3(0,0,-1))*0.5 + 0.5;
	light += vec3(0.400, 0.714, 0.145) * sqrt(ld) * .5;

	// color
	vec4 color = vec4((tint + light) * shade, 1.0);

	// temporal buffer (fireflies style): cool previous frame
	// WOW2 controls the decay intensity (higher = faster fade)
	vec4 prev = texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0);
	// Lower iWow2 = faster fade; Higher iWow2 = slower fade (stronger range)
	float decay = mix(0.16, 0.001, clamp(iWow2, 0.0, 1.0));
	prev = max(vec4(0.0), prev - vec4(decay));
	fragColor = max(color, prev);
}

