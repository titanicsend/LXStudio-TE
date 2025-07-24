package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import com.jogamp.opengl.GL4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class GLNDIOutEffect extends GLShaderEffect {

  private boolean isInitialized = false;

  // Output frame size
  private final int width;
  private final int height;

  private DevolaySender ndiSender;
  private DevolayVideoFrame ndiFrame;
  private final ByteBuffer ndiBuffer;

  // NDI source configuration  
  private final String ndiSourceName;

  // Logging and performance tracking
  private long frameCount = 0;
  private long lastLogTime = 0;
  private long lastFrameTime = 0;
  private static final long LOG_INTERVAL_MS = 5000; // Log every 5 seconds
  private static final long FRAME_LOG_INTERVAL = 60; // Log frame details every 60 frames

  // GPU texture readback buffer
  private IntBuffer pixelBuffer;
  
  // Input texture from GPU mixer
  private int inputTextureHandle = -1;

  public GLNDIOutEffect(LX lx) {
    super(lx);

    // Generate channel-specific NDI source name  
    String channelName = "master"; // Default to master channel
    this.ndiSourceName = "te_ndi_out_gpu_" + channelName.toLowerCase().replaceAll("[^a-z0-9]", "_");

    // Get dimensions from GLEngine
    this.width = GLEngine.current.getWidth();
    this.height = GLEngine.current.getHeight();
    
    // Calculate buffer size: 4 bytes per pixel (BGRA format)
    int bufferSize = this.width * this.height * 4;
    this.ndiBuffer = ByteBuffer.allocateDirect(bufferSize);
    this.pixelBuffer = IntBuffer.allocate(this.width * this.height);
    
    // Enhanced initialization logging
    long currentTime = System.currentTimeMillis();
    this.lastLogTime = currentTime;
    this.lastFrameTime = currentTime;
    
    LX.log("GLNDIOutEffect: 🎮 GPU NDI EFFECT CREATED - Resolution " + this.width + "x" + this.height);
    LX.log("GLNDIOutEffect: 🎮 GPU-compatible NDI source: '" + this.ndiSourceName + "', Memory: " + 
           String.format("%.2f", bufferSize / 1024.0 / 1024.0) + " MB");
    LX.log("GLNDIOutEffect: ✅ COMPATIBLE with GPU mixer - will capture GPU textures directly");
  }

  private void initializeNDI() {
    try {
      // Create NDI sender with descriptive name
      this.ndiSender = new DevolaySender(this.ndiSourceName);
      
      // Create video frame with our buffer
      this.ndiFrame = new DevolayVideoFrame();
      this.ndiFrame.setResolution(this.width, this.height);
      this.ndiFrame.setFourCCType(DevolayFrameFourCCType.BGRX);
      this.ndiFrame.setData(this.ndiBuffer);
      this.ndiFrame.setFrameRate(60, 1);
      this.ndiFrame.setAspectRatio(1);
      
      this.isInitialized = true;
      LX.log("GLNDIOutEffect: ✅ GPU NDI SENDER INITIALIZED - Source Name: '" + this.ndiSourceName + "'");
      LX.log("GLNDIOutEffect: 📡 Publishing GPU-rendered content on NDI network as: '" + this.ndiSourceName + "'");
      LX.log("GLNDIOutEffect: 🎮 Frame format: " + this.width + "x" + this.height + " BGRX, 60fps");
      
    } catch (Exception e) {
      LX.error("GLNDIOutEffect: ❌ Failed to initialize GPU NDI: " + e.getMessage());
      e.printStackTrace();
      this.isInitialized = false;
    }
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    LX.log("GLNDIOutEffect: 🚀 GPU EFFECT ENABLED - Starting GPU-to-NDI video stream...");
    if (!this.isInitialized) {
      initializeNDI();
    }
    if (this.isInitialized) {
      LX.log("GLNDIOutEffect: 📡 NOW BROADCASTING GPU content to NDI source '" + this.ndiSourceName + "'");
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    LX.log("GLNDIOutEffect: 🛑 GPU EFFECT DISABLED - Stopping GPU NDI broadcast from '" + this.ndiSourceName + "'");
    
    if (this.ndiFrame != null) {
      this.ndiFrame.close();
      this.ndiFrame = null;
    }
    if (this.ndiSender != null) {
      this.ndiSender.close();
      this.ndiSender = null;
    }
    this.isInitialized = false;
    LX.log("GLNDIOutEffect: ✅ GPU NDI resources cleaned up, broadcast stopped");
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
      // Declare variables that will be used in logging
      int samplePixel = 0;
      int sampleColor = 0;
      int processedPixels = 0;
      int nonZeroPixels = 0;
      
      // GPU mode: Read texture data directly from GPU
      if (this.inputTextureHandle > 0) {
        readTextureData(this.inputTextureHandle);
        // ndiBuffer is already flipped by readTextureData
        processedPixels = this.width * this.height;
        // For GPU mode, we can't easily count non-zero pixels without reading the buffer
        nonZeroPixels = processedPixels; // Assume all pixels are active for GPU mode
      } else {
        // Fallback: Use CPU color array if no GPU texture available
        if (this.colors == null || this.colors.length == 0) {
          if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
            LX.log("GLNDIOutEffect: ⚠️  No GPU texture OR CPU data available - skipping frame");
            this.lastLogTime = currentTime;
          }
          return;
        }
        
        // Clear buffer and fill with CPU color data
        this.ndiBuffer.clear();
        
        // Sample a few pixels for validation
        samplePixel = this.colors.length / 2; // Middle pixel
        sampleColor = this.colors[samplePixel];
        
        // Convert colors array to BGRX format
        for (int i = 0; i < this.colors.length && i < (this.width * this.height); i++) {
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
          this.ndiBuffer.put((byte) blue);
          this.ndiBuffer.put((byte) green);
          this.ndiBuffer.put((byte) red);
          this.ndiBuffer.put((byte) 0); // X padding
          
          processedPixels++;
        }
        
        // Flip buffer for reading
        this.ndiBuffer.flip();
      }
      
      // Send the frame via NDI (common for both GPU and CPU paths)
      this.ndiSender.sendVideoFrame(this.ndiFrame);
      
      // Log frame transmission every 60 frames (approximately once per second at 60fps)
      if (this.frameCount % FRAME_LOG_INTERVAL == 0) {
        LX.log("GLNDIOutEffect: 📡 GPU FRAME SENT #" + this.frameCount + " to NDI source '" + this.ndiSourceName + "'");
      }
      
      // Periodic detailed logging
      if (this.frameCount % (FRAME_LOG_INTERVAL * 5) == 0 || currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
        double fps = 0.0;
        if (currentTime > this.lastFrameTime) {
          fps = (FRAME_LOG_INTERVAL * 5) / ((currentTime - this.lastFrameTime) / 1000.0);
        }
        
        double nonZeroPercent = (nonZeroPixels * 100.0) / processedPixels;
        
        LX.log("GLNDIOutEffect: 📊 GPU STREAMING STATS - Frame " + this.frameCount + 
               " @ " + String.format("%.1f", fps) + " FPS to '" + this.ndiSourceName + "'");
        LX.log("GLNDIOutEffect: 🎮 GPU Content: " + processedPixels + " pixels, " + 
               nonZeroPixels + " non-black (" + String.format("%.1f", nonZeroPercent) + "%)");
        LX.log("GLNDIOutEffect: 🎮 Sample pixel[" + samplePixel + "] = 0x" + 
               Integer.toHexString(sampleColor) + ", Buffer: " + 
               (this.ndiBuffer.limit() / 1024) + "KB");
        
        this.lastLogTime = currentTime;
        this.lastFrameTime = currentTime;
      }
      
    } catch (Exception e) {
      if (currentTime - this.lastLogTime > LOG_INTERVAL_MS) {
        LX.error("GLNDIOutEffect: ❌ GPU NDI Error sending frame " + this.frameCount + ": " + e.getMessage());
        e.printStackTrace();
        this.lastLogTime = currentTime;
      }
    }
  }

  // GPU Mixer Interface Methods (implemented from GLShaderEffect)
  
  @Override
  public void setInput(int inputTextureHandle) {
    this.inputTextureHandle = inputTextureHandle;
    LX.log("GLNDIOutEffect: 🎯 INPUT TEXTURE SET - Handle: " + inputTextureHandle);
  }
  
  @Override
  public void run() {
    // This is called by GLMixer with the input texture already set.
    // Capture the input texture for NDI transmission (no need to re-run the effect).
    if (!this.isInitialized || this.ndiSender == null) {
      return; // NDI not ready
    }
    
    if (this.inputTextureHandle > 0) {
      try {
        // Read the already-rendered texture from GPU and transmit via NDI
        readTextureData(this.inputTextureHandle);
        this.ndiSender.sendVideoFrameAsync(this.ndiFrame);
        this.frameCount++;
        
        // Periodic logging
        if (this.frameCount % FRAME_LOG_INTERVAL == 0) {
          LX.log("GLNDIOutEffect: 📡 GPU FRAME SENT #" + this.frameCount + " to NDI source '" + this.ndiSourceName + 
                 "' (input texture: " + this.inputTextureHandle + ")");
        }
        
      } catch (Exception e) {
        LX.log("GLNDIOutEffect: ❌ Error capturing GPU texture for NDI: " + e.getMessage());
      }
    }
  }
  
  @Override
  public int getRenderTexture() {
    // NDI effect is output-only, return input texture unchanged for pass-through
    return this.inputTextureHandle;
  }

  /**
   * Read texture data from GPU to CPU buffer for NDI transmission
   */
  private void readTextureData(int textureHandle) {
    if (textureHandle <= 0) {
      return;
    }
    
    try {
      GL4 gl4 = GLEngine.current.getCanvas().getGL().getGL4();
      
      // Bind the input texture
      gl4.glActiveTexture(GL4.GL_TEXTURE0);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, textureHandle);
      
      // Read texture pixels directly into our NDI buffer (BGRA format)
      this.ndiBuffer.clear();
      gl4.glGetTexImage(GL4.GL_TEXTURE_2D, 0, GL4.GL_BGRA, GL4.GL_UNSIGNED_BYTE, this.ndiBuffer);
      
      // Flip buffer for reading/sending
      this.ndiBuffer.flip();
      
      // Unbind texture
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, 0);
      
    } catch (Exception e) {
      LX.log("GLNDIOutEffect: ❌ Failed to read GPU texture " + textureHandle + ": " + e.getMessage());
    }
  }
} 