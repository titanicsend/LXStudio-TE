/*
  FireFlies
  
  This is a fork of the sparks pattern where each spark is:
    - Slowed down 
    - Given a longer lifetime 
    - Allowed to loop from one end to the other
  
  This is a highly upvoted pattern generously contributed to the community
  pattern library by an unknown person. Please reach out if you'd like an
  attribution link here!
*/

sparkHue = .05       // Set the hue for each spark
sparkSaturation = 1  // Set the saturation for each spark (0 = white)
numSparks = 1 + floor(pixelCount / 10)  // Scale number of sparks based on # LEDs
decay = .99          // Decay their energy/speed. Use .999 for slower
maxSpeed = .4        // The maximum initial speed of any spark / firefly
newThreshhold = .01  // Recycle any spark under this energy

sparks = array(numSparks)
sparkX = array(numSparks)
pixels = array(pixelCount)


export function beforeRender(delta) {
  delta *= .1
  
  for (i = 0; i < pixelCount; i++) pixels[i] *= .9 // Air cooling
  
  for (i = 0; i < numSparks; i++) {
    // Recycle dead sparks
    if (abs(sparks[i]) <= newThreshhold) {
      sparks[i] = (maxSpeed / 2) - random(maxSpeed)
      sparkX[i] = random(pixelCount)
    }
    
    sparks[i] *= decay  // Spark energy decay
    sparkX[i] += sparks[i] * delta  // Advance each position ∝ its energy
    
    // Allow sparks to loop around each end
    if (sparkX[i] >= pixelCount) sparkX[i] = 0
    if (sparkX[i] < 0) sparkX[i] = pixelCount - 1
    
    // Heat up the pixel at this spark's X position
    pixels[floor(sparkX[i])] += sparks[i]
  }
}

export function render(index) {
  v = pixels[index]
  hsv(sparkHue, sparkSaturation, v * v * 10)
}
