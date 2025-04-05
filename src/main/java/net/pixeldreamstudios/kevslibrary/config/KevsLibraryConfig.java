package net.pixeldreamstudios.kevslibrary.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class KevsLibraryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/kevs_library_config.json");
    public boolean useBookBackground = true; // true = use book.png, false = NAME_BG + VALUE_BG

    public int xOffset = -61;
    public int yOffset = 10;

    public static KevsLibraryConfig INSTANCE = new KevsLibraryConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, KevsLibraryConfig.class);
                return;
            } catch (Exception e) {
                System.err.println("[KevsLibrary] Failed to read config: " + e.getMessage());
            }
        }

        // If not found or failed, save defaults
        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            System.err.println("[KevsLibrary] Failed to write config: " + e.getMessage());
        }
    }
    public static void apply() {
        // If the panel is already rendered, update offsets directly.
        // We'll hook into this from wherever the buttonX/Y gets used.
        System.out.println("[KevsLibrary] Config applied: xOffset=" + INSTANCE.xOffset + ", yOffset=" + INSTANCE.yOffset);
    }
}
