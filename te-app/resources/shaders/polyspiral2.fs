#pragma name "PolySpiral2"
#iUniform color3 iColorRGB=vec3(.964,.144,.519)
#iUniform color3 iColor2RGB=vec3(.226,.046,.636)
#iUniform float iRotationAngle=0.in{0.,6.28}
#iUniform float iQuantity=7.in{3.,12.}// iSides
#iUniform float iScale=.85 in{.5,.99}
#iUniform float iSpeed=.1 in{-1.,1.}
#iUniform float iWow2=5.in{1.,10.}// num turns
#iUniform float iWow1=1.in{0.,3.}// zSpeed

#pragma TEControl.YPOS.Value(-.07)
#pragma TEControl.SPIN.Value(.1)
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable

//float iWow1=1.5 in{.5,3.} // perspective
//float iWow1=.05 in{.001,.05}// line width

float iHue=180.;//in{0.,360.}
const float iLineWidth=.02;

// #iUniform float stemDrums=.9 in{0.,1.}
// #iUniform float stemVocals=.9 in{0.,1.}
// #iUniform float levelReact=.9 in{0.,1.}
// #iUniform float frequencyReact=.9 in{0.,1.}

precision mediump float;

#define PI 3.14159265
#define TWO_PI 6.28318530
// HSV to RGB conversion
vec3 hsv2rgb(vec3 c){
    vec4 K=vec4(1.,2./3.,1./3.,3.);
    vec3 p=abs(fract(c.xxx+K.xyz)*6.-K.www);
    return c.z*mix(K.xxx,clamp(p-K.xxx,0.,1.),c.y);
}

// Project 3D point to 2D with perspective
vec2 project3D(vec3 pos,float focalLength){
    return pos.xy*focalLength/(focalLength+pos.z);
}

// Distance from point to line segment in 2D
float distanceToLineSegment(vec2 p,vec2 a,vec2 b){
    vec2 pa=p-a;
    vec2 ba=b-a;
    float h=clamp(dot(pa,ba)/dot(ba,ba),0.,1.);
    return length(pa-ba*h);
}

// Get vertex of regular polygon in 3D
vec3 getPolygonVertex3D(float sides,float index,float radius,float z){
    float angle=TWO_PI*index/sides;
    return vec3(cos(angle)*radius,sin(angle)*radius,z);
}

