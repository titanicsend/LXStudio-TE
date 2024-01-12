
void mainImage(out vec4 fragColor, in vec2 coord) {

    vec2 uv = ( coord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);
    vec4 color = vec4(iColorRGB,1.);

    float lit = 999.;
    for(float i = 0; i < iQuantity; i++) {
        float t = iTime + float(i)*.1;
        vec2 v = cos(t*.1*float(i))*vec2( cos(t*1.5), sin(t*3.) ) + sin(t*.1*float(i)) * vec2( cos(t*2.), sin(t*.75) );
        lit = min(lit, length(p-v));
    }

    fragColor = vec4( .01/lit ) + 0.925  * color * texture2D( iBackbuffer, uv );
}