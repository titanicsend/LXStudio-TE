#pragma auto
#pragma name "SuperSimpleDemo"

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // normalize incoming coordinates
    vec2 uv = fragCoord/iResolution.xy;

    // Time varying pixel color
    vec3 col = 0.5 + 0.5*cos(iTime+uv.xyx+vec3(0,2,4));

    // Output to screen
    fragColor = vec4(col,1.0);
}