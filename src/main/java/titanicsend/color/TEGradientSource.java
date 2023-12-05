package titanicsend.color;

import heronarts.lx.color.LXSwatch;
import titanicsend.lx.LXGradientUtils;

public class TEGradientSource {

    private static LXGradientUtils.ColorStops initColorStops(int numStops) {
        LXGradientUtils.ColorStops colorStops = new LXGradientUtils.ColorStops();
        colorStops.setNumStops(numStops);
        return colorStops;
    }

    // Whole palette gradient across all 5 stops. Usually starts and ends with black.
    public LXGradientUtils.ColorStops paletteGradient =
            initColorStops(5); // [X] [X] [X] [X] [X]  All five color entries
    public LXGradientUtils.ColorStops primaryGradient =
            initColorStops(3); // [X] [ ] [X] [ ] [ ]  Background primary -> Primary
    public LXGradientUtils.ColorStops secondaryGradient =
            initColorStops(3); // [ ] [ ] [ ] [X] [X]  Background secondary -> Secondary
    public LXGradientUtils.ColorStops foregroundGradient =
            initColorStops(3); // [ ] [ ] [X] [X] [ ]  Primary -> Secondary

    // If a pattern uses the standard gradients, call this in run() to ensure
    // palette changes are known and transitions are smooth
    public TEGradientSource updateGradients(LXSwatch swatch) {
        paletteGradient.stops[0].set(swatch.getColor(0));
        paletteGradient.stops[1].set(swatch.getColor(1));
        paletteGradient.stops[2].set(swatch.getColor(2));
        paletteGradient.stops[3].set(swatch.getColor(3));
        paletteGradient.stops[4].set(swatch.getColor(4));
        primaryGradient.stops[0].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
        primaryGradient.stops[1].set(swatch.getColor(TEColorType.BACKGROUND.swatchIndex()));
        primaryGradient.stops[2].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
        secondaryGradient.stops[0].set(swatch.getColor(TEColorType.SECONDARY.swatchIndex()));
        secondaryGradient.stops[1].set(swatch.getColor(TEColorType.SECONDARY_BACKGROUND.swatchIndex()));
        secondaryGradient.stops[2].set(swatch.getColor(TEColorType.SECONDARY.swatchIndex()));
        foregroundGradient.stops[0].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
        foregroundGradient.stops[1].set(swatch.getColor(TEColorType.SECONDARY.swatchIndex()));
        foregroundGradient.stops[2].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
        return this;
    }
}
