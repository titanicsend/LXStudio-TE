// Sphere - Signed Distance Function
float sphere(vec3 p, float radius){
    return length(p)-radius;
}


// Locate objects
float map(vec3 p)
{
    // Sphere radius
    float sphereSize = 0.6;

    // Transform coordinate space so spheres repeat
    vec3 q = fract(p) * 2.0 - 1.1;

    int tx = int(q.y);
    float fft  = texelFetch( iChannel0, ivec2(tx,0), 0 ).x;
	fft *= 1.0;

    // Signed distance of sphere
    float s = sphere(abs(tan(q)), sphereSize);
    float d = 0.08 * (cos(q.x*10.*fft)*cos(q.y*10.*fft) * tan(q.z*10.*fft)  );
    //return s +wave;
    return s+d;
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


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;
    vec3 color = 0.5 + 0.5*cos(iTime+uv.xyx+vec3(0,2,4));

    uv = uv *2.-1.; // Remap the space to -1. to 1.
    uv.x *= iResolution.x/iResolution.y;

   	// Create ray to fire into scene
   	vec3 ray = normalize(vec3(uv, 1.5));

    // Create origin of scene
    vec3 origin = vec3(0., 0.,iTime); //iTime changes z perspective, going into screen


    // Trace any objects in the scene
    float t = trace(origin, ray);

    // Generate fog based on distance from t
    float fog = 1.0 / (1.0 + t * t * 0.1);

    // Final color with includes sdf + fog
    vec3 fc = vec3(fog);


    //fc -= abs(log(tan(color))); //different transitioning of color
    fc -= tan(color);

    // Output to screen
    fragColor = vec4(fc, color);
}