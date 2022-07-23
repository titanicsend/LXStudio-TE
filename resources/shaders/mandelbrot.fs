/*
Copyright 2022 Meekail Zain

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

#define M_PI 3.1415926535897932384626433832795


vec2 scale(vec2 point, vec2 X_bounds, vec2 Y_bounds){
    float nu_x = (X_bounds.y-X_bounds.x)*point.x+X_bounds.x;
    float nu_y = (Y_bounds.y-Y_bounds.x)*point.y+Y_bounds.x;
    return vec2(nu_x, nu_y);
}

float point_dist(vec2 point, vec2 trap_point){
    return length(point-trap_point);
}
float line_dist(vec2 point, vec2 trap_point){
    return abs(point.x-trap_point.x);
}
float circle_dist(vec2 point, vec2 trap_point, float radius){
    return abs(length(point-trap_point)-radius);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;

    vec2 X_BOUNDS = vec2(-2., 1.);
    vec2 Y_BOUNDS = vec2(-1.5, 1.5);

    // Center coordinate
    uv = scale(uv, X_BOUNDS, Y_BOUNDS);

    // Rescale for aspect-ratio
    // uv.x *= iResolution.x/iResolution.y;

    // vec2 trap = scale(vec2(0.5, 0.5), X_BOUNDS, Y_BOUNDS);
    // vec2 trap = scale(iMouse.xy/iResolution.xy, X_BOUNDS, Y_BOUNDS);
    float trap_radius = .25;
    float scaled_time = iTime;
    float trap_path_radius = 2.*cos(scaled_time)*sin(3.*scaled_time);
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
        /*
        if(uv.x*uv.x+uv.y*uv.y>4.){
            final_score = float(i)+1.;
        }
        */
        final_score = d;
    }

    float c;
    // if(d <r) c = 1.; else c = 1.-1./(1+exp());
    // c = float(final_score)/float(max_iter+1);
    c = 1./(1.+sqrt(final_score));
    vec3 col = vec3(c);

    // Output to screen
    fragColor = vec4(col,1.0);
}