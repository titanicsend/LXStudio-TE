void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // invert the colors in the backbuffer
    fragColor = 1.0 - texture(iBackbuffer,fragCoord / iResolution.xy);
}