package titanicsend.ui.color;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import heronarts.lx.utils.LXUtils;
import titanicsend.color.ColorPaletteManager;
import titanicsend.color.PaletteStrategy;

public class UIColorPaletteManager extends UI2dContainer
    implements UIControls, ColorPaletteManager.Listener {
  private static final float GRADIENT_HEIGHT = 8.0F;
  // WIP(look): feature flags for UI elements I'm unsure about / experimenting with.
  private static final boolean DISPLAY_GRADIENTS_ABOVE_SLIDERS = false;

  private final ColorPaletteManager manager;
  private final LXParameterListener paletteStrategyListener;

  private LXDynamicColor color1 = null;

  private float h = 0;
  private float s = 0;
  private float b = 0;
  private int calculatedColor = LXColor.BLACK;

  private final AllStrategies allStrategies;
  private final ColorSelector colorSelector;

  public UIColorPaletteManager(LXStudio.UI ui, ColorPaletteManager manager, float w) {
    super(0, 0, w, 0);
    this.setLayout(Layout.VERTICAL, 4F);
    this.setPadding(2, 0);
    this.manager = manager;

    float contentWidth = getContentWidth();

    addChildren(
        // One row for each Palette Strategy
        this.allStrategies = new AllStrategies(ui, contentWidth),

        // Component selectors (grid when expanded, sliders when collapsed)
        this.colorSelector = new ColorSelector(ui, contentWidth));

    manager.addListener(this, true);
    manager.paletteStrategy.addListener(
        this.paletteStrategyListener =
            (p) -> {
              PaletteStrategy strategy = manager.paletteStrategy.getEnum();
              for (AllStrategies.UIPaletteStrategy uiStrategy : allStrategies.strategies) {
                uiStrategy.setSelected(uiStrategy.strategy == strategy);
              }
            },
        true);

    // Recolor all the squares at 15fps
    addLoopTask(
        new UITimerTask(15, UITimerTask.Mode.FPS) {
          @Override
          public void run() {
            redrawColors();
          }
        });
  }

  /** The LXDynamicColor object in the first position of the managed swatch has changed */
  @Override
  public void color1Changed(LXDynamicColor color1) {
    setColor1(color1);
  }

  private void setColor1(LXDynamicColor color1) {
    if (this.color1 != color1) {
      if (this.color1 != null) {
        this.color1.color.removeListener(this.color1ValueChanged);
      }

      this.color1 = color1;
      this.colorSelector.grid.setParameter(this.color1 != null ? this.color1.color : null);

      if (this.color1 != null) {
        this.color1.color.addListener(this.color1ValueChanged, true);
      }
    }
  }

  /** Calculated color within the LXDynamicColor was changed */
  private final LXParameterListener color1ValueChanged =
      (p) -> {
        redrawColors();
      };

  private void redrawColors() {
    // Calculate h,s,b for reuse by all strategies
    if (this.color1 != null) {
      this.h = this.color1.getHuef();
      this.s = this.color1.getSaturation();
      this.b = this.color1.getBrightness();
      this.calculatedColor = this.color1.color.getColor();
    } else {
      this.h = 0;
      this.s = 0;
      this.b = 0;
      this.calculatedColor = LXColor.BLACK;
    }

    // Each strategy should recalculate its color samples
    for (AllStrategies.UIPaletteStrategy uiStrategy : allStrategies.strategies) {
      uiStrategy.redrawColors();
    }
  }

  private UI2dContainer newHierarchichalSwatch(
      ColorPaletteManager paletteMgr, float totalWidth, float totalHeight) {
    float swatchChildSpacing = 2F;
    float swatchTotalHeight = totalHeight - swatchChildSpacing / 2;
    float swatchSegmentHeight = swatchTotalHeight / 2;
    float swatchSegmentWidth = (totalWidth / 2) - (swatchChildSpacing / 2);

    UI2dContainer squareSwatch =
        UI2dContainer.newVerticalContainer(
            totalWidth,
            swatchChildSpacing,
            new UISingleColorDisplay(null, totalWidth, swatchSegmentHeight),
            UI2dContainer.newHorizontalContainer(
                swatchSegmentHeight,
                2F,
                new UISingleColorDisplay(
                    paletteMgr.getManagedSwatch().getColor(1).color,
                    swatchSegmentWidth,
                    swatchSegmentHeight),
                new UISingleColorDisplay(
                    paletteMgr.getManagedSwatch().getColor(2).color,
                    swatchSegmentWidth,
                    swatchSegmentHeight)));
    return squareSwatch;
  }

  private UI2dContainer buildColorSlidersRow(ColorPaletteManager paletteMgr) {
    float SLIDER_SPACING = 4;
    float controlWidth = this.width / 3;
    float sliderWidth = controlWidth - (2 * SLIDER_SPACING);
    float height = 42;
    UI2dContainer colorSlidersRow = UI2dContainer.newHorizontalContainer(height, SLIDER_SPACING);
    if (DISPLAY_GRADIENTS_ABOVE_SLIDERS) {
      addColumn(
          colorSlidersRow,
          new UIHueDisplay(null /*paletteMgr.color1.color.hue*/, sliderWidth),
          new UISlider(
              UISlider.Direction.HORIZONTAL,
              0.0F,
              0.0F,
              sliderWidth,
              12.0F, /*paletteMgr.color1.color.hue*/
              null));
      addColumn(
          colorSlidersRow,
          new UISaturationDisplay(sliderWidth),
          new UISlider(
              UISlider.Direction.HORIZONTAL,
              0.0F,
              0.0F,
              sliderWidth,
              12.0F, /*paletteMgr.color1.color.saturation*/
              null));
    } else {
      addColumn(
          colorSlidersRow,
          new UISlider(
              UISlider.Direction.HORIZONTAL,
              0.0F,
              0.0F,
              sliderWidth,
              12.0F, /*paletteMgr.color1.color.hue*/
              null));
      addColumn(
          colorSlidersRow,
          new UISlider(
              UISlider.Direction.HORIZONTAL,
              0.0F,
              0.0F,
              sliderWidth,
              12.0F, /*paletteMgr.color1.color.saturation*/
              null));
    }

    return colorSlidersRow;
  }

  @Override
  public void dispose() {
    this.manager.paletteStrategy.removeListener(this.paletteStrategyListener);
    this.manager.removeListener(this);
    super.dispose();
  }

  private class UISingleColorDisplay extends UI2dComponent {
    private int color = LXColor.BLACK;

    private UISingleColorDisplay(ColorParameter colorParameter, float dimension) {
      this(colorParameter, dimension, dimension);
    }

    private UISingleColorDisplay(ColorParameter colorParameter, float w, float h) {
      super(0, 0, w, h);
      if (colorParameter != null) {
        this.addListener(
            colorParameter,
            (p) -> {
              this.color = ((ColorParameter) p).getColor();
              this.redraw();
            },
            true);
      }
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.fillColor(this.color);
      vg.rect(0, 0, this.getWidth(), this.getHeight());
      vg.fill();
    }
  }

  private class UIHueDisplay extends UIGradientDisplay {
    private double hueOffset = 0.0;

    private UIHueDisplay(BoundedParameter hueOffset, float gradientWidth) {
      super(gradientWidth, GRADIENT_HEIGHT);
      this.addListener(
          hueOffset,
          (p) -> {
            this.hueOffset = (double) hueOffset.getValuef();
            this.redraw();
          });
    }

    public void onDraw(UI ui, VGraphics vg) {
      int numStops = 12;
      float stopWidth = this.width / (float) numStops;

      for (int i = 0; i < numStops; ++i) {
        float x0 = stopWidth * (float) i;
        float x1 = LXUtils.clampf(x0 + stopWidth + 0.5F, 0.0F, this.width);
        vg.beginPath();
        vg.fillLinearGradient(
            x0,
            0.0F,
            x1,
            0.0F,
            LXColor.hsb(this.hueOffset + (double) (i * 360 / numStops), 100.0, 100.0),
            LXColor.hsb(this.hueOffset + (double) ((i + 1) * 360 / numStops), 100.0, 100.0));
        vg.rect(x0, 0.0F, x1 - x0, this.height);
        vg.fill();
      }
    }
  }

  private class UISaturationDisplay extends UIGradientDisplay {
    private UISaturationDisplay(float gradientWidth) {
      super(gradientWidth, GRADIENT_HEIGHT);
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillLinearGradient(
          0.0F,
          0.0F,
          this.width,
          0.0F,
          LXColor.hsb(0.0F, 0.0F, 50.0F),
          LXColor.hsb(0.0F, 100.0F, 100.0F));
      vg.rect(0.0F, 0.0F, this.width, this.height);
      vg.fill();
    }
  }

  private class UIGradientDisplay extends UI2dComponent {
    protected UIGradientDisplay(float gradientWidth, float gradientHeight) {
      super(0.0F, 0.0F, gradientWidth, gradientHeight);
    }
  }

  private static class CollapsibleChild extends UI2dContainer {
    private boolean collapsed = false;

    public CollapsibleChild(UI ui, float w) {
      super(0, 0, w, 0);
    }

    public final void setCollapsed(boolean collapsed) {
      this.collapsed = collapsed;
      onCollapsedChanged(collapsed);
      // this.redraw();
    }

    protected void onCollapsedChanged(boolean collapsed) {}
  }

  private class AllStrategies extends CollapsibleChild {

    private final UIPaletteStrategy[] strategies =
        new UIPaletteStrategy[PaletteStrategy.values().length];

    public AllStrategies(UI ui, float w) {
      super(ui, w);
      setLayout(Layout.VERTICAL, 2);
      setBackgroundColor(ui.theme.listBackgroundColor);
      setBorderRounding(4);
      setPadding(4);

      int i = 0;
      for (PaletteStrategy strategy : PaletteStrategy.values()) {
        UIPaletteStrategy uiStrategy = new UIPaletteStrategy(ui, strategy, getContentWidth());
        uiStrategy.addToContainer(this);
        this.strategies[i++] = uiStrategy;
      }
    }

    private class UIPaletteStrategy extends UI2dContainer {

      private static final float HEIGHT = 16f;
      private final PaletteStrategy strategy;
      private final ColorSample[] samples = new ColorSample[3];
      private boolean isSelected = false;

      public UIPaletteStrategy(UI ui, PaletteStrategy strategy, float w) {
        super(0, 0, w, HEIGHT);
        this.setLayout(Layout.HORIZONTAL, 4);
        setBackgroundColor(ui.theme.listItemBackgroundColor);
        setBorderRounding(4);
        setPadding(4);

        this.strategy = strategy;

        new UILabel(85, this.strategy.label)
            .setFont(ui.theme.getControlFont())
            .addToContainer(this);

        float indicatorY = 2.5f;
        float indicatorHeight = HEIGHT - (2 * indicatorY);
        for (int i = 0; i < samples.length; ++i) {
          ColorSample sample = new ColorSample(ui, indicatorY, indicatorHeight);
          sample.addToContainer(this);
          this.samples[i] = sample;
        }
        redrawColors();
      }

      private void redrawColors() {
        this.samples[0].setColor(calculatedColor);
        this.samples[1].setColor(this.strategy.getColor2(h, s, b));
        this.samples[2].setColor(this.strategy.getColor3(h, s, b));
      }

      public void setSelected(boolean isSelected) {
        if (this.isSelected != isSelected) {
          this.isSelected = isSelected;
          redraw();
        }
      }

      @Override
      protected void drawBorder(UI ui, VGraphics vg) {
        if (this.isSelected) {
          float weight = 1f;
          vg.beginPath();
          vgRoundedRect(vg, weight * .5f, weight * .5f, this.width - weight, this.height - weight);
          vg.strokeWidth(weight);
          vg.strokeColor(ui.theme.controlTextColor);
          vg.stroke();

          // Reset stroke weight
          vg.strokeWidth(1);
        }
        super.drawBorder(ui, vg);
      }

      @Override
      protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
        // Mouse click on strategy row
        manager.paletteStrategy.setValue(this.strategy);
        super.onMousePressed(mouseEvent, mx, my);
      }
    }
  }

  /** Rounded square color sample */
  private static class ColorSample extends UI2dComponent {

    private int color = LXColor.BLACK;

    public ColorSample(UI ui, float y, float size) {
      this(ui, 0, y, size, size);
    }

    public ColorSample(UI ui, float x, float y, float w, float h) {
      super(x, y, w, h);
      setBorderRounding(4);
    }

    public void setColor(int color) {
      if (this.color != color) {
        this.color = color;
        redraw();
      }
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillColor(this.color);
      vgRoundedRect(vg, 0, 0, this.width, this.height);
      vg.fill();
    }
  }

  private class ColorSelector extends CollapsibleChild {

    private final UI2dContainer col1;
    private final UI2dContainer sliders;
    private final UIColorGrid grid;

    final UIButton pushButton;

    public ColorSelector(UI ui, float w) {
      super(ui, w);
      setLayout(Layout.HORIZONTAL, 2);

      col1 =
          addColumn(
              this,
              100,
              this.sliders = buildColorSlidersRow(manager),
              this.grid = new UIColorGrid(null, 0, 0, 100, 60));

      addColumn(
              this,
              40,
              this.pushButton =
                  newButton(manager.pushSwatch, 40F)
                      .setActiveLabel("ON")
                      .setInactiveLabel("PUSH")
                      .setMomentary(true),
              newButton(manager.pinSwatch, 40F).setActiveLabel("PINNED").setInactiveLabel("PIN"))
          .setX(getContentWidth() - 40F)
          .setY(2);

      // Disable Push when Pin is enabled
      addListener(
          manager.pinSwatch,
          (p) -> {
            pushButton.setEnabled(!manager.pinSwatch.isOn());
          },
          true);

      updateCollapsibleState(false);
    }

    @Override
    protected void onCollapsedChanged(boolean collapsed) {
      updateCollapsibleState(collapsed);
    }

    private void updateCollapsibleState(boolean collapsed) {
      this.sliders.setVisible(collapsed);
      setContentHeight(col1.getContentHeight());
    }
  }
}
