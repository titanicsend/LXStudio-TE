package titanicsend.preset;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dScrollContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.studio.LXStudio;

public class UIUserPresetManager extends UICollapsibleSection {

  private static final int BUTTON_SPACING = 3;

  private static UIUserPresetManager current;

  public static UIUserPresetManager get() {
    return current;
  }

  public UIUserPresetManager(UI ui, LXStudio lx, float w) {
    super(ui, w, 0);
    current = this;
    setTitle("USER PRESETS");
    setLayout(UI2dContainer.Layout.VERTICAL, PADDING);

    PresetEngine engine = PresetEngine.get();

    final float buttonWidth = (getContentWidth() - (2 * BUTTON_SPACING)) / 4;

    final UILabel fileName =
        (UILabel)
            new UILabel(0, 0, getContentWidth(), 18)
                .setBackgroundColor(ui.theme.controlBackgroundColor)
                .setBorderColor(ui.theme.controlBorderColor)
                .setBorderRounding(4)
                .setTextAlignment(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE)
                .setDescription("Displays the name of the current User Presets file")
                .addToContainer(this);

    UI2dContainer.newHorizontalContainer(
            18,
            BUTTON_SPACING,
            // New
            new UIButton(0, 0, buttonWidth, 18) {
              @Override
              public void onClick() {
                lx.showConfirmDialog(
                    "Clear current User Presets? Any unsaved changes will be lost.",
                    engine::newLibrary);
              }
            }.setLabel("New")
                .setMomentary(true)
                .setBorderRounding(4)
                .setDescription("Start an empty User Presets library"),

            // Load
            new UIButton(0, 0, buttonWidth, 18) {
              @Override
              public void onClick() {
                if (engine.getLibrary() == null) {
                  LX.error("No user preset library found, can not save.");
                  return;
                }

                lx.showConfirmDialog(
                    "Open User Presets file? Any unsaved changes will be lost.",
                    () ->
                        lx.showOpenFileDialog(
                            "Open User Presets Library",
                            "User Presets File",
                            new String[] {"userPresets"},
                            engine.getLibrary().getFile().getPath(),
                            engine::loadLibrary));
              }
            }.setLabel("Load")
                .setMomentary(true)
                .setBorderRounding(4)
                .setDescription(
                    "Load a User Presets library (Replacing previously-loaded library)"),

            // Import
            new UIButton(0, 0, buttonWidth, 18) {
              @Override
              public void onClick() {
                if (engine.getLibrary() == null) {
                  LX.error("No user preset library found, can not save.");
                  return;
                }

                lx.showConfirmDialog(
                    "Import another User Presets file? Presets in the selected file will be added to the current loaded file.",
                    () ->
                        lx.showOpenFileDialog(
                            "Import User Presets Library",
                            "User Presets File",
                            new String[] {"userPresets"},
                            engine.getLibrary().getFile().getPath(),
                            engine::importLibrary));
              }
            }.setLabel("Import")
                .setMomentary(true)
                .setBorderRounding(4)
                .setDescription("Import a User Presets library into the already-loaded library"),

            // Save
            new UIButton(64, 0, buttonWidth, 18) {
              @Override
              public void onClick() {
                lx.showSaveFileDialog(
                    "Save User Presets",
                    "User Presets File",
                    new String[] {"userPresets"},
                    engine.getLibrary().getFile().getPath(),
                    engine::saveLibrary);
              }
            }.setLabel("Save")
                .setMomentary(true)
                .setBorderRounding(4)
                .setDescription("Save User Presets Library"))
        .addToContainer(this);

    addListener(
        engine.libraryName,
        (p) -> {
          fileName.setLabel(engine.libraryName.getString());
        },
        true);
  }

  public void scrollTo() {
    if (getParent() instanceof UI2dScrollContainer scrollContainer) {
      scrollContainer.setScrollY(0 - getY());
    }
  }
}
