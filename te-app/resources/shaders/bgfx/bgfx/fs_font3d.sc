$input v_texcoord0
  
#include "common.sh"

uniform vec4 u_color;
uniform vec4 u_background;
SAMPLER2D(s_texFont, 0);

void main() {
    float alpha = texture2D(s_texFont, v_texcoord0).r;
	gl_FragColor = (u_background * (1.0 - alpha)) + u_color * alpha;
}
