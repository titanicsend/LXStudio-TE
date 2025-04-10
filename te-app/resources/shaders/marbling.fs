//fork of https://www.shadertoy.com/view/WtdXR8

float TIME = -iTime;

void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    vec2 uv =  (iScale * fragCoord - iResolution.xy) / min(iResolution.x, iResolution.y);

    for(float i = 1.0; i < iQuantity; i++){
        uv.x += 0.6 / i * cos(i * iWow1* uv.y + TIME);
        uv.y += 0.6 / i * cos(i * iWow2 * uv.x + TIME);
    }

    vec3 col = iColorRGB*abs(sin(TIME-uv.y-uv.x));
    fragColor = vec4(col, 1.);
}
