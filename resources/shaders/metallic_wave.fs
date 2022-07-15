void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float speed = 1.;

    float i = iTime;
	vec2 uv = fragCoord.xy / iResolution.xy * 2.0 - 1.0;
    vec4 c = vec4(1.0);
    float d = length(uv);
    float a = atan(uv.y, uv.x) + sin(i * 0.2) * .5;
    uv.x = cos(a) * d;
    uv.y = sin(a) * d;

    d -= i;
    uv.x += sin(uv.y * 2. + i) * .1;
    uv += sin(uv * 1234.567 + i) * iMouse.x * .5;
    c.r = abs(mod(uv.y + uv.x * speed * d, uv.x * 1.1));

	fragColor = c.rrra;
}