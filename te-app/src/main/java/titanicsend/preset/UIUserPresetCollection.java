package titanicsend.preset;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import java.util.ArrayList;
import java.util.List;

public class UIUserPresetCollection extends UI2dContainer implements UIControls, UIContextActions {

  private static final int PRESETS_LIST_WIDTH = 120;
  private static final int BUTTONS_WIDTH = 50;
  private static final int WIDTH = PRESETS_LIST_WIDTH + 4 + BUTTONS_WIDTH;

  private final UserPresetCollection collection;
  private final LXPresetComponent component;
  private final UIUserPresetList presetList;

  private final LXStudio.UI ui;

  public UIUserPresetCollection(final LXStudio.UI ui, final LXPresetComponent component, float h) {
    super(0, 0, 0, h);
    setLayout(Layout.HORIZONTAL, 4);
    this.ui = ui;

    this.component = component;
    this.collection = PresetEngine.get().getLibrary().get(component);
    String presetName = PresetEngine.getPresetShortName(component);

    // Buttons
    UIButton addButton =
        (UIButton)
            new UIButton(BUTTONS_WIDTH, 16) {
              @Override
              public void onToggle(boolean active) {
                if (active) {
                  add();
                }
              }
            }.setLabel("Add").setMomentary(true).setTopMargin(20);

    UIButton updateButton =
        new UIButton(BUTTONS_WIDTH, 16) {
          @Override
          public void onToggle(boolean active) {
            if (active) {
              update();
            }
          }
        }.setLabel("Update").setMomentary(true);

    UIButton removeButton =
        (UIButton)
            new UIButton(BUTTONS_WIDTH, 16) {
              @Override
              public void onToggle(boolean active) {
                if (active) {
                  remove();
                }
              }
            }.setLabel("Remove").setMomentary(true).setTopMargin(50);

    UI2dContainer buttons =
        newVerticalContainer(BUTTONS_WIDTH, 6, addButton, updateButton, removeButton);

    newVerticalContainer(
            WIDTH,
            4,

            // Title
            sectionLabel("PRESETS: " + presetName, WIDTH).setTextAlignment(VGraphics.Align.LEFT),
            newHorizontalContainer(
                getContentHeight() - 20,
                4,
                // List
                this.presetList =
                    (UIUserPresetList)
                        new UIUserPresetList(
                                ui,
                                0,
                                0,
                                PRESETS_LIST_WIDTH,
                                (int) h - 20,
                                this.collection,
                                this.component)
                            .setDescription(
                                "Presets available for this pattern, click to select, double-click to activate"),

                // Buttons
                buttons))
        .addToContainer(this);
  }

  private void add() {
    this.presetList.addPreset();
  }

  private void update() {
    this.presetList.updateActive();
  }

  private void remove() {
    this.presetList.removeSelected();
  }

  private void showLibraryManager() {
    this.ui.leftPane.setActiveSection(0);
    UIUserPresetManager.get().scrollTo();
  }

  /*
   * Context Menu
   */

  public final UIContextActions.Action actionImportPresets =
      new UIContextActions.Action("Import all presets for pattern") {
        @Override
        public void onContextAction(UI ui) {
          PresetEngine.get().importPresets(component);
        }
      };

  public final UIContextActions.Action actionImportAllPresets =
      new UIContextActions.Action("Import presets for ALL patterns") {
        @Override
        public void onContextAction(UI ui) {
          PresetEngine.get().importAllPatternPresets();
        }
      };

  public final UIContextActions.Action actionManageLibrary =
      new UIContextActions.Action("Manage Library...") {
        @Override
        public void onContextAction(UI ui) {
          showLibraryManager();
        }
      };

  @Override
  public List<Action> getContextActions() {
    List<Action> actions = new ArrayList<>();
    actions.add(this.actionImportPresets);
    actions.add(this.actionManageLibrary);
    return actions;
  }
}
