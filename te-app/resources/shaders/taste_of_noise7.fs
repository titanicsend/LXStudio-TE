// taste of noise 7 by leon denise 2021/10/14
// adapted for TE pattern pipeline (fireflies style backbuffer + signatures)
// original credits: Inigo Quilez, David Hoskins, NuSan; reviews by Fabrice Neyret
#define TE_NOPOSTPROCESSING

#ifdef GL_ES
precision mediump float;
#endif

// ---------- SPEED / QUALITY ----------
#define RAY_STEPS      22
#define HIT_EPS        0.015
#define MAX_DIST       28.0
#define STEP_JITTER    0.92
#define FAST_LIGHTING  1
#define FAST_NORMAL    1
#define FBM1_OCTAVES   4
#define FBM3_OCTAVES   3

// ---- host param aliases (prevents iSize/iScale mismatches) ----
#ifndef iScale
#ifdef iSize
#define iScale iSize
#endif
#endif

#ifndef iQuantity
#ifdef iQty
#define iQuantity iQty
#endif
#endif

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

// Reactive controls (provided by host pattern system):
//   levelReact      → increases shape contrast, hardens unions, boosts rim lighting
//   frequencyReact  → pushes fold amplitude & per-cell variation frequency

// --------------------------
// 1D helpers for time-warp (value noise + fbm)
// --------------------------
float hash11(float x){ return fract(sin(x*123.456)*789.123); }
float noise1(float x){
	float i = floor(x), f = fract(x);
	float a = hash11(i), b = hash11(i+1.0);
	float u = f*f*(3.0 - 2.0*f);
	return mix(a,b,u);
}
float fbm1(float x){
	float a = 0.5;
	float s = 0.0;
	float f = 1.0;
	for (int k = 0; k < FBM1_OCTAVES; ++k){
		s += a * noise1(x * f);
		f *= 2.01;
		a *= 0.55;
		if (a < 0.03) {
			break;
		}
	}
	return s;
}

float noise3(vec3 x) {
	vec3 i = floor(x);
	vec3 f = fract(x);
	vec3 u = f*f*(3.0 - 2.0*f);

	float n000 = hash13(i);
	float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
	float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
	float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
	float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
	float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
	float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
	float n111 = hash13(i + vec3(1.0, 1.0, 1.0));

	float nx00 = mix(n000, n100, u.x);
	float nx10 = mix(n010, n110, u.x);
	float nx01 = mix(n001, n101, u.x);
	float nx11 = mix(n011, n111, u.x);
	float nxy0 = mix(nx00, nx10, u.y);
	float nxy1 = mix(nx01, nx11, u.y);
	return mix(nxy0, nxy1, u.z);
}

float fbm3(vec3 x) {
	float amplitude = 0.5;
	float value = 0.0;
	vec3 shift = vec3(37.1, 61.7, 12.3);
	for (int i = 0; i < FBM3_OCTAVES; ++i) {
		value += amplitude * noise3(x);
		x = x * 2.07 + shift;
		amplitude *= 0.53;
		if (amplitude < 0.06) {
			break;
		}
	}
	return value;
}

float fbm3Fast(vec3 x) {
	float amplitude = 0.5;
	float value = 0.0;
	vec3 shift = vec3(37.1, 61.7, 12.3);
	for (int i = 0; i < 3; ++i) {
		value += amplitude * noise3(x);
		x = x * 2.07 + shift;
		amplitude *= 0.53;
	}
	return value;
}

// tiny, deterministic per-cell drift (breaks cell-to-cell sync without seams)
float cellDrift(vec3 cell, float t) {
	// hash cell to a scalar, then meander it with slow fbm
	float h = hash13(cell);
	return fbm1(t * 0.031 + h * 17.0) - 0.5; // ~[-0.5, 0.5]
}

