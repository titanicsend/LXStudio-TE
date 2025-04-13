package titanicsend.pattern.glengine;

import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;

public class ShaderConfiguration {
  public ShaderConfigOpcode opcode;
  public TEControlTag parameterId;
  public LXParameter lxParameter;
  public BoundedParameter.NormalizationCurve normalizationCurve;
  public double value;
  public double v1;
  public double v2;
  public int textureChannel;
  public String name;
}
