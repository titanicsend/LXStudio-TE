#pragma name "StarField2"
#pragma TEControl.SIZE.Range(0.0175,.005,0.04)
#pragma TEControl.QUANTITY.Value(0.8)
#pragma TEControl.WOW2.Range(0.1,0.0,0.3)
#pragma TEControl.WOW2.Disable
#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable

// Simpler stars, but you can have *lots* of them!

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

float star(vec2 uv,float scale,float seed) {
    uv *= scale;

    // determine which uv tile we're in. We use this
    // to seed the prng to make sure we get the same star
    // in every frame.
    vec2 s=floor(uv);
    vec2 f=fract(uv);

    // generate random position
    vec2 p=.5+.44*sin(11.*fract(sin((s+seed)*mat2(7.5,3.3,6.2,5.4))*55.))-f;
    float d = length(p);
    float k = min(d,3.0);

    // draw smoothed blob for star
    return smoothstep(0.,k,iScale);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv=(fragCoord.xy * 2.-iResolution.xy) / iResolution.y;
    uv *= rot(iRotationAngle);

    float c = 0;
    float q = iQuantity * 10.0;
    for(float i=0.;i<20.;i+=2.) {
        c += star(uv,mod(q+i-iTime,q),i*1.61823);
    }
    vec3 color = (iColorRGB * c) + iWow2 * iColor2RGB;
    fragColor = vec4(color,1.0);
}
