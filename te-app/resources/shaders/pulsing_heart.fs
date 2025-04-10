//fork of https://www.shadertoy.com/view/7dtfzr

float circle(vec2 pos, vec2 uv, float rad) {
	return smoothstep(rad, rad - 0.1, length(pos - uv));

}

float ring(vec2 pos, vec2 uv, float rad) {
    float length = length(pos - uv);
    return smoothstep(rad - 0.5, rad, length) * smoothstep(rad + 0.25, rad, length);
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
    uv = rotate(uv,iRotationAngle) * iScale;

    vec3 col = vec3(0.9);

    float size = 0.2 + 0.025 * beat; //sin(iTime) + sin(iTime * 2.0) + sin(iTime*5.0);
    //size *= 0.02 + 0.2;
    float c = circle(vec2(0, -1.05), vec2(uv.x, uv.y - sqrt(abs(uv.x) + 1.0)), size);

    float pulse = ring(vec2(0), uv, (iTime / 2.0 - floor(iTime * 0.5)) * 2.0);

    // iWow2 controls background level
    col = max(iColorRGB * iWow2,iColorRGB * c * c);
    col += iColorRGB * pulse;

    fragColor = vec4(col, 1.);
}
