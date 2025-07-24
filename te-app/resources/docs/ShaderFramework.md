# Guide to the Titanic's End Shader Framework

## What is a Shader?

It's a small program that runs on a GPU, takes the coordinates of a single pixel
as a parameter, and answers one question:  
**_What color should this pixel be?_**

OpenGL Shaders are written in a C-like language called GLSL. If you've programmed
in C, C++, Java, Javascript, etc., you'll find it mostly familiar.

If you're new to shaders, there's a resources section at the bottom of this document.
And for a gentle, thorough introduction, I can't recommend [The Book of Shaders](https://thebookofshaders.com) enough.
It is really excellent!

## Why write patterns this way?

Short answer: Speed. It gives you a lot more freedom and flexibility to design great
looking patterns.

While GPUs vary greatly in capability, they will all run your shader in parallel,
on many cores at the same time. This means you can make much more interesting, organic-looking,
realistic, etc. graphics without bogging down the CPU.

## How it works on TE
If you've worked with shaders before, you've most likely seen them used to apply textures to geometry.  In games,
for example, a shader might be used to apply a texture to a 3D character, or create explosions or water effects.  In 
programs like ShaderToy, the geometry is less complicated.  Shaders are used to draw in 2D rectangles that are displayed
on the screen.

TE's shader engine is a little different. It's mostly Shadertoy compatible, but it is designed to work with the LED
fixtures on our vehicles.  The shader engine takes the output of your shader and maps it to the vehicle's LEDs, so
that the pattern you create is displayed on the car.  It is possible to do this by taking a Shadertoy-like rectangular texture
and mapping each of its pixels to the nearest corresponding LED, but this has some important limitations when dealing with 3D 
objects like TE and Mothership. Our art cars are definitely not rectangular, and they contain a lot of empty spaces where LEDs
are sparse or nonexistent. (Think, the hole in the middle of Mothership, or the big space for the DJ booth on TE.)  

Mapping a rectangular texture to this sort of object requires rendering a lot of pixels that will never be displayed.  Also
the pixels in the map are not always well aligned with the LEDs, so the texture needs to be filtered to get the right color
for each LED. This is somewhat computationally expensive, and can produce undesirable artifacts, like blurring and color bleed.  

To solve these problems, and to give shaders the ability to address each pixel on our vehicles in full 3D, the latest TE shader engine
uses a different approach.  Instead of rendering a rectangle and mapping it to the car's LEDs, the engine renders only the exact pixels
found on the car. This is done by giving the shader access to the normalized physical space coordinates of the pixel it is rendering.

In general, this means that fewer pixels need to be rendered, and the images produced on the LEDs are sharper, smoother in motion,
and just plain better looking.

For the most part, this happens automatically and does not affect how shaders are written.  But there are a few things
to be aware of, especially if you want to use the engine's more advanced features:  

- iBackbuffer, the sampler containing the previous frame is not a rectangular texture. It contains pixel values from the previous frame
in linear order, matching the order of pixels in the current Java LXModel view.  See the section on iBackbuffer below for more
information on how to use the backbuffer in your shader.

- For shaders that do filtering or other convolution effects, the iMappedBuffer texture is available.  This texture contains the
data rendered by the previous shader in "normal" rectangular buffer form.  It is currently available only to effect shaders. See
the section on iMappedBuffer below for more information.

- If you want to work directly with the normalized 3D model coordinates in your shader, use the lxModelCoords uniform sampler as
described below.

## Uniforms - Data Supplied by the TE Framework
Patterns on Titanic's End are highly interactive. Each show is unique, with visuals
generated in real time in response to audio, directed by a VJ running the controls.
To make this possible, the TE framework provides your pattern with a *lot* of data. For
shaders, this data is passed in as *uniforms*.

A uniform is effectively a read-only constant. It is set at frame rendering time,
before the shader is run, and can be accessed by your shader code. The uniforms described 
below are available to every shader running on the TE platform.

#### Complete List of Uniforms

