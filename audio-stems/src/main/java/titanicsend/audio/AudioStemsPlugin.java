package titanicsend.audio;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.studio.LXStudio;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@LXPlugin.Name("Audio Stems")
public class AudioStemsPlugin implements LXStudio.Plugin {

  private AudioStems audioStems;

  public AudioStemsPlugin(LX lx) {
    LOG.log("AudioStemsPlugin(LX) version: " + loadVersion());
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

  /**
   * Loads 'audioStems.properties', after maven resource filtering has been applied. Note that you
   * may need to run `mvn clean package` once from inside `audio-stems` directory to generate the
   * templated properties file. To verify: `cat target/classes/audioStems.properties`.
   */
  private String loadVersion() {
    String version = "";
    Properties properties = new Properties();
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("audioStems.properties")) {
      properties.load(inputStream);
      version = properties.getProperty("audioStems.version");
    } catch (IOException e) {
      LOG.error("Failed to load version information " + e);
    }
    return version;
  }
}
