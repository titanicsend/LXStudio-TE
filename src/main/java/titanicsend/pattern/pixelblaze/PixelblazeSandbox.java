package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;

@LXCategory("Test")
public class PixelblazeSandbox extends PixelblazePattern {

	public PixelblazeSandbox(LX lx) {
		super(lx);
	}

	@Override
	protected String getScriptName() {
		return "sandbox";
	}
}
