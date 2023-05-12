/**
 * Licensing Notes (JKB)
 *
 * As this is a variation of ViewCentral:
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

import java.util.ArrayList;
import java.util.List;
import heronarts.lx.LX;
import heronarts.lx.LXModelComponent;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import titanicsend.app.TEApp;

public class ColorCentral extends ChannelExtension<titanicsend.model.justin.ColorCentral.ColorPerChannel> implements LXPalette.Listener {

  /*
   * Static
   */
  static public final boolean ENABLED = TEApp.ENABLE_COLOR_CENTRAL;
  
  static public final int CURRENT_SWATCH_INDEX = -1;

  static public interface ColorCentralListener {
    abstract public void ColorCentralLoaded();
  }

  // Static central reference keeps imports looking clean
  private static ColorCentral current;
  public static ColorCentral get() {
    return current;
  }

  public static boolean isLoaded() {
    return current != null;
  }

  static private final List<ColorCentralListener> listenersOnce = new ArrayList<ColorCentralListener>();

  static public void listenOnce(ColorCentralListener listener) {
    listenersOnce.add(listener);
  }

  static private void notifyListeners() {
    for (int i = listenersOnce.size()-1; i>=0; --i) {
      listenersOnce.remove(i).ColorCentralLoaded();
    }
  }

  /*
   * Non-static
   */

  protected List<SwatchDefinition> swatches;

  public ColorCentral(LX lx) {
    super(lx);
    current = this;

    this.lx.engine.palette.addListener(this);

    notifyListeners();
  }

  /**
   * Called by parent constructor before current channels are added
   */
  @Override
  protected void initialize() {
    createSwatchDefinitions();
  }
  
  private void createSwatchDefinitions() {
    if (this.swatches == null) {
      this.swatches = new ArrayList<SwatchDefinition>();
    } else {
      this.swatches.clear();
    }

    // 1) Top Level, view=off
    this.swatches.add(new SwatchDefinition("CURRENT", CURRENT_SWATCH_INDEX));

    // Global off switch for feature
    if (!ColorCentral.ENABLED) {
      return;
    }

    // 2) All other swatches in the global palette
    int i = 0;
    for (LXSwatch s : this.lx.engine.palette.swatches) {
      this.swatches.add(new SwatchDefinition(s.getLabel(), i++));
    }
  }
  
  @Override
  protected ColorPerChannel createItem(LXAbstractChannel channel) {
    return new ColorPerChannel(this.lx, channel);
  }

  public LXSwatch getSwatch(SwatchDefinition swatchDefinition) {
    if (swatchDefinition.index == CURRENT_SWATCH_INDEX) {
      return this.lx.engine.palette.swatch;
    } else {
      if (swatchDefinition.index >= this.lx.engine.palette.swatches.size()) {
        return this.lx.engine.palette.swatches.get(this.lx.engine.palette.swatches.size() - 1);
      }
      return this.lx.engine.palette.swatches.get(swatchDefinition.index);
    }
  }

  // LXPalette.Listener methods

  @Override
  public void swatchAdded(LXPalette palette, LXSwatch swatch) {
    // Quick hack solution.  Improve later.
    createSwatchDefinitions();
  }

  @Override
  public void swatchRemoved(LXPalette palette, LXSwatch swatch) {
    // Quick hack solution.  Improve later.
    createSwatchDefinitions();
  }

  @Override
  public void swatchMoved(LXPalette palette, LXSwatch swatch) {
    // TODO: update parameters to follow their selected swatch
  }

  @Override
  public void dispose() {
    this.lx.engine.palette.removeListener(this);
    super.dispose();
  }

  /**
   * One of these will be created per LX channel.
   * A list of all instances is maintained by the ViewCentral class.
   */
  public class ColorPerChannel extends LXModelComponent {

    public final SwatchParameter selectedSwatch;
    
    private final LXParameterListener selectedSwatchListener = (p) -> {
      onSelectedSwatchChanged();
    };

    private LXAbstractChannel channel;

    protected ColorPerChannel(LX lx, LXAbstractChannel channel) {
      super(lx);
      this.channel = channel;

      this.selectedSwatch = new SwatchParameter("Swatch", getSwatches());
      addParameter("view", this.selectedSwatch);      
      this.selectedSwatch.addListener(selectedSwatchListener);
    }

    private SwatchDefinition[] getSwatches() {
      // No custom entries here as there are with ViewCentral.
      return swatches.toArray(new SwatchDefinition[0]);
    }
    
    protected void onSelectedSwatchChanged() {
      if (this.lx instanceof LXStudio) {
        ((LXStudio) lx).ui.setMouseoverHelpText(channel.getLabel() + "   Swatch:  " + this.selectedSwatch.getObject());
      }
    }
    
    @Override
    public void dispose() {
      this.selectedSwatch.removeListener(selectedSwatchListener);
      super.dispose();
    }
  }
}
