package titanicsend.ui.text3d;

import heronarts.lx.color.LXColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

// Information needed to render a single 3d text label
public class Label {
    public Vector3f position;
    public Vector3f rotation;
    public Matrix4f modelMatrix = new Matrix4f();
    public FloatBuffer modelMatrixBuf;
    int vertexCount;
    //int indexCount;
    public ByteBuffer vertexBuffer = null;
    public short vbh;
    String text;
    int color;

    public Label(String text, Vector3f pos, Vector3f rot, int color) {
        this.text = text;
        this.position = pos;
        this.rotation = rot;
        this.color = color;

        // we use all 6 vertices (two triangles) here to save setting up an index buffer
        this.vertexCount = this.text.length() * 6;

        this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
        this.modelMatrix.get(this.modelMatrixBuf);
        this.modelMatrix.identity().translate(position.x, position.y, position.z).setRotationXYZ(rotation.x, rotation.y, rotation.z);
    }

    public Label(String text, Vector3f pos, Vector3f rot) {
        this(text, pos, rot, 0xffff0000);
    }

    public void dispose() {
        MemoryUtil.memFree(this.vertexBuffer);
    }

}
