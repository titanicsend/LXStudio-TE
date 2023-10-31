package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.io.FilenameFilter;

public class ShaderPatternClassFactory {

    // function to iterate through .fs files in the shaders directory and create a class for each
    // one. The class will be named after the shader file, and will extend TEAutoShaderPattern.
    // The class will override the getShaderFile() method to return the shader file name.
    public static void registerShaders(LX lx) {
        String dir = GLPreprocessor.SHADER_PATH;

        // get a list of all shaders in resource path
        File[] files = new File(dir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".fs");
            }
        });

        // iterate through shaders and create a class for each one
        for (File file : files) {
            String shaderFile = file.getName();
            String className = shaderFile.substring(0, shaderFile.length() - 3);
            try {
                Class<?> clazz = new ShaderPatternClassFactory().make(className, shaderFile);
                lx.registry.addPattern((Class<? extends LXPattern>) clazz);
                System.out.println("Registered shader: " + className);
            } catch (Exception e) {
                System.out.println("Error registering shader: " + className);
                e.printStackTrace();
            }
        }
    }

    public Class<?> make(String className, String shaderFile)  {
        return new ByteBuddy()
            .with(new NamingStrategy.AbstractBase() {
                @Override
                protected String name(TypeDescription superClass) {
                    return className;
                }})
            .subclass(TEAutoShaderPattern.class)
            .method(ElementMatchers.named("getShaderFile"))
            .intercept(FixedValue.value(shaderFile))
            .make()
            .load(getClass().getClassLoader())
            .getLoaded();
    }
}
