# Guide to the Titanic's End Shader Framework

## What is a Shader?

It's a small program that runs on a GPU, takes the coordinates of a single pixel
as a parameter, and answers one question:  
What color should this pixel be?

OpenGL Shaders are written in a C-like language called GLSL. If you've programmed
in C, C++, Java, Javascript, etc., you'll find it mostly familiar. 

If you're new to shaders, there's a resources section at the bottom of this file. 
And I can't recommend [The Book of Shaders](https://thebookofshaders.com) enough. It is really excellent!

## Why write patterns this way?

Short answer: Speed. It gives you a lot more freedom and flexibility to design great
looking patterns.

While GPUs can vary greatly in capability, they will all run your shader in parallel,
on many cores at the same time.  This means you can make much more interesting, organic-looking,
realistic, etc. graphics without bogging down the CPU. 

## Uniforms - Data Supplied by the TE Framework

Chromatik passes audio and control data to your shader as uniforms.
A uniform is effectively a constant - it has the same value across 
all GPU threads. It is set at frame rendering time, and can be read,
but not changed. (The compiler will complain if you try.) The uniforms below
are available to every shader running on the TE platform.

#### Complete List of Uniforms
The following uniforms are available to all shaders, preset with values returned from
the common controls where applicable.  For additional documentation see the sections
below.
```c
// standard shadertoy
uniform float iTime;       // this is actually linked to the speed control
uniform vec2 iResolution;  // pixel resolution of the drawing surface
uniform vec4 iMouse;       // for compatibility only. Always zero.

// TE Audio
uniform float beat;
uniform float sinPhaseBeat;
uniform float bassLevel;
uniform float trebleLevel;

// TE color
uniform vec3 iColorRGB;   // color 1 - the color returned by calcColor() 
uniform vec3 iColorHSB;   // color 1 in the HSB colorspace
uniform vec3 iColor2RGB;  // color 2 the color returned by calcColor2()
uniform vec3 iColor2HSB;  // color 2 in the HSB colorspace

// TE common controls
uniform float iSpeed;
uniform float iScale;
uniform float iQuantity;
uniform vec2  iTranslate;
uniform float iSpin;            // value of the "spin" control
uniform float iRotationAngle;   // rotation angle derived from spin
uniform float iBrightness;      // shaders use this automatically as "contrast"
uniform float iWow1;
uniform float iWow2;
uniform bool  iWowTrigger;

// Shadertoy audio channel + optional textures
uniform sampler2D iChannel0;
uniform sampler2D iChannel1;
uniform sampler2D iChannel2;
uniform sampler2D iChannel3;
```

## Uniforms by Functional Area
-----
### ShaderToy/General Utility

#### iTime (uniform float iTime;)
'Time' since your pattern started running, in seconds.millis.  With the common controls,
this rate can vary with the setting of the speed control.

Since shaders frequently render movement as a function of iTime shaders, this variable speed
time gives you smooth speed control without any additional code in the shader.  Importantly,
time can run both forwards and backwards, so be sure your pattern's math works in both directions.

#### iResolution (uniform vec2 iResolution;)
The resolution of the "display" surface.  Note that these are the dimensions
of the off-screen 2D frame buffer that OpenGL uses for drawing. It is only
indirectly related to the number and layout of LEDs on the vehicle.

#### iMouse (uniform vec4 iMouse;)
All zeros at this time. Never changes. Included for compatibility with ShaderToy
shaders.  There's no reason to use this in shader code.

-----
### Color Uniforms

#### iColorRGB (uniform vec3 iColorRGB;)
The RGB color from the color control returned by the calcColor() function.  Colors in
shaders are normalized to a floating point 0.0 to 1.0 range.  You do not have to multiply them back
to 0-255, and you don't have to worry about color components under- or overflowing while doing 
calculations. They are automatically clamped to the proper range on output.

