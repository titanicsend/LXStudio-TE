/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 *
 * JKB note: To get fixture-like behavior in the immutable model,
 * this class mashes together pieces of LXFixture, LXProtocolFixture,
 * ModelBuffer, etc.
 */

package titanicsend.dmx.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import heronarts.lx.model.LXPoint;
import heronarts.lx.output.ArtNetDatagram;
import heronarts.lx.output.LXBufferOutput;
import heronarts.lx.output.LXBufferOutput.ByteOrder;
import heronarts.lx.output.LXOutput;
import heronarts.lx.output.OPCOutput;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.StringParameter;
import titanicsend.dmx.DmxBuffer;
import titanicsend.dmx.parameter.DmxParameter;
import titanicsend.model.TEModel;

abstract public class DmxModel extends TEModel implements LXParameterListener {

  static public int FIELD_NOT_FOUND = -1;

  /**
   *  Common configuration elements across beacons and DJ lights
   */
  static public class DmxCommonConfig {
    public String id;
    public double x;
    public double y;
    public double z;
    public double yaw;
    public double pitch;
    public double roll;
    public String host;
    public boolean sequenceEnabled;
    public float fps;
    public int universe;
    public int channel;
  }

  /**
   * Comparable to LXPoint.
   * Can be extended to add custom functionality.
   */
  public class FieldDefinition {

    /**
     * Number of DMX bytes used by this field.
     * Default 1, sometimes 2 such as for high-resolution tilt/pan parameters.
     */
    public int numBytes;

    public enum BlendMode {
      JUMP("Jump"),
      LERP("Lerp");

      public final String label;

      private BlendMode(String label) {
        this.label = label;
      }

      @Override
      public String toString() {
        return this.label;
      }
    }

    public BlendMode blendMode;

    private DmxParameter templateParameter;

    // Why are we using a full parameter as a template? Just to save dev time.
    // This could be changed to a definition later.
    public FieldDefinition(DmxParameter templateParameter) {
      this.blendMode = BlendMode.LERP;
      setTemplateParameter(templateParameter);
    }

    public FieldDefinition setTemplateParameter(DmxParameter templateParameter) {
      this.templateParameter = templateParameter;
      setNumBytes();
      return this;
    }

    private void setNumBytes() {
      if (this.templateParameter != null) {
        this.numBytes = this.templateParameter.getNumBytes();
      } else {
        this.numBytes = 0;
      }
    }

    /* temporary commenting
    public int getValueDmx(DmxBuffer buffer) {
      return buffer.get(this.index);
    }

    public void set(DmxBuffer buffer, int value) {
      buffer.set(this.index, value);
    }
    */

  }

  private final List<FieldDefinition> fields = new ArrayList<FieldDefinition>(); 

  public String id;

  /**
   * Index in DmxBuffer[] array
   */
  public int index;

  /**
   * Number of dmx fields.
   */
  public int size = 0;

  public int numBytes = 0;

  public DmxModel(String teModelType, DmxCommonConfig config, String ... tags) {
    super(teModelType, new ArrayList<LXPoint>(), tags);

    this.id = config.id;
    this.x.setValue(config.x);
    this.y.setValue(config.y);
    this.z.setValue(config.z);
    this.yaw.setValue(config.yaw);
    this.pitch.setValue(config.pitch);
    this.roll.setValue(config.roll);
    this.host.setValue(config.host);
    this.artNetSequenceEnabled.setValue(config.sequenceEnabled);
    this.fps.setValue(config.fps);
    this.artNetUniverse.setValue(config.universe);
    this.dmxChannel.setValue(config.channel);

    // It's a baby version of LXStructure classes.
    // If user changes an output parameter, let's notice and rebuild the output.
    // Allows runtime adjustments such as from a UI element
    addOutputParameter("byteOrder", this.byteOrder);
    addOutputParameter("host", this.host);
    addOutputParameter("port", this.port);
    addOutputParameter("fps", this.fps);
    addOutputParameter("dmxChannel", this.dmxChannel);
    addOutputParameter("artNetUniverse", this.artNetUniverse);
    addOutputParameter("artNetSequenceEnabled", this.artNetSequenceEnabled);
  }

  @Override
  public String getId() {
    return this.id;
  }

  protected void addField(FieldDefinition field) {
    this.fields.add(field);
    this.size++;
    this.numBytes += field.numBytes;
  }

  /**
   * Returns a default buffer for this fixture
   */
  public DmxBuffer createBuffer() {
    List<DmxParameter> params = new ArrayList<DmxParameter>();
    for (FieldDefinition field : this.fields) {
      params.add(createParameter(field));
    }
    return new DmxBuffer(params.toArray(new DmxParameter[0]));
  }

  private DmxParameter createParameter(FieldDefinition field) {
    return field.templateParameter.copy();
  }

  /**
   * Make sure all DMX values in the buffer are compatible for this fixture
   * and for each other.
   * Called on each DmxBuffer after pattern has run and after blending.
   * Child classes can override and do any cross-field safety checking.
   * Child classes should set parameters to the corrected values.
   * Range limits are enforced elsewhere and do not need to be enforced here.
   */
  public void validate(DmxBuffer buffer) { }

  /**
   * Child classes can override and return field indices
   * to allow patterns to find fields by string
   */
  public int getFieldIndex(String field) {
    return FIELD_NOT_FOUND;
  }

  /*
   * GEOMETRY
   * 
   * Normally these would live in the LXFixture
   * but for the immutable model we'll add them here.
   */

  private static final double POSITION_RANGE = 1000000;

  public final BoundedParameter x =
      new BoundedParameter("X", 0, -POSITION_RANGE, POSITION_RANGE)
      .setDescription("Base X position of the fixture in space");

