//fork of https://www.shadertoy.com/view/MssSRS

// Noise animation - Watery by nimitz (twitter: @stormoid)
// https://www.shadertoy.com/view/MssSRS
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
// Contact the author for other licensing options

//The domain is rotated by the values of a preliminary fbm call
//then the fbm function is called again to color the screen.
//Turbulent fbm (aka ridged) is used for better effect.
//define centered to see the rotation better.

float time = iTime * -0.2;

vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}


mat2 makem2(in float theta) {float c = cos(theta);float s = sin(theta);return mat2(c, -s, s, c);}
float noise(in vec2 x) {return texture(iChannel1, x * .01).x;}

mat2 m2 = mat2(0.80, 0.60, -0.60, 0.80);
float fbm(in vec2 p)
{
    float z = 2.;
    float rz = 0.;
    for (float i = 1.;i < 7.; i++)
    {
        rz += abs((noise(p) - 0.5) * 2.) / z;
        z = z * {%noise[2, 1, 12]};
        p = p * {%noise2[2, 1, 12]};
        p *= m2;
    }
    return rz;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 p = fragCoord.xy / iResolution.xy * 2. - 1.;
    p.x *= iResolution.x / iResolution.y;
    p = rotate(p,iRotationAngle);
    p *= iScale;
    vec2 bp = p;

    // iWow2 controls switch to radial mode
    // TODO - if possible, should make this more visible in the LX UI
    bool CENTERED = (iWow2 > 0.5);
    if (!CENTERED) {
        p += 5.;
        p *= 0.6;
    }
    float rb = fbm(p * .5 + time * .17) * .1;
    rb = sqrt(rb);
    if (!CENTERED) {
        p *= makem2(rb * .2 + atan(p.y, p.x) * 1.);
    } else {
        p *= makem2(rb * .2 + atan(p.y, p.x) * 2.);
    }

    //coloring
    float rz = fbm(p * .9 - time * .7);
    rz *= dot(bp * 5., bp) + .5;
    rz *= sin(p.x * .5 + time * 4.) * 10.0;
    vec3 col = (iColorRGB) / sqrt(abs(.1 - rz));
    col = pow(col,vec3(2. * iWow1));
    fragColor = vec4(col, 1.);
}
