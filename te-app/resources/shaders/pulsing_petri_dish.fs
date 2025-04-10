const float PI = 3.1415924;
const float TAU = PI * 2.0;
const float QUARTER_WAVE = PI / 2.0;

// Sphere - Signed Distance Function
float sphere(vec3 p, float radius) {
    return length(p) - radius;
}

// Locate objects
float map(vec3 p)
{
    // Sphere radius
    float sphereSize = 0.6;

    // Transform coordinate space so spheres repeat
    vec3 q = fract(p) * 2.0 - 1.1;

    int tx = int(q.y);
    float fft = 0.0;   // reads texture in original pattern
    fft *= 1.0;

    // Signed distance of sphere
    float s = sphere(abs(tan(q)), sphereSize);

    // unused calculations involving sampled texture
    //float d = 0.08 * (cos(q.x * 10. * fft) * cos(q.y * 10. * fft) * tan(q.z * 10. * fft));
    //return s +wave;
    return s;
}


// Trace rays
float trace(vec3 origin, vec3 r)
{
    float t = 0.0; // Distance Traveled
    for (int i = 0; i < 5; ++i) {
        vec3 p = origin + r * t;
        float d = map(p); // Locate object
        t += d * 0.8; // Step along the ray
    }
    return tan(t); //tan inverses shadows
}

float wave(float n) {
    return 0.5 + 0.5 * sin(-QUARTER_WAVE + TAU * fract(n));
}

// build 2D rotation matrix
mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord / iResolution.xy;
    uv = uv * 2. - 1.; // Remap the space to -1. to 1.
    uv.x *= iResolution.x / iResolution.y;

    // Create ray to fire into scene
    vec3 ray = normalize(vec3(uv, 1.5 * iScale));
    ray.xy *= rotate(-iRotationAngle);

    // Create origin of scene
    vec3 origin = vec3(0., 0., iTime); //iTime changes z perspective, going into screen

    // Trace any objects in the scene
    float t = trace(origin, ray);

    // Generate brightness based on distance from objects
    float fog = 1.0 / (1.0 + t * t * 0.1);

    // Wow2 controls foreground vs gradient color mix.
    vec3 fc = fog * mix(iColorRGB, mix(iColorRGB, iColor2RGB, wave(fog * 3.0)), iWow2);

    fragColor = vec4(fc, 1.);
}