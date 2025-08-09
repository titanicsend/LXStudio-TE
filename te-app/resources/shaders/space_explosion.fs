// space_explosion v2:  The Volumetric Version
// adapted for TE from:
// https://www.shadertoy.com/view/Xd3GWn
// NOTE: I love how parameterized this is.


#include <include/constants.fs>
#include <include/colorspace.fs>

#define CAM_ROTATION_SPEED 11.7
#define CAM_TILT .20
#define CAM_DIST 2.8
#define MAX_MULT_EXPLOSIONS 15

// the bounding sphere of the entire explosion. We use this to reduce the number of rays we need
// to march through the explosion's density field.  If a ray lies outside the sphere, we know it isn't
// going to be in the explosion, and we can skip it.
const float expRadius = 1.75;
const int mult_explosions = MAX_MULT_EXPLOSIONS;	// how many explosion sub-spheres to draw
const int steps = 64;				// max ray marching steps
const float delay_range = 0.15;		// maximum delay for explosion start up.

float explosion_seed = 0.618;       // seed for explosion noise.
float downscale = 1.25;				// how much smaller (than expRadius) one explosion ball should be. bigger value = smaller. 1.0 = no scale down.
float grain = 3.0;					// increase for more detailed explosions
float speed = 0.5;					// animation speed (time stretch). nice = 0.5, default = 0.4
float ballness = 1.0;				// lower values makes explosion look more like a cloud. higher values more like a ball.
float growth = 2.2;					// initial growth to explosion ball. lower values makes explosion grow faster
float fade = 1.6;					// greater values make fade go faster but later. Greater values leave more smoke at the end.
float thinout_smooth = 0.7;			// thinning out of the outer bounding sphere. 1.0 = none, 0.0 = lots
float density = 1.35;				// higher values increase contrast
vec2 brightness = vec2(3.0, 2.2);	// x = constant offset, y = time-dependent factor
vec2 brightrad = vec2(1.3, 1.0);	// adds some variation to the radius of the brightness falloff.
float contrast = 1.0;				// final color contrast. higher values make ligher contrast. default = 1.0
float rolling_init_damp = 0.20;		// rolling animation initial damping. 0.0 = no damping. nice = 0.2, default = 0.15
float rolling_speed = 0.6;			// rolling animation speed (static over time). default = 1.0
float variation_seed = 0.618;		// influences position variation of the different explosion balls
float delay_seed = 0.0;				// influences the start delay variation of the different explosion balls
float ball_spread = 1.0;			// how much to spread ball starting positions from the up vector.

// Everything you need to know about the explosion's component spheres
struct Ball {
    vec3 offset;
    vec3 dir;
    float delay;
};

Ball balls[MAX_MULT_EXPLOSIONS];

float tmax = 1.0 + delay_range;
float getTime() {
    return fract(iTime * speed / tmax) * tmax;
}

// hash and noise functions from shadertoy
float hash( float n ) {
    return fract(cos(n)*41415.92653);
}

vec2 hash2( float n ) {
    return fract(sin(vec2(n,n+1.0))*vec2(13.5453123,31.1459123));
}

vec3 hash3( float n ) {
    return fract(sin(vec3(n,n+1.0,n+2.0))*vec3(13.5453123,31.1459123,37.3490423));
}

float hash13(vec3 p3) {
    p3  = fract(p3 * vec3(.1031,.11369,.13787));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract((p3.x + p3.y) * p3.z);
}

float noise( in vec3 x ) {
    vec3 f = fract(x);
    vec3 p = x - f;
    f = f*f*(3.0-2.0*f);

    float n = p.x + p.y*157.0 + 113.0*p.z;
    return mix(mix(mix( hash(n+  0.0), hash(n+  1.0),f.x),
    mix( hash(n+157.0), hash(n+158.0),f.x),f.y),
    mix(mix( hash(n+113.0), hash(n+114.0),f.x),
    mix( hash(n+270.0), hash(n+271.0),f.x),f.y),f.z);
}

float fbm( vec3 p, vec3 dir ) {
    float f;
    vec3 q = p - dir; f  = 0.50000*noise( q );
    q = q*2.02 - dir; f += 0.25000*noise( q );
    q = q*2.03 - dir; f += 0.12500*noise( q );
    q = q*2.01 - dir; f += 0.06250*noise( q );
    q = q*2.02 - dir; f += 0.03125*noise( q );
    return f;
}

