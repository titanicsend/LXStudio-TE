package titanicsend.ui.text3d;

// Renderer types for BGFX, taken from most recent BGFX source.
public enum BGFXRendererType {
  Noop,         // 0 - No rendering.
  Agc,          // AGC
  Direct3D11,   // Direct3D 11.0
  Direct3D12,   // Direct3D 12.0
  Gnm,          // GNM
  Metal,        // Metal
  Nvn,          // NVN
  OpenGLES,     // OpenGL ES 2.0+
  OpenGL,       // OpenGL 2.1+
  Vulkan,       // Vulkan
  Count         // Number of renderers
}
