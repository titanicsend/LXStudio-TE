void mainImage(out vec4 fragColor, in vec2 coord) {
    vec2 uv = ( coord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);
    vec4 color = vec4(iColorRGB,1.);

    // blob size pulses with the beat, controlled by
    // WOW2
    float pulse = 0.01 - (iScale * iWow2 / 100. * beat);

    // calculate the contribution of each firefly to the current pixel
    float lit = 999.;
    for (float i = 0; i < iQuantity; i++) {
        // distance between fireflies;
        float t = iTime + i *.1;

        // position of the firefly - looks random, but is actually a deterministic
        // wobbly circular path.
        float d = 0.05 * i;
        vec2 v = cos(t*d)*vec2( cos(t*1.5), sin(t*3.) ) + sin(t*d) * vec2( cos(t*2.), sin(t*.75) );
        lit = min(lit, 0.25 * length(p-v) * iScale);
    }
    // cool the entire backbuffer by a small amount
    fragColor = max(vec4(0.),texture2D( iBackbuffer, uv ) - iWow1 / iScale);

    // add heat where the fireflies are
    fragColor += clamp(color * pulse/lit,0.,1.);
}