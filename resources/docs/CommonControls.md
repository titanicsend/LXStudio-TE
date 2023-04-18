# How to use the Common Controls


## The Super Quickstart Guide:
- Derive your pattern's class from TEPerformancePattern (This is already done for native shader patterns
made using ConstructedPattern)
- In your constructor use the ```control.setXXX(TECommonControlTag.xxx,value(s)...)``` helper functions to configure the controls
as needed. 
- Once your've configured the common controls, call ```addCommonControls()``` from your constructor to register them
with the UI.
- If your pattern has any additional controls, you should also add them in your constructor, after the call to
- ```addCommonControls``` so the UI will be consistent across all patterns.  
- In your pattern java code, use the helper methods in TEPerformancePattern (```getSpeed()```, ```getSpin()```,
etc.) to get values from the controls.
- If your pattern supports rotation, you can get smoothly changing, beat-linked
rotation angles from the helper methods
```
double getRotationAngleFromSpeed() -- rotation speed determined by speed control
double getRotationAngleFromSpin()  -- rotation speed determined by spin control
```

- If your pattern has a GLSL shader, use the uniform variables listed below to get control values.

### Examples:
- jon/phasers.java (and resources/shaders/phasers.fs) is a Java/shader hybrid pattern that uses
all 10 controls.
- jon/SpiralDiamonds.java shows how to use the common controls in a pure Java pattern

## What is TEPerformancePattern:

The TEPerformancePattern class gives your pattern all the capabilities
of TEPattern and TEAudioPattern, plus access to the 10 "common controls"
used with MIDI surfaces for live performances.

To use TEPerformancePattern

Derive your pattern class from it rather than one of the other TE pattern classes.  Creation of the default common
controls is handled automatically by the constructor.

Make any needed adjustments to control parameters by using the helper functions described below, then
call ```addCommonControls()``` to add the common controls to the UI.

## List of Common Controls
The common controls, their associated helper functions, and the tags used to get and
set control values are:
```text
Name		Helper 					tag                     
Color		int getCurrentColor()   <none>
Speed		double getSpeed()	TEControlTag.SPEED
xPos		double getXPos()	TEControlTag.XPOS
yPos 		double getYPos() 	TEControlTag.YPOS
Size 		double getSize()	TEControlTag.SIZE
Quantity	double getQuantity()	TEControlTag.QUANTITY
Spin		double getSpin()	TEControlTag.SPIN
Brightness	double getBrightness()	TEControlTag.BRIGHTNESS
Wow1		double getWow1()	TEControlTag.WOW1
Wow2		double getWow2()	TEControlTag.WOW2
WowTrigger      boolean getWowTrigger() TEControlTag.WOWTRIGGER
Angle           double getStaticRotationAngle() TEControlTag.ANGLE
```

### Default Ranges and Values
Most controls are initialized to unit ranges - either 0.0 to 1.0, or -1.0 to 1.0, with default initial values
appropriate for the control. Exceptions are:

- Speed - interacts with engine bpm.  It has a default range of -4.0 to 4.0 (16th notes at current tempo),
and is initialized to 0.5 (synced to half notes at the current tempo.)  Speed and Spin controls both use this
tempo syncing mechanism and are described in their own section below.
- Size - controls zoom, has a range of 0.0 to 5.0 and is initialized to 1.0
- Angle - static rotational angle.  The UI range is -180 to 180 degrees, all APIs use the same angles, but in radians.
- Brightness - controls color brightness without changing alpha. (May be removed in the future, so avoid using
  this except to multiply color as the last step in a pattern.  This is handled automatically for shader-based patterns.)

### Helper functions in TECommonControls
From a TEPerformancePattern derived class, you can access the 'controls' object, which lets you
manipulate the common controls.  The most commonly used functions are:  

```
controls.setRange(TEControlTag tag, double value, double v0, double v1)
controls.setNormalizationCurve(TEControlTag tag, BoundedParameter.NormalizationCurve curve)
controls.setExponent(TEControlTag tag, double exp)
controls.setUnits(TEControlTag tag, LXListenableParameter.Units units)

LXListenableParameter controls.getLXControl(TEControlTag tag)
```
## Modifying (and Replacing) Controls
The common controls are designed to adapt to the needs of your pattern.  With the exception of the
color control, they can be modified or even completely replaced.

### Helper functions in TEPerformancePattern
These functions can be called directly from TEPerformancePattern, and are used to override a control's 
default getter function, or to replace the control with a completely new one.

- ```void setControl(TEControlTag tag,LXListenableParameter lxp)``` - installs a new control 
using the default getter function for the specified control type.
- ```void setControl(TEControlTag tag,LXListenableParameter lxp,_CommonControlGetter getFn)```  - installs a
new control and a custom getter function for the specified control type.
- ```void SetGetterFunction(TEControlTag tag, _CommonControlGetter getFn```)  - installs a custom getter function
for the specified existing control.

For example, to make a custom getter function for the speed control, first define it:
```_CommonControlGetter myNewGetter = new _CommonControlGetter() {
@Override public double getValue(LXListenableParameter ctl) {
 return ctl.getValuef(); }
}
```
Then call ```SetGetterFunction(TEControlTag.SPEED, myNewGetter)``` to install it, and everything that
uses ```TEPerformancePattern.getSpeed()``` will be calling your new function.

To completely replace the speed control, define a new control that can be cast to LXListenableParameter, build a getter,
and call ```setControl(```)```, like this:
```
LXListenableParameter p = new CompoundParameter("Speed", 0.5f, 0f, 1.0)
.setDescription("New Speed Control");
setControl(TEControlTag.SPEED,p,myNewGetter);
```
Your new control will appear in the UI instead of the default speed control in the UI after you call
```addCommonControls()```.

### Additional handy helpers:
```double getRotationalAngleFromSpeed()``` - Returns a loosely beat-linked rotation angle in radians.  Overall speed
is determined by the "Speed" control, but will automatically speed up and slow down as the LX engine's beat
speed changes. If current speed is zero, returned angle will also be zero, to allow easy reset of patterns.

```double getRotationalAngleFromSpin()``` - Returns a loosely beat-linked rotation angle in radians.  Overall speed
is determined by the "Speed" control, but will automatically speed up and slow down as the LX engine's beat
speed changes. If current speed is zero, returned angle will also be zero, to allow easy reset of patterns.

```double getStaticRotationAngle()``` get the current angle set by the Angle control, in radians

```void retrigger(TEControlTag tag)``` can be called to sync the speed or spin clocks to beats, measures or
other events.  Starts the next variable clock "second" when called.

```int getPrimaryGradientColor(lerp)``` Returns a color interpolated from the palette's primary gradient,
with brightness modified by the Brightness control.

```int getSecondaryGradientColor(lerp``` Returns a color interpolated from the palette's secondary gradient,
with brightness modified by the brightness control.

### Speed and Spin:  Tempo Linked Parameters
The common Speed and Spin controls are linked to the engine bpm, so changes in tempo will be automatically reflected
in pattern visuals.  Both controls act as beat multipliers, so it is very easy to sync patterns to particular musical
time divisions (see below).  In addition, if you need tighter synchronization, there is a ```retrigger()``` API 
available that you can call from pattern code to sync to beats, measures or other events.

These controls run on a variable speed internal clock. This means if your pattern bases its movement on iTime (GLSL)
or getTime() (Java), you will get the ability to smoothly change speed and direction without visual glitches, *and*
your pattern will be running at some multiple of the beat speed, without any additional work on your part. 

The core clock speed is 1 second/beat, and Speed and Spin multiply that according to their settings. So to set
your pattern's speed to specific time divisions, you just need to pick the multiplier:

- speed = 0.25 : whole notes
- speed = 0.5 : half notes
- speed = 1 : quarter notes
- speed = 2 : eighth notes
- speed = 3 : eighth note triplets
- speed = 4 : is 16th notes

If you need to go faster than 16ths in a pattern, expand the range with setRange()
in your constructor. Of course, non-integer speeds work too, and can create
interesting syncopated visuals.

### The Angle Control: Static Rotation Angle
The Angle control allows you to set a fine-tuned static rotation angle for a pattern.

Once you've set an angle, you can use the Spin control to spin your pattern.  When you stop spinning,
resetting the Angle control returns you to your preset angle.  Patterns that support Spin will
automatically support Angle.


#### List of TE Standard Uniform Variables

The following uniforms are available to shaders, preset with values returned from
the common controls where applicable:
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
uniform vec3 iPalette[5]; // the complete currently active palette
uniform vec3 iColorRGB;   // the color currently showing in the color control
uniform vec3 iColorHSB;   // current color in HSB format

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