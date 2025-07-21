#pragma name "SparkWheel"
// #pragma TEControl.SIZE.Range(3.0,0.1,5.0)
// #pragma TEControl.QUANTITY.Range(4.0,3.0,24.0)
// #pragma TEControl.WOW1.Range(1.0,0.0,1.0)

// Steel Wool with Spiral Mask - Authentic spiral spark patterns
const float PI=3.14159;

// Hash function for spark variation
float hash(float p){
    return fract(sin(p*127.1)*43758.5453);
}

void mainImage(out vec4 fragColor,in vec2 fragCoord){
    vec2 uv=(fragCoord.xy*2.-iResolution.xy)/iResolution.y;

    // Convert to polar coordinates
    float angle=atan(uv.y,uv.x);
    float radius=length(uv);

    vec3 color=vec3(0.);

    // Spinning parameters
    float spinSpeed=6.;
    float currentSpinAngle=iTime*spinSpeed;

    // Create distinct spark lines
    const float numSparks=60.;

    for(float i=0.;i<numSparks;i++){
        // Each spark line has its own angle and timing
        float sparkIndex=i+floor(iTime*2.);// New sparks spawn over time
        float sparkBaseAngle=hash(sparkIndex)*2.*PI;
        float sparkCurrentAngle=sparkBaseAngle+currentSpinAngle;

        // Normalize the angle difference between pixel and spark line
        float angleDiff=angle-sparkCurrentAngle;

        // Wrap angle difference to [-PI, PI]
        angleDiff=mod(angleDiff+PI,2.*PI)-PI;

        // Create sharp line by using small angle tolerance
        float lineThickness=.02;// Very thin lines
        float lineDistance=abs(angleDiff);

        if(lineDistance<lineThickness){
            // We're close to a spark line - now check radial distance
            float sparkAge=mod(iTime*3.+hash(sparkIndex)*2.,2.);
            float sparkLength=sparkAge*2.5;// How far the spark has traveled

            // Only show sparks within their current length and beyond center
            if(radius>.01&&radius<sparkLength&&sparkLength>.1){
                // Distance from the line (perpendicular distance)
                float distFromLine=lineDistance/lineThickness;

                // Create sharp falloff for thin lines
                float lineIntensity=1.-smoothstep(0.,2.,distFromLine);

                // Spark gets dimmer as it travels outward
                float distanceFade=1.-smoothstep(0.,sparkLength,radius);
                distanceFade=pow(distanceFade,.1);// Adjust falloff curve

                // Spark brightness with some randomness
                float sparkBrightness=.5+.5*hash(sparkIndex+1.);

                // Final intensity
                float intensity=lineIntensity*distanceFade*sparkBrightness;

                // Color based on age and distance - hot to cool
                vec3 hotColor=vec3(1.,1.,.8);// White-hot
                vec3 warmColor=vec3(1.,.6,.2);// Orange
                vec3 coolColor=vec3(1.,.2,0.);// Red

                float tempFactor=1.-(radius/sparkLength);
                vec3 sparkColor;
                if(tempFactor>.7){
                    sparkColor=mix(warmColor,hotColor,(tempFactor-.7)/.3);
                }else{
                    sparkColor=mix(coolColor,warmColor,tempFactor/.7);
                }

                color+=sparkColor*intensity*.8;
            }
        }
    }

    // Apply spiral mask to create the characteristic steel wool spiral pattern
    float theta=atan(uv.y,uv.x);
    float spiralMask=fract(2.5*theta/PI+7.*pow(radius,.4)-2.5*iTime);

    // Create spiral bands - only show sparks where the spiral mask is "on"
    float spiralThreshold=.6;// Adjust this to change spiral width
    if(spiralMask>spiralThreshold){
        color*=.1;// Heavily dim sparks outside spiral bands
    }

    fragColor=vec4(color,1.);
}