// signed distance field for scene
float map(vec3 p) {
	float levelAmt = clamp(levelReact, 0.0, 1.0);
	float freqAmt = clamp(frequencyReact, 0.0, 1.0);
	bool useFreqNoise = freqAmt > 0.001;
	float sizeAmt = clamp((iScale - 0.1) / (0.75 - 0.1), 0.0, 1.0);
	float qtyAmt  = clamp(iQuantity, 0.0, 1.0);
	float zoom = mix(1.6, 0.6, sizeAmt);
	p /= zoom;

	// (1) strengthened time-warp: two slow, incommensurate meanders
	const float warpAmp  = 60.0;
	const float warpFreq = 0.004;     // slower drift => longer pseudo-period
	float t  = iTime + warpAmp * (fbm1(iTime * warpFreq) - 0.5) + rng*0.9;
	float t2 = iTime * 0.731 + 34.0 * fbm1(iTime * 0.0113);

	// domain repetition
	float grid = mix(6.0, 3.0, sizeAmt);
	vec3 cell = floor(p/grid);
	p = repeat(p,grid);
	float cellHash = hash13(cell);

	// distance from origin with slight irrational scaling to break micro-patterns
	float dp = length(p) * 1.0039216;
	
	// Gentle incommensurate nudge to spatial coords for extra de-sync
	p *= vec3(1.0, 1.0002442, 0.9995117);

	// (2) quasi-periodic rotation params (irrational ratios to avoid common periods)
	vec3 angle = vec3(0.1, -0.5, 0.1)
	           + dp * vec3(0.47, 0.5*1.41421356, 0.53*1.61803399)
	           + p  * vec3(0.11*2.41421356, 0.09*1.32471796, 0.10*1.73205081)
	           + cell;

	// shrink sphere size with WOW1-driven micro-variation in time using secondary time warp
	float size = sin(rng*3.14159 + iWow1 * 0.5 * sin(t2 * 0.83 + cellHash * 6.28318));

	// (2) quasi-periodic wave (incommensurate multipliers for dp and t)
	// Use both time warps to break periodicity further
	float wave = sin(-dp*1.32471796 + 0.92387953*t + 0.67*t2 + cellHash*6.2831853) * 0.5;
	// WOW1: add a gentle, cell-based noisy wobble to the fold offset to evolve patterns
	float wowNoise = sin(t * 0.79 + t2 * 1.13 + cellHash * 6.28318);
	// Stronger WOW1 influence on structural wobble; frequencyReact scales motion
	wave += (0.6 * iWow1 + 0.25 * freqAmt) * wowNoise;
	if (useFreqNoise) {
		vec3 waveBase = p * 0.42 + vec3(0.68, 1.13, 1.79);
		waveBase.z += iTime * 0.16 + cellHash * 2.3;
		float waveNoise = fbm3Fast(waveBase);
		wave += (waveNoise - 0.5) * 0.35 * freqAmt;
	}

	// Extra fold amplitude for stronger silhouettes (driven by frequencyReact)
	float foldAmp = (0.5 + wave) * (1.0 + 0.8 * freqAmt) * mix(0.75, 1.3, qtyAmt);

	// slow, persistent drift plus noise jitter keeps growth paths evolving
	float drift =
		0.10 * cellDrift(cell, iTime * 0.21) +         // persistent meander
		0.08 * iWow1 * cellDrift(cell, iTime * 0.53);  // user-influenced wobble
	float driftJitter = 0.0;
	if (useFreqNoise) {
		vec3 driftBase = cell * 1.73;
		driftBase.z += iTime * 0.19 + 5.3;
		driftJitter = (fbm3Fast(driftBase) - 0.5) * (0.12 * freqAmt * mix(0.6, 1.25, qtyAmt));
	}

	// kaleidoscopic iterated function
	const int count = 4;
	float a = 1.0;
	float scene = 1000.;
	float shape = 1000.;
	float iterLimit = mix(1.5, float(count), qtyAmt);
	float noiseGain = 0.35 * freqAmt * mix(0.65, 1.2, qtyAmt);
	float driftGain = 0.08 * freqAmt * mix(0.6, 1.2, qtyAmt);
	for (int index = 0; index < count; ++index) {
		if (float(index) >= iterLimit) {
			break;
		}
		vec3 sampleP = p;
		float noiseScale = 0.62 + 0.08 * float(index);
		vec3 noiseBase = sampleP * noiseScale;
		noiseBase += vec3(
			hash13(cell + vec3(0.0, float(index) * 1.3, 2.0)),
			hash13(cell + vec3(1.7, 0.0, float(index) * 0.7)),
			hash13(cell + vec3(2.3, 1.1, 0.0))
		);
		noiseBase.z += iTime * 0.18 + float(index) * 1.73 + cellHash * 3.9;

		vec2 noiseOffset = vec2(0.0);
		float driftNoise = 0.0;
		if (useFreqNoise) {
			vec3 offsetA = noiseBase + vec3(13.1, 7.7, 3.0);
			vec3 offsetB = noiseBase + vec3(4.3, 17.1, 11.2);
			offsetA += vec3(0.0, 0.0, float(index) * 0.37);
			offsetB += vec3(0.0, 0.0, float(index) * 0.61);
			noiseOffset = vec2(
				fbm3Fast(offsetA),
				fbm3Fast(offsetB)
			) - 0.5;
			noiseOffset *= noiseGain;

			driftNoise = (fbm3Fast(noiseBase + vec3(21.7, 9.3, 5.5)) - 0.5) * driftGain;
		}
		float foldRadius = (foldAmp + drift + driftJitter + driftNoise) * a;
		// fold and translate with noise offset to randomize growth paths
		p.xz = abs(p.xz + noiseOffset) - foldRadius;

		float inva = 1.0 / a;
		float ay = angle.y * inva;
		float ax = angle.x * inva;
		float az = angle.z * inva;
		float sy = sin(ay), cy = cos(ay);
		float sx = sin(ax), cx = cos(ax);
		float sz = sin(az), cz = cos(az);

		// rotate using precomputed trig
		mat2 rotY = mat2(cy, -sy, sy, cy);
		mat2 rotX = mat2(cx, -sx, sx, cx);
		mat2 rotZ = mat2(cz, -sz, sz, cz);

		vec2 xz = rotY * p.xz;
		p.x = xz.x;
		p.z = xz.y;

		vec2 yz = rotX * vec2(p.y, p.z);
		p.y = yz.x;
		p.z = yz.y;

		vec2 yx = rotZ * vec2(p.y, p.x);
		p.y = yx.x;
		p.x = yx.y;

		// per-cell radius variance:
		//   - amplitude scales with levelReact (more distinction)
		//   - frequency scales with frequencyReact (finer differences)
		float varFreq = 1.0 + 4.0 * freqAmt * mix(0.7, 1.3, qtyAmt);
		float cellVar =
			1.0 + (hash13(cell * varFreq + float(index) * 3.11) - 0.5)
				* 0.8 * levelAmt * mix(0.7, 1.3, qtyAmt);
		shape = length(p) - 0.2 * a * size * cellVar;

		// material blending:
		//   lower baseK -> crisper transitions as levelReact rises
		float baseK = mix(0.60, 0.15, levelAmt);
		float k = mix(0.25, 0.6, iWow1) * a;    // keep WOW1 influence
		k = min(k, baseK * a);                  // enforce max smoothness from levelReact
		material = mix(material, float(index), smoothing(shape, scene, k));

		// union:
		//   blend from smooth min to hard min as levelReact increases
		float kUnion = 1.0 * a;
		float sminVal = smin(scene, shape, kUnion);
		float minVal  = min(scene, shape);
		float hardMix = smoothstep(0.5, 1.0, levelAmt);
		scene = mix(sminVal, minVal, hardMix);

		// falloff transformations
		a *= (1.0 / 1.9);
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
	uv = rot(-iRotationAngle) * uv;

	// simple camera
	vec3 eye = vec3(1.0, 1.0, 1.0);
	vec3 at  = vec3(0.0, 0.0, 0.0);
	vec3 z   = normalize(at - eye);
	vec3 x   = normalize(cross(z, vec3(0.0, 1.0, 0.0)));
	vec3 y   = cross(x, z);
	vec3 ray = normalize(uv.x * x + uv.y * y + z);
	vec3 pos = eye;

	// no mouse in TE fireflies style – keep camera static
	// white noise seed
	vec3 seed = vec3(fragCoord.xy, iTime);
	rng = hash13(seed);

	// raymarch
	const float steps = float(RAY_STEPS);
	float index;
	float lastD = 1e9;
	float traveled = 0.0;
	for (index = steps; index > 0.0; --index) {
		float d = map(pos);
		lastD = d;

		if (d < HIT_EPS) {
			break;
		}

		traveled += d;
		if (traveled > MAX_DIST) {
			index = 0.0;
			break;
		}

		d *= STEP_JITTER + (1.0 - STEP_JITTER) * rng;
		pos += ray * d;
	}

	// ambient occlusion-ish shading from steps count
	float shade = index / steps;
	// deepen cavities slightly as levelReact rises
	shade = pow(shade, mix(1.0, 0.75, clamp(levelReact, 0.0, 1.0)));

	// normal estimate (FAST_NORMAL)
#if FAST_NORMAL
	vec2 off = vec2(0.001, 0.0);
	float d0 = lastD;
	float dx = map(pos - vec3(off.x, off.y, off.y)) - d0;
	float dy = map(pos - vec3(off.y, off.x, off.y)) - d0;
	float dz = map(pos - vec3(off.y, off.y, off.x)) - d0;
	vec3 normal = normalize(vec3(d0 - dx, d0 - dy, d0 - dz));
#else
	vec2 off = vec2(.001, 0.0);
	vec3 normal = normalize(
		map(pos) - vec3(
			map(pos - off.xyy),
			map(pos - off.yxy),
			map(pos - off.yyx)
		)
	);
#endif

	// palette (IQ)
	vec3 tint = .5 + .5 * cos(vec3(3.0, 2.0, 1.0) + material*.5 + length(pos)*.5);

#if FAST_LIGHTING
	float nd1 = max(dot(normal, vec3(0.0, 1.0, 0.0)), 0.0);
	float nd2 = max(dot(normal, vec3(0.0, 0.0, -1.0)), 0.0);
	vec3 light = vec3(1.000, 0.502, 0.502) * nd1
		       + vec3(0.400, 0.714, 0.145) * 0.6 * nd2;
	float rim = pow(clamp(1.0 - dot(normal, -ray), 0.0, 1.0),
				mix(2.0, 0.7, clamp(levelReact, 0.0, 1.0)));
	light += vec3(rim) * (0.8 * clamp(levelReact, 0.0, 1.0));
#else
	float ld = dot(reflect(ray, normal), vec3(0,1,0))*0.5 + 0.5;
	vec3 light = vec3(1.000, 0.502, 0.502) * sqrt(ld);
	ld = dot(reflect(ray, normal), vec3(0,0,-1))*0.5 + 0.5;
	light += vec3(0.400, 0.714, 0.145) * sqrt(ld) * .5;
	float rim = pow(1.0 - max(dot(normal, -ray), 0.0),
				mix(2.0, 0.7, clamp(levelReact, 0.0, 1.0)));
	light += vec3(rim) * (0.8 * clamp(levelReact, 0.0, 1.0));
#endif

	// color with gentle ceiling to prevent firefly hotspot accumulation
	vec4 color = vec4((tint + light) * shade, 1.0);
	color.rgb = clamp(color.rgb, 0.0, 1.2);

	// ------------------------------------------------------------
	// Temporal feedback (a.k.a. "firefly trail"):
	// We read the previous frame (iBackbuffer), fade it slightly,
	// then keep the brightest value per pixel. This creates glowing
	// trails that persist and slowly cool down.
	//
	//   prev = prev - fade;        // cool down old pixels
	//   frag = max(current, prev); // keep brightest → firefly look
	// ------------------------------------------------------------
	vec4 prev = texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0);
	float w = clamp(iWow2, 0.0, 1.0);
	float decay = mix(0.16, 0.001, w * w);
	prev.rgb = max(prev.rgb - decay, 0.0);
	fragColor.rgb = max(color.rgb, prev.rgb);
	fragColor.a = 1.0;
}