// Get gradient color from the palette based on field density
vec4 applyColor( float density, float radius, float bright ) {
    // TODO: give the option for fire colored fire as well.
    vec4 result = vec4(bright * getGradientColor(density) * density * min( (radius+0.5)*0.588, 1.0 ) ,density);

    return result;
}

// map 3d position to density
float densityFn( in vec3 p, in float r, float t, in vec3 dir, float seed ) {
    float den = ballness + (growth+ballness)*log(t)*r;
    den -= (2.5+ballness)*pow(t,fade)/r;
    if ( den <= -3. ) return -1.;

    // offset noise based on seed
    // plus a time based offset for the rolling effect (together with the space inversion below)
    float s = seed-(rolling_speed/(sin(min(t*3.,1.57))+rolling_init_damp));
    dir *= s;

    // invert space
    p = -grain*p/(dot(p,p)*downscale);

    // participating media
    float f = fbm( p, dir );

    // add in noise with scale factor
    den += 4.0*f;
    return den;
}

// rad = radius of complete explosion (range 0 to 1)
// r = radius of the explosion ball that contributes the highest density
// rawDens = non-clamped density at the current marching location on the current ray
// foffset = factor for offset how much the offsetting should be applied. best to pass a time-based value.
void computeDensity( in vec3 pos, out float rad, out float r, out float rawDens, in float t, in float foffset, out vec4 col, in float bright )
{
    float radiusFromExpCenter = length(pos);
    rad = radiusFromExpCenter / expRadius;

    r = 0.0;
    rawDens = 0.0;
    col = vec4(0.0);

    for ( int k = 0; k < mult_explosions; ++k ) {
        float t0 = t - balls[k].delay;
        if ( t0 < 0.0 || t0 > 1.0 ) continue;

        vec3 p = pos - balls[k].offset * foffset;
        float radiusFromExpCenter0 = length(p);

        float r0 = downscale* radiusFromExpCenter0 / expRadius;
        if( r0 > 1.0 ) continue;

        float rawDens0 = densityFn( p, r0, t0, balls[k].dir, explosion_seed + 33.7*float(k) ) * density;

        // thin out the volume at the far extends of the bounding sphere to avoid
        // clipping with the bounding sphere
        rawDens0 *= 1.-smoothstep(thinout_smooth,1.,r0);

        float dens = clamp( rawDens0, 0.0, 1.0 );
        vec4 col0 = applyColor(dens, r0*(brightrad.x+brightrad.y*rawDens0), bright);	// also adds some variation to the radius

        // scale density to the range 0 to 1
        col0.a *= (col0.a + .4) * (1. - r0*r0);
        col0.rgb *= col0.a;
        col += col0;

        rawDens = max(rawDens, rawDens0);
    }
}

// use alpha to blend in contribution at current point
void addColor( in vec4 col, inout vec4 sum ) {
    sum = sum + col*(1.0 - sum.a);
    sum.a+=0.15*col.a;
}

vec4 raymarch( in vec3 rayo, in vec3 rayd, in vec2 expInter, in float t, out float d ) {
    vec4 sum = vec4( 0.0 );
    float step = 1.5 / float(steps);
    vec3 pos = rayo + rayd * (expInter.x);	// no dither

    float march_pos = expInter.x;
    d = 4000.0;

    // t goes from 0 to 1 + mult delay. that is 0 to 1 is for one explosion ball. the delay for time distribution of the multiple explosion balls.
    // t_norm is 0 to 1 for the whole animation (incl mult delay).
    float t_norm = t / tmax;
    float smooth_t = sin(t_norm*2.1);	//sin(t*2.);

    //float bright = 6.1;
    float t1 = 1.0 - t_norm;	// we use t_norm instead of t so that final color is reached at end of whole animation and not already at end of first explosion ball.

    float bright = brightness.x + brightness.y * t1*t1;

    for( int i=0; i<steps; i++ ) {
        if( sum.a >= 0.98 ) { d = march_pos; break; }
        if ( march_pos >= expInter.y ) break;

        float rad, r, rawDens;
        vec4 col;
        computeDensity( pos, rad, r, rawDens, t, smooth_t, col, bright );

        if ( rawDens <= 0.0 )
        {
            float s = step * 2.0;
            pos += rayd * s;
            march_pos += s;
            continue;
        }

        addColor( col, sum );

        // take larger steps through low densities.
        // something like using the density function as a SDF.
        float stepMult = 1.0 + (1.-clamp(rawDens+col.a,0.,1.));
        // step along ray
        pos += rayd * step * stepMult;
        march_pos += step * stepMult;
    }

    return clamp( sum, 0.0, 1.0 );
}

