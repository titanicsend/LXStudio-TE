precision mediump float;

// Plot a line on Y using a value between 0.0-1.0
float plot(vec2 st) {
    return smoothstep(0.02, 0.0, abs(st.y - st.x));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )  {
	vec2 st = fragCoord.xy/iResolution.xy;

    float y = st.x;

    vec4 color = vec4(0);

    // Plot a line
    float pct = plot(st);
    color = (1.0-pct)*color+pct*vec4(0.0,1.0,0.0,0.1);

	fragColor = color;
}