void mainImage(out vec4 fragColor,in vec2 fragCoord){
    vec2 uv=(fragCoord.xy-.5*iResolution.xy)/min(iResolution.x,iResolution.y);

    float iZSpeed=iWow1;
    float zTime=iTime*iZSpeed;
    float minDist=1000.;

    float iTurns=iWow2;
    float iPerspective=3.;

    // Number of polygon layers to draw (increased for smoother effect)
    int maxLayers=int(iTurns*3.);

    // Smoother z-movement parameters - less aggressive
    float zRange=8.;// Reduced from 12.0 for gentler movement
    float zOffset=-4.;// Less extreme starting position

    for(int layer=0;layer<30;layer++){
        if(layer>=maxLayers)break;

        float t=float(layer);
        float radius=pow(iScale,t);

        // Much smoother, less aggressive Z position calculation
        float layerPhase=t*.3;// Reduced phase offset for less staggering

        // Apply z-speed more subtly with dampening
        float dampedZSpeed=iZSpeed*.3;// Reduce the impact of zSpeed
        float zCycle=mod(zTime*dampedZSpeed+layerPhase,zRange);

        // Smooth the movement with easing
        float easedZCycle=smoothstep(0.,zRange,zCycle)*zRange;
        float z=zOffset+easedZCycle;

        // More gradual visibility transitions
        float visibilityFactor=1.;

        // Gentler fade in/out ranges
        if(z<-2.){
            visibilityFactor*=smoothstep(-4.,-2.,z);
        }

        if(z>.5){
            visibilityFactor*=smoothstep(2.,.5,z);
        }

        // Skip nearly invisible layers for performance
        if(visibilityFactor<.01)continue;

        // Reduce rotation variation for steadier appearance
        float rotation=iRotationAngle+sin(zTime*.2+t)*.05;

        // Create rotation matrix
        float cosR=cos(rotation);
        float sinR=sin(rotation);
        mat2 rotMat=mat2(cosR,-sinR,sinR,cosR);

        // More conservative perspective scaling
        float perspectiveFactor=iPerspective/(iPerspective-z*.08);// Reduced from 0.15
        float adjustedRadius=radius*perspectiveFactor;

        // Draw polygon edges with 3D projection
        for(int i=0;i<12;i++){
            if(float(i)>=iQuantity)break;

            vec3 v1_3d=getPolygonVertex3D(iQuantity,float(i),adjustedRadius,z);
            vec3 v2_3d=getPolygonVertex3D(iQuantity,mod(float(i)+1.,iQuantity),adjustedRadius,z);

            // Apply rotation to x,y coordinates
            v1_3d.xy=rotMat*v1_3d.xy;
            v2_3d.xy=rotMat*v2_3d.xy;

            // Project to 2D
            vec2 v1=project3D(v1_3d,iPerspective);
            vec2 v2=project3D(v2_3d,iPerspective);

            float dist=distanceToLineSegment(uv,v1,v2);

            // More conservative depth weighting
            float depthWeight=mix(.3,1.,smoothstep(-3.,1.,z))*visibilityFactor;
            dist/=(depthWeight+.2);

            minDist=min(minDist,dist);
        }

        // Connect to next layer with improved smoothness
        if(layer<maxLayers-1){
            float nextT=t+1.;
            float nextRadius=pow(iScale,nextT);
            float nextLayerPhase=nextT*.3;
            float nextZCycle=mod(zTime*dampedZSpeed+nextLayerPhase,zRange);
            float nextEasedZCycle=smoothstep(0.,zRange,nextZCycle)*zRange;
            float nextZ=zOffset+nextEasedZCycle;

            // Check if next layer is visible enough to connect
            float nextVisibility=1.;
            if(nextZ<-2.)nextVisibility*=smoothstep(-4.,-2.,nextZ);
            if(nextZ>.5)nextVisibility*=smoothstep(2.,.5,nextZ);

//             if(nextVisibility>.01){
                float nextRotation=nextT*TWO_PI/iQuantity+iTime+sin(zTime*.2+nextT)*.05;
                float nextCosR=cos(nextRotation);
                float nextSinR=sin(nextRotation);
                mat2 nextRotMat=mat2(nextCosR,-nextSinR,nextSinR,nextCosR);

                float nextPerspectiveFactor=iPerspective/(iPerspective-nextZ*.08);
                float nextAdjustedRadius=nextRadius*nextPerspectiveFactor;

                // Connect corresponding vertices with smooth interpolation
                for(int i=0;i<12;i++){
                    if(float(i)>=iQuantity)break;

                    vec3 currentVertex3D=getPolygonVertex3D(iQuantity,float(i),adjustedRadius,z);
                    vec3 nextVertex3D=getPolygonVertex3D(iQuantity,float(i),nextAdjustedRadius,nextZ);

                    currentVertex3D.xy=rotMat*currentVertex3D.xy;
                    nextVertex3D.xy=nextRotMat*nextVertex3D.xy;

                    vec2 currentVertex=project3D(currentVertex3D,iPerspective);
                    vec2 nextVertex=project3D(nextVertex3D,iPerspective);

                    float dist=distanceToLineSegment(uv,currentVertex,nextVertex);

                    // Smooth combined visibility for connection lines
                    float avgVisibility=(visibilityFactor+nextVisibility)*.5;
                    float avgZ=(z+nextZ)*.5;
                    float avgDepthWeight=mix(.3,1.,smoothstep(-3.,1.,avgZ))*avgVisibility;

                    dist/=(avgDepthWeight+.2);
                    minDist=min(minDist,dist);
            }
//             }
        }
    }

    // Calculate intensity based on distance
    float intensity=1.-smoothstep(0.,iLineWidth,minDist);

    // Smoother color transitions - less affected by z movement
    float centerDist=length(uv);
    float colorT=fract(centerDist*2.+iTime*.3+sin(zTime*.3)*.2);

    vec3 color=hsv2rgb(vec3(iHue/360.+colorT*.2,.8,1.));

    // Smoother glow effect
    float glowIntensity=.4*(1.+sin(zTime*.8)*.3);
    float glow=exp(-minDist*40.)*glowIntensity;
    glow=pow(.02/minDist,glowIntensity);
    color*=glow;

    // float d=minDist;
    // d=pow(.01/d,1.5);

    // Gentler pulsing
    float pulse=1.+sin(zTime*1.2)*.15;
    color*=pulse;

    fragColor=vec4(color*intensity,intensity);
}