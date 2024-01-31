uniform sampler2D ndivideo;


// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return origin + rotationMatrix * (point - origin);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv.y = 1.0 - uv.y;

    uv = rotate(uv, vec2(0.5), -iRotationAngle);

    vec4 vid =  texture(ndivideo, uv);
    float gray = dot(vid.rgb, vec3(0.299, 0.587, 0.114));

    fragColor = mix(vid, vec4(gray * iColorRGB, vid.a), iWow1);
}