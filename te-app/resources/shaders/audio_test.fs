void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 h = (iResolution.xy*0.5);
	float r = length((fragCoord.xy - h) / h.y);
    //fragColor = vec4(0.0,r > 1.0 ? 0.0 : .8,0.0, 1.0);
    fragColor = vec4(0.0,r > 1.0 ? 0.0 : texture( iChannel0, vec2(r,0) ).x,0.0, 1.0);
}
