
#ifdef SHADER_TOY
#iUniform color3 iColorRGB=vec3(.964,.144,.519)
#iUniform color3 iColor2RGB=vec3(.226,.046,.636)
#iUniform float iRotationAngle=0.in{0.,6.28}
#iUniform float iQuantity=7.in{3.,12.}// iSides
#iUniform float iScale=.85 in{.5,.99}
#iUniform float iSpeed=0.1 in{-1.,1.}
#iUniform float iWow2=5.in{1.,10.}// num turns
#iUniform float iWow1=1.in{0.,3.}// zSpeed
#endif

#include <include/colorspace.fs>

//float iWow1=1.5 in{.5,3.} // perspective
//float iWow1=.05 in{.001,.05}// line width

float iHue=180.;//in{0.,360.}
const float iLineWidth=0.08;

// #iUniform float stemDrums=.9 in{0.,1.}
// #iUniform float stemVocals=.9 in{0.,1.}
// #iUniform float levelReact=.9 in{0.,1.}
// #iUniform float frequencyReact=.9 in{0.,1.}

precision mediump float;

#define PI 3.14159265
#define TWO_PI 6.28318530

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

    // move to car center on y axis and repeat pattern over x axis
    // at an interval. (0.5 gets you two copies, smaller is more, larger, less)
    // This should duplicate the center of the pattern on each end of the car.  We'll probably want to
    // disable this or set interval to 1.0 for projection mapping use.
    float interval = 0.5;
    uv.x = mod(uv.x,interval) - 0.5 * interval;
    uv.y += 0.3;

    float iZSpeed=iWow1;
    float zTime=iTime*iZSpeed;
    float minDist=1000.;

    float iTurns=iWow2;
    float iPerspective=3.;


    float focalLength=iPerspective;

    // Number of polygon layers to draw (increased for smoother effect)
    int maxLayers=int(iTurns*3.);

    // Smooth z-movement parameters
    float zRange=12.;// Total z depth range
    float zOffset=-6.;// Starting z position

    for(int layer=0;layer<30;layer++){
        if(layer>=maxLayers)break;

        float t=float(layer);
        float radius=pow(iScale,t);

        // Smooth, continuous Z position with better spacing
        float layerPhase=t*.8;// Phase offset between layers
        float zCycle=mod(zTime*.7+layerPhase,zRange);
        float z=zOffset+zCycle;

        // Smooth visibility fade instead of hard cutoff
        float visibilityFactor=1.;

        // Fade in as layers approach from far
        if(z<-4.){
            visibilityFactor*=smoothstep(-6.,-4.,z);
        }

        // Fade out as layers get too close
        if(z>1.){
            visibilityFactor*=smoothstep(3.,1.,z);
        }

        // Skip nearly invisible layers for performance
        if(visibilityFactor<.01)continue;

        // Smooth rotation for spiral effect
        //float rotation=t*TWO_PI/iQuantity+iTime+sin(zTime*.3+t)*.1;

        float rotation=iRotationAngle;
        // Create rotation matrix
        float cosR=cos(rotation);
        float sinR=sin(rotation);
        mat2 rotMat=mat2(cosR,-sinR,sinR,cosR);

        // Smooth depth scaling with perspective correction
        float perspectiveFactor=focalLength/(focalLength-z*.15);
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
            vec2 v1=project3D(v1_3d,focalLength);
            vec2 v2=project3D(v2_3d,focalLength);

            float dist=distanceToLineSegment(uv,v1,v2);

            // Smooth depth weighting
            float depthWeight=mix(.2,1.,smoothstep(-4.,2.,z))*visibilityFactor;
            dist/=(depthWeight+.1);

            minDist=min(minDist,dist);
        }

        // Connect to next layer with improved smoothness
        if(layer<maxLayers-1){
            float nextT=t+1.;
            float nextRadius=pow(iScale,nextT);
            float nextLayerPhase=nextT*.8;
            float nextZCycle=mod(zTime*.7+nextLayerPhase,zRange);
            float nextZ=zOffset+nextZCycle;

            // Check if next layer is visible enough to connect
            float nextVisibility=1.;
            if(nextZ<-4.)nextVisibility*=smoothstep(-6.,-4.,nextZ);
            if(nextZ>1.)nextVisibility*=smoothstep(3.,1.,nextZ);

            if(nextVisibility>.01){
                float nextRotation=nextT*TWO_PI/iQuantity+iTime+sin(zTime*.3+nextT)*.1;
                float nextCosR=cos(nextRotation);
                float nextSinR=sin(nextRotation);
                mat2 nextRotMat=mat2(nextCosR,-nextSinR,nextSinR,nextCosR);

                float nextPerspectiveFactor=focalLength/(focalLength-nextZ*.15);
                float nextAdjustedRadius=nextRadius*nextPerspectiveFactor;

                // Connect corresponding vertices with smooth interpolation
                for(int i=0;i<12;i++){
                    if(float(i)>=iQuantity)break;

                    vec3 currentVertex3D=getPolygonVertex3D(iQuantity,float(i),adjustedRadius,z);
                    vec3 nextVertex3D=getPolygonVertex3D(iQuantity,float(i),nextAdjustedRadius,nextZ);

                    currentVertex3D.xy=rotMat*currentVertex3D.xy;
                    nextVertex3D.xy=nextRotMat*nextVertex3D.xy;

                    vec2 currentVertex=project3D(currentVertex3D,focalLength);
                    vec2 nextVertex=project3D(nextVertex3D,focalLength);

                    float dist=distanceToLineSegment(uv,currentVertex,nextVertex);

                    // Smooth combined visibility for connection lines
                    float avgVisibility=(visibilityFactor+nextVisibility)*.5;
                    float avgZ=(z+nextZ)*.5;
                    float avgDepthWeight=mix(.2,1.,smoothstep(-4.,2.,avgZ))*avgVisibility;

                    dist/=(avgDepthWeight+.1);
                    minDist=min(minDist,dist);
                }
            }
        }
    }

    // Calculate intensity based on distance
    float intensity=1.-smoothstep(0.,iLineWidth*(.1+abs(distance(uv,vec2(0.)))),minDist);

    // Smoother color transitions
    float centerDist=length(uv);
    float colorT=fract(centerDist*2.5+iTime*.4+sin(zTime*.5)*.3);

    vec3 color=getGradientColor(colorT);

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