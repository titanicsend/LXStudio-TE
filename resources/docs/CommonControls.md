# How to use the Common Controls

#### WORK IN PROGRESS -- THERE MAY BE USEFUL STUFF HERE, BUT THERE IS STILL A LOT OF CHAOS
#### COMING SOON!!!    LESS CHAOS!!!



## The Super Quickstart Version:
Derive your pattern's class from TEPerformancePattern (This is already done for native shader patterns
made using ConstructedPattern)

Use the ```control.setXXX(TECommonControlTag.xxx,value(s)...)``` methods to configure the controls
as needed
In your pattern java code, use the helper methods in TEPerformancePattern (```getSpeed()```, ```getSpin()```, etc.) to get values
from the controls.
If your pattern has a shader, use the uniform variables listed below to get control values.

See jon/phasers.java and resources/shaders/phasers.fs for an example.


## What is TEPerformancePattern:

The TEPerformancePattern class gives your pattern all the capabilities
of TEPattern and TEAudioPattern, plus access to the 10 "common controls"
used with MIDI surfaces for live performances.

To use TEPerformancePattern, derive your pattern class from it rather than
one of the other TE pattern classes.  Creation and initialization of the common
controls is handled automatically by the constructor.

## List of Common Controls
The common controls, their associated helper functions, and the tags used to get and
set control values are:
Name		Helper 					tag                     
Color		int getCurrentColor()   <none>
Speed		double getSpeed()		TEControlTag.SPEED
xPos		double getXPos()		TEControlTag.XPOS
yPos 		double getYPos()		TEControlTag.YPOS
Size 		double getSize()		TEControlTag.SIZE
Quantity	double getQuantity()	TEControlTag.QUANTITY
Spin		double getSpin()		TEControlTag.SPIN
Brightness	double getBrightness()	TEControlTag.BRIGHTNESS
Wow1		double getWow1()		TEControlTag.WOW1
Wow2		double getWow2()		TEControlTag.WOW2
WowTrigger  boolean getWowTrigger()	TEControlTag.WOWTRIGGER

### Default Ranges and Values
Controls are initialized to unit ranges - either 0.0 to 1.0, or -1.0 to 1.0, with default initial values
appropriate for the control.  (The Size control has a range of 0.0 to 5.0, and is initialized to 1.0)



There are
### Helper functions in TECommonControls
From a TEPerformancePattern derived class, you can access the 'controls' object, which lets you
manipulate the common controls.  The available functions are:  

```
controls.setRange(TEControlTag tag, double value, double v0, double v1)
controls.setNormalizationCurve(TEControlTag tag, BoundedParameter.NormalizationCurve curve)
controls.setExponent(TEControlTag tag, double exp)
controls.setUnits(TEControlTag tag, LXParameter.Units units)
//TODO - complete this list
```
## Modifying (and Replacing) Controls
The common controls are designed to adapt to the needs of your pattern.  With the exception of the
color control, they can be modified or even completely replaced.

### Helper functions in TEPerformancePattern
These functions can be called directly from TEPerformancePattern, and are used to override a control's 
default getter function, or to replace the control with a completely new one.

```void setControl(TEControlTag tag,LXParameter lxp,_CommonControlGetter getFn)```  - installs a completely new
control and associated "getter" function for the specified control type.

```void SetGetterFunction(TEControlTag tag, _CommonControlGetter getFn```)  - lets you use a custom getter function
for a currently installed control, if you need to scale the output or calculate its value using
other inputs.

For example, to make a custom getter function for the speed control, first define it:
```_CommonControlGetter myNewGetter = new _CommonControlGetter() {
@Override public double getValue(LXParameter ctl) { return ctl.getValuef(); }
}
```
Then call ```SetGetterFunction(TEControlTag.SPEED, myNewGetter)``` to install it, and everything that
uses ```TEPerformancePattern.getSpeed()``` will be calling your new function.

To completely replace the speed control, define a new control that can be cast to LXParameter, build a getter,
and call ```setControl(```)```, like this:
LXParameter p = new CompoundParameter("Speed", 0.5f, 0f, 1.0)
.setDescription("New Speed Control");
setControl(TEControlTag.SPEED,p,myNewGetter);

Your new control will replace the existing speed control. (Although at present, the controls in the UI may change position.  I can fix this, but it's lower priority for the moment.)

### Additional handy helpers:
```double getRotationalAngle()``` - returns the current "spin" angle in radians, derived from a real-time
LFO, the setting of the Spin control, and the value of max_rotations_per_second.

```double getRotationalAngleOverBeat()``` - returns a beat-linked "spin" angle in radians derived from getTempo().basis(),
the setting of the spin control, and a preset maximum rotations per beat.

```void setMaxRotationsPerSecond(double maxRotationsPerSecond)```
```void setMaxRotationsPerBeat(double maxRotationsPerBeat)```
111void setMaxTimeMultiplier(double m)``` - sets the maximum value of speed.  (the parameter m is basically the number of
fake seconds per real second. Default is 1. Higher is faster, lower is slower.)

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