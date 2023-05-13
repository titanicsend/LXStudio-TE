/*
Copyright 2022 Meekail Zain

Redistribution and use in source and binary forms, with or without modification, are permitted provided that
the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions
and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or
promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    float c = cos(angle); float s = sin(angle);
    mat2 rotationMatrix = mat2(c, -s, s, c);
    return (rotationMatrix * (point + origin)) - origin;
}

float circle_dist(vec2 point, vec2 trap_point, float radius){
    return length(point-trap_point) - radius;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // scale and center pixel coordinates to range -2 to 2
    vec2 uv = -2.0 + (4.0 * fragCoord) / iResolution.xy;
    vec2 ouv = uv;

    uv = rotate(uv,vec2(0.1,0.0),-iRotationAngle);
    uv *= iScale / 2.0;

    float trap_radius = 0.25;
    float scaled_time = iTime;
    float trap_path_radius = cos(scaled_time)*sin(3.*scaled_time);
    vec2 trap = trap_path_radius*vec2(cos(scaled_time), sin(scaled_time));

    float d=1000.;
    float xx, yy, t_x, t_y;
    float a = uv.x;
    float b = uv.y;

    int max_iter = 50;
    float final_score = 0.;
    for(int i = 0; i < max_iter; i++){
        d = min(d, circle_dist(uv,trap, trap_radius));
        xx = uv.x*uv.x;
        yy = uv.y*uv.y;
        t_x = xx - yy + a;
        t_y = (uv.x+uv.x)*uv.y + b;
        uv.x = t_x;
        uv.y = t_y;
        final_score = d;
    }

    //
    float c = 1.0 - sqrt(-final_score);
    c = smoothstep(0.3,1.0,max(0.0,c));

    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = c * mix(iColorRGB, mix(iColorRGB, iColor2RGB, c),iWow2);
    col  = max(col,iColor2RGB * 0.2);

    float w = 0.05;
    col = (abs(ouv.x) <= w) ? vec3(1,0,0) : col;
    col = (abs(ouv.y) <= w) ? vec3(1,0,0) : col;

    // Output to screen
    fragColor = vec4(col,c);
}