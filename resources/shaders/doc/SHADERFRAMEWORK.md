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

LX passes audio and control data to your shader as uniforms.
A uniform is effectively a constant - it has the same value across 
all GPU threads. It is set at frame rendering time, and can be read,
but not changed. (The compiler will complain if you try.) The uniforms below
are available to every shader running on the TE platform.

-----
### General Utility Uniforms

#### iTime (uniform float iTime;)
Time since your pattern started running, in seconds.millis

#### iResolution (uniform vec2 iResolution;)
The resolution of the "display" surface.  Note that these are the dimensions
of the off-screen 2D frame buffer that OpenGL uses for drawing. It is only
indirectly related to the number and layout of LEDs on the vehicle.

#### iMouse (uniform vec4 iMouse;)
All zeros at this time. Never changes. Included for compatibility with ShaderToy
shaders. 

-----
### Color Uniforms

#### iColorRGB (uniform vec3 iColorRGB;)
The RGB color currently selected in the UI color control for this pattern.  Colors in
shaders are scaled to a floating point 0.0 to 1.0 range.  You do not have to multiply them back
to 0-255, and you don't have to worry about color components under- or overflowing while doing 
calculations. They are automatically clamped to the proper range on output.

#### iPalette (uniform vec3 iPalette[5];)
An array of 5 RGB colors, containing TE's current palette. You can
select the colors by using the following defined constants.

- **TE_PRIMARY**      - Primary color to use on edges
- **TE_SECONDARY** - Secondary color to use on edges or panels
- **TE_PRIMARY**     - Primary color to use on panels
- **TE_BACKGROUND**   - Background color to use on edges
- **TE_SECONDARY_BACKGROUND**  - Background color to use on edges

For example to get the current primary panel color, use:

```
	vec3 color = iPalette[TE_PRIMARY];
	
	// to get individual color channel values
	float red = color.r;   // also color.x or color[0]
	float green = color.g; // also color.y or color[1]
	float blue = color.b;  // also color.z or color[2]
```

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
### Texture Uniforms

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
### LX Control Uniforms

From your shader code, you can create a uniform that is automatically linked to an LX control.  When you change
the control from the UI, the value of the uniform will change.  To do this, you need to include a specially 
encoded control description.  For example:

```
     float thickness = {%thickness[5,5,10]};
```

Creates a control named "thickness" in your pattern's UI, with an initial value of 5, a lower limit of 5
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
are two ways of going about this.

-----
### The Easy Way

- Write your shader, and save it as an .fs file in the *resources/shaders* directory.
- Follow the boilerplate code and add a uniquely named class for your shader to
 either [ShaderPanelsPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderPanelsPatternConfig.java)
- or [ShaderEdgesPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderEdgesPatternConfig.java) in
 the directory *src/main/java/titanicsend/pattern/yoffa/config/*
- That's it!  Run TE and look for your new pattern in the content list.

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

### The Slightly Harder Way

If you want to send arrays or other custom uniforms to your shader, you'll need to build your pattern as a
normal *TEAudioPattern*, and create your own *NativeShaderPatternEffect* to manage the shader.

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