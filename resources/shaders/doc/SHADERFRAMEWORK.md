# Guide to the Titanic's End Shader Framework

## What is a Shader?
It's a small program that runs on a GPU, gets the coordinates of a single pixel
as a parameter, and answers one question: 
What color should this pixel be?

Shaders are written in a C-like language called GLSL. If you've programmed
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
A uniform is effectively a constant. It is set
at the time the shader is called, and can be read, but not changed.
(The compiler will complain if you try.) The uniforms below
are available to every shader running on the TE platform.

-----
### General Utility Uniforms

#### iTime (uniform float iTime;)
Time since your pattern started running, in seconds.millis

#### iResolution (uniform vec2 iResolution;)
The resolution of the "display" surface.  Note that this is just
the off-screen surface that the framework draws on, not the resolution
of the vehicle.

#### iMouse (uniform vec4 iMouse;)
All zeros at this time. Never changes. Included for compatibility with ShaderToy
shaders. 

-----
### Color Uniforms

#### iColorRGB (uniform vec3 iColorRGB;)
The RGB color currently selected in the UI color control for this pattern.  Colors are
scaled to a floating point 0.0 to 1.0 range.  You do not have to multiply them back
to 0-255, and they are automatically clamped to the proper range on output.

#### iPalette (uniform vec3 iPalette[5];)
An array of 5 RGB colors, containing TE's current palette. You can
select the colors by using the following defined constants.

- **TE_EDGE**      - Primary color to use on edges
- **TE_SECONDARY** - Secondary color to use on edges or panels
- **TE_PANEL**     - Primary color to use on panels
- **TE_EDGE_BG**   - Background color to use on edges
- **TE_PANEL_BG**  - Background color to use on edges

For example to get the current primary panel color, use:

```
	vec3 color = iPalette[TE_PANEL];
```

-----
### Audio Uniforms

#### beat (uniform float beat;) 
Sawtooth wave that moves from 0 to 1 on the beat
 
#### sinPhaseBeat (uniform float sinPhaseBeat;)
Sinusoidal wave that alternates between 0 and 1 on the beat.

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

*TODO - need examples of using texture() and calculated coordinates to get texture data.*

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

*TODO - document this for real, but in
the meantime, to see an example, see the **Phasers** pattern.*

## Adding a Shader to TE
*TODO - need a complete example or two*

- write your shader, and save it as an .fs file in the *resources/shaders directory*.
- Follow the boilerplate code and add a uniquely names class for your shader to
 either **ShaderPanelsPatternConfig.java** or **ShaderEdgesPatternConfig.java** in
 the directory *src/main/java/titanicsend/pattern/yoffa/config/ShaderPanelsPatternConfig.java*

*TODO - describe the DIY method too*


## Tips and Traps
*TODO - many things to talk about here.*

## Resources
- [The Book of Shaders](https://thebookofshaders.com/) 
- [Ronja's Tutorials - 2D SDF Basics](https://www.ronja-tutorials.com/post/034-2d-sdf-basics/)
- [Inigo Quilez's Intro to Distance Functions](https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm)