package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.effect.LXEffect;
import titanicsend.model.TEWholeModel;

public abstract class TEEffect extends LXEffect {

    protected final TEWholeModel modelTE;

    public TEEffect(LX lx) {
        super(lx);

        this.modelTE = (TEWholeModel) lx.getModel();
    }
}
