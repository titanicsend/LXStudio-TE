package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.pattern.LXPattern;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

// NOTE: Compiling this class may generate an unchecked cast warning or
// other annotation related warnings. These can be safely ignored.  This
// dynamic class generation lives by the principle that an object is an
// object is an object.
public class ShaderPatternClassFactory {

  public String getShaderClassName(File shaderFile, List<ShaderConfiguration> config) {
    // if not otherwise specified, we'll use the shader's name as the class name
    String className = shaderFile.getName().substring(0, shaderFile.getName().length() - 3);

    // see if a name was specified in the config options
    for (ShaderConfiguration c : config) {
      if (c.opcode == ShaderConfigOpcode.SET_CLASS_NAME) {
        className = c.name;
        break; // there can be only one.
      }
    }
    // prepend package name so the class loader will be happy
    return "titanicsend.pattern.glengine." + className;
  }

  public String getLXCategory(List<ShaderConfiguration> config) {
    // if not otherwise specified, default to "Auto Shader" as the LXCategory
    String category = "Auto Shader";

    // see if an LXCategory was specified in the config options
    for (ShaderConfiguration c : config) {
      if (c.opcode == ShaderConfigOpcode.SET_LX_CATEGORY) {
        category = c.name;
        break;
      }
    }
    return category;
  }

  public boolean isDriftPattern(List<ShaderConfiguration> config) {
    for (ShaderConfiguration c : config) {
      if (c.opcode == ShaderConfigOpcode.SET_TRANSLATE_MODE_DRIFT) {
        return true;
      }
    }
    return false;
  }

  public boolean isAutoShader(List<ShaderConfiguration> config) {
    for (ShaderConfiguration c : config) {
      if (c.opcode == ShaderConfigOpcode.AUTO || c.opcode == ShaderConfigOpcode.SET_CLASS_NAME)
        return true;
    }
    return false;
  }

  public boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Iterate through .fs files in the shaders directory and create a class for each shader that has
   * any configuration pragmas in its code. The new class will be named after the shader file by
   * default, and will extend TEAutoShaderPattern or TEAutoDriftPattern depending on configuration
   * options. See comments in the code for the make() method below for details.
   */
  @SuppressWarnings("unchecked")
  public void registerShaders(LX lx) {
    String dir = ShaderUtils.SHADER_PATH;
    GLPreprocessor glp = new GLPreprocessor();

    // get a list of all shaders in resource path
    File[] files = new File(dir).listFiles((dir1, name) -> name.endsWith(".fs"));
    if (files == null) {
      // TE.log("No shaders found in " + dir);
      return;
    }

    // iterate through shaders and try to create a class for each one
    for (File file : files) {
      String shaderFile = file.getName();

      // get shader configuration data from the shader file
      ArrayList<ShaderConfiguration> config = new ArrayList<>();
      try {
        glp.preprocessShader(file, config);
      } catch (Exception e) {
        TE.error("Error scanning shader " + file.getName() + "\n" + e.getMessage());
        continue;
      }

      // if the shader has no embedded configuration at all, we have to assume
      // that it's set up the "normal" way.  Skip it.
      if (isAutoShader(config) == false) {
        // TE.log("Shader " + shaderFile + " is not an auto shader.  Skipping.");
        continue;
      }

      // if class doesn't already exist, create and register it.
      String className = getShaderClassName(file, config);
      if (!classExists(className)) {
        // create the class
        TE.log("Creating Shader class: " + className + " for " + shaderFile);
        try {
          Class<?> clazz =
              new ShaderPatternClassFactory()
                  .make(className, getLXCategory(config), shaderFile, isDriftPattern(config));
          lx.registry.addPattern((Class<? extends LXPattern>) clazz);
          // TE.log("Registered shader class: " + className);
        } catch (Exception e) {
          TE.error(
              "Error. Shader class "
                  + className
                  + " could not be registered."
                  + "\n"
                  + e.getMessage());
        }
      }
    }
  }

  /*
   Here, we create a new shader pattern class, derived from either TEAutoShaderPattern or
   TEAutoDriftPattern.  How it works:

   We use ByteBuddy (www.bytebuddy.net) to dynamically create and annotate a new class.
   The ByteBuddy code below is roughly equivalent to the following Java code:

    public class NewShaderPattern extends TEAutoShaderPattern {
        @LXCategory("My Category")
        public String getShaderFile() {
            return "my_shader.fs";
        }
    }

   The new class is named after the shader file, or given the name specified in the shader code, and
   it is placed in the titanicsend.pattern.glengine package. (This makes Chromatik happy, because
   at project load time, it uses a class loader that wants to find the class in a package for
   which it has a valid .jar file.)

   The "Auto" pattern classes have a method called getShaderFile() that returns the name of the
   shader file to be used.  We override that method to return the name of the shader file we want
   to use in the new class.

   ByteBuddy's ElementMatcher is used to find the correct method to override and we use its
   FixedValue class to implement the new method that returns the shader file name
   string to load at pattern initialization time.

   ByteBuddy's AnnotationDescription is used to add the LXCategory annotation to the new class so
   we can specify where we want to live in the UI.

   Finally, the class is created and loaded into the JVM, ready to register with Chromatik.  Whew!
  */
  public Class<?> make(
      String className, String category, String shaderFile, boolean isDriftPattern) {
    AnnotationDescription lxcategory =
        AnnotationDescription.Builder.ofType(LXCategory.class).define("value", category).build();

    return new ByteBuddy()
        .with(
            new NamingStrategy.AbstractBase() {
              @Override
              protected String name(TypeDescription superClass) {
                return className;
              }
            })
        .subclass((isDriftPattern) ? TEAutoDriftPattern.class : TEAutoShaderPattern.class)
        .annotateType(lxcategory)
        .method(ElementMatchers.named("getShaderFile"))
        .intercept(FixedValue.value(shaderFile))
        .make()
        .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)
        .getLoaded();
  }
}
