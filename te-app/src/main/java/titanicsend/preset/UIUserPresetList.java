package titanicsend.preset;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIItemList;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.command.LXCommand;
import heronarts.lx.studio.LXStudio;
import java.util.HashMap;
import java.util.Map;
import titanicsend.pattern.TEPattern;

public class UIUserPresetList extends UIItemList.ScrollList {

  private final UserPresetCollection collection;
  private final LXPresetComponent component;
  final Map<UserPreset, PresetItem> presetToItem = new HashMap<UserPreset, PresetItem>();
  private final UserPresetCollection.Listener collectionListener;

  private boolean isDeleteEnabled = true;

  private PresetItem activeItem;

  public UIUserPresetList(
      LXStudio.UI ui,
      int x,
      int y,
      int w,
      int h,
      final UserPresetCollection collection,
      LXPresetComponent component) {
    super(ui, x, y, w, h);
    setRenamable(true);
    setReorderable(true);
    setDeletable(true);

    this.collection = collection;
    this.component = component;

    for (UserPreset pattern : collection.getPresets()) {
      onPresetAdded(pattern);
    }

    collection.addListener(
        this.collectionListener =
            new UserPresetCollection.Listener() {
              @Override
              public void presetAdded(UserPreset pattern) {
                onPresetAdded(pattern);
              }

              @Override
              public void presetMoved(UserPreset pattern) {
                onPresetMoved(pattern);
              }

              @Override
              public void presetRemoved(UserPreset pattern) {
                onPresetRemoved(pattern);
              }
            });
  }

  private void setActiveItem(PresetItem item) {
    this.activeItem = item;
    // Active item is UI-instance specific, therefore need to redraw
    // now instead of waiting for an underlying parameter to change.
    redraw();
  }

  public UIUserPresetList setDeleteEnabled(boolean isDeleteEnabled) {
    this.isDeleteEnabled = isDeleteEnabled;
    return this;
  }

  private void onPresetAdded(UserPreset preset) {
    PresetItem item = new PresetItem(preset);
    this.presetToItem.put(preset, item);
    addItem(preset.getIndex(), item);
  }

  private void onPresetMoved(UserPreset preset) {
    PresetItem presetItem = this.presetToItem.get(preset);
    if (presetItem == null) {
      throw new IllegalStateException("Preset moved from collection not found in map: " + preset);
    }
    moveItem(presetItem, preset.getIndex());
  }

  private void onPresetRemoved(UserPreset preset) {
    PresetItem presetItem = this.presetToItem.remove(preset);
    if (presetItem == null) {
      throw new IllegalStateException("Preset removed from collection not found in map: " + preset);
    }
    if (presetItem.equals(this.activeItem)) {
      setActiveItem(null);
    }
    removeItem(presetItem);
    presetItem.dispose();
  }

  /**
   * By adding a preset through the list, we can make it the active preset in this list instance.
   */
  public UserPreset addPreset() {
    UserPreset preset = this.collection.addPreset(component);
    // Listener will have added the listItem
    setActiveItem(this.presetToItem.get(preset));
    return preset;
  }

  public UIUserPresetList updateActive() {
    if (this.activeItem != null) {
      this.activeItem.preset.capture(this.component);
    }
    return this;
  }

  public UIUserPresetList removeSelected() {
    PresetItem item = (PresetItem) this.getFocusedItem();
    if (item != null) {
      collection.removePreset(item.preset);
    }
    return this;
  }

  @Override
  public void dispose() {
    this.collection.removeListener(this.collectionListener);
    for (Item item : getItems()) {
      item.dispose();
    }
    super.dispose();
  }

  class PresetItem extends UIItemList.Item {
    private final UserPreset preset;

    PresetItem(UserPreset preset) {
      this.preset = preset;

      preset.label.addListener(redraw);
    }

    @Override
    public boolean isActive() {
      return this.equals(activeItem);
    }

    @Override
    public int getActiveColor(UI ui) {
      return this.equals(activeItem)
          ? ui.theme.primaryColor.get()
          : ui.theme.listItemSecondaryColor.get();
    }

    @Override
    public String getLabel() {
      return this.preset.getLabel();
    }

    @Override
    public void onActivate() {
      if (component instanceof TEPattern) {
        // For TEPattern / TEPerformancePattern, use
        ((TEPattern) component).restore(this.preset);
      } else {
        this.preset.restore(component);
      }
      setActiveItem(this);
    }

    @Override
    public void onRename(String label) {
      getLX().command.perform(new LXCommand.Parameter.SetString(this.preset.label, label));
    }

    @Override
    public void onReorder(int index) {
      collection.movePreset(preset, index);
    }

    @Override
    public void onDelete() {
      if (isDeleteEnabled) {
        collection.removePreset(preset);
      }
    }

    @Override
    public void onDeactivate() {}

    @Override
    public void onFocus() {}

    @Override
    public void dispose() {
      this.preset.label.removeListener(redraw);
    }
  }
}
