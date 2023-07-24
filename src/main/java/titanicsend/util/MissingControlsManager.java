package titanicsend.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MissingControlsManager {
    public static final class MissingControls {
        public final String shader_name;
        public final Boolean uses_palette;
        public final List<TEControlTag> missing_control_tags;
        public final List<String> pattern_classes;

        // Gson will override fields using reflection, if defined in json
        public MissingControls() {
            shader_name = "";
            uses_palette = true;
            missing_control_tags = new ArrayList<>();
            pattern_classes = new ArrayList<>();
        }
    }

    private final Map<String, MissingControls> classToMissingControls = new HashMap<>();

    private static MissingControlsManager instance;

    public static synchronized MissingControlsManager get() {
        if (instance == null) {
            instance = new MissingControlsManager();
        }
        return instance;
    }

    private MissingControlsManager(){
        try {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(loadFile("resources/pattern/missingControls.json"));
            MissingControls[] missingControls = gson.fromJson(reader, MissingControls[].class);

            for (MissingControls mc : missingControls) {
                for (String patternClass : mc.pattern_classes) {
                    classToMissingControls.put(patternClass, mc);
                }
            }
        } catch (Exception e) {
            TE.err(e, "Error reading missingControls.json");
        }
    }

    public MissingControls findMissingControls(Class patternClass) {
        return classToMissingControls.get(extractUnqualifiedClassname(patternClass));
    }

    protected static String extractUnqualifiedClassname(Class clazz) {
        String packageAndClassname = clazz.getName();
        if (packageAndClassname.contains("$")) {
            String[] parts = packageAndClassname.split("\\$");
            return parts[parts.length - 1];
        } else {
            String[] parts = packageAndClassname.split("\\.");
            return parts[parts.length - 1];
        }
    }

    protected static BufferedReader loadFile(String filename) {
        try {
            File f = new File(filename);
            return new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException e) {
            throw new Error(filename + " not found below " + System.getProperty("user.dir"));
        }
    }
}
