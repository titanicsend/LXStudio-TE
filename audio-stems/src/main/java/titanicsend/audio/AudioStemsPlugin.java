package titanicsend.audio;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.studio.LXStudio;

@LXPlugin.Name("Audio Stems")
public class AudioStemsPlugin implements LXStudio.Plugin {

  // This string must be manually updated to match the pom.xml version
  private static final String VERSION = "0.1.2-SNAPSHOT";

  private AudioStems audioStems;

  public AudioStemsPlugin(LX lx) {
    LOG.log("AudioStemsPlugin(LX) version: " + VERSION);
  }

  @Override
  public void initialize(LX lx) {
    lx.engine.registerComponent("audioStems", this.audioStems = new AudioStems(lx));

    // This will get picked up by the package import, no need to directly add.
    // lx.registry.addModulator(AudioStemModulator.class);
  }

  @Override
  public void initializeUI(LXStudio lxStudio, LXStudio.UI ui) {}

  @Override
  public void onUIReady(LXStudio lxStudio, LXStudio.UI ui) {
    new UIAudioStems(ui, this.audioStems, ui.leftPane.global.getContentWidth())
        .addToContainer(ui.leftPane.global, 2);

    new UIAudioStems(ui, this.audioStems, ui.leftPerformance.tools.getContentWidth())
        .addToContainer(ui.leftPerformance.tools, 0);
  }

  @Override
  public void dispose() {
    this.audioStems.dispose();
  }

  /**
   * Projects that import this library and are NOT an LXPackage (such as a custom build) should call
   * this method from their initialize().
   */
  public static void registerComponents(LX lx) {
    lx.registry.addModulator(AudioStemModulator.class);
  }
}
