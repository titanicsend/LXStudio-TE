//fork of https://www.shadertoy.com/view/WtdXR8

void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    vec2 uv =  ({%tile[.5,2,15]} * fragCoord - iResolution.xy) / min(iResolution.x, iResolution.y);

    for(float i = 1.0; i < {%curve[10,1,10]}; i++){
        uv.x += 0.6 / i * cos(i * {%xWave[2.5,1,10]}* uv.y + iTime);
        uv.y += 0.6 / i * cos(i * {%yWave[1.5,1,10]} * uv.x + iTime);
    }

    //fragColor = vec4(vec3(0.1)/abs(sin(iTime-uv.y-uv.x)),1.0);
    fragColor = vec4(iColorRGB*abs(sin(iTime-uv.y-uv.x)),1.0);
}