The following uniforms are available to all shaders, preset with values returned from
the common controls where applicable. For detailed descriptions see the [Uniforms by Functional Area](#uniforms-by-functional-area)
section below.

```c
// standard shadertoy
uniform float iTime;       // variable speed time, linked to the speed control
uniform vec2 iResolution;  // pixel resolution of the drawing surface
uniform vec4 iMouse;       // for compatibility only. Always zero.

// TE Audio
uniform float beat;
uniform float sinPhaseBeat;
uniform float bassLevel;
uniform float trebleLevel;

uniform float volumeRatio;  // ratio of current volume to recent average volume
uniform float bassRatio;
uniform float trebleRatio;

uniform float levelReact;   // reactivity to changes in audio level
uniform float frequencyReact; // reactivity to audio frequency content

// Current values from audio stems 
uniform float stemBass;
uniform float stemDrums;
uniform float stemVocals;
uniform float stemOther;
uniform stem  drumHits;

// TE color
uniform vec3 iColorRGB;   // color 1 - the color returned by calcColor() 
uniform vec3 iColorHSB;   // color 1 in the HSB colorspace
uniform vec3 iColor2RGB;  // color 2 the color returned by calcColor2()
uniform vec3 iColor2HSB;  // color 2 in the HSB colorspace

// TE common controls
uniform float iSpeed;     // speed control setting. Most shaders use iTime instead.
uniform float iScale;
uniform float iQuantity;
uniform vec2  iTranslate;
uniform float iSpin;            // value of the "spin" control
uniform float iRotationAngle;   // rotation angle derived from spin
uniform float iBrightness;      // shaders use this automatically as "contrast"
uniform float iWow1;
uniform float iWow2;
uniform bool  iWowTrigger;      // true if the WowTrigger button is pressed

// Shadertoy audio channel + optional textures
uniform sampler2D iChannel0;   // 512x2 texture containing audio data. Always available
uniform sampler2D iChannel1;   // first optional texture  
uniform sampler2D iChannel2;   // second optional texture
uniform sampler2D iChannel3;   

// iBackbuffer contains the output of the previous shader pass or
// frame. 
// This is really a buffer, not a rectangular texture . It contains 
// pixel values from the previous frame in linear order, matching the
// order of pixels in the Java LXModel.  There is no implied spatial
// relationship between pixels in the buffer.
// Use texelFetch() on the gl_FragCoords passed to the shader to access
// the values for the pixel you're working on. For example:
// vec4 pix = texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0);
// 
uniform sampler2D iBackbuffer;

// iMappedBuffer is a texture containing the data rendered by the
// previous shader.  It is available to effect shaders, and can be
// used for convolution and other effects that need rectangular "neighborhoods"
// of pixels.
// Note that only pixels that exist on the target LED fixture will be colored.
// Everything else will be zero.  So blur, bloom and other filters may not have the
// expected effect.
// It is currently not supported in pattern shaders, but will be 
// available as an option at some point in the future. 
//
uniform sampler2D iMappedBuffer;

// A floating point array containing normalized 3D coordinates for the
// selected view of the current LX model.  The points are in linear order
// corresponding to the order of pixels in the Java LXModel.  
// Most shaders will not need to interact directly with lxModelCoords, but
// if you need to access the 3D coordinates of the current pixel, you can
// use texelFetch() on the gl_FragCoords passed to the shader to access the
// values for the pixel you're working on. For example:
// vec3 coords3D = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0).xyz;
//
uniform sampler2D lxModelCoords;

```

## Uniforms by Functional Area
-----

### ShaderToy/General Utility

#### iTime (uniform float iTime)

'Time' since your pattern started running, in seconds.millis. With the common controls,
the rate at which time passes will vary with the setting of the speed control.

Since shaders often use iTime to render animation as a function of time, this
variable speed timer gives you smooth speed control without any additional code in the shader. Importantly,
time can run both forwards and backwards, so be sure your pattern's math works in both directions.

#### iResolution (uniform vec2 iResolution)

The resolution of the "display" surface, used in shaders to normalize incoming pixel coordinates.
Note that these are the dimensions of the off-screen 2D frame buffer that OpenGL uses for drawing and
are only indirectly related to the number and layout of LEDs on the vehicle. Tools to aid in mapping
between the frame buffer and car geometry are available in the TE
framework's [CarGeometryPatternTools](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/CarGeometryPatternTools.java)
class.

#### iMouse (uniform vec4 iMouse)

All zeros at this time. Never changes. Included for compatibility with ShaderToy
shaders. There's no reason to ever use this in shader code.

-----

### Color Uniforms

#### iColorRGB (uniform vec3 iColorRGB)

The RGB color from the color control returned by the calcColor() function. Colors in
shaders are normalized to a floating point 0.0 to 1.0 range. You do not have to multiply them back
to 0-255, and you don't have to worry about color components under- or overflowing while doing
calculations. They are automatically clamped to the proper range on output.

#### iColorHSB (uniform vec3 iColorHSB)

The same color as iColorRGB, but pre-converted to normalized HSB format. (All components are
in the range 0.0 to 1.0. It's just like a Pixelblaze!)

#### iColor2RGB (uniform vec3 iColorRGB)

The RGB color from the color control returned by the calcColor2() function, normalized as above.

#### iColor2HSB (uniform vec3 iColorHSB)

iColor2RGB converted to HSB colorspace and normalized to the range 0.0 to 1.0.

-----

### Audio Uniforms

#### levelReact (uniform float levelReact)
Used by audio-reactive patterns to control how the pattern responds to changes in audio level. 

#### frequencyReact (uniform float frequencyReact)
Used by audio-reactive patterns to control how the pattern responds to changes in audio frequency content.

#### beat (uniform float beat)

Sawtooth wave that moves from 0 to 1 with the beat. On the beat the value
will be 0, then ramp up to 1 before the next beat triggers.

#### sinPhaseBeat (uniform float sinPhaseBeat)

Sinusoidal wave that alternates between 0 and 1 with the beat.

#### bassLevel (uniform float bassLevel)

Average level of low frequency content in the current audio signal.

#### trebleLevel (uniform float trebleLevel)

Average level of high frequency content in the current audio signal.

#### volumeRatio (uniform float volumeRatio)
Ratio of the current volume to the recent average volume. This is useful for
auto-scaling effects to the current volume level. For example:

- .01 = 1% of recent average
-  1 = Exactly the recent average
-  5 = 5 times higher than the recent average 

Values may vary greatly, depending on the audio content.

#### bassRatio (uniform float bassRatio)
Ratio of the current bass frequency content to the recent average.

#### trebleRatio (uniform float trebleRatio)
Ratio of the current treble frequency content to the recent average.

-----

### Current values from audio stems
Depending on the model used by the audio stem splitter, values should
range from -1.0 to 1.0

#### uniform float stemBass;
RMS energy of the bass stem - low frequency, non-percussion instruments.

#### uniform float stemDrums;
RMS energy of the drum stem - percussion instruments.

#### uniform float stemVocals;
RMS energy of the vocal stem - human voices, possibly other midrange melodic instruments.

#### uniform float stemOther;
RMS energy of all other audio content.

#### uniform stem drumHits;
RMS energy of drum hits in the current audio signal.  This will, of course, be zero unless
the drums are actually being hit.  Note that the precise content of this signal is determined
by settings in the AudioStems plugin and in the VJLab app that provides the audio stems.

-----

### TE Common Control Uniforms

#### iSpeed (uniform float iSpeed)

Current value of the "Speed" common control. Most shaders will not need to use this because
speed will be automatically controlled by the variable iTime mechanism described above.

#### iScale (uniform float iScale)

Current value of the "Scale" common control.

#### iQuantity (uniform float iQuantity)

Current value of the "Quantity" common control.

#### iTranslate (uniform vec2  iTranslate)

(x,y) translation vector, derived from the settings of the XPos and YPos common controls.

#### iSpin (uniform float iSpin)

Current value of the "Spin" common control.

#### iRotationAngle (uniform float iRotationAngle)

Beat-linked rotation angle derived from the current setting of the "Spin" common control.

#### iBrightness (uniform float iBrightness)

The current value of the "Brightness" common control. The shader framework uses this automatically
as "contrast". It reduces the brightness of colors without affecting alpha.

#### iWow1 (uniform float iWow1)

Current setting of the "Wow1" common control. Wow1 controls the level of an optional "special"
pattern-specific feature.

#### iWow2 (uniform float iWow2)

Current setting of the "Wow2" common control. Wow2 controls the level of an optional "special"
pattern-specific feature.

#### iWowTrigger (uniform bool iWowTrigger)

Current setting of the "Wow1" common control. WowTrigger is a momentary contact button that can
trigger an (optional) pattern-specific feature.

-----

### ShaderToy Texture Uniforms

#### iChannel0 (uniform sampler2D iChannel0)

A 2D texture (2x512) containing audio data from the LX engine.

The first row contains FFT data -- the frequency spectrum of the current playing music.
The second contains a normalized version of the music's waveform,scaled to the range -1.0 to 1.0.
See the **AudioTest2** pattern for an example of how this data can be used.

#### iChannel1 (uniform sampler2D iChannel1)

#### iChannel2 (uniform sampler2D iChannel2)

#### iChannel3 (uniform sampler2D iChannel3)

iChannels 1 through 3 are 2D textures loaded from user specified files. Some ShaderToy shaders
require these, and it is possible for you to build your own textures and load them at pattern
creation time.

### iBackbuffer (uniform sampler2D iBackbuffer)

iBackbuffer contains the output of the previous shader pass or frame. Important: This is really
a buffer, not a rectangular texture . It contains pixel values from the previous frame in linear order, matching the
order of pixels in the current Java LXModel view.  There is no implied spatial relationship between pixels in the buffer.

Use texelFetch() with the gl_FragCoords passed to the shader to retrieve the color values for the
current pixel you're working on. For example:

`vec4 pix = texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0);`

### iMappedBuffer (uniform sampler2D iMappedBuffer)

iMappedBuffer is an optional texture containing the data rendered by the previous shader in "normal"
rectangular buffer form.  It is currently available only to effect shaders, and can be used for convolution
and other effects that need rectangular "neighborhoods" of pixels.

Note that only pixels that actually exist on the target LED fixture will be colored. All other pixels
will be zero.  So blur, bloom and similar filters may not have the expected effect.

It is currently not supported in pattern shaders, but will be available as an option at some point in the future.

### lxModelCoords (uniform sampler2D lxModelCoords)

A floating point array containing normalized 3D coordinates for the selected view of the current LX model.
The points are in linear order corresponding to the order of pixels in the corresponding Java LXModel. 

An appropriately scaled 2D version of these coordinates is automatically supplied to shaders in the
fragCoord uniform, so most shaders will not need to interact directly with `lxModelCoords`.  However,
if you need to access the full 3D coordinates of the current pixel, you can use texelFetch() on the
`gl_FragCoords` passed to the shader to access the values for the pixel you're working on. For example:

`vec3 coords3D = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0).xyz;`

-----

### Automatic LX Control Uniforms

*(Note: This is a legacy feature. It's still supported, but is not recommended
for use in new shaders. Any controls you create this way will not be visible in the UI. In the future, we may update or
repurpose this feature.
For now, the best way to create controls from shader code is to use the automatic shader wrapping method
described below.)*

In your shader, you can create a uniform that is automatically linked to an LX control. When you change
the control from the UI, the value of the uniform will change. This is especially handy for including extra
controls patterns built the ConstructedPattern framework. To generate controls from your
shader code, include the encoded control description as follows:

```
     float thickness = {%thickness[5,5,10]};
```

This creates a control named "thickness" in your pattern's UI, with an initial value of 5, a lower limit of 5
and an upper limit of 10. When this line of GLSL is executed, the variable "thickness" will be assigned to the
current value of the control.

You can create controls of two types:  float (as above) and boolean. Here's an example of a boolean control:

```
   if (!{%noGlow[bool]}) {
      fragColor = pow(fragColor, vec4(.4545));
   }
```

If you need to access your control uniform multiple times in a shader, you can assign it to a variable as in
the first example, or you can refer to it by its actual name, which is the name of the control followed by the suffix,
_parameter. So to access the two example controls, you would use:

```
    thickness_parameter, and 
    noGlow_parameter
```

-----

### Other Custom Uniforms

TE shaders can also have custom uniforms of many different types, including
arbitrary int and float arrays. This means you can send vehicle geometry data
and other fun things to your pattern.

Instructions on how to build a pattern with custom uniforms are below, in the
[## Adding a Shader to TE](#adding-a-shader-to-te) section.
For an example,
see [Phasers](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/Phasers.java).

## Adding a Shader to TE

There are three ways to add a shader to TE. The easiest is to use the automatic shader wrapping
feature described below. The other two methods require a little Java coding, but give you more
control over the shader's behavior. If you're porting a shader from ShaderToy, the easiest way
is to use the automatic shader wrapping feature. For more complex shaders, requiring custom uniforms
and/or frame-time calculations in Java, you'll need to use one of the other methods.

### Easiest:  Automatic Shader Wrapping

For the 2023/2024 season, we've introduced a way to add shaders to TE without writing any Java code at all.
With this method you can set up controls directly from shader code, and even live-edit the shader
with any text editor while it's running on the vehicle. This is the easiest way to get started with shaders on TE.

To use this method:

- Write your shader in any text editor.
- Include the line ```#pragma auto``` in your shader code, and save it as an .fs file in the
  *resources/shaders* directory. Be sure the file name is unique, and is a valid Java class name.
  (No spaces, no special characters.)
- The next time you start the TE App, your shader will be available in the pattern browser panel, under
  the 'Auto Shaders' category. (You can use additional #pragmas, described below, to change the name and category, as
  well as
  set up UI controls for your shader.)

To live edit a shader, first add it to an active channel so you can see what it's doing. Then make your changes to the
.fs
file and save it. To see your changes on the car, delete the shader from the active channel list, and re-add it
from the pattern browser panel.

#### Preprocessor Directives for Automatic Shader Wrapping

```glsl
    // basic #include support (handles nested includes, up to 9 levels)
#include "resources/shaders/library/file.fs"
//...or...
#include <library/file.fs> // prefixes with default resource path

// Use the automatic wrapper for this shader. Recommended, but only required if you
// use no other configuration pragmas. 
#pragma auto 

// set the name of the pattern's java class (and the pattern name in the UI) If not specified
// the name of the shader file (not including .fs) will be used.  If the specified class exists
// a new class will not be created for the shader.)
#pragma Name("ReallyCoolPattern");        // must be unique, and a valid java class name

// Set the shader's pattern browser category
#pragma LXCategory("Best Shaders Ever!")

// Configure common controls at setup time.  The control names and
// configuration functions are as described in the common controls documentation. 
#pragma TEControl.SPEED.Value(1.0)       // setValue()
#pragma TEControl.QUANTITY.Range(1,0,5)     // setRange() 
#pragma TEControl.WOW1.Label("Timmy")   // setLabel()
#pragma TEControl.SIZE.Exponent(2.25)   // setExponent()

// Set the shape of the control's normalization curve to optimize
// "feel" and responsiveness. Possible values are: REVERSE,NORMAL,BIAS_CENTER,BIAS_OUTER
#pragma TEControl.QUANTITY.NormalizationCurve(REVERSE)  // setNormalizationCurve()

// Hide the control in the Chromatik UI
#pragma TEControl.SPEED.Disable	

// specify up to 9 textures
#pragma iChannel1 "resources/shaders/textures/test.png"
//  ... or  ...
#pragma iChannel1 <textures/test.png>	

// Choose how the pattern interacts with x/y translation controls
// Possible values are: DRIFT, NORMAL
// If this pragma is not specified, the default mode is NORMAL, which
// is what you want unless you're deliberately creating a drift-aware pattern.
// (See the section on drift enabled patterns below for more information.)
#pragma TEControl.TranslateMode(DRIFT,NORMAL)

```

### Easy: Shader Code + Java ConstructedShaderPattern

If you have more complex control setup needs, need to send custom uniforms to your shader
or use data generated in Java at frame time, this is the way to go.  (It also has a
slight advantage in initial load time over the automatic shader wrapping method, although this
only affects application startup.)

It requires a small amount Java coding, and allows you to customize the behavior of your shader without building a full
GLShaderPattern implementation. Most of our shaders are built this way. To use this method:

- Write your shader, and save it as an .fs file in the *resources/shaders* directory.
- Follow the boilerplate code and add a uniquely named class for your shader to
  either [ShaderPanelsPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderPanelsPatternConfig.java)
-
or [ShaderEdgesPatternConfig.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/yoffa/config/ShaderEdgesPatternConfig.java)
in
the directory *src/main/java/titanicsend/pattern/yoffa/config/*

- Run TE and look for your new pattern in the content list.

Here's an example of code to add a shader effect to *ShaderPanelsPatternConfig*. This
sets parameters for the common controls, and adds a shader with an associated texture
(on iChannel1) to the "Native Shaders Panels" category.

```
  @LXCategory("Native Shaders Panels")
  public static class StormScanner extends ConstructedShaderPattern {
    public StormScanner(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 1, 3, 0.5); // overall scale
      controls.setRange(TEControlTag.WOW1, .35, 0.1, 1); // Contrast

      addShader("storm_scanner.fs", "gray_noise.png");
    }
  }
```

### Slightly Harder: GLShaderPattern

If you need to:

- Create a custom pattern class that does more than just run a shader
- Create a pattern that uses custom uniforms
- Create a pattern that uses car geometry
- Create a multipass pattern that uses more than one shader per frame

This is the way to go. All the previously described methods of creating shader
patterns are built on top of this class. It's a little more work, but gives you
the most control and flexibility. To use this method:

Create a class that extends GLShaderPattern, call the super constructor, configure
your controls, and add your shader(s). That's it.

Here's a complete example of a pattern class that uses a shaderwith custom uniforms. (The "Fireflies" pattern
shown here is included in the TE library so you can run Chromatik and see it in action. You can find additional
examples in the project's pattern directory.)

```
@LXCategory("Combo FG")
public class Fireflies extends GLShaderPattern {

  public Fireflies(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls
        .setRange(TEControlTag.QUANTITY, 20, 1, 32)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 0.9,1, 0.25);
    controls.setRange(TEControlTag.WOW1, 0.36,1, 0.1)
      .setExponent(TEControlTag.WOW1, 2);

    // register common controls with LX
    addCommonControls();

    addShader(
        "fireflies.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform("iQuantity", (float) getQuantity());
            s.setUniform("iSize", (float) getSize());
            s.setUniform("iWow2", (float) getWow2());
          }
        });
  }
}
``` 

### Setting Custom Uniforms

Notice the OnFrame() method in the example above. This is where you set custom uniforms
for your shader. The GLShaderFrameSetup interface is a functional interface, so you can
use a lambda expression instead of a full class if you prefer. The OnFrame() method is
called once per frame, before the shader is run. It is passed a pointer to the shader
object, so you can call any of the shader's methods from within OnFrame().

To set uniforms, use the shader's *setUniform()* method as shown in the example. This method
is overloaded to handle many different types of data. See the section below on *setUniform()*
for details.

If you're sending a custom uniform to your shader note that you must declare it in the shader.
For example, to send a 3 element float vector to your shader, first declare the uniform by
including the statement

```
    uniform vec3 myUniform;
```

at the top of your shader code, outside any function (It behaves like a global constant).  
Then, in your OnFrame() method, set the uniform with

```
   float x1,y1,z1;
   // code that calculates values for x1,y1,z1 
   .
   .
   s.setUniform("myUniform",x1,y1,z1);

```

Now, when your run your code, ```myUniform.xyz``` in your shader will have access to the values
you passed in from Java.

When doing this, YOU ARE RESPONSIBLE for seeing that the uniform names and data types match
between Java and GLSL. Otherwise ...nothing... will happen. Also, according to the OpenGL
spec, each shader can have 1024 uniforms. I'd try to keep it a little under that.

You can use *setUniform()* to send the following data types to your shader:

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
ambiguity at all. For example, if you want to send a floating point vec3 of zeros to the shader,
specify ```setUniform("name",0f,0f,0f)```, or you might wind up sending an integer vector instead.
When in doubt be specific. Cast if necessary for clarity.

## Advanced Pattern Building


### Colors, Color Spaces and Color Mixing
Each running shader can use:
- a single static color selected from the UI (the color will be in `iColorRGB/HSB` if the static color source
is enabled.)
- two "default" system colors (`iColorRGB/HSB` and `iColor2RGB/HSB`), taken from the current active color swatch. These
are always available.
- the `getPaletteColor(index)` function to retrieve individual RGB colors from the current palette. (This works
regardless of the color source selected for the pattern in the Chromatik UI.)
- the `getGradientColor(lerp)` function to get an interpolated color from the current palette. (This will return the
user selected static color if the `STATIC` color source is selected in the Chromatik UI.)

To use `getPaletteColor()` or `getGradientColor()`, you must include the following line in your shader code:

```#include <include/colorspace.fs>```


By default, color interpolation is performed in Oklab color space. This is a perceptually uniform color space 
that provides smoother color mixing than RGB or HSV.  If you'd prefer to use a different color space,
your shader can call `getGradientColor_linear(lerp)` or `getGradientColor_hsv(lerp)` to interpolate in linear RGB
or HSV color spaces, respectively. These functions all return colors in RGB, regardless of the color space
used to do the interpolation.  

### Using Car Geometry
You can access the car's geometry - edges and triangles - from a shader to create patterns that uniquely fit
Titanic's End. The
[CarGeometryPatternTools](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/CarGeometryPatternTools.java)
class provides basic tools to extract features from the car's geometry and pass them to your shader as uniforms.
Building
patterns this way requires the combined Java/Shader approach described above. Below are two examples, both of which
would be computationally impractical in Java alone.

**ArcEdges** lights the car's edges and nearby panel areas, and creates a series of glowing "electrical arcs" between
them.

[ArcEdges.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/ArcEdges.java)
[arcedges.fs](https://github.com/titanicsend/LXStudio-TE/blob/main/resources/shaders/arcedges.fs)

**Edgefall** illustrates a way of animating car geometry. It lights edges, and "explodes" them outward
when the WowTrigger button is pressed.
[EdgeFall.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/EdgeFall.java)
[edgefall.fs](https://github.com/titanicsend/LXStudio-TE/blob/main/resources/shaders/edgefall.fs)

### Drift Enabled Patterns

Some pattern types, for example noise, clouds and starfields, look best with continuous, unbounded, "drifting"
x/y movement. To create a pattern that moves this way, derive it from the ```DriftEnabledPattern```
class instead of ```GLShaderPattern```. This overrides the default translation behavior so that
instead of an absolute x/y offset, the XPos/YPos controls set a 'drift' direction and speed.

The pattern's position will then smoothly change over time at a rate controlled by XPos/YPos. The maximum
movment rate is based on the real-time clock, independent of the speed control setting.

Note that patterns must be "drift-aware" - they must know that the offset controls now control drift
and that the current position is available in Java from the getXPosition() and getYPosition()
functions.

GLSL shaders using this class should define #TE_NOTRANSLATE in their code to disable the default control
behavior in the shader engine.

A couple of DriftEnabledPattern examples are:

#### RainBands

[RainBands.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/RainBands.java)
[RainBands.fs](https://github.com/titanicsend/LXStudio-TE/blob/main/resources/shaders/rain_noise.fs)

and

#### TriangleNoise

[TriangleNoise.java](https://github.com/titanicsend/LXStudio-TE/blob/main/src/main/java/titanicsend/pattern/jon/TriangleNoise.java)
[TriangleNoise.fs](https://github.com/titanicsend/LXStudio-TE/blob/main/resources/shaders/triangle_noise.fs)

### Multipass Rendering

Some patterns require more than one shader to render a frame. A pattern might, for
example, implement sharpening or blurring effects by processing each frame generated
frame through a convolution filter. Or it might get an image from a video stream, and
then want to perform color processing on the resulting image.

To support this, the TE framework allows you to add multiple shaders to a pattern. Each
shader is run in sequence, and the output of the previous shader is passed to the next
shader as a texture. The last shader in the sequence is the one that actually draws
the frame.

To create a multipass pattern, derive your pattern class from ```GLShaderPattern```  
and configure your controls as described above. Then, before adding any shaders, you must
create shared backing store for the intermediate results. The ```GLShader``` class provides
a function for doing this:

    // allocate a backbuffer for all the shaders to share
    Bytebuffer buffer = GLShader.allocateBackBuffer();

Once you've allocated the buffer, you can add shaders to your pattern using multiple calls
to addShader(). For example to create a pattern with two shaders:

    // add the first shader, passing in the default controlData object and shared backbuffer
    shader = new GLShader(lx, "fire.fs",controlData, buffer);
    addShader(shader);

    // add the second shader, which applies a simple edge detection filter to the
    // output of the first shader
    shader = new GLShader(lx, "multipass1.fs", controlData, buffer);
    addShader(shader );

To add more shaders, just keep calling addShader(), remembering to pass in the shared
backbuffer. The last shader in the sequence will set the final output color.

One important note: The shader rendering system can adjust the color and contrast
of output for best appearance on the car. In a multipass pattern, it's possible that this
adjustment might change values passed between shaders in an unexpected way.

If you're seeing strange results in a multipass shader, you can disable this post processing on 
individual shaders by defining ```#TE_NOPOSTPROCESSING``` in the shader code.

### Preprocessor Directives for Output Control

#### Rationale 
The TE shader system was originally built to be compatible with ShaderToy shaders. In general
ShaderToy does not use the alpha channel when drawing to the screen, so many of our original 
shaders did not set alpha properly. This created some problems when blending multiple channels
and caused some patterns to display at less than full brightness.

The current shader engine has the ability to optimize shader behavior by substituting brightness
for alpha in certain circumstances. This works well for most shaders, but can cause problems
in rare circumstances.  

The following preprocessor directives are available to control this behavior and other
interactions between the shader and the TE framework.

#### #define TE_NOALPHAFIX
The TE_NOALPHAFIX directive causes the renderer to use the "old", pre-EDC 2023 alpha
handling behavior. This behavior maximizes brightness at the expense of detailed
transparency by forcing black pixels to full transparency, and otherwise
use shader provided alpha which, at that time was generally clamped to 1.0.

#### #define TE_ALPHATHRESHOLD (value in the range 0.0 to 1.0)
When using the default alpha handling behavior, the TE_ALPHATHRESHOLD directive
allows you to set a brightness threshold above which colors will be fully opaque
(alpha == 1.0).  This gives you precise control over the tradeoff between brightness
and transparency.

#### #define TE_NOPOSTPROCESSING
The TE_NOPOSTPROCESSING directive disables all automatic color and alpha adjustment
of shader output. This is useful in multipass patterns where the output of one shader
is passed to another as data, and the precise values must be preserved.

#### #define TE_EFFECTSHADER
This directive tells the preprocessor and control management scripts that
this shader provides a pre- or post-processing effect and doesn't interact with the
common controls so including it as part of a pattern will not cause any controls to be
disabled for that pattern.

## Tips and Traps

### Resolution

ShaderToy and other shader demo sites are full of [beautiful things](https://www.shadertoy.com/view/Xl2XRW). Not all of
them will look good on at lower resolution on a 55 foot, irregularly shaped vehicle.

Fine lines might wind up broken and pixelated, and hi-res detail might devolve to noisy static.
If possible, give yourself a way of adjusting line width and detail level, so your pattern can be
tuned to look its best.

### Performance

As of this writing, TE's main computer will be a Mac Studio. Within the bounds of reason, performance shouldn't be
a problem.

### Alpha

TE patterns are meant to be layerable and mixable. Where possible, your pattern should
calculate a reasonable alpha channel. If you are porting a pattern, and it doesn't
do the right thing, you can either set the color to it's highest possible brightness
and use alpha to control the brightness level, or set alpha to 1.0 and let the framework
do it for you.

### Mirroring vs 3D Wrapping
By default, the TE shader engine produces symmetrically mirrored images on the port and starboard
sides of the vehicle.  In most cases, this is the desired behavior because the audience is generally
on only one side of the car, and the far side is not visible.  

However, for effects, panoramic views and other "special occasions" you may want to produce a full
3D wrap-around effect.  To enable this, all you need to do is call `setPainter(new ShaderPaint3d() {});` in
your pattern's constructor.  

### Avoiding Version Chaos

OpenGl implementations are tightly tied to hardware. Even though
a [standard](https://registry.khronos.org/OpenGL-Refpages/gl4/)
exists, quality and completeness varies greatly among implementations. As with
web browsers, it's possible to write code that works brilliantly on one
platform, and breaks, or does something really strange on others.

The best way to avoid trouble is to prototype and test on [ShaderToy](https://www.shadertoy.com) , which uses a subset
that everybody seems to support. The TE framework was designed with easy porting of ShaderToy shaders
as a goal, so it is easy to cut and paste between the two, at least until you start using the TE specific
audio and color uniforms.

## Resources

- [The Book of Shaders](https://thebookofshaders.com/)
- [Inigo Quilez's Intro to Distance Functions](https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm)
- [ShaderToy](https://www.shadertoy.com)
- [GraphToy](https://www.graphtoy.com)
- [Ronja's Tutorials - 2D SDF Basics](https://www.ronja-tutorials.com/post/034-2d-sdf-basics/)
- [OpenGL Reference Pages](https://registry.khronos.org/OpenGL-Refpages/gl4/)