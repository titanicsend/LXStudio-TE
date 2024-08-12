#pragma name "StarField1"
#pragma TEControl.QUANTITY.Range(1.5,2.5,0.15)
#pragma TEControl.SIZE.Range(0.05,0.025,0.125)
#pragma TEControl.WOW2.Value(0.7);
#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable

// Starfield with "more complicated" stars and clouds.

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

// prng from somewhere on shadertoy.
// TODO - there has to be a more efficient way to do this.
float rand(vec2 co){ return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453); }

// draw star with twinkly diffraction spikes
vec3 star(vec2 position) {
    // Spin the star, so the spikes will rotate too.
    position *= rot(iTime);

    // define spike angle, shape and brightness
    float spike = 0.7 * clamp(pow(sin(atan(position.y,position.x )*6.0)+0.1, 3.0), 0.0, 1.0);

    // Add spikes to star center blob and scale brightness
    float bri =  1.0 / length(position) * iScale;
    bri += spike * 1.0 / length(position)* (iScale * 1.5);

    return iColorRGB * bri;
}

// hash and noise functions from http://glsl.heroku.com/e#4982.0
float hash( float n ) { return fract(sin(n)*43758.5453); }

float noise( in vec2 x ) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n = p.x + p.y * 57.0;
    float res = mix(mix(hash(n+0.0), hash(n+1.0),f.x), mix(hash(n+57.0), hash(n+58.0),f.x),f.y);
    return res;
}


// simple galactic clouds from four octaves of noise
vec3 cloud(vec2 p) {
    float f = 0.0;
    f += 0.5000*noise(p*10.0);
    f += 0.2500*noise(p*20.0);
    f += 0.1250*noise(p*40.0);
    f += 0.0625*noise(p*80.0);
    f *= f * f;
    return vec3(f * iColor2RGB * iWow2);
}

// global star parameters
const float numLayers    = 5.0;
const float timeSpeed    = -0.005;

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {

    // center, normalize and convert to polar coordinates
    vec2   pos = fragCoord.xy - iResolution.xy * 0.5;
    pos *= rot(-iRotationAngle);
    float dist = length(pos) / iResolution.y;
    vec2 coord = vec2(pow(dist, 0.1), atan(pos.y, pos.x) / (3.1415926*2.0));

    // noise "clouds" in far background
    vec3 color = cloud(pos/iResolution.xy);

    // stars in the far background - really just points scattered
    // using the same cell subdivision method as the bigger stars
    float a = pow((1.0-dist),20.0);
    float t = timeSpeed * (iTime*-.05);
    float r = coord.x - t;
    float c = fract(a+coord.y + 0.0*.543);
    vec2  p = vec2(r, c*.5)*4000.0;
    vec2 uv = fract(p)*2.0-1.0;
    float m = clamp((rand(floor(p))-.925)*10.0, 0.0, 1.0);

    color +=  clamp(mix(iColor2RGB,iColorRGB,m)*m*dist, 0.0, 1.0);

    // draw layers of fancy flying stars
    for (float i = 1.0; i <= numLayers; i++) {
        float i2 = i * i;
        a = pow((1.0 - dist),20.0);
        t = timeSpeed * (i * 10.0 - iTime * i2);
        r = coord.x - t;
        c = fract(a+coord.y + i * .618);
        p = vec2(r, c * .5) * 80. * (numLayers / i2);
        uv = fract(p) * 2.0 - 1.0;
        m = clamp((rand(floor(p)) - iQuantity/i) * 10.0, 0.0, 1.0);
        color += clamp(star(uv * 0.5) * m * dist, 0.0, 1.0);
    }

    fragColor = vec4(color, 1.0);
}