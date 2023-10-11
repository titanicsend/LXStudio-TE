package titanicsend.app.dev;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXSerializable;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

import static titanicsend.util.TE.log;

public class ArtNetInput extends LXComponent implements LXSerializable {

    public final BooleanParameter artNetReceive =
            new BooleanParameter("ArtNet Receiving", false)
                    .setMode(BooleanParameter.Mode.TOGGLE)
                    .setDescription("Art Net Receiving Mode");

    public ArtNetInput(LX lx) {
        super(lx, "artNetInput");
        log("new ArtNetInput()");
        addParameter("artNetReceive", this.artNetReceive);
    }

//    private final LXParameterListener parameterListener = (p) -> {
//        log("parameterListener: " + p);
//        onParameterChanged(p);
//    };

    @Override
    public void onParameterChanged(LXParameter p) {
        log("onParamChanged: " + p);
        if (p == this.artNetReceive) {
            log("about to set artNetReceiveActive to " + this.artNetReceive.getValue());
            this.lx.engine.dmx.artNetReceiveActive.setValue(this.artNetReceive.getValue());
        }
    }
}
