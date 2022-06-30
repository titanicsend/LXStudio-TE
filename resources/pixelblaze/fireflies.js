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

numSparks = 1 + floor(pixelCount / 10)  // Scale number of sparks based on # LEDs
decay = .99          // Decay their energy/speed. Use .999 for slower
maxSpeed = .4        // The maximum initial speed of any spark / firefly
newThreshhold = .01  // Recycle any spark under this energy
fade = .9
speedMultiplier = 1
colorRange = 0
beatSpawn = 0

sparks = array(numSparks)
sparkX = array(numSparks)
pixels = array(pixelCount)


export function sliderSpeed(v) {
  speedMultiplier = 1 + (v*v*40 - .5)
}

export function sliderDecay(v) {
  decay = .8 + (1-v) * .199
}

export function sliderFade(v) {
  fade = 0.5 + (1-v) * .49
}

export function sliderNumSparks(v) {
  numSparks = 1 + floor(pixelCount * v / 10)  // Scale number of sparks based on # LEDs
}

export function sliderColor(v) {
  colorRange = v
}

export function beforeRender(delta) {
  delta *= .1

  for (i = 0; i < pixelCount; i++) pixels[i] *= fade // Air cooling

  for (i = 0; i < numSparks; i++) {
    // Recycle dead sparks
    if (abs(sparks[i]) <= newThreshhold) {
      sparks[i] = (maxSpeed / 2) - random(maxSpeed)
      sparkX[i] = random(pixelCount)
    }
    
    sparks[i] *= decay  // Spark energy decay
    var lastX = floor(sparkX[i])
    sparkX[i] += sparks[i] * delta * speedMultiplier  // Advance each position ∝ its energy
    
    // Allow sparks to loop around each end
    if (sparkX[i] >= pixelCount) sparkX[i] = 0
    if (sparkX[i] < 0) sparkX[i] = pixelCount - 1
    
    // Heat up the pixel at this spark's X position
    pixels[floor(sparkX[i])] = 1
    //cover any gaps
    var up = sparks[i] >= 0
    var j = lastX;
    while (j != floor(sparkX[i])) {
      pixels[j] = 1;
      up ? j++ : j--;
      //follow any wrapping
      if (j >= pixelCount) j = 0;
      if (j < 0) j = pixelCount - 1;
    }
  }
}

export function render(index) {
  v = pixels[index]

  paint(colorRange - v * colorRange) // Paint palette color
  setAlpha(v) //cut out areas that would otherwise be dark
}
