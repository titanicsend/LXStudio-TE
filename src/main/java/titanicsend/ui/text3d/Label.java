package titanicsend.ui.text3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.bgfx.BGFX.bgfx_destroy_vertex_buffer;

// Information needed to render a single 3d text label
public class Label {
  public Vector3f position;
  public Vector3f rotation;
  public Matrix4f modelMatrix = new Matrix4f();
  public FloatBuffer modelMatrixBuf;
  int vertexCount;
  public ByteBuffer vertexBuffer = null;
  public short vbh;
  String text;
  int color;
  int background;

  public Label(String text, Vector3f pos, Vector3f rot, int color, int background) {
    this.text = text;
    this.position = pos;
    this.rotation = rot;
    this.color = color;
    this.background = background;

    // we use all 6 vertices (two triangles) here to save setting up an index buffer
    this.vertexCount = this.text.length() * 6;

    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);
    this.modelMatrix
        .identity()
        .translate(position.x, position.y, position.z)
        .setRotationXYZ(rotation.x, rotation.y, rotation.z);
  }

  public Label(String text, Vector3f pos, Vector3f rot) {
    this(text, pos, rot, 0xffffffff, 0);
  }

  public void dispose() {
    bgfx_destroy_vertex_buffer(vbh);

    MemoryUtil.memFree(this.modelMatrixBuf);
    MemoryUtil.memFree(this.vertexBuffer);
  }
}
