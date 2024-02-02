// default primary shader for NDI video patterns
// just displays the incoming image plus or minus
// a few basic effects based on common control settings.
uniform float gain;
uniform sampler2D ndivideo;

#define TE_NOTRANSLATE

// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return origin + rotationMatrix * (point - origin);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalize coords and flip y for OpenGL
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv.y = 1.0 - uv.y;

    // scale uv while keeping it centered in the window
    vec2 offset = vec2(0.5) * (1.0-iScale);
    uv = offset + uv * iScale;

    // rotate about current image center
    uv = rotate(uv, vec2(0.5), -iRotationAngle);

    // translate
    uv += vec2(-1.0,1.0) * iTranslate;

    // out-of-range pixels are transparent black
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0);
        return;
    }

    // otherwise, get the color, and calculate its luminance value
    vec4 vid =  gain * texture(ndivideo, uv);
    float gray = dot(vid.rgb, vec3(0.299, 0.587, 0.114));

    // return color/grayscale mixed based on Wow1 control setting
    fragColor = mix(vid, vec4(gray * iColorRGB, 1.0), iWow1);
}