# Adding Shader Effects

This guide explains how to introduce a new GPU shader effect into the Titanic's End LXStudio fork. Follow the stages below sequentially: prepare the assets, expose controls, register the effect, and validate the result.

## 1. Plan the Feature
- **Define the behavior:** Capture the visual intention (e.g., edge glow, hue rotation) and decide whether it is a post-process or a geometry shader.
- **Identify parameters:** List user-facing controls, their ranges, units, defaults, and short descriptions. Favor normalized ranges unless a physical unit (Hz, radians) improves usability.
- **Assess dependencies:** Confirm whether the shader needs tempo, modulators, or shared uniforms from the engine.

## 2. Create the Shader File
- Add a new fragment shader under `te-app/resources/shaders/`; use the naming convention `shader_<effect>.fs`.
- Top of file should specify `#define TE_EFFECTSHADER`. Only add `TE_NOPOSTPROCESSING` if the shader must bypass downstream post effects.
- Declare uniforms that mirror the Java parameters. Include a sampler uniform (`uniform sampler2D iDst;`) for the prior buffer.
- Operate in normalized UV space (`vec2 uv = (fragCoord + 0.5) * invRes;`) and clamp all samples to `[0, 1]` so activating the effect does not skew the source image.
- Keep math branch-free where possible. Use helper functions for conversions (RGB ↔ HSL, Sobel kernels) to keep `mainImage` concise.

## 3. Build the Java Effect Class
- Create the effect class in `te-app/src/main/java/titanicsend/effect/` extending `GLShaderEffect`.
- Instantiate parameters (`BoundedParameter`, `CompoundParameter`, `BooleanParameter`, etc.) with clear labels and `.setDescription(...)` text to surface in the UI tooltips.
- Register each parameter via `addParameter`. Use lowercase keys consistent with prior effects (e.g., `addParameter("mix", this.mix)`).
- Supply defaults immediately after parameter creation to ensure deterministic initial state.
- In the constructor, attach the shader: `addShader(GLShader.config(lx).withFilename("shader_<effect>.fs").withUniformSource(this::setUniforms));`
- Implement the `setUniforms` callback to push parameter values into the shader. Convert to ints/floats as needed and clamp or round before passing.

## 4. Register the Effect
- Import the new class inside `heronarts.lx.studio.TEApp`.
- Add `lx.registry.addEffect(NewEffect.class);` within the existing effect registration block so it appears in the UI.
- If the effect needs presets or automation hooks, wire those separately (out of scope for this doc).

## 5. Build & Verify
- Run `mvn package -DskipTests` from `te-app/` to ensure the project compiles and the shader is bundled.
- Launch the app (`java -ea -Dgpu -cp target/te-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar heronarts.lx.studio.TEApp`).
- Insert the effect on a global bus. Toggle the bypass control to confirm it is visually neutral when disabled.
- Exercise every parameter range. Check for:
	- Visual artifacts (NaNs, flicker, or resolution changes).
	- Performance regressions (monitor FPS in the HUD).
	- Correct tempo syncing or modulation if applicable.

## 6. Debugging Tips
- **Shader compilation issues:** Review console output; the GL engine prints GLSL errors with line numbers. Use simpler constant values to isolate failing expressions.
- **Wrong coordinate space:** If enabling the effect shifts or scales the image, ensure UV-based sampling and clamp coordinates to `[0,1]`.
- **Uniform mismatch:** Confirm the Java parameter names match the shader uniforms exactly. When using enums or toggles, convert to `int` before passing.
- **Temporal flicker:** Cache intermediate calculations or add smoothing (e.g., blur softness parameters) instead of sampling with wildly varying offsets.

## 7. Documentation & QA
- Update user-facing release notes or internal docs describing the effect’s purpose and key controls.
- Capture screenshots or short videos for visual reference in future regressions.
- Before merging, run through a quick regression playlist to make sure other shader effects still behave as expected.

