package titanicsend.pattern.jon;


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

        int getColor() { return LXColor.rgb(r,g,b); }
    }

    private ArrayList<ButtonInfo> map;

    public ButtonColorMgr() {
        map = new ArrayList<ButtonInfo>(8);
    }

    public void addButton(int id, int color) {
        ButtonInfo bi = new ButtonInfo(id,color);
        map.add(bi);
    }

    public void removeButton(int id) {
        for (ButtonInfo bi : map) {
            if (bi.id == id) {
                map.remove(bi);
                break;
            }
        }
    }

    public int getColorCount() {
        return map.size();
    }

    /**
     * @return int array containing the colors associated with all currently
     * pressed buttons.  If no buttons are down, returns a single element
     * array containing the TRANSPARENT color.
     */
    public int[] getAllColors() {
        int[] result;
        int sz = getColorCount();

        if (sz == 0) {
            result = new int[1];
            result[0] = TRANSPARENT;
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
