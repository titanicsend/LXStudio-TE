#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

// how far to move in our backbuffer to get the next pixel
const float stepx = 1.0/640.0;
const float stepy = 1.0/480.0;

// Sobel filter implementation from edge detection shader by Jeroen Baert
// jeroen.baert@cs.kuleuven.be
//
float intensity(in vec4 color){
    return sqrt((color.x*color.x)+(color.y*color.y)+(color.z*color.z));
}

vec4 sobel(vec2 center){
    // sample neighborhood around pixel
    float tleft = intensity(texture(iBackbuffer, center + vec2(-stepx, stepy)));
    float left = intensity(texture(iBackbuffer, center + vec2(-stepx, 0)));
    float bleft = intensity(texture(iBackbuffer, center + vec2(-stepx, -stepy)));
    float top = intensity(texture(iBackbuffer, center + vec2(0, stepy)));
    float bottom = intensity(texture(iBackbuffer, center + vec2(0, -stepy)));
    float tright = intensity(texture(iBackbuffer, center + vec2(stepx, stepy)));
    float right = intensity(texture(iBackbuffer, center + vec2(stepx, 0)));
    float bright = intensity(texture(iBackbuffer, center + vec2(stepx, -stepy)));

    // apply horizontal and vertical portions of the Sobel filter
    float x = tleft + 2.0*left + bleft - tright - 2.0*right - bright;
    float y = -tleft - 2.0*top - tright + bleft + 2.0 * bottom + bright;
    float color = sqrt((x*x) + (y*y));

    return vec4(color * iColorRGB, 0.9995);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec4 color = texture(iBackbuffer, uv);

    // returns backbuffer color unless WoW2 control is active
    fragColor = (iWow2 > 0.0) ? mix(color, sobel(uv), iWow2) : color;
}