#### iColorHSB (uniform vec3 iColorHSB;)
The same color as iColorRGB, but pre-converted to normalized HSB format. (All components are
in the range 0.0 to 1.0.  It's just like a Pixelblaze!)

#### iColor2RGB (uniform vec3 iColorRGB;)
The RGB color from the color control returned by the calcColor2() function, normalized as above. 

#### iColor2HSB (uniform vec3 iColorHSB;)
iColor2RGB converted to HSB colorspace and normalized to the range 0.0 to 1.0.

-----
### Audio Uniforms

#### beat (uniform float beat;) 
Sawtooth wave that moves from 0 to 1 with the beat. On the beat the value
will be 0, then ramp up to 1 before the next beat triggers.
 
#### sinPhaseBeat (uniform float sinPhaseBeat;)
Sinusoidal wave that alternates between 0 and 1 with the beat.

#### bassLevel (uniform float bassLevel;)
Average level of low frequency content in the current audio signal.

#### trebleLevel (uniform float trebleLevel;)
Average level of high frequency content in the current audio signal.

-----
### TE Common Control Uniforms

#### iSpeed (uniform float iSpeed;)
Current value of the "Speed" common control. Most shaders will not need to use this because
speed will be automatically controlled by the variable iTime mechanism described above.
#### iScale (uniform float iScale;)
Current value of the "Scale" common control.
#### iQuantity (uniform float iQuantity;)
Current value of the "Quantity" common control.
#### iTranslate (uniform vec2  iTranslate;)
(x,y) translation vector, derived from the settings of the XPos and YPos common controls.
#### iSpin (uniform float iSpin;)
Current value of the "Spin" common control.
#### iRotationAngle (uniform float iRotationAngle;)
Beat-linked rotation angle derived from the current setting of the "Spin" common control.
#### iBrightness (uniform float iBrightness;)
The current value of the "Brightness" common control. The shader framework uses this automatically
as "contrast".  It reduces the brightness of colors without affecting alpha.
#### iWow1 (uniform float iWow1;)
Current setting of the "Wow1" common control. Wow1 controls the level of an optional "special"
pattern-specific feature.
#### iWow2 (uniform float iWow2;)
Current setting of the "Wow2" common control. Wow2 controls the level of an optional "special"
pattern-specific feature.
#### iWowTrigger (uniform bool  iWowTrigger;)
Current setting of the "Wow1" common control. WowTrigger is a momentary contact button that can
trigger an (optional) pattern-specific feature.

-----
### ShaderToy Texture Uniforms

#### iChannel0 (uniform sampler2D iChannel0;)
A 2D texture (2x512) containing audio data from the LX engine.

The first row contains FFT data -- the frequency spectrum of the current playing music.
The second contains a normalized version of the music's waveform,scaled to the range -1.0 to 1.0.
See the **AudioTest2** pattern for an example of how this data can be used.

#### iChannel1 (uniform sampler2D iChannel1;)
#### iChannel2 (uniform sampler2D iChannel2;)
#### iChannel3 (uniform sampler2D iChannel3;)
iChannels 1 through 3 are 2D textures loaded from user specified files.  Some ShaderToy shaders
require these, and it is possible for you to build your own textures and load them at pattern
creation time.

Use the GLSL [```texture(sampler2D textureName,vec2D coords)```](https://registry.khronos.org/OpenGL-Refpages/gl4/html/texture.xhtml) 
function to retrieve data from these textures.


-----
### Automatic LX Control Uniforms

In your shader, you can create a uniform that is automatically linked to an LX control.  When you change
the control from the UI, the value of the uniform will change.  This is especially handy for including extra
controls patterns built the ConstructedPattern framework.  To generate controls from your
shader code, include the encoded control description as follows:

```
     float thickness = {%thickness[5,5,10]};
```

This creates a control named "thickness" in your pattern's UI, with an initial value of 5, a lower limit of 5
and an upper limit of 10.  When this line of GLSL is executed, the variable "thickness" will be assigned to the
current value of the control. 

You can create controls of two types:  float (as above) and boolean.  Here's an example of a boolean control:

```
   if (!{%noGlow[bool]}) {
      fragColor = pow(fragColor, vec4(.4545));
   }
```

If you need to access your control uniform multiple times in a shader, you can assign it to a variable as in 
the first example, or you can refer to it by its actual name, which is the name of the control followed by the suffix,
_parameter.  So to access the two example controls, you would use:

```
    thickness_parameter, and 
    noGlow_parameter
```

-----
### Other Custom Uniforms

TE shaders can also have custom uniforms of many different types, including
arbitrary int and float arrays. This means you can send vehicle geometry data
and other fun things to your pattern.

Instructions on how to build a pattern with custom uniforms are below, in the **Adding a Shader to TE** section.
For an example, see [Phasers](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/Phasers.java).

## Adding a Shader to TE

To run a shader on TE, we need to wrap it in a TEAudioPattern.  There
are ~~two~~ three ways of going about this.

### Easiest:  Automatic Shader Wrapping
For the 2023/2024 season, we've introduced a way to add shaders to TE without writing any Java code at all. 
With this method you can set up the shader's controls directly from shader code, and even live-edit the shader
with any text editor while it's running on the vehicle. This is the easiest way to get started with shaders on TE.

To use this method:
- Write your shader in any text editor.
- Include the line ```#pragma auto``` in your shader code, and save it as an .fs file in the
*resources/shaders* directory.  Be sure the file name is unique, and is a valid Java class name.
(No spaces, no special characters.)
- The next time you start the TE App, your shader will be available in the pattern browser panel, under
the 'Auto Shaders' category. (You can use additional #pragmas, described below, to change the name and category, as well as 
set up UI controls for your shader.)

To live edit a shader, first add it to an active channel so you can see what it's doing.  Then make your changes to the .fs
file and save it.  To see your changes on the car, delete the shader from the active channel list, and re-add it
from the pattern browser panel.

#### Preprocessor Directives for Automatic Shader Wrapping

```glsl
    // basic #include support (handles nested includes, up to 9 levels)
#include "resources/shaders/library/file.fs"
//...or...
#include <library/file.fs>// prefixes with default resource path

// Use the automatic wrapper for ths shader. Recommended, but only required if you
// use no other configuration pragmas. 
#pragma auto 

// set the name of the pattern's java class (and the pattern name in the UI) If not specified
// the name of the shader file (not including .fs) will be used.  If the specified class exists
// a new class will not be created for the shader.)
#pragma Name("ReallyCoolPattern");        // must be unique, and a valid java class name

// Set the shader's pattern browser category
#pragma LXCategory("Best Shaders Ever!)

// Configure common controls at setup time.  The control names and
// configuration functions are as described in the common controls documentation. 
#pragma TEControl.SPEED.Value(1.0)       // setValue()
#pragma TEControl.QUANTITY.Range(1,0,5)     // setRange() 
#pragma TEControl.WOW1.Label("Timmy")   // setLabel()
#pragma TeControl.SCALE.Exponent(2.25)   // setExponent()
#pragma TeControl.QUANTITY.NormalizationCurve(REVERSE,NORMAL,BIAS_CENTER,BIAS_OUTER)  // setNormalizationCurve()
#pragma TEControl.SPEED.Disable	         // markUnused() - hide the control in the UI

// specify up to 9 textures
#pragma iChannel1 "shaders/resources/textures/test.png"
//  ... or  ...
#pragma iChannel1 <textures/test.png>	

// choose how the pattern interacts with x/y translation controls
#pragma TEControl.TranslateMode(DRIFT,NORMAL)

```



###  Easy: Shader Code + Java ConstructedPattern

This method requires minor, boilerplate Java coding, and allows you to customize controls for your
shader without building a full TEPerformancePattern.  It is used by most of our first and second year
shaders.  It's not as simple as the automatic method above, but does have a slight advantage in load time.

- Write your shader, and save it as an .fs file in the *resources/shaders* directory.
- Follow the boilerplate code and add a uniquely named class for your shader to
 either [ShaderPanelsPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderPanelsPatternConfig.java)
- or [ShaderEdgesPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderEdgesPatternConfig.java) in
 the directory *src/main/java/titanicsend/pattern/yoffa/config/*

- Run TE and look for your new pattern in the content list.

Here's an example of code to add a new shader effect to *ShaderPanelsPatternConfig*.
```
    @LXCategory("Native Shaders Panels")
    public static class MyShaderClass extends ConstructedPattern {
        public MyShaderClass(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("my_shader.fs",
                    PatternTarget.allPanelsAsCanvas(this)));
        }
    }
```

### Slightly Harder: Shader + Java + custom Uniforms

If you want to send arrays, car geometry or other custom uniforms to your shader, you'll need to derive your
pattern from the *TEPerformancePattern*, class and create your own *NativeShaderPatternEffect* to manage the shader.

To create a *NativeShaderPatternEffect*, use a constructor like this during your pattern's creation: 
```
     effect = new NativeShaderPatternEffect("fourstar.fs",
        PatternTarget.allPanelsAsCanvas(this), options);
```
This creates a new shader effect given a shader file name, a target set of points, and an (optional) *ShaderOptions*
structure. If you build your pattern this way, before you can render, you must get a pointer to the 
*NativeShaderPatternEffect*'s *NativeShader* object. (To prevent interference with LX startup, the shader 
isn't actually initialized until your pattern is activated.)

You can get a valid pointer by implementing *OnActive()* in your pattern as follows:
```
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }
```
Once you've got the pointer, to run your shader, just add a call to
```
    shader.run(deltaMs)
```
in your *runTEAudioPattern()* method. The *deltaMs* variable can be the one that's passed to *runTEAudioPattern()*.  

### Setting Custom Uniforms
Now that you've created a *TEAudioPattern* with a shader object attached, and retrieved a pointer to the
initialized shader as described above, you can use *setUniform(name, data,...)* to send custom data
to your shader. For example, to send a 3 element float vector to your shader, first declare the uniform by
including the statement
```
    uniform vec3 myUniform;
```
at the top of your shader code, outside any function (It behaves like a global constant).  
Then, in your Java code, before you call ```shader.run()```, set the uniform with

```
   float x1,y1,z1;
   // code that calculates values for x1,y1,z1 
   .
   .
   shader.setUniform("myUniform",x1,y1,z1);

```
Now, when your run your code, ```myUniform.xyz``` in your shader will have whatever values you passed in from Java. 

When doing this, YOU ARE RESPONSIBLE for seeing that the uniform names and data types match
between Java and GLSL.  Otherwise ...nothing... will happen.  Also, according to the OpenGL
spec, each shader can have 1024 uniforms.  I'd try to keep it a little under that.

The currently available *setUniform()* variants are:

```
    setUniform(name,int);  // integer, 1 element
    setUniform(name,int,int);  // integer, 2 elements (ivec2)
    setUniform(name,int,int,int);  // integer, 3 elements (ivec3)
    setUniform(name,int,int,int,int);  // integer, 4 element (ivec4)

    setUniform(name,float);  // float, 1 element
    setUniform(name,float,float);  // float, 2 elements (vec2)
    setUniform(name,float,float,float);  // float, 3 elements (vec3)
    setUniform(name,float,float,float,float);  // float, 4 element (vec4)

    setUniform(name,int[],columnCount);    // int array, any number of rows, from 1 to 4 columns
    setUniform(name,float[],columnCount);  // float array, any number of rows, from 1 to 4 columns    
    
    setUniform(String name, float[][] matrix); // 2x2, 3x3 or 4x4 matrix
    setUniform(String name, Matrix matrix);  // 2x2, 3x3 or 4x4 matrix from Jama.Matrix class  
    setUniform(String name, Texture tex); // creates SAMPLER2D from jogl Texture object.  
    
```


### Important Notes about Uniforms:
If you're passing int or float arrays to a shader, the arrays must be allocated as 
direct buffers, with the nio.Buffers methods or the similar GLBuffer methods. The arrays
should also be the size of the data they are to contain. For example, to allocate a 5x4 float array for
use as a uniform:
```
        // size is 5 rows * 4 columns * 4 bytes per item.
        FloatBuffer buf = Buffers.newDirectFloatBuffer(5 * 4 * 4);
        float [] myArray = buf.array();
```

Be very careful about parameter type when you use setUniform() in a situation where there's any 
ambiguity at all.  For example, if you want to send a floating point vec3 of zeros to the shader, 
specify ```setUniform("name",0f,0f,0f)```, or you might wind up sending an integer vector instead.
When in doubt be specific.  Cast if necessary for clarity.

## Tips and Traps

### Resolution
ShaderToy and other shader demo sites are full of [beautiful things](https://www.shadertoy.com/view/Xl2XRW).  Not all of them will look
good on at lower resolution on a 55 foot, irregularly shaped vehicle. Fine lines might wind up
pixelated, and hi-res detail might devolve to noise.  If you can, give yourself a way of adjusting
line width and detail level, so your pattern can be tuned to look its best.

### Performance
As of this writing, TE's main computer will be a Mac Studio.  Within the bounds of reason, performance shouldn't be 
a problem.  

### Alpha
TE patterns are meant to be layerable and mixable.  Where possible, your pattern should
calculate a reasonable alpha channel.  If you are porting a pattern, and it doesn't 
do the right thing, you can derive alpha from overall brightness by including
the following line of GLSL as the last line in *mainImage()*.
```
    // alpha, derived from brightness, for LX blending.
    fragColor.a = max(fragColor.r,max(fragColor.g,fragColor.b));
```

### Avoiding Version Chaos
OpenGl implementations are tightly tied to hardware. Even though a [standard](https://registry.khronos.org/OpenGL-Refpages/gl4/)
exists, quality and completeness varies greatly among implementations. As with
web browsers, it's possible to write code that works brilliantly on one
platform, and breaks, or does something really strange on others. 

The best way to avoid trouble is to prototype and test on [ShaderToy](https://www.shadertoy.com) , which uses a nice
least-common denominator subset that everybody seems to support. (The TE framework was
designed with easy porting of ShaderToy shaders as a goal, so it is easy to cut and
paste between the two, at least until you start using the TE specific audio and color uniforms.)

## Resources
- [The Book of Shaders](https://thebookofshaders.com/)
- [Inigo Quilez's Intro to Distance Functions](https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm)
- [ShaderToy](https://www.shadertoy.com)
- [GraphToy](https://www.graphtoy.com)
- [Ronja's Tutorials - 2D SDF Basics](https://www.ronja-tutorials.com/post/034-2d-sdf-basics/) 
- [OpenGL Reference Pages](https://registry.khronos.org/OpenGL-Refpages/gl4/)