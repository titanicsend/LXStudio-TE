//fork of https://www.shadertoy.com/view/7dsXRr

#define EPSILON 0.001
#define PI 3.141592
#define MAX_DIST 32.0
#define MAX_STEPS 128
#define BLOOM_DEPTH 16.0
#define BLOOM_IT 128
#define ANG 0.0

float REPEAT;
float seed;
vec3 triColor;
float radius;

float rand()
{
    seed += 0.15342;
    return fract(sin(seed) * 35423.7652344);
}

mat2 rot(float ang)
{
    float s = sin(ang);
    float c = cos(ang);
    return mat2(c, -s, s, c);
}

vec3 rotVec(vec3 p, vec3 r)
{
    p.yz *= rot(r.x);
    p.xz *= rot(r.y);
    p.xy *= rot(r.z);
    return p;
}

// vector v indicates axes to rotate - e.g. (0.,1.,0.) rotates Y axis, (1.,1.,0) rotates X and Y, etc.
mat3 buildRotationMatrix3D(vec3 v, float angle) {
float c = cos(angle); float s = sin(angle);

  return mat3(c + (1.0 - c) * v.x * v.x, (1.0 - c) * v.x * v.y - s * v.z, (1.0 - c) * v.x * v.z + s * v.y,
    (1.0 - c) * v.x * v.y + s * v.z, c + (1.0 - c) * v.y * v.y, (1.0 - c) * v.y * v.z - s * v.x,
    (1.0 - c) * v.x * v.z - s * v.y, (1.0 - c) * v.y * v.z + s * v.x, c + (1.0 - c) * v.z * v.z
  );
}

vec3 makeRay(vec2 origin)
{
    vec2 res;
    res.x = origin.x - iResolution.x * 0.5;
    res.y = origin.y - iResolution.y * 0.5;
    return normalize(vec3(res / iResolution.yy, 0.5));
}

float capsule(vec3 a, vec3 b, float r, vec3 p)
{
    vec3 pa = p - a, ba = b - a;
  	float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
  	return length(pa - ba * h) - r;
}

vec3 dirByAng(float deg, float mag)
{
    float rad = deg * PI / 180.0;
    return vec3(sin(rad), cos(rad), 0) * mag;
}

float getDist(vec3 origin)
{
    float ang = origin.z / REPEAT * ANG - ANG;
    origin.z = mod(origin.z + REPEAT * 0.5, REPEAT) - REPEAT * 0.5;

    vec3 a = dirByAng(0.0 + ang, 1.0);
    vec3 b = dirByAng(120.0 + ang, 1.0);
    vec3 c = dirByAng(240.0 + ang, 1.0);

    float radius = 0.01 * iScale;
    float cap1 = capsule(a, b, radius, origin);
    float cap2 = capsule(b, c, radius, origin);
    float cap3 = capsule(c, a, radius, origin);
    return min(cap1, min(cap2, cap3));
}

vec3 getCol(float z)
{
float fac = (cos(z / REPEAT * PI) + 1.0) * 0.5;
return mix(iColorRGB,mix(iColorRGB,iColor2RGB,iWow2),fac);
}

float rayMarch(vec3 origin, vec3 direct)
{
    vec3 tmp;
    float res = 0.0;

    for (int i = 0; i < MAX_STEPS; i++)
    {
        tmp = origin + direct * res;
        float d = getDist(tmp);
        res += d;

        if (res >= MAX_DIST || d < EPSILON)
        	break;
    }

    triColor = getCol(tmp.z);
    return res;
}

vec3 getBloom(vec3 pos, vec3 dir)
{
    vec3 res = vec3(0);
    vec3 end = pos + dir * BLOOM_DEPTH;

    for (int i = 1; i <= BLOOM_IT; i++)
    {
        float fac = (float(i) + rand())  / float(BLOOM_IT);
        vec3 p = mix(pos, end, fac);

        // adjust distance to control triangle glow curve
        float d = getDist(p);  d = 0.05 + d * d;
        res += getCol(p.z) / d / float(BLOOM_IT);
    }

    return res * 0.25 * iWow1;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    radius = 0.01 * iScale;
    REPEAT = 2.0 * iQuantity;

    mat3 viewMat = buildRotationMatrix3D(vec3(0.,0.,1.),-iRotationAngle);

    seed = iTime + iResolution.y * fragCoord.x / iResolution.x + fragCoord.y / iResolution.y;
    vec3 pos = vec3(0, 0, iTime * REPEAT);
    vec3 dir = makeRay(fragCoord) * viewMat;

    float res = rayMarch(pos, dir);
    vec3 col = getBloom(pos, dir);

    if (res <= MAX_DIST) {
        col = triColor * max(1.0,iWow1);
    }

    fragColor = vec4(col, 1.);
}
