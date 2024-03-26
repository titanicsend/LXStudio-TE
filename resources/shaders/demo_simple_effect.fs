#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

// super simple colorizer.  Applies the current foreground color to the image.
void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    vec2 uv = fragCoord.xy / iResolution.xy;
    vec4 color = texture(iBackbuffer, uv);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // return color/grayscale mixed based on the uniform iWow2
    // you can use any uniform you want - I used iWow2 here because
    // it's in the framework and I don't have to declare it, plus it
    // makes this shader immediately usable in multipass patterns.
    fragColor = mix(color, vec4(gray * iColorRGB, color.a), iWow2);
}