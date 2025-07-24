package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.util.TE;

@LXCategory("Titanics End")
public class NDIOutRawEffect extends TEEffect {

  private boolean isInitialized = false;

  // output frame size
  private final int width;
  private final int height;

  private DevolaySender ndiSender;
  private DevolayVideoFrame ndiFrame;
  private final ByteBuffer buffer;

  // Logging and performance tracking
  private long frameCount = 0;
  private long lastLogTime = 0;
  private long lastFrameTime = 0;
  private static final long LOG_INTERVAL_MS = 5000; // Log every 5 seconds
  private static final long FRAME_LOG_INTERVAL = 300; // Log frame details every 300 frames
  
  // NDI source configuration
  private static final String NDI_SOURCE_NAME = "TE-Output";

  public NDIOutRawEffect(LX lx) {
    super(lx);

    // Get dimensions from GLEngine
    this.width = GLEngine.current.getWidth();
    this.height = GLEngine.current.getHeight();
    
    // Calculate buffer size: 4 bytes per pixel (BGRA format)
    int bufferSize = this.width * this.height * 4;
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
    
    // Enhanced initialization logging
    long currentTime = System.currentTimeMillis();
    this.lastLogTime = currentTime;
    this.lastFrameTime = currentTime;
    
    LX.log("NDIOutRawEffect: Created with resolution " + this.width + "x" + this.height + 
           ", buffer size: " + bufferSize + " bytes");
    LX.log("NDIOutRawEffect: Memory allocated: " + (bufferSize / 1024.0 / 1024.0) + " MB");
  }

  private void initializeNDI() {
    try {
      // Create NDI sender with descriptive name
      this.ndiSender = new DevolaySender(NDI_SOURCE_NAME);
      
      // Create video frame with our buffer
      this.ndiFrame = new DevolayVideoFrame();
      this.ndiFrame.setResolution(this.width, this.height);
      this.ndiFrame.setFourCCType(DevolayFrameFourCCType.BGRX);
      this.ndiFrame.setData(this.buffer);
      this.ndiFrame.setFrameRate(60, 1);
      this.ndiFrame.setAspectRatio(1);
      
      this.isInitialized = true;
      LX.log("NDIOutRawEffect: ✅ NDI SENDER INITIALIZED - Source Name: '" + NDI_SOURCE_NAME + "'");
      LX.log("NDIOutRawEffect: Publishing on NDI network as: '" + NDI_SOURCE_NAME + "'");
      LX.log("NDIOutRawEffect: Frame format: " + this.width + "x" + this.height + " BGRX, 60fps");
      
    } catch (Exception e) {
      LX.error("NDIOutRawEffect: Failed to initialize NDI: " + e.getMessage());
      e.printStackTrace();
      this.isInitialized = false;
    }
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    LX.log("NDIOutRawEffect: 🚀 EFFECT ENABLED - Starting NDI video stream...");
    if (!this.isInitialized) {
      initializeNDI();
    }
    if (this.isInitialized) {
      LX.log("NDIOutRawEffect: 📡 NOW BROADCASTING to NDI source '" + NDI_SOURCE_NAME + "'");
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    LX.log("NDIOutRawEffect: 🛑 EFFECT DISABLED - Stopping NDI broadcast from '" + NDI_SOURCE_NAME + "'");
    
    if (this.ndiFrame != null) {
      this.ndiFrame.close();
      this.ndiFrame = null;
    }
    if (this.ndiSender != null) {
      this.ndiSender.close();
      this.ndiSender = null;
    }
    this.isInitialized = false;
    LX.log("NDIOutRawEffect: ✅ NDI resources cleaned up, broadcast stopped");
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    if (!this.isInitialized) {
      // Try to initialize if we haven't yet
      initializeNDI();
      if (!this.isInitialized) {
        return; // Skip this frame if initialization failed
      }
    }

    long currentTime = System.currentTimeMillis();
    this.frameCount++;
    
    try {
      // Validate colors array
      if (this.colors == null) {
        if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
          LX.log("NDIOutRawEffect: Colors array is null, skipping frame " + this.frameCount);
          this.lastLogTime = currentTime;
        }
        return;
      }
      
      if (this.colors.length == 0) {
        if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
          LX.log("NDIOutRawEffect: Colors array is empty (length=0), skipping frame " + this.frameCount);
          this.lastLogTime = currentTime;
        }
        return;
      }
      
      int expectedPixels = this.width * this.height;
      if (this.colors.length != expectedPixels) {
        if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
          LX.log("NDIOutRawEffect: Color array size mismatch - expected: " + expectedPixels + 
                 ", actual: " + this.colors.length + " (frame " + this.frameCount + ")");
          this.lastLogTime = currentTime;
        }
        return;
      }

      // Clear buffer and fill with color data
      this.buffer.clear();
      
      // Sample a few pixels for validation
      int samplePixel = this.colors.length / 2; // Middle pixel
      int sampleColor = this.colors[samplePixel];
      
      // Convert colors array to BGRX format
      int processedPixels = 0;
      int nonZeroPixels = 0;
      
      for (int i = 0; i < this.colors.length; i++) {
        int color = this.colors[i];
        
        // Count non-black pixels for statistics
        if (color != 0) {
          nonZeroPixels++;
        }
        
        // Extract ARGB components
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        
        // Convert to BGRX format (Blue, Green, Red, X)
        this.buffer.put((byte) blue);
        this.buffer.put((byte) green);
        this.buffer.put((byte) red);
        this.buffer.put((byte) 0); // X padding
        
        processedPixels++;
      }
      
      // Flip buffer for reading
      this.buffer.flip();
      
      // Send the frame via NDI
      this.ndiSender.sendVideoFrame(this.ndiFrame);
      
      // Log frame transmission every 60 frames (approximately once per second at 60fps)
      if (this.frameCount % 60 == 0) {
        LX.log("NDIOutRawEffect: 📡 FRAME SENT #" + this.frameCount + " to NDI source '" + NDI_SOURCE_NAME + "'");
      }
      
      // Periodic detailed logging
      if (this.frameCount % FRAME_LOG_INTERVAL == 0 || currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
        double fps = 0.0;
        if (currentTime > this.lastFrameTime) {
          fps = FRAME_LOG_INTERVAL / ((currentTime - this.lastFrameTime) / 1000.0);
        }
        
        double nonZeroPercent = (nonZeroPixels * 100.0) / this.colors.length;
        
        LX.log("NDIOutRawEffect: 📊 STREAMING STATS - Frame " + this.frameCount + 
               " @ " + String.format("%.1f", fps) + " FPS to '" + NDI_SOURCE_NAME + "'");
        LX.log("NDIOutRawEffect: Content: " + processedPixels + " pixels, " + 
               nonZeroPixels + " non-black (" + String.format("%.1f", nonZeroPercent) + "%)");
        LX.log("NDIOutRawEffect: Sample pixel[" + samplePixel + "] = 0x" + 
               Integer.toHexString(sampleColor) + ", Buffer: " + 
               (this.buffer.limit() / 1024) + "KB");
        
        this.lastLogTime = currentTime;
        this.lastFrameTime = currentTime;
      }
      
    } catch (Exception e) {
      if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
        LX.error("NDIOutRawEffect: Error sending frame " + this.frameCount + ": " + e.getMessage());
        e.printStackTrace();
        this.lastLogTime = currentTime;
      }
    }
  }
}
