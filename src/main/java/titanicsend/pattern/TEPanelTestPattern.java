package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.model.TEPanelModel;

@LXCategory("Test")
public class TEPanelTestPattern extends TEPattern {

  public final ObjectParameter<String> target =
      (ObjectParameter<String>)
          new ObjectParameter<String>(
              "Panel",
              new String[] {
                "FA", "FB", "FSA", "FSB", "FSC", "FPA", "FPB", "FPC", "AA", "AB", "ASA", "ASB",
                "ASC", "APA", "APB", "APC", "SFA", "SFB", "SEA", "SFC", "SFD", "SEB", "SEC", "SED",
                "SDA", "SDB", "SDC", "SEE", "SCA", "SCB", "SCC", "SBA", "SBB", "SBC", "SBD", "SBE",
                "SAA", "SAB", "SAC", "SAD", "SUF", "SUA", "PAA", "PAB", "PBA", "PAC", "PAD", "PBB",
                "PBC", "PBD", "PCA", "PCB", "PCC", "PBE", "PDA", "PDB", "PDC", "PEA", "PEB", "PEC",
                "PED", "PEE", "PFA", "PFB", "PFC", "PFD", "PUF", "PUA"
              });

  public final ColorParameter color = new ColorParameter("Color", LXColor.RED);

  public TEPanelTestPattern(LX lx) {
    super(lx);

    addParameter(this.target.getLabel(), target);
    addParameter(this.color.getLabel(), color);
  }

  @Override
  protected void run(double deltaMs) {
    clearPixels();
    String id = this.target.getObject();

    for (TEPanelModel model : this.modelTE.getAllPanels()) {
      if (model.getId().equalsIgnoreCase(id)) {
        for (LXPoint p : model.getPoints()) {
          colors[p.index] = this.color.getColor();
        }
        break;
      }
    }
  }
}
