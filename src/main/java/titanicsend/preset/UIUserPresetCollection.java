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

  private final UserPresetCollection collection;
  private final LXPresetComponent component;
  private final UIUserPresetList presetList;

  public UIUserPresetCollection(final LXStudio.UI ui, final LXPresetComponent component, float h) {
    super(0, 0, 0, h);
    setLayout(Layout.HORIZONTAL, 4);

    this.component = component;
    this.collection = PresetEngine.get().currentLibrary.get(component);
    String presetName = PresetEngine.getPresetShortName(component);

    // List
    newVerticalContainer(PRESETS_LIST_WIDTH, 4,
      sectionLabel("PRESETS: " + presetName, PRESETS_LIST_WIDTH)
        .setTextAlignment(VGraphics.Align.LEFT),
      this.presetList = (UIUserPresetList)
        new UIUserPresetList(ui, 0, 0, PRESETS_LIST_WIDTH, (int) h - 20, this.collection, this.component)
          .setDescription("Presets available for this pattern, click to select, double-click to activate")
      ).addToContainer(this);


    // Buttons
    UIButton addButton = (UIButton) new UIButton(50, 16) {
      @Override
      public void onToggle(boolean active) {
        if (active) {
          add();
        }
      }
    }
    .setLabel("Add")
    .setMomentary(true)
    .setTopMargin(20);

    UIButton updateButton = new UIButton(50, 16) {
      @Override
      public void onToggle(boolean active) {
        if (active) {
          update();
        }
      }
    }
    .setLabel("Update")
    .setMomentary(true);

    UIButton removeButton = (UIButton) new UIButton(50, 16) {
      @Override
      public void onToggle(boolean active) {
        if (active) {
          remove();
        }
      }
    }
    .setLabel("Remove")
    .setMomentary(true)
    .setTopMargin(50);

    newVerticalContainer(50, 6,
      addButton,
      updateButton,
      removeButton
    ).addToContainer(this);
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

  /*
   * Context Menu
   */

  public final UIContextActions.Action actionImportPresets = new UIContextActions.Action("Import Presets") {
    @Override
    public void onContextAction(UI ui) {
      PresetEngine.get().importPresets(component);
    }
  };

  public final UIContextActions.Action actionImportAllPresets = new UIContextActions.Action("Import presets for ALL patterns") {
    @Override
    public void onContextAction(UI ui) {
      PresetEngine.get().importAllPatternPresets();
    }
  };

  @Override
  public List<Action> getContextActions() {
    List<Action> actions = new ArrayList<Action>();
    actions.add(this.actionImportPresets);
    // actions.add(this.actionImportAllPresets);  // TODO
    return actions;
  }
}
