/**
 * Licensing Notes (JKB)
 *
 * The expected permanent home for this concept and its derivatives is the LX Studio
 * software library or a LX Studio / Chromatik extension distributed by JKB.
 *
 * Due to time constraints, doing a first release of this code in either
 * of the above code bases would add too much delay to be usable
 * for the immediate TE events.
 *
 * It is acknowledged that by releasing the code here, the TE code base may
 * continue to use this original version in perpetuity.
 * It is also the stated intent that the long-term license for this code
 * and its derivatives will be the LX Studio Software License and
 * Distribution Agreement (http://lx.studio/license), or another license
 * as determined by the author. 
 *
 * @author Justin Belcher <jkbelcher@gmail.com>
 */

package titanicsend.model.justin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import heronarts.lx.LX;
import heronarts.lx.LXModelComponent;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXView;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.TEApp;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.jon.ModelBender;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

/**
 * Generate a set of LXViews once, then allow each channel to scroll through
 * the views with a parameter.
 */
public class ViewCentral extends ChannelExtension<titanicsend.model.justin.ViewCentral.ViewPerChannel> {

  /*
   * Static
   */

  static public final boolean ENABLED = TEApp.ENABLE_VIEW_CENTRAL;

  static public final String FILENAME_VIEWS = "views.txt";

  static public interface ViewCentralListener {
    abstract public void ViewCentralLoaded();
  }

  // Static central reference keeps imports looking clean
  private static ViewCentral current;
  public static ViewCentral get() {
    return current;
  }

  public static boolean isLoaded() {
    return current != null;
  }

  static private final List<ViewCentralListener> listenersOnce = new ArrayList<ViewCentralListener>();

  static public void listenOnce(ViewCentralListener listener) {
    listenersOnce.add(listener);
  }

  static private void notifyListeners() {
    for (int i = listenersOnce.size()-1; i>=0; --i) {
      listenersOnce.remove(i).ViewCentralLoaded();
    }
  }

  /*
   * Non-static
   */

  protected List<ViewDefinition> views;

  public ViewCentral(LX lx) {
    super(lx);
    current = this;
    notifyListeners();
  }

