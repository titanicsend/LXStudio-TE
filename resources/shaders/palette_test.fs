// fork of https://www.shadertoy.com/view/WtdXR8
// Demo of new iPalette capability - switches between palette colors every second


// these constants are predefined and can be used by all shaders
// to index iPalette[] when choosing a palette color:
//
// TE_BACKGROUND  - Background color
// TE_TRANSITION  - Gradient path from TE_BACKGROUND to TE_PRIMARY
// TE_PRIMARY     - Primary color to use on edges or panels
// TE_SECONDARY   - Secondary color to use on edges or panels
// TE_SECONDARY_BACKGROUND  - Background color path from TE_SECONDARY


void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    vec2 uv =  ({%tile[.5,2,15]} * fragCoord - iResolution.xy) / min(iResolution.x, iResolution.y);

    for(float i = 1.0; i < {%curve[10,1,10]}; i++){
        uv.x += 0.6 / i * cos(i * {%xWave[2.5,1,10]}* uv.y + iTime);
        uv.y += 0.6 / i * cos(i * {%yWave[1.5,1,10]} * uv.x + iTime);
    }

    //fragColor = vec4(vec3(0.1)/abs(sin(iTime-uv.y-uv.x)),1.0);

    // switch between palette colors at 1hz!
    vec3 col = iPalette[int(mod(iTime,5.0))];
    fragColor = vec4(col*abs(sin(iTime-uv.y-uv.x)),1.0);
}
