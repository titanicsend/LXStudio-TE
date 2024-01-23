package titanicsend.pattern.sina;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.bytedeco.leptonica.NUMA;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

@LXCategory("AAA")
public class TDPattern extends GLShaderPattern {
  private static final String MEMORY_MAPPED_FILE = "/Users/ssolaimanpour/workspace/Python-NDI/ndi_video_frame.bin";
  private static final int WIDTH = 640;
  private static final int HEIGHT = 480;
  private static final int CHANNELS = 4; // RGBA
  private static final long BUFFER_SIZE = WIDTH * HEIGHT * CHANNELS;

  private MappedByteBuffer mappedBuffer;
  FileChannel fileChannel;
  ByteBuffer buffer;
  GLShader shader;

  // simple demo of multipass rendering
  public TDPattern(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

//    // Open the memory-mapped file
//    try (fileChannel = (FileChannel.open(Paths.get(MEMORY_MAPPED_FILE),
//        StandardOpenOption.READ, StandardOpenOption.WRITE))) {
//      // Map the file into memory
//      mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);
//    } catch (IOException e) {
//      TE.err("Could not load the memory mapped file.");
//    }

    // add the second shader, which applies a simple edge detection filter to the
    // output of the first shader
    shader = new GLShader(lx, "memory_mapped_video.fs", this, buffer);
    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            if (mappedBuffer != null){
              buffer.clear();
              // Ensure that the mapped buffer is ready for reading
              mappedBuffer.rewind();

              // Transfer the data from the memory-mapped file to the main buffer
              buffer.clear();
              // Pixel values should be sorted as BGRA
              buffer.put(mappedBuffer);
              buffer.flip(); // Prepare the buffer for reading
            } else {
              // try to initialize the memory mapped file.
              try {
                fileChannel = FileChannel.open(Paths.get(MEMORY_MAPPED_FILE), StandardOpenOption.READ, StandardOpenOption.WRITE);

                // Map the file into memory
                mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);
              } catch (IOException e) {
                TE.err("Could not load the memory mapped file.");

                buffer.clear();
                for (int i = 0; i < BUFFER_SIZE; i += 4) {
                  buffer.put((byte) 0);    // Blue
                  buffer.put((byte) 0);    // Green
                  buffer.put((byte) 255);  // Red
                  buffer.put((byte) 255);  // Alpha
                }
                buffer.flip(); // Prepare the buffer for reading
              }
            }
          }
        });
  }

}