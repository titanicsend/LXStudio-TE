// fork of https://www.shadertoy.com/view/WsSXzt

vec2 rotate(vec2 point, vec2 origin, float angle) {
    float c = cos(angle); float s = sin(angle);
    mat2 rotationMatrix = mat2(c, -s, s, c);
    return origin + rotationMatrix * (point - origin);
}

// rotate (2D) a point about the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    return rotate(point, vec2(0.0), angle);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;

    // iWow2 controls how much the rotation speed changes w/radius,
    // which we use later to generate a sawtooth effect
    uv = rotate(uv,iRotationAngle + iWow2 * length(uv));

    uv *= iScale;

    vec2 gv = uv * 2.*iQuantity;
    gv = fract(gv) - 0.5;

    float t = -iTime * 4.0;

    // build a wave to perturb the radius of the main circular pulse
    float d = iWow1 * sin(9. * atan(uv.x,uv.y));

    // generate circular waves
    float s = 0.5 + 0.5 * sin(t - (d+length(uv * 2.0)) * 5.0);

    // draw the pixelated dots between waves
    float m = smoothstep(s, s - 0.05, length(gv)) + s * 2.0;

    vec3 col = mix(iColor2RGB,iColorRGB,s) * min(1.1,(m - (m * 0.44 * s)));
    fragColor = vec4(col, 1.);
}


