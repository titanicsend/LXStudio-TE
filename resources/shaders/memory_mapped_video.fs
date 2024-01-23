
// how far to move in our backbuffer to get the next pixel
const float stepx = 1.0/640.0;
const float stepy = 1.0/480.0;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // slow fade between original and edge detected images
    vec2 uv = fragCoord.xy / iResolution.xy;

    vec4 currentPixelColor = texture(iBackbuffer, uv);
    // Pixel values should be sorted as BGRA
    fragColor =  currentPixelColor;
}