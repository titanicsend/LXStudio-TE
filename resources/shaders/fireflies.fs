// generate 2D rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

void mainImage(out vec4 fragColor, in vec2 coord) {
    vec2 uv = ( coord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);
    //p *= r2d(-iRotationAngle);

    // blob size pulses with the beat, amount controlled by levelReact
    float radius = iScale + (levelReact * iScale * (-1. + 2.0 * beat));

    // inter-firefly spacing changes w/bassRatio, controlled by frequencyReact
    // turned up, this gets really fun!
    float spacing = max(0.1,(bassRatio * frequencyReact) / 4.0);

    // offset the start time a little so that the fireflies don't start
    // grouped tightly together
    float flyTime = iTime + 3.;

    // calculate the contribution of each firefly to the current pixel
    float lit = 0.;
    for (float i = 0; i < iQuantity; i++) {
        // distance between fireflies;
        float t = flyTime + i * spacing;
        float d = 0.05 * i;
        // v is the current position of the firefly - looks random, but is actually a deterministic
        // wobbly circular path.
        vec2 v = cos(t*d)*vec2(cos(t*1.5), sin(t*3.) ) + sin(t*d) * vec2( cos(t*2.), sin(t*.75) );

        // brightness of the firefly at a given pixel location is inversely
        // proportional to the distance from firefly center.
        // we restrict light falloff range by making it so that distances
        // greater than the desired radius go negative, and get clamped to 0.
        float l = max(0.0,1.0 - length(p-v)/radius);
        // sharpen brightness curve so tail decay will look more natural
        lit += l * l;
    }
    // cool the entire backbuffer by a small amount
    fragColor = max(vec4(0.),texture(iBackbuffer, uv) - iWow1 / 10.);

    // iWow2 controls the mix of primary color vs. primary + secondary.
    // In cases where color 2 is black, we'll "adjust" the controls so only the primary is used
    // (Otherwise, fireflies will be significantly less visible w/single color palettes)
    float colorMix =  (iColor2RGB == vec3(0.)) ? 0. : iWow2;

    vec3 color = min(1.0,lit) * mix(iColorRGB, mix(iColorRGB, iColor2RGB, fract(lit)), colorMix);

    // add heat where the fireflies are
    fragColor += vec4(color,1.0);
}