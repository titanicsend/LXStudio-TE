void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord.xy / iResolution.xy;
    float time = iTime;
    vec3 raintex = texture(iChannel1,vec2(uv.x*2.0,uv.y*0.1+time*0.125)).rgb/8.0;
    vec2 where = (uv.xy-raintex.xy);
    vec3 texchur1 = texture(iBackbuffer,vec2(where.x,where.y)).rgb;

    fragColor = vec4(texchur1,1.0);
}