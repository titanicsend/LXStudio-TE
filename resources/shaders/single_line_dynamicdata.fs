#define TE_NOTRANSLATE

uniform vec2[250] currPoints;
uniform int currCount;
uniform float currLength;
uniform float currProgress;

uniform vec2[250] prevPoints;
uniform int prevCount;
uniform float prevLength;
uniform float prevProgress;

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

    st.y += iTranslate.y;

    vec3 color = vec3(0.0);
    float currPct = 0.;
    float prevPct = 0.;

    float drawingProgress = currProgress; //0.5 + 0.5*sin(iTime);
    float stroke = 0.005;

    // before rendering the current drawing, mirror the coordinate space along the x-axis,
    // with a gap defined by XPOS control.
    vec2 currST = vec2(abs(st.x) - iTranslate.x + 0.2*bassLevel, st.y);
    // use SIZE control to set the size of the drawing, without modifying the scale of the
    // whole coordinate space.
    float desiredHeight = iScale;

    // how much of the drawing should be drawn.
    float currPartialLength = currLength * desiredHeight * drawingProgress;

    // how much of the drawing has been covered as we loop through the line segments,
    // looking for a hit on the distance field of one segment.
    float currTotalDist = 0.0;
    // whether or not we should break out of the loop to avoid unnecessary computations
    // (when we don't intend to render the full drawing).
    bool stopIter = false;

    for (int i = 0; i < currCount; i++) {
        vec2 a = currPoints[i];
        vec2 b = currPoints[i+1];
        a *= desiredHeight;
        b *= desiredHeight;
        float nextDist = distance(a, b);

        float seg = sdSegment(currST, a, b);

        // if the total distance covered so far plus the length of the current line segment is less than the total
        // length of the drawing we want to render, use the full line from 'a' to 'b'.
        if (currTotalDist + nextDist < currPartialLength) {
            currTotalDist += nextDist;
        } else {
            // otherwise, we're on the last line segment we need to render. the rest of the points are irrelevant, so
            // stop looping through them after we've given the render instruction.
            stopIter = true;

            // determine what 'fraction' of the current line we should draw, and compute the distance from a line segment
            // starting at 'a' and ending at 'b' - 'a' * 'fraction'
            float targetDist = currPartialLength - currTotalDist;
            float ratio = targetDist / nextDist;
            vec2 delta = b - a;
            seg = sdSegment(currST, a, a + ratio*delta);
        }

        // apply a threshold to the segment distance to get our line drawn.
        currPct += 1.-step(stroke * (1. + 0.5*trebleRatio), seg);

        // if either:
        // (a) our progress through the drawing doesn't require looping through the remaining points, or
        // (b) we already made a match on at least one segment,
        // we can stop looping.
        if (stopIter || seg <= stroke) {
            break;
        }
    }
    color += currPct * iColorRGB;

    // how much of the drawing should be drawn.
    float prevHeight = (1. + 3.*drawingProgress) * desiredHeight;
    float inverseProgress = (1. - .8*drawingProgress);
    float prevPartialLength = prevLength * prevHeight * inverseProgress;

    vec2 prevST = vec2(abs(st.x) - inverseProgress*iTranslate.x + 0.2*bassLevel, st.y);

    // how much of the drawing has been covered as we loop through the line segments,
    // looking for a hit on the distance field of one segment.
    float prevTotalDist = 0.0;
    // whether or not we should break out of the loop to avoid unnecessary computations
    // (when we don't intend to render the full drawing).
    stopIter = false;

    for (int i = 0; i < prevCount; i++) {
        vec2 a = prevPoints[i];
        vec2 b = prevPoints[i+1];
        a *= prevHeight;
        b *= prevHeight;
        //a *= desiredHeight;
        //b *= desiredHeight;
        float nextDist = distance(a, b);

        float seg = sdSegment(prevST, a, b);

        // if the total distance covered so far plus the length of the prevent line segment is less than the total
        // length of the drawing we want to render, use the full line from 'a' to 'b'.
        if (prevTotalDist + nextDist < prevPartialLength) {
            prevTotalDist += nextDist;
        } else {
            // otherwise, we're on the last line segment we need to render. the rest of the points are irrelevant, so
            // stop looping through them after we've given the render instruction.
            stopIter = true;

            // determine what 'fraction' of the prevent line we should draw, and compute the distance from a line segment
            // starting at 'a' and ending at 'b' - 'a' * 'fraction'
            float targetDist = prevPartialLength - prevTotalDist;
            float ratio = targetDist / nextDist;
            vec2 delta = b - a;
            seg = sdSegment(prevST, a, a + ratio*delta);
        }

        // apply a threshold to the segment distance to get our line drawn.
        prevPct += 1.-step(stroke * (1. + bassRatio) * (2 * (1. + drawingProgress)), seg);

        // if either:
        // (a) our progress through the drawing doesn't require looping through the remaining points, or
        // (b) we already made a match on at least one segment,
        // we can stop looping.
        if (stopIter || seg <= stroke) {
            break;
        }
    }
    color += prevPct * iColor2RGB;

/*
    // debugging: draw coord space axes.
    color.r += 1. -smoothstep(0., 0.01, abs(st.x));
    color.g += 1. -smoothstep(0., 0.01, abs(st.y));
*/

    fragColor = vec4(color,1.0);
}