// iq's sphere intersection for a sphere centered at (0,0,0)
vec2 iSphere(in vec3 ro, in vec3 rd, in float rad) {
    float b = dot(ro, rd);
    float c = dot(ro, ro) - rad * rad;
    float h = b*b - c;

    if (h < 0.0) {
        return vec2(-1.0);
    }

    // this should really be sqrt(h), but we just need a rough estimate - we're
    // distorting the heck out of spheres anyway.
    h *= 0.5;
    return vec2(-b-h, -b+h);
}

vec3 computePixelRay( in vec2 p, out vec3 cameraPos ) {
    // camera orbits around explosion at fairly high speed to give it a
    // rolling effect.

    float camRadius = CAM_DIST;
    float a = iTime*CAM_ROTATION_SPEED;
    float b = CAM_TILT * sin(a * .014);

    float phi = b * PI;
    float camRadiusProjectedDown = camRadius * cos(phi);
    float theta = -(a-iResolution.x)/80.;
    float xoff = camRadiusProjectedDown * cos(theta);
    float zoff = camRadiusProjectedDown * sin(theta);
    float yoff = camRadius * sin(phi);
    cameraPos = vec3(xoff,yoff,zoff);

    // camera target
    vec3 target = vec3(0.);

    // camera frame
    vec3 fo = normalize(target-cameraPos);
    vec3 ri = normalize(vec3(fo.z, 0., -fo.x ));
    vec3 up = normalize(cross(fo,ri));

    // multiplier to emulate a fov control
    float fov = .5;

    // ray direction
    return normalize(fo + fov*p.x*ri + fov*p.y*up);
}

void setup() {
    // first ball always centered
    balls[0] = Ball(
      vec3(0.),
      vec3(0.,.7,0.),
      0.0
    );

    float pseed = variation_seed;
    float tseed = delay_seed;
    float maxdelay = 0.0;
    for ( int k = 1; k < mult_explosions; ++k ) {
        float pseed = variation_seed + 3. * float(k-1);
        float tseed = delay_seed + 3. * float(k-1);
        vec2 phi = hash2(pseed) * vec2(2.*PI, PI*ball_spread);
        vec2 tilted = vec2( sin(phi.y), cos(phi.y) );
        vec3 rotated = vec3( tilted.x * cos(phi.x), tilted.y, tilted.x * sin(phi.x) );
        balls[k].offset = 0.7 * rotated;
        balls[k].dir = normalize( balls[k].offset );
        balls[k].delay = delay_range * hash(tseed);
        pseed += 3.;
        tseed += 3.;
        maxdelay = max(maxdelay, balls[k].delay);
    }

    if ( maxdelay > 0.0 ) {
        // Now stretch the ball explosion delays to the maximum allowed range.
        // So that the last ball starts with a delay of exactly delay_range and thus we do not waste any final time with just empty space.
        for ( int k = 0; k < mult_explosions; ++k )
        balls[k].delay *= delay_range / maxdelay;
    }
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    float t = getTime();

    setup();

    // get aspect corrected normalized pixel coordinate
    vec2 q = (fragCoord.xy / iResolution.xy);
    vec2 p = -1.0 + 2.0*q;
    p *= iScale;

    // move down a bit to center explosion on car
    p.y += 0.3;

    // set up camera for ray marching
    vec3 rayDir, cameraPos;
    rayDir = computePixelRay( p, cameraPos );

    // starting color is black
    vec4 col = vec4(0.);

    // does pixel ray intersect with the explosion's bounding sphere?
    vec2 boundingSphereInter = iSphere( cameraPos, rayDir, expRadius );
    if( boundingSphereInter.x > 0. ) {
        // if so, we raymarch through the explosion to get field density
        float d = 4000.0;
        col = raymarch( cameraPos, rayDir, boundingSphereInter, t, d );
    }

    // adjust color contrast and brightness
    col.xyz = col.xyz*col.xyz*(1.0+contrast*(1.0-col.xyz));
    col.a = 0.995;
    fragColor = col;
}
