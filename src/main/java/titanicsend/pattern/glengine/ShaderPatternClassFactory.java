package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.pattern.LXPattern;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

// NOTE: Compiling this class may generate an unchecked cast warning.
// It can be safely ignored.  An object is an object is an object when
// you're creating new derived classes at runtime.
public class ShaderPatternClassFactory {

    public String getShaderClassName(File shaderFile, List<ShaderConfiguration> config) {
        // if not otherwise specified, we'll use the shader's name as the class name
        String className = shaderFile.getName().substring(0, shaderFile.getName().length() - 3);

        // see if a name was specified in the config options
        for (ShaderConfiguration c : config) {
            if (c.operation == ShaderConfigOperation.SET_CLASS_NAME) {
                className = c.name;
                break; // there can be only one.
            }
        }
        // prepend package name so the class loader will be happy
        return "titanicsend.pattern.glengine." + className;
    }

    public String getLXCategory(List<ShaderConfiguration> config) {
        // if not otherwise specified, we'll use "Auto Shader" as the category
        String category = "Auto Shader";

        // see if an LXCategory was specified in the config options
        for (ShaderConfiguration c : config) {
            if (c.operation == ShaderConfigOperation.SET_LX_CATEGORY) {
                category = c.name;
                break;
            }
        }
        return category;
    }

    public boolean isDriftPattern(List<ShaderConfiguration> config) {
        for (ShaderConfiguration c : config) {
            if (c.operation == ShaderConfigOperation.SET_TRANSLATE_MODE_DRIFT) {
                return true;
            }
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

    // function to iterate through .fs files in the shaders directory and create a class for each
    // one. The class will be named after the shader file, and will extend TEAutoShaderPattern.
    // The class will override the getShaderFile() method to return the shader file name.

    @SuppressWarnings("unchecked")
    public void registerShaders(LX lx) {
        String dir = ShaderUtils.SHADER_PATH;
        GLPreprocessor glp = new GLPreprocessor();

        // get a list of all shaders in resource path
        File[] files = new File(dir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".fs");
            }
        });
        if (files == null) {
            //TE.log("No shaders found in " + dir);
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
                TE.err("Error scanning shader " + file.getName() + "\n" + e.getMessage());
                continue;
            }

            // if the shader has no embedded configuration at all, we have to assume
            // that it's set up the "normal" way.  Skip it.
            if (config.isEmpty()) {
                //TE.log("Shader " + shaderFile + " has no configuration data.  Skipping.");
                continue;
            }

            // if class doesn't already exist, create and register it.
            String className = getShaderClassName(file, config);
            if (!classExists(className)) {
                // create the class
                TE.log("Creating Shader class: " + className + " for " + shaderFile);
                try {
                    Class<?> clazz = new ShaderPatternClassFactory().make(className,
                        getLXCategory(config), shaderFile, isDriftPattern(config));
                    lx.registry.addPattern((Class<? extends LXPattern>) clazz);
                    //TE.log("Registered shader class: " + className);
                } catch (Exception e) {
                    TE.err("Error. Shader class " + className + " could not be registered." +
                        "\n" + e.getMessage());
                }
            }
        }
    }

    // Create a new shader pattern class, derived from either TEAutoShaderPattern or
    // TEAutoDriftPattern.
    public Class<?> make(String className, String category, String shaderFile, boolean isDriftPattern) {
        AnnotationDescription lxcategory = AnnotationDescription.Builder.ofType(LXCategory.class)
            .define("value", category)
            .build();

        if (isDriftPattern) {
            return new ByteBuddy()
                .with(new NamingStrategy.AbstractBase() {
                    @Override
                    protected String name(TypeDescription superClass) {
                        return className;
                    }
                })
                .subclass(TEAutoDriftPattern.class)
                .annotateType(lxcategory)
                .method(ElementMatchers.named("getShaderFile"))
                .intercept(FixedValue.value(shaderFile))
                .make()
                .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)

                .getLoaded();
        } else {
            return new ByteBuddy()
                .with(new NamingStrategy.AbstractBase() {
                    @Override
                    protected String name(TypeDescription superClass) {
                        return className;
                    }
                })
                .subclass(TEAutoShaderPattern.class)
                .annotateType(lxcategory)
                .method(ElementMatchers.named("getShaderFile"))
                .intercept(FixedValue.value(shaderFile))
                .make()
                //.load(getClass().getClassLoader())
                .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        }
    }
}
