#define LINE_COUNT 52
uniform vec4[LINE_COUNT] lines;

// Simple fbm noise system, used to generate the noise
// field we use for electri arcs
vec2 hash (in vec2 p) {
  p = vec2 (dot (p, vec2 (127.1, 311.7)),
            dot (p, vec2 (269.5, 183.3)));

  return -1. + 2.*fract (sin (p)*43758.5453123);
}

float noise (in vec2 p) {
  const float K1 = .366025404;
  const float K2 = .211324865;

  vec2 i = floor (p + (p.x + p.y)*K1);
   
  vec2 a = p - i + (i.x + i.y)*K2;
  vec2 o = step (a.yx, a.xy);    
  vec2 b = a - o + K2;
  vec2 c = a - 1. + 2.*K2;

  vec3 h = max (.5 - vec3 (dot (a, a), dot (b, b), dot (c, c) ), .0);

  vec3 n = h*h*h*h*vec3 (dot (a, hash (i + .0)),
                         dot (b, hash (i + o)),
                         dot (c, hash (i + 1.)));

  return dot (n, vec3 (52.));
}

float fbm(vec2 pos, float tm) {
    vec2 offset = vec2(cos(tm), 0.0);
    float aggr = 0.0;
    
    aggr += noise(pos);
    aggr += noise(pos + offset) * 0.5;
    aggr += noise(pos + offset.yx) * 0.25;
    aggr += noise(pos - offset) * 0.125;

    aggr /= 1.0 + 0.5 + 0.25 + 0.125;

    return (aggr * 0.5) + 0.5;
}

// NOTE: Two color shader.  Uses iColorRGB as the first color, and the
// secondary palette color as the second color.
vec3 electrify(vec2 pos, float offset,float direction) {
    vec3 col = vec3(0.0);
    vec2 f = vec2(0.0, iTime * 0.25*direction);
    float noiseMag = offset * iWow1;

    for (int i = 0; i < 2; i++) {
        float time = direction * iTime + float(i);

        float d1 = abs(noiseMag / (offset - fbm((pos + f) * 2.55, 2. * time)));
        float d2 = abs(noiseMag / (offset - fbm((pos + f) * 1.41, time + 10.0)));
        col += vec3(d1 * iColorRGB);
        col += vec3(d2 * iPalette[TE_SECONDARY]);
    }
    
    return col;
}

// more-or-less standard signed distance to line segment
float glowline2(vec2 p, vec4 seg,float width) {
    vec2 ld = seg.xy - seg.zw;
    vec2 pd = p - seg.zw;
    
    return length(pd - ld*clamp( dot(pd, ld)/dot(ld, ld), 0.0, 1.0)) - width;    
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = -1. + 2. * fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    uv *= 0.5;

    vec3 finalColor = vec3(0.0);
    for (int i = 0; i < LINE_COUNT; i++) {

      float dist = glowline2(uv,lines[i],iScale);

      // add contribution of this segment's "electric arcs"
      vec3 col = electrify(uv, dist + iQuantity,(mod(i,2) == 0) ? 1. : -1.);
      col *= smoothstep(0.125,-0.01,dist);

      // add some glow to the base line
      col += iColorRGB * (0.75-dist) * smoothstep(0.01, -0.0051, dist);
      finalColor += col * col * col;
	}
    
    fragColor = vec4(finalColor, max(finalColor.r,max(finalColor.g,finalColor.b)));
}






