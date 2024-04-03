uniform vec2[250] points;
uniform int numPoints;
uniform float totalLength;

// https://iquilezles.org/articles/distfunctions2d/
float sdSegment( in vec2 p, in vec2 a, in vec2 b )
{
    vec2 pa = p-a, ba = b-a;
    float h = clamp( dot(pa,ba)/dot(ba,ba), 0.0, 1.0 );
    return length( pa - ba*h );
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 st = fragCoord.xy/iResolution.xy;
    st = st * 2. - 1.;
    st.x *= iResolution.x/iResolution.y;

    // position it centrally on a panel
    st.x += 0.8;
    st.y += 0.8;

    st /= iScale;

    vec3 color = vec3(0.2);
    float pct = 0.;

    float norm_y = 0.5 + 0.5*sin(iTime);

    float desiredHeight = 0.5;

    float maxDist = totalLength * desiredHeight * norm_y;

    float totalDist = 0.0;
    for (int i = 0; i < numPoints; i++) {
        vec2 a = points[i];
        vec2 b = points[i+1];
        a *= desiredHeight;
        b *= desiredHeight;
        float nextDist = distance(a, b);
        if (totalDist + nextDist < maxDist) {
            totalDist += nextDist;
            float seg = sdSegment(st, a, b);
            pct += 1.-step(0.005, seg);
        } else {
            float targetDist = maxDist - totalDist;
            float ratio = targetDist / nextDist;
            vec2 delta = b - a;

            float seg = sdSegment(st, a, a + ratio*delta);
            pct += 1.-step(0.005, seg);
            break;
        }
    }

    color = vec3(pct);
    fragColor = vec4(color,1.0);
}
