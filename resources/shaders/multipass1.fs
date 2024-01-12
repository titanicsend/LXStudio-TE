void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    fragColor = 1.0 - texture(iBackbuffer,fragCoord / iResolution.xy);
}