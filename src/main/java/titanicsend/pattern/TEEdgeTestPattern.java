package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.model.TEEdgeModel;

@LXCategory("Test")
public class TEEdgeTestPattern extends TEPattern {

    public final ObjectParameter<String> target =
            (ObjectParameter<String>) new ObjectParameter<String>("Edge", new String[] {
                "7597", "983", "9114", "9116", "912", "10100", "10115", "10117", "1011", "1198", "11100", "1285",
                "1283", "2584", "2583", "2588", "2527", "25110", "25114", "2699", "26100", "26102", "2628", "26111",
                "26115", "27109", "27110", "27112", "27114", "28109", "28111", "28113", "28115", "30118", "3031",
                "3033", "3038", "3042", "31118", "31119", "3133", "3139", "31121", "3337", "3338", "3339", "3637",
                "3638", "3643", "36123", "3656", "3657", "3739", "3744", "37123", "3750", "3842", "3843", "39101",
                "39121", "3944", "42118", "42119", "42120", "4243", "4386", "43120", "4357", "4447", "44101", "4450",
                "4547", "454", "45122", "45123", "4550", "4658", "46122", "46123", "4656", "4790", "4751", "47122",
                "4750", "4754", "50123", "5182", "5190", "5169", "5154", "5292", "5275", "5296", "5258", "5255", "5482",
                "54122", "5592", "5558", "55122", "5658", "56123", "5657", "5786", "5758", "5896", "58122", "6093",
                "60125", "60127", "6065", "6567", "6593", "6790", "6793", "6982", "6990", "6993", "69127", "6970",
                "7081", "7082", "7089", "70127", "7381", "7391", "7392", "7375", "73128", "7592", "7596", "7597",
                "75128", "7897", "78128", "78129", "7879", "7980", "7997", "8096", "8097", "8182", "8189", "8191",
                "8192", "8292", "82122", "8385", "8384", "83114", "8486", "8485", "8488", "8688", "86120", "88110",
                "88119", "88120", "8991", "89125", "89126", "89127", "9093", "91126", "91128", "91129", "92122",
                "93127", "9697", "97128", "9899", "98100", "99100", "99101", "99102", "100115", "101102", "101121",
                "102111", "102119", "102121", "109110", "109111", "109112", "109113", "110111", "110119", "111119",
                "112113", "112114", "112116", "112124", "113115", "113117", "113124", "114116", "115117", "116124",
                "117124", "118119", "119120", "119121", "125126", "125127", "126129", "128129"
            });

    public final ColorParameter color = new ColorParameter("Color", LXColor.RED);

    public TEEdgeTestPattern(LX lx) {
        super(lx);

        addParameter(this.target.getLabel(), target);
        addParameter(this.color.getLabel(), color);
    }

    @Override
    protected void run(double deltaMs) {
        clearPixels();
        String id = this.target.getObject();

        for (TEEdgeModel model : this.modelTE.getAllEdges()) {
            if (model.getShortId().equalsIgnoreCase(id)) {
                for (LXPoint p : model.getPoints()) {
                    colors[p.index] = this.color.getColor();
                }
                break;
            }
        }
    }
}
