//fork of https://www.shadertoy.com/view/7dtfzr

float circle(vec2 pos, vec2 uv, float rad) {
	return smoothstep(rad, rad - 0.1, length(pos - uv));

}

float ring(vec2 pos, vec2 uv, float rad) {
    float length = length(pos - uv);
    return smoothstep(rad - 0.5, rad, length) * smoothstep(rad + 0.25, rad, length);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
    vec3 col = vec3(0.9);

	col = 0.5 + 0.5*cos(iTime+uv.xyx+vec3(0,2,4));


    float size = sin(iTime) + sin(iTime * 2.0) + sin(iTime*5.0);
    size = abs(size) * 0.02 + 0.2;
    float c = circle(vec2(0, -1.05), vec2(uv.x, uv.y - sqrt(abs(uv.x) + 1.0)), size);


    float pulse = ring(vec2(0), uv, (iTime / 2.0 - floor(iTime * 0.5)) * 2.0);

    col += c * 2.0;
    col += pulse;
    col = vec3(col.r, col.g * (uv.x + 0.5), col.b * (uv.y + 0.5));
    // Output to screen
    fragColor = vec4(col,1.0);
}
