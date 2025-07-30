package titanicsend.ui.color;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

public class UIColorGrid extends UI2dComponent implements UIFocus {

  private static final float PADDING = 6;

  private final float gridX;
  private final float gridY;
  private final float gridWidth;
  private final float gridHeight;

  private final float brightSliderX;
  private final float brightSliderY;
  private final float brightSliderWidth;
  private final float brightSliderHeight;

  private boolean enabled = true;

  private ColorParameter color;

  private final LXParameterListener colorValueChanged =
      (p) -> {
        redraw();
      };

  public UIColorGrid(ColorParameter color, float x, float y, float w, float h) {
    super(x, y, w, h);
    this.gridX = this.gridY = PADDING;
    this.gridWidth = this.gridHeight = h - (PADDING * 2);

    this.brightSliderX = this.gridWidth + (PADDING * 2) + 8;
    this.brightSliderY = PADDING;
    this.brightSliderWidth = 12;
    this.brightSliderHeight = this.gridHeight;

    setFocusCorners(false);
    setParameter(color);
  }

  public UIColorGrid setParameter(ColorParameter color) {
    if (this.color != color) {
      if (this.color != null) {
        this.color.removeListener(this.colorValueChanged);
      }
      this.color = color;
      if (this.color != null) {
        this.color.addListener(this.colorValueChanged);
      }
    }
    return this;
  }

  private void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    final int xStops = 6;
    final int yStops = 40;
    final float xStep = gridWidth / xStops;
    final float yStep = gridHeight / yStops;

    float hue = color != null ? color.hue.getBaseValuef() : 0;
    float saturation = color != null ? color.saturation.getBaseValuef() : 0;
    float brightness = color != null ? color.brightness.getBaseValuef() : 0;

    // Main color grid
    for (int y = 0; y < yStops; ++y) {
      for (int x = 0; x < xStops; ++x) {
        vg.fillLinearGradient(
            gridX + x * xStep,
            0,
            gridX + (x + 1) * xStep,
            0,
            LXColor.hsb(x * 360f / xStops, 100f - y * 100f / yStops, brightness),
            LXColor.hsb((x + 1) * 360f / xStops, 100f - y * 100f / yStops, brightness));
        vg.beginPath();
        vg.rect(gridX + x * xStep - .5f, gridY + y * yStep - .5f, xStep + 1, yStep + 1);
        vg.fill();
      }
    }

    // Brightness slider
    vg.fillLinearGradient(
        brightSliderX,
        brightSliderY,
        brightSliderX,
        brightSliderHeight,
        LXColor.hsb(hue, saturation, 100),
        LXColor.hsb(hue, saturation, 0));
    vg.beginPath();
    vg.rect(brightSliderX, brightSliderY - .5f, brightSliderWidth, brightSliderHeight + 1);
    vg.fill();

    // Position indicator
    if (this.color != null) {
      vg.beginPath();
      vg.strokeColor(brightness < 50 ? UIColor.WHITE : UIColor.BLACK);
      vg.ellipse(gridX + hue / 360 * gridWidth, gridY + (1 - saturation / 100) * gridHeight, 4, 4);
      vg.stroke();
    }

    // Brightness triangle
    vg.beginPath();
    vg.fillColor(ui.theme.controlTextColor);
    float xp = brightSliderX;
    float yp = brightSliderY + (1 - brightness / 100) * brightSliderHeight;
    vg.moveTo(xp, yp);
    vg.lineTo(xp - 6, yp - 4);
    vg.lineTo(xp - 6, yp + 4);
    vg.closePath();
    vg.moveTo(xp + brightSliderWidth, yp);
    vg.lineTo(xp + brightSliderWidth + 6, yp + 4);
    vg.lineTo(xp + brightSliderWidth + 6, yp - 4);
    vg.closePath();
    vg.fill();
  }

  private boolean draggingBrightness = false;
  private LXCommand.Parameter.SetValue setBrightness = null;
  private LXCommand.Parameter.SetColor setColor = null;

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (!this.enabled || this.color == null) {
      return;
    }
    this.setBrightness = null;
    this.setColor = null;
    if (this.draggingBrightness = (mx > gridX + gridWidth)) {
      this.setBrightness =
          new LXCommand.Parameter.SetValue(color.brightness, color.brightness.getBaseValue());
    } else {
      this.setColor = new LXCommand.Parameter.SetColor(color);
      setHueSaturation(mx, my);
    }
  }

  @Override
  public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    this.setBrightness = null;
    this.setColor = null;
  }

  private void setHueSaturation(float mx, float my) {
    mx = LXUtils.clampf(mx - gridX, 0, gridWidth);
    my = LXUtils.clampf(my - gridY, 0, gridWidth);
    double hue = mx / gridWidth * 360;
    double saturation = 100 - my / gridHeight * 100;
    getLX().command.perform(this.setColor.update(hue, saturation));
  }

  @Override
  public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (!this.enabled || this.color == null) {
      return;
    }
    if (this.draggingBrightness) {
      if (dy != 0) {
        float brightness = color.brightness.getBaseValuef();
        brightness = LXUtils.clampf(brightness - 100 * dy / brightSliderHeight, 0, 100);
        getLX().command.perform(this.setBrightness.update(brightness));
      }
    } else {
      setHueSaturation(mx, my);
    }
    mouseEvent.consume();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (!this.enabled || this.color == null) {
      return;
    }
    float inc = keyEvent.isShiftDown() ? 10 : 2;
    if (keyCode == java.awt.event.KeyEvent.VK_UP) {
      keyEvent.consume();
      getLX()
          .command
          .perform(
              new LXCommand.Parameter.SetValue(
                  color.saturation,
                  LXUtils.clampf(color.saturation.getBaseValuef() + inc, 0, 100)));
    } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
      keyEvent.consume();
      getLX()
          .command
          .perform(
              new LXCommand.Parameter.SetValue(
                  color.saturation,
                  LXUtils.clampf(color.saturation.getBaseValuef() - inc, 0, 100)));
    } else if (keyCode == java.awt.event.KeyEvent.VK_LEFT) {
      keyEvent.consume();
      getLX()
          .command
          .perform(
              new LXCommand.Parameter.SetValue(
                  color.hue, LXUtils.clampf(color.hue.getBaseValuef() - 3 * inc, 0, 360)));
    } else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
      keyEvent.consume();
      getLX()
          .command
          .perform(
              new LXCommand.Parameter.SetValue(
                  color.hue, LXUtils.clampf(color.hue.getBaseValuef() + 3 * inc, 0, 360)));
    }
  }
}
