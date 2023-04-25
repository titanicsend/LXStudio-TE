/**
 * This file is mainly a copy Heron Art's P4LX UIColorPicker:
 * https://github.com/heronarts/P4LX/blob/master/src/main/java/heronarts/p4lx/ui/component/UIColorPicker.java
 * ...Additional controls were added at the bottom for TE.
 * 
 * This file could be simplified in the future with a few changes
 * to the restrictions in UIColorPicker.
 * 
 * 
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.ui;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.pattern.UIGradientPattern.UIPaletteGradient;
import heronarts.lx.utils.LXUtils;
import heronarts.p4lx.ui.UI;
import heronarts.p4lx.ui.UI2dComponent;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.UIFocus;
import heronarts.p4lx.ui.UITimerTask;
import heronarts.p4lx.ui.component.UIButton;
import heronarts.p4lx.ui.component.UIDoubleBox;
import heronarts.p4lx.ui.component.UIKnob;
import heronarts.p4lx.ui.component.UILabel;
import heronarts.p4lx.ui.component.UIParameterControl;
import heronarts.p4lx.ui.component.UISlider;

public class UITEColorPicker extends UI2dComponent {

  public enum Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
  };

  private Corner corner = Corner.BOTTOM_RIGHT;

  private TEColorParameter color;

  private UITEColorOverlay uiColorOverlay = null;

  private boolean enabled = true;

  private int drawColor = 0xff000000;

  private boolean deviceMode = false;

  public UITEColorPicker(TEColorParameter color) {
    this(UIKnob.WIDTH, UIKnob.WIDTH, color);
  }

  public UITEColorPicker(float w, float h, TEColorParameter color) {
    this(0, 0, w, h, color);
  }

  public UITEColorPicker(float x, float y, float w, float h, TEColorParameter color) {
    this(x, y, w, h, color, false);
  }

  protected UITEColorPicker(float x, float y, float w, float h, TEColorParameter color, boolean isDynamic) {
    super(x, y, w, h);
    setColor(color);

    // Redraw with color in real-time, if modulated
    if (!isDynamic) {
      setDescription(UIParameterControl.getDescription(color));
      addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
        @Override
        protected void run() {
          setDrawColor(color.calcColor());
        }
      });
    }
  }

  protected void setDrawColor(int drawColor) {
    if (this.drawColor != drawColor) {
      this.drawColor = drawColor;
      redraw();
    }
  }

  public UITEColorPicker setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  protected UITEColorPicker setDeviceMode(boolean deviceMode) {
    this.deviceMode = deviceMode;
    return this;
  }

  private final LXParameterListener redrawSwatch = (p) -> {
    if (this.uiColorOverlay != null) {
      this.uiColorOverlay.swatch.redraw();
      this.uiColorOverlay.swatchDimmer.redraw();
    }
  };

  protected UITEColorOverlay buildColorOverlay(UI ui) {
    return new UITEColorOverlay(ui);
  }

  public UITEColorPicker setCorner(Corner corner) {
    this.corner = corner;
    return this;
  }

  void setColor(TEColorParameter color) {
    if (this.color != null) {
      this.color.removeListener(this.redrawSwatch);
    }
    this.color = color;
    if (color != null) {
      color.addListener(this.redrawSwatch);
      redrawSwatch.onParameterChanged(color);
    } else {
      setDrawColor(LXColor.BLACK);
    }
  }

  @Override
  public void onDraw(UI ui, PGraphics pg) {
    pg.stroke(ui.theme.getControlBorderColor());
    pg.fill(this.drawColor);
    if (this.deviceMode) {
      pg.rect(UIKnob.KNOB_MARGIN, 0, UIKnob.KNOB_SIZE, UIKnob.KNOB_SIZE);
      UIParameterControl.drawParameterLabel(ui, pg, this, (this.color != null) ? this.color.getLabel() : "-");
    } else {
      pg.rect(0, 0, this.width, this.height);
    }
  }

  protected void hideOverlay() {
    getUI().hideContextOverlay();
  }

  private void showOverlay() {
    final float overlap = 6;

    if (this.uiColorOverlay == null) {
      this.uiColorOverlay = buildColorOverlay(getUI());
    }

    switch (this.corner) {
    case BOTTOM_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), this.height - overlap);
      break;
    case BOTTOM_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, this.height - overlap);
      break;
    case TOP_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), overlap - this.uiColorOverlay.getHeight());
      break;
    case TOP_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, overlap - this.uiColorOverlay.getHeight());
      break;
    }

    getUI().showContextOverlay(this.uiColorOverlay);
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      consumeMousePress();
      showOverlay();
    }
    super.onMousePressed(mouseEvent, mx, my);
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.enabled) {
      if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_SPACE) {
        consumeKeyEvent();
        showOverlay();
      } else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
        if ((this.uiColorOverlay != null) && (this.uiColorOverlay.isVisible())) {
          consumeKeyEvent();
          hideOverlay();
        }
      }
    }
    super.onKeyPressed(keyEvent, keyChar, keyCode);
  }

  
  protected class UITEColorOverlay extends UI2dContainer {

    private static final int PADDING = 12;

    private final UISwatch swatch;
    private final UI2dComponent swatchDimmer;
    private final UI2dComponent color1;
    private final UI2dComponent color2;

    UITEColorOverlay(UI ui) {
      this(ui, 324, color instanceof TEColorParameter ? 60 : 8);
    }

    UITEColorOverlay(UI ui, float width, float extraHeight) {
      super(0, 0, width, UISwatch.HEIGHT + extraHeight);

      setBackgroundColor(UI.get().theme.getDeviceBackgroundColor());
      setBorderColor(UI.get().theme.getControlBorderColor());
      setBorderRounding(6);

      this.swatch = new UISwatch();
      this.swatch.addToContainer(this);

      float xp = this.swatch.getX() + this.swatch.getWidth();
      float yp = 16;
      final UIDoubleBox hueBox = (UIDoubleBox) new UIDoubleBox(xp, yp, 56, color.hue).addToContainer(this);
      new UILabel(xp, yp + 16, 56, "Hue").setTextAlignment(PConstants.CENTER).addToContainer(this);

      yp += 40;

      final UIDoubleBox satBox = (UIDoubleBox) new UIDoubleBox(xp, yp, 56, color.saturation).addToContainer(this);
      new UILabel(xp, yp + 16, 56, "Sat").setTextAlignment(PConstants.CENTER).addToContainer(this);

      yp += 40;

      final UIDoubleBox brtBox = (UIDoubleBox) new UIDoubleBox(xp, yp, 56, color.brightness).addToContainer(this);
      new UILabel(xp, yp + 16, 56, "Bright").setTextAlignment(PConstants.CENTER).addToContainer(this);

      // Horizontal break
      new UI2dComponent(PADDING, 140, this.width - 2*PADDING, 1) {}
      .setBorderColor(ui.theme.getDarkBackgroundColor())
      .addToContainer(this);

      
      // Special for TEColorParameter

      // Gray overlay for swatch when not using STATIC source
      swatchDimmer = new UILabel(PADDING, PADDING-1, this.width - 2*PADDING, 130)
      .setBackgroundColor(LXColor.rgba(75,75,75,200))
      .addToContainer(this);

      // Solid color
      UI2dContainer.newHorizontalContainer(16, 4,
        new UILabel(58, 12, "Solid Color:").setFont(ui.theme.getControlFont()),
        new UIButton(128, 16, color.solidSource)
      )
      .setPosition(PADDING, 148)
      .addToContainer(this);

      // Gradient
      UI2dContainer.newHorizontalContainer(16, 4,
          new UILabel(58, 12, "Gradient:").setFont(ui.theme.getControlFont()),
          new UIButton(68, 16, color.gradient),
          new UIButton(56, 16, color.blendMode)
        )
      .setPosition(PADDING, 171)
      .addToContainer(this);
      
      // Offset
      new UIKnob(204, 147, color.offset).addToContainer(this);

      // Solid color preview
      UI2dContainer.newHorizontalContainer(16, 1,
          color1 = new UI2dComponent(0, 0, 16, 16) {}
            .setBorderColor(ui.theme.getControlBorderColor())
            .setBackgroundColor(color.calcColor())
            .setBorderRounding(4),
          new UISlider(UISlider.Direction.HORIZONTAL, 30, 16, color.color2offset)
            .setShowLabel(false),
          color2 = new UI2dComponent(0, 0, 16, 16) {}
            .setBorderColor(ui.theme.getControlBorderColor())
            .setBackgroundColor(color.calcColor2())
            .setBorderRounding(4)
        )
        .setPosition(248, 148)
        .addToContainer(this);
      
      addListener(color, p -> {
        color1.setBackgroundColor(color.calcColor());
        color2.setBackgroundColor(color.calcColor2());        
      });

      // Gradient preview
      new UIPaletteGradient((LXStudio.UI)ui, color, 64, 16)
      .setPosition(248, 171)
      .addToContainer(this);
      
      color.solidSource.addListener((p) -> {
        boolean isStatic = color.solidSource.getEnum() == TEColorParameter.SolidColorSource.STATIC;
        swatch.setEnabled(isStatic);
        hueBox.setEnabled(isStatic);
        satBox.setEnabled(isStatic);
        brtBox.setEnabled(isStatic);
        swatchDimmer.setVisible(!isStatic);
      }, true);
    }
    
    private class UISwatch extends UI2dComponent implements UIFocus {

      private static final float PADDING = 8;

      private static final float GRID_X = PADDING;
      private static final float GRID_Y = PADDING;

      private static final float GRID_WIDTH = 120;
      private static final float GRID_HEIGHT = 120;

      private static final float BRIGHT_SLIDER_X = 140;
      private static final float BRIGHT_SLIDER_Y = PADDING;
      private static final float BRIGHT_SLIDER_WIDTH = 16;
      private static final float BRIGHT_SLIDER_HEIGHT = GRID_HEIGHT;

      private static final float WIDTH = BRIGHT_SLIDER_X + BRIGHT_SLIDER_WIDTH + 2*PADDING;
      private static final float HEIGHT = GRID_HEIGHT + 2*PADDING;

      private boolean enabled = true;

      public UISwatch() {
        super(4, 4, WIDTH, HEIGHT);
        color.addListener((p) -> { redraw(); });
        setFocusCorners(false);
      }

      private void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      @Override
      public void onDraw(UI ui, PGraphics pg) {

        float hue = color.hue.getBaseValuef();
        float saturation = color.saturation.getBaseValuef();
        float brightness = color.brightness.getBaseValuef();

        // Main color grid
        for (int x = 0; x < GRID_WIDTH; ++x) {
          for (int y = 0; y < GRID_HEIGHT; ++y) {
            pg.stroke(LXColor.hsb(
              360 * (x / GRID_WIDTH),
              100 * (1 - y / GRID_HEIGHT),
              brightness
            ));
            pg.point(GRID_X + x, GRID_Y + y);
          }
        }

        // Brightness slider
        for (int y = 0; y < BRIGHT_SLIDER_HEIGHT; ++y) {
          pg.stroke(LXColor.hsb(hue, saturation, 100 - 100 * (y / BRIGHT_SLIDER_HEIGHT)));
          pg.line(BRIGHT_SLIDER_X, BRIGHT_SLIDER_Y + y, BRIGHT_SLIDER_X + BRIGHT_SLIDER_WIDTH - 1, BRIGHT_SLIDER_Y + y);
        }

        // Color square
        pg.noFill();
        pg.stroke(brightness < 50 ? 0xffffffff : 0xff000000);
        pg.ellipseMode(PConstants.CORNER);
        pg.ellipse(
          GRID_X + hue / 360 * GRID_WIDTH,
          GRID_Y + (1 - saturation / 100) * GRID_HEIGHT,
          4,
          4
        );

        // Brightness triangle
        pg.fill(0xffcccccc);
        pg.noStroke();

        pg.beginShape(PConstants.TRIANGLES);
        float xp = BRIGHT_SLIDER_X;
        float yp = BRIGHT_SLIDER_Y + (1 - brightness / 100) * BRIGHT_SLIDER_HEIGHT;
        pg.vertex(xp, yp);
        pg.vertex(xp - 6, yp - 4);
        pg.vertex(xp - 6, yp + 4);

        pg.vertex(xp + BRIGHT_SLIDER_WIDTH, yp);
        pg.vertex(xp + BRIGHT_SLIDER_WIDTH + 6, yp + 4);
        pg.vertex(xp + BRIGHT_SLIDER_WIDTH + 6, yp - 4);

      }

      private boolean draggingBrightness = false;
      private LXCommand.Parameter.SetValue setBrightness = null;
      private LXCommand.Parameter.SetColor setColor = null;

      @Override
      public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
        if (!this.enabled) {
          return;
        }

        this.setBrightness = null;
        this.setColor = null;
        if (this.draggingBrightness = (mx > GRID_X + GRID_WIDTH)) {
          this.setBrightness = new LXCommand.Parameter.SetValue(color.brightness, color.brightness.getBaseValue());
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
        mx = LXUtils.clampf(mx - GRID_X, 0, GRID_WIDTH);
        my = LXUtils.clampf(my - GRID_Y, 0, GRID_WIDTH);
        double hue = mx / GRID_WIDTH * 360;
        double saturation = 100 - my / GRID_HEIGHT * 100;
        getLX().command.perform(this.setColor.update(hue, saturation));
      }

      @Override
      public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
        if (!this.enabled) {
          return;
        }
        if (this.draggingBrightness) {
          if (dy != 0) {
            float brightness = color.brightness.getBaseValuef();
            brightness = LXUtils.clampf(brightness - 100 * dy / BRIGHT_SLIDER_HEIGHT, 0, 100);
            getLX().command.perform(this.setBrightness.update(brightness));
          }
        } else {
          setHueSaturation(mx, my);
        }
      }

      @Override
      public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
        if (!this.enabled) {
          return;
        }
        float inc = keyEvent.isShiftDown() ? 10 : 2;
        if (keyCode == java.awt.event.KeyEvent.VK_UP) {
          consumeKeyEvent();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.saturation,
            LXUtils.clampf(color.saturation.getBaseValuef() + inc, 0, 100)
          ));
        } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
          consumeKeyEvent();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.saturation,
            LXUtils.clampf(color.saturation.getBaseValuef() - inc, 0, 100)
          ));
        } else if (keyCode == java.awt.event.KeyEvent.VK_LEFT) {
          consumeKeyEvent();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.hue,
            LXUtils.clampf(color.hue.getBaseValuef() - 3*inc, 0, 360)
          ));
        } else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
          consumeKeyEvent();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.hue,
            LXUtils.clampf(color.hue.getBaseValuef() + 3*inc, 0, 360)
          ));
        }
      }

    }
    
  }
}
