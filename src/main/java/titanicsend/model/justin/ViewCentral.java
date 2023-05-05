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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import heronarts.lx.LX;
import heronarts.lx.LXModelComponent;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXView;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.app.TEApp;
import titanicsend.util.TE;

/**
 * Generate a set of LXViews once, then allow each channel to scroll through
 * the views with a parameter.
 */
public class ViewCentral extends ChannelExtension<titanicsend.model.justin.ViewCentral.ViewPerChannel> {

  // Static central reference keeps imports looking clean
  private static ViewCentral current;
  public static ViewCentral get() {
    return current;
  }
  
  protected List<ViewDefinition> views;

  public ViewCentral(LX lx) {
    super(lx);
    current = this;
  }

  @Override
  protected void initialize() {
    this.views = new ArrayList<ViewDefinition>();    

    // Global feature off switch
    if (!TEApp.ENABLE_VIEW_CENTRAL) {
      return;
    }

    // Load view definitions from file
    // TODO: Load from an ordered file type.
    Properties savedViews = new Properties();
    try (InputStream is = new FileInputStream("resources/vehicle/viewKnobs.properties")) {
      savedViews.load(is);
    } catch (IOException e) {
      TE.err(e, "Error loading views:");
      return;
    }

    for (String label : savedViews.stringPropertyNames()) {
      this.views.add(new ViewDefinition(label, savedViews.getProperty(label), LXView.Normalization.RELATIVE));
    }
    
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
  }

  @Override
  protected ViewPerChannel createItem(LXAbstractChannel channel) {
    return new ViewPerChannel(this.lx, channel);
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  /**
   * One of these will be created per LX channel.
   * A list of all instances is maintained by the ViewCentral class.
   */
  public class ViewPerChannel extends LXModelComponent {

    public final ObjectParameter<ViewDefinition> view;
    
    private final LXParameterListener viewListener = (p) -> {
      onViewChanged();
    };

    private LXAbstractChannel channel;

    protected ViewPerChannel(LX lx, LXAbstractChannel channel) {
      super(lx);
      this.channel = channel;

      this.view = new ObjectParameter<ViewDefinition>("View", getViews());
      addParameter("view", this.view);      
      this.view.addListener(viewListener);

      // TODO: Initialize parameter to current channel view

      // TODO: register for channel.onModelChanged(), check to see if it was us or a manual view change
    }

    private ViewDefinition[] getViews() {
      // 1) Top Level, view=off
      ViewDefinition[] allViews = new ViewDefinition[views.size() + 2];
      allViews[0] = new ViewDefinition("----", false);
      allViews[0].setModel(this.lx.getModel());

      // Global feature off switch
      if (!TEApp.ENABLE_VIEW_CENTRAL) {
        return allViews;
      }
      // 2) The most recent user-typed view on this channel
      ViewDefinition custom = new ViewDefinition("Custom", channel.viewSelector.getString(), channel.viewNormalization.getEnum());
      custom.setModel(channel.getModelView());
      allViews[1] = custom;

      // 3) All other views
      int i = 2;
      for (ViewDefinition v : views) {
        allViews[i++] = v;
      }

      return allViews;      
    }
    
    protected void onViewChanged() {
      // JKB note: bit of a power move here.  For a less aggressive option,
      // patterns could override getModel() and call this parameter.
      this.channel.setModel(getModel());
    }

    public LXModel getModel() {
      return this.view.getObject().getModel();
    }
    
    @Override
    public void dispose() {
      this.channel = null;
      super.dispose();
    }
  }

}
