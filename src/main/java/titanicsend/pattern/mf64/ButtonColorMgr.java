package titanicsend.pattern.mf64;


import heronarts.lx.color.LXColor;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class ButtonColorMgr {
    private class ButtonInfo {
        int id;
        int r,g,b;

        ButtonInfo(int id, int color) {
            this.id = id;
            this.r = 0xFF & LXColor.red(color);
            this.g = 0xFF & LXColor.green(color);
            this.b = 0xFF & LXColor.blue(color);
        }

        int getColor() { return LXColor.rgba(r,g,b,255); }
    }
    private int refCount;
    private int defaultColor = TRANSPARENT;
    private final ArrayList<ButtonInfo> map;

    public ButtonColorMgr() {
        map = new ArrayList<ButtonInfo>(8);
        refCount = 0;
    }

    public int getRefCount() {
        return refCount;
    }

    public void reset() {
        map.clear();
        refCount = 0;
    }

    /**
     * Adds button to the list of currently "down" buttons
     * @param id button ID
     * @param color color associated with button
     * @return number of buttons currently pressed
     */
    public int addButton(int id, int color) {
        defaultColor = color;
        ButtonInfo bi = new ButtonInfo(id,color);
        map.add(bi);
        return refCount++;
    }

    /**
     * Remove button from list of currently active buttons
     * @param id button ID
     * @return number of buttons currently pressed
     */
    public int removeButton(int id) {
        for (ButtonInfo bi : map) {
            if (bi.id == id) {
                defaultColor = getCurrentColor();
                map.remove(bi);
                refCount--;
                break;
            }
        }
        return refCount;
    }

    public int getColorCount() {
        return map.size();
    }

    /**
     * @return int array containing the colors associated with all currently
     * pressed buttons.  If no buttons are down, returns a single element
     * array containing the color of the button that was most recently down.
     */
    public int[] getAllColors() {
        int[] result;
        int sz = getColorCount();

        if (sz == 0) {
            result = new int[1];
            result[0] = defaultColor;
        }
        else {
            result = new int[getColorCount()];
            for (int i = 0; i < sz; i++) {
                result[i] = map.get(i).getColor();
            }
        }
        return result;
    }

    /**
     * @return Interpolated blend of the colors of all currently pressed buttons
     */
     public int getBlendedColor() {
        int sz = map.size();
        if (sz < 1) return TRANSPARENT;

        int index = 0;
        ButtonInfo bi = map.get(index++);
        int r = bi.r; int g = bi.g; int b = bi.b;

        // blend colors by progressive interpolation.
        // Who knows what color you'll wind up with?
        while (index < sz) {
            bi = map.get(index++);
            r = r  + bi.r;
            g = g  + bi.g;
            b = b  + bi.b;
        }
        r = Math.min(r/sz,255); g = Math.min(g/sz,255); b = Math.min(b/sz,255);

        return LXColor.rgb(r,g,b);
    }

    /**
     * @return Color from most recently pressed (and currently down) button
     */
    public int getCurrentColor() {
        int sz = map.size();
        if (sz < 1) return TRANSPARENT;
        return map.get(sz-1).getColor();
    }
}
