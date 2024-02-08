void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec4 col = texture(iBackbuffer, uv);
    float luminance = dot(col.rgb, vec3(0.299, 0.587, 0.114));
    fragColor = luminance * vec4(1.0, 0.0, 0.0, 1.0);
}
