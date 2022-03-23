Titanic's End LXStudio
==

You may wish to get context first from the [Lighting Design Doc](https://docs.google.com/document/d/1YK9umrhOodwnRWGRzYOR1iOocrO6Cf9ZpHF7FWgBKKI/edit#).

## Learning LX and Developing Patterns

TE is using the full IDE-ready distribution instead of the P4 Processing Applet version. Don't struggle - ask questions in [#Lighting on Slack](https://titanicsend.slack.com/archives/C02L0MDQB2M).

We need your help right now. The problems that need to be solved are on a [board on Notion](https://www.notion.so/titanicsend/d4a7f54ab5f84784b79268e81c9342a7?v=1950f7f8703d498cb51e6e01ec84c577).

### Suggested quick start

_Note: Some of these instructions are from when we only had the testahedron;
if you run into trouble, ask for help on Slack and/or edit these instructions._

* Clone the repo. Have Jeff give you write access on GitHub.
    ```
    git clone https://github.com/titanicsend/LXStudio-TE.git
    ```
* Download and install [Processing 4](https://processing.org/download). Just drag the uncompressed app to Applications. If you already had a Java IDE running, you should now restart it to resolve dependencies on GlueGen, JOGL, and Processing Core.
* Follow the [quick IDE setup](IDE%20Setup.md)
* Build and run the project under /LXStudio-IDE.
    * Play with the UI for 10-30 minutes
    * Read the [LX Studio Wiki](https://github.com/heronarts/LXStudio/wiki)
    * Play with the UI until you have a modulator controlling the parameter for a pattern, and an effect applied on top.
       * See [this guide](https://github.com/tracyscott/RainbowStudio/blob/master/LXStudioUserGuide.md) from another memorable Burning Man art piece
    * Define a new fixture in the UI
    * [Optional] Save your playgorund as a new project with your name: `Playground <YourName>.lxp`. You can mess this project up and experiment broadly.
* Load Testahedron.lxp
    * Save as `Testaherdron <YourName>.lxp`. 
    * Make a sound reactive pattern in the UI; chose a song. Demo this at a weekly TE meeting.
* Let's code
    * Look through the very [BasicRainbowPattern](https://github.com/titanicsend/testahedron/blob/main/LXStudio-IDE/src/main/java/titanicsend/pattern/jeff/BasicRainbowPattern.java) by Jeff
    * Look through the patterns developed by others in these projects:
        * [EnvelopLX](https://github.com/EnvelopSound/EnvelopLX)'s [patterns](https://github.com/EnvelopSound/EnvelopLX/blob/master/EnvelopLX/Patterns.pde) are advanced (by Mark the creator of LX)
        * [Temple Galaxia](https://github.com/temple2018/Galaxia) (2018) [patterns](https://github.com/temple2018/Galaxia/tree/master/src/main/java/org/templegalaxia/patterns) - I think this has a sane multi-contributor repo layout
        * [star-cats/blinky-dome](https://github.com/star-cats/blinky-dome) (2018) [patterns](https://github.com/star-cats/blinky-dome/tree/master/src/main/java/com/github/starcats/blinkydome/pattern)
        * [Entwined](https://github.com/squaredproject/Entwined) (2022) has some nice beginner [pattern code](https://github.com/squaredproject/Entwined/blob/master/oldlx/Trees/Patterns_ColinHunt.java)
        * [RainbowBridge](https://github.com/tracyscott/RainbowStudio) (2018) kind of an example of a more scattered file structure. [Patterns](https://github.com/tracyscott/RainbowStudio/tree/master/src/main/java/com/giantrainbow/patterns)
        * [Titanic's End 2014](https://github.com/nottombrown/TitanicsEnd) was one of the first Burning Man projects to use LX. It uses a very early version, but the basic renderer paradigm is easier to study in these patterns.
    * Develop a pattern in Java using the [LX renderer/shader convention for patterns](https://github.com/heronarts/LXStudio/wiki/Learning-LX:-Patterns)
    * Save as `testahedron/LXStudio-IDE/src/main/java/titanicsend/pattern/<YourName>/<PatternName>.java`. (Java directory conventions - just, whoa)
    * Remember to register it to LXStudioApp's initialize() to see it listed in the UI
* Resources for getting better
    * LX (underlying engine, not the Studio UI) [core classes](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx)

### Switching between titanicsend and testahedron

At press time, the default model imported for the mapping of edges, panels, and points is the mapping produced from the CAD renderings for the art car, Titanic's End. You can see these files (just text files) listed under: `LXStudio-IDE/resources/vehicle`

However, some of the original purpose of this repo is to test with a scaled-down testahedron shape. The geometry for these points also exist in a similar resource location. Switching between the geometries is pretty simple!

1. Open [TEApp.java](https://github.com/titanicsend/testahedron/blob/main/LXStudio-IDE/src/main/java/titanicsend/app/TEApp.java#L58)
2. Uncomment whichever `subdir` line is commented out and comment the other one
3. Rebuild

### Writing a Pattern

You've got the IDE up and running and you're working in the simulator. You saw some examples above. Check out the example and the breakdown below.

#### Example

Here's a very simple example:

https://github.com/titanicsend/testahedron/blob/main/LXStudio-IDE/src/main/java/titanicsend/pattern/tom/Bounce.java

This class only operates on the edges. (the metal bars lit with LEDs that follow the superstructure)

It defines a public class `public class Bounce extends TEPattern` that defines a pattern.

It instantiates and extends some base LXStudio attributes (parameter: `CompoundParameter`, modulator: `SinLFO`) you can [read about here in source](https://github.com/heronarts/LX/tree/master/src/main/java/heronarts/lx).

It exposes a public constructor (`public Bounce`) start starts the modulator we instantiated (the `SinLFO`) and adds a new `rate` parameter. (the `CompoundParameter`) This should be public.

Then, it exposes a public `run` method that LXStudio will call. The only information passed between is the time the `run` was last called, in milliseconds. We range over all edges (`model.edgesById.values()`) and then over all points on the edge (`edge.points`) and update the semi-public `colors` map (which addresses all possible lit pixels as points, by index) and bounces white pixels along the edges between the vertices.

### What's next?

- Visualize: What would be cool as a pattern on the art car?
- Break down: How can you distill this vision into a series of programmable steps, addressing individual pixels along edges and 
- Contribute: create a new github branch, (`git checkout -b <your branch name>`) add a pattern under `src/main/java/titanicsend/pattern/<your name>/<PatternName>.java`, and make it flow in the simulator
  - When you feel good, commit your code to your branch (`git commit -v -m <your message for your commit>`) put up a pull request and get a review (`git push -u origin HEAD` and open it with a button that'll appear at https://github.com/titanicsend/testahedron)
- Orchestrate: How would that work in concert with music, lasers, projection mapping, etc. to make a complete experience?
- Forecast: How will people operate this on an art car in a dust storm? How will a DJ be able to synchronize a certain pattern to the music?
- Collaborate: Check out the [SW Tasks page on Notion](https://www.notion.so/titanicsend/d4a7f54ab5f84784b79268e81c9342a7?v=1950f7f8703d498cb51e6e01ec84c577) and the #lighting channel in Slack

### About LX Studio

Initial impressions are that [LX](https://github.com/heronarts/LXStudio) is powerful and thoughtful, though less documented than ideal. The maintainer, Mark Slee, is incredibly kind and responsive over email ([mark@heronarts.com](mailto:mark@heronarts.com)). LX is not open source - I've copied some of it's license here:

---

**BY DOWNLOADING OR USING THE LX STUDIO SOFTWARE OR ANY PART THEREOF, YOU AGREE TO THE TERMS AND CONDITIONS OF THE [LX STUDIO SOFTWARE LICENSE AND DISTRIBUTION AGREEMENT](http://lx.studio/license).**

Please note that LX Studio is not open-source software. The license grants permission to use this software freely in non-commercial applications. Commercial use is subject to a total annual revenue limit of $25K on any and all projects associated with the software. If this licensing is obstructive to your needs or you are unclear as to whether your desired use case is compliant, contact me to discuss proprietary licensing: mark@heronarts.com