  public final BoundedParameter y =
      new BoundedParameter("Y", 0, -POSITION_RANGE, POSITION_RANGE)
      .setDescription("Base Y position of the fixture in space");

  public final BoundedParameter z =
      new BoundedParameter("Z", 0, -POSITION_RANGE, POSITION_RANGE)
      .setDescription("Base Z position of the fixture in space");

  public final BoundedParameter yaw =
      new BoundedParameter("Yaw", 0, -360, 360)
      .setDescription("Rotation of the fixture about the vertical axis")
      .setUnits(LXParameter.Units.DEGREES);

  public final BoundedParameter pitch =
      new BoundedParameter("Pitch", 0, -360, 360)
      .setDescription("Rotation of the fixture about the horizontal plane")
      .setUnits(LXParameter.Units.DEGREES);

  public final BoundedParameter roll =
      new BoundedParameter("Roll", 0, -360, 360)
      .setDescription("Rotation of the fixture about its normal vector")
      .setUnits(LXParameter.Units.DEGREES);

  /*
   * PROTOCOL / OUTPUT
   * 
   * Normally these would live in the LXProtocolFixture,
   * but for the immutable model we'll add them here.
   */

  public static class DmxOutputDefinition {

    protected final static float FPS_UNSPECIFIED = 0f;

    public final InetAddress address;
    public final int port;
    public final int universe;
    public final int channel;
    public final boolean sequenceEnabled;
    public final float fps;
    public final int numFields;
    public final int numBytes;
    public ByteOrder byteOrder;

    public DmxOutputDefinition(InetAddress address, int port, int universe, int channel, boolean sequenceEnabled, float fps, int numFields, int numBytes, ByteOrder byteOrder) {
      this.address = address;
      this.port = port;
      this.universe = universe;
      this.channel = channel;
      this.sequenceEnabled = sequenceEnabled;
      this.fps = fps;
      this.numFields = numFields;
      this.numBytes = numBytes;
      this.byteOrder = byteOrder;
    }
  }

  public final EnumParameter<LXBufferOutput.ByteOrder> byteOrder =
      new EnumParameter<LXBufferOutput.ByteOrder>("Byte Order", LXBufferOutput.ByteOrder.RGB)
      .setDescription("Which byte ordering the output uses");

  public final StringParameter host =
      new StringParameter("Host", "127.0.0.1")
      .setDescription("Host/IP this fixture transmits to");

  public final CompoundParameter fps =
      new CompoundParameter("FPS", DmxOutputDefinition.FPS_UNSPECIFIED, 0, 300)
      .setDescription("FPS limiter, zero for no limit");

  public final BooleanParameter unknownHost =
      new BooleanParameter("Unknown Host", false);

  public final DiscreteParameter port =
      new DiscreteParameter("Port", OPCOutput.DEFAULT_PORT, 0, 65536)
      .setDescription("Port number this fixture transmits to");

  public final DiscreteParameter dmxChannel =
      new DiscreteParameter("DMX Channel", 0, 512)
      .setUnits(LXParameter.Units.INTEGER)
      .setDescription("Starting DMX data channel offset for ArtNet/SACN/Kinet");

  public final DiscreteParameter artNetUniverse =
      new DiscreteParameter("ArtNet Universe", 0, 0, ArtNetDatagram.MAX_UNIVERSE)
      .setUnits(LXParameter.Units.INTEGER)
      .setDescription("Which ArtNet universe is used");

  public final BooleanParameter artNetSequenceEnabled =
      new BooleanParameter("ArtNet Sequence", false)
      .setDescription("Whether ArtNet sequence numbers are used");

  public final BooleanParameter enabled =
      new BooleanParameter("Enabled", true)
      .setDescription("Whether output to this fixture is enabled");

  protected final Map<String, LXParameter> parameters = new LinkedHashMap<String, LXParameter>();
  private final Set<LXParameter> outputParameters = new HashSet<LXParameter>();
  public boolean outputChanged = true;

  // Abbreviated version, hacked out of LXComponent
  protected void addParameter(String path, LXParameter parameter) {
    if (this.parameters.containsValue(parameter)) {
      throw new IllegalStateException(
          "Cannot add parameter twice: " + path + " / " + parameter);
    }
    this.parameters.put(path, parameter);
    if (parameter instanceof LXListenableParameter) {
      ((LXListenableParameter) parameter).addListener(this);
    }
  }

  /**
   * Adds a parameter which impacts the outputs of the fixture. Whenever
   * one is changed, the outputs will be regenerated.
   */
  protected DmxModel addOutputParameter(String path, LXParameter parameter) {
    addParameter(path, parameter);
    this.outputParameters.add(parameter);
    return this;
  }

  public void onParameterChanged(LXParameter parameter) {
    if (this.outputParameters.contains(parameter)) {
      this.outputChanged = true;
    }
  }

  public DmxOutputDefinition getDmxOutputDefinition() {
    return new DmxOutputDefinition(
        resolveHostAddress(), 
        this.port.getValuei(), 
        this.artNetUniverse.getValuei(), 
        this.dmxChannel.getValuei(), 
        this.artNetSequenceEnabled.getValueb(),
        this.fps.getValuef(),
        this.size,
        this.numBytes,
        this.byteOrder.getEnum());
  }

  protected InetAddress resolveHostAddress() {
    try {
      InetAddress address = InetAddress.getByName(this.host.getString());
      this.unknownHost.setValue(false);
      return address;
    } catch (UnknownHostException uhx) {
      LXOutput.error("Unknown host for fixture datagram: " + uhx.getLocalizedMessage());
      this.unknownHost.setValue(true);
    }
    return null;
  }
}