  /**
   * Called by parent constructor before current channels are added
   */
  @Override
  protected void initialize() {
    this.views = new ArrayList<ViewDefinition>();    

    // Global off switch for feature
    if (!ViewCentral.ENABLED) {
      return;
    }

    // Load view definitions from file
    try (Scanner s = new Scanner(new File("resources/vehicle/" + FILENAME_VIEWS))) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (line.startsWith("#")) {
          continue;
        }
        String[] tokens = line.split("\t");
        // Asserts default to off in Java...
        if (tokens.length == 3) {
          String label = tokens[0];
          // Normalization: true = relative, false = absolute
          LXView.Normalization n = Boolean.parseBoolean(tokens[1]) ? LXView.Normalization.RELATIVE : LXView.Normalization.ABSOLUTE;
          String selector = tokens[2];
          this.views.add(new ViewDefinition(label, selector, n));
        } else {
          LX.error("Invalid number of columns in " + FILENAME_VIEWS +" config file, found " + tokens.length + ": " + line);
        }
      }
    } catch (IOException e) {
      LX.error(e, "Error loading views from file:");
      return;
    }

    // adjust model geometry for easy texture mapping in views
    ModelBender mb = new ModelBender();
    mb.adjustEndGeometry((TEWholeModel) this.lx.getModel());

    // Create LXViews once
    for (ViewDefinition v : this.views) {
      // View creation code borrowed from LXAbstractChannel.class
      if (v.viewEnabled && (v.viewSelector != null) && !v.viewSelector.isEmpty()) {
        v.setModel(LXView.create(this.lx.getModel(), v.viewSelector, v.viewNormalization));

      } else {
        LX.warning("View Definition is not a filter.");
        v.setModel(this.lx.getModel());
      }
    }

    // restore original model geometry
    mb.restoreModel((TEWholeModel) this.lx.getModel());
  }

  @Override
  protected ViewPerChannel createItem(LXAbstractChannel channel) {
    return new ViewPerChannel(this.lx, channel);
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  /*
   * View-Per-Pattern (or per-anything)
   *
   * 10th hour change, therefore not fully deleting the per-channel linkage.
   */

  public ViewParameter createParameter() {
    return new ViewParameter("View");
  }

  private ViewDefinition[] getViews() {
    // 1) Top Level, view=off
    ViewDefinition[] allViews = new ViewDefinition[views.size() + 1];
    allViews[0] = new ViewDefinition("----", false);
    allViews[0].setModel(this.lx.getModel());

    // Global feature off switch
    if (!ViewCentral.ENABLED) {
      return allViews;
    }
    /* JKB note: Skip this for now, leave here for possible later development
    // 2) The most recent user-typed view on this channel
    ViewDefinition custom = new ViewDefinition("Custom", channel.viewSelector.getString(), channel.viewNormalization.getEnum());
    custom.setModel(channel.getModelView());
    allViews[1] = custom;
     */

    // 3) All other views from file
    int i = 1;
    for (ViewDefinition v : views) {
      allViews[i++] = v;
    }

    return allViews;
  }

  public class ViewParameter extends ObjectParameter<ViewDefinition> implements DisposableParameter {

    String defaultView = null;

    public ViewParameter(String label) {
      this(label, getViews());
    }

    public ViewParameter(String label, ViewDefinition[] objects) {
      super(label, objects);
      setIncrementMode(IncrementMode.RELATIVE);
      setWrappable(false);
    }

    public LXModel getModel() {
      return getObject().getModel();
    }

    /**
     * Set to view with the matching label.
     *
     * If parameter is null, sets to default / no view
     * If label is not found, does nothing
     */
    public ViewParameter setView(String label) {
      return setView(label, false);
    }

    public ViewParameter setView(String label, boolean defaultOnNotFound) {
      ViewDefinition view = null;
      if (!TE.isEmpty(label)) {
        view = Arrays.stream(getObjects())
            .filter(v -> label.equals(v.label))
            .findFirst()
            .orElse(null);
      }
      if (view != null) {
        setValue(view);
      } else {
        setValue(0);
      }
      return this;
    }

    public ViewParameter setDefault(String label) {
      this.defaultView = label;
      return this;
    }

    @Override
    public ViewParameter reset() {
      if (this.defaultView != null) {
        return setView(this.defaultView);
      }
      return (ViewParameter)super.reset();
    }

    public ViewParameter reset(String label) {
      setDefault(label);
      return reset();
    }

    /*
     * TE-specific
     */

    public ViewParameter setView(TEShaderView shaderView) {
      return setView(shaderView, false);
    }

    public ViewParameter setView(TEShaderView shaderView, boolean defaultOnNotFound) {
      return setView(shaderView.getParameterKey(), defaultOnNotFound);
    }

    /**
     * Sets the default value and resets immediately if second parameter is true
     */
    public ViewParameter setDefault(TEShaderView shaderView, boolean applyImmediately) {
      if (shaderView != null) {
        this.defaultView = shaderView.getParameterKey();
      } else {
        this.defaultView = null;
      }
      if (applyImmediately) {
        return reset();
      }
      return this;
    }

    // Dev note: reset(TEShaderView shaderView) would better match LXListenableParameter.reset(double),
    // but that creates an ambiguous method call in the case of reset(null).

    private final List<DisposeListener> disposeListeners = new ArrayList<DisposeListener>();

    public void listenDispose(DisposeListener listener) {
      disposeListeners.add(listener);
    }

    public void unlistenDispose(DisposeListener listener) {
      if (!disposeListeners.remove(listener)) {
        LX.warning("Tried to remove unregistered DisposeListener");
      }
    }

    private void notifyDisposing() {
      for (int i = disposeListeners.size()-1; i>=0; --i) {
        disposeListeners.remove(i).disposing(this);
      }
    }

    @Override
    public void dispose() {
      notifyDisposing();
      super.dispose();
    }
  }

  /**
   * One of these will be created per LX channel.
   * A list of all instances is maintained by the ViewCentral class.
   */
  public class ViewPerChannel extends LXModelComponent {

    public final ViewParameter view;

    private final LXParameterListener viewListener = (p) -> {
      onViewChanged();
    };

    private LXAbstractChannel channel;

    protected ViewPerChannel(LX lx, LXAbstractChannel channel) {
      super(lx);
      this.channel = channel;

      this.view = createParameter();
      addParameter("view", this.view);      
      this.view.addListener(viewListener);

      // TODO: Initialize parameter to current channel view

      // TODO: register for channel.onModelChanged(), check to see if it was us or a manual view change
    }

    protected void onViewChanged() {
      // JKB note: bit of a power move here.  For a less aggressive implementation,
      // patterns could override getModel() and call this parameter.
      this.channel.setModel(getModel());

      if (this.lx instanceof LXStudio) {
        ((LXStudio) lx).ui.setMouseoverHelpText(channel.getLabel() + "   View:  " + this.view.getObject().toString());
      }
    }

    public LXModel getModel() {
      return this.view.getObject().getModel();
    }

    @Override
    public void dispose() {
      this.view.removeListener(viewListener);
      this.channel = null;
      super.dispose();
    }
  }

}
