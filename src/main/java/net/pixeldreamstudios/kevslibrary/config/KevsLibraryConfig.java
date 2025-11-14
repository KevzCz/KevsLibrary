package net.pixeldreamstudios.kevslibrary.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KevsLibraryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kevslibrary.json");

    private static KevsLibraryConfig INSTANCE;

    public Map<String, Boolean> attributes = new HashMap<>();

    public Map<String, Boolean> systems = new HashMap<>();

    private static final Map<String, List<String>> SYSTEM_ATTRIBUTES = new HashMap<>();

    static {
        SYSTEM_ATTRIBUTES.put("multistrike", Arrays.asList(
                "multistrike_chance",
                "multistrike_count",
                "multistrike_damage"
        ));

        SYSTEM_ATTRIBUTES.put("chain_lightning", Arrays.asList(
                "chain_lightning_chance",
                "chain_lightning_count",
                "chain_lightning_overload_chance"
        ));

        SYSTEM_ATTRIBUTES.put("fire_tornado", Arrays.asList(
                "fire_tornado_chance",
                "fire_tornado_overload_chance"
        ));

        SYSTEM_ATTRIBUTES.put("frost_nova", Arrays.asList(
                "frost_nova_chance",
                "frost_nova_count",
                "frost_nova_overload_chance"
        ));

        SYSTEM_ATTRIBUTES.put("soul_link", Arrays.asList(
                "soul_link_chance",
                "soul_link_damage"
        ));

        SYSTEM_ATTRIBUTES.put("arcane_rupture", Arrays.asList(
                "arcane_rupture_chance",
                "arcane_rupture_damage",
                "arcane_rupture_overload_chance"
        ));

        SYSTEM_ATTRIBUTES.put("thorns", Arrays.asList(
                "thorns_chance",
                "thorns_amp",
                "thorns_true_damage_chance"
        ));

        SYSTEM_ATTRIBUTES.put("cleave", Arrays.asList(
                "cleave_chance",
                "cleave_damage_multiplier",
                "cleave_range"
        ));

        SYSTEM_ATTRIBUTES.put("piercing", Arrays.asList(
                "pierce_chance"
        ));

        SYSTEM_ATTRIBUTES.put("barrage", Arrays.asList(
                "barrage_chance"
        ));

        SYSTEM_ATTRIBUTES.put("projectile_storm", Arrays.asList(
                "projectile_storm_chance",
                "projectile_storm_range",
                "projectile_storm_duration"
        ));

        SYSTEM_ATTRIBUTES.put("pet_inheritance", Arrays.asList(
                "pet_inheritance_ratio",
                "pet_damage_bonus"
        ));

        SYSTEM_ATTRIBUTES.put("crit", Arrays.asList(
                "crit_chance",
                "crit_damage"
        ));

        SYSTEM_ATTRIBUTES.put("damage", Arrays.asList(
                "damage"
        ));

        SYSTEM_ATTRIBUTES.put("trident", Arrays.asList(
                "trident_damage_multiplier"
        ));

        SYSTEM_ATTRIBUTES.put("armor_penetration", Arrays.asList(
                "armor_penetration",
                "armor_penetration_flat"
        ));

        SYSTEM_ATTRIBUTES.put("hunger", Arrays.asList(
                "hunger_consumption"
        ));
    }

    public KevsLibraryConfig() {
        attributes.put("crit_chance", true);
        attributes.put("crit_damage", true);
        attributes.put("multistrike_chance", false);
        attributes.put("multistrike_count", true);
        attributes.put("multistrike_damage", true);
        attributes.put("damage", true);
        attributes.put("chain_lightning_chance", true);
        attributes.put("chain_lightning_count", true);
        attributes.put("chain_lightning_overload_chance", true);
        attributes.put("fire_tornado_chance", true);
        attributes.put("fire_tornado_overload_chance", true);
        attributes.put("frost_nova_chance", true);
        attributes.put("frost_nova_count", true);
        attributes.put("frost_nova_overload_chance", true);
        attributes.put("pet_inheritance_ratio", true);
        attributes.put("pet_damage_bonus", true);
        attributes.put("soul_link_chance", true);
        attributes.put("soul_link_damage", true);
        attributes.put("arcane_rupture_chance", true);
        attributes.put("arcane_rupture_damage", true);
        attributes.put("arcane_rupture_overload_chance", true);
        attributes.put("trident_damage_multiplier", true);
        attributes.put("armor_penetration", true);
        attributes.put("armor_penetration_flat", true);
        attributes.put("thorns_chance", true);
        attributes.put("thorns_amp", true);
        attributes.put("thorns_true_damage_chance", true);
        attributes.put("cleave_chance", true);
        attributes.put("cleave_damage_multiplier", true);
        attributes.put("cleave_range", true);
        attributes.put("pierce_chance", true);
        attributes.put("barrage_chance", true);
        attributes.put("projectile_storm_chance", true);
        attributes.put("projectile_storm_range", true);
        attributes.put("projectile_storm_duration", true);
        attributes.put("hunger_consumption", true);

        systems.put("multistrike", true);
        systems.put("chain_lightning", true);
        systems.put("fire_tornado", true);
        systems.put("frost_nova", true);
        systems.put("soul_link", true);
        systems.put("arcane_rupture", true);
        systems.put("thorns", true);
        systems.put("cleave", true);
        systems.put("piercing", true);
        systems.put("barrage", true);
        systems.put("projectile_storm", true);
        systems.put("pet_inheritance", true);
        systems.put("crit", true);
        systems.put("damage", true);
        systems.put("trident", true);
        systems.put("armor_penetration", true);
        systems.put("hunger", true);
    }

    public static KevsLibraryConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
    }

    private static KevsLibraryConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                KevsLibraryConfig config = GSON.fromJson(json, KevsLibraryConfig.class);

                KevsLibraryConfig defaults = new KevsLibraryConfig();
                boolean needsSave = false;

                for (Map.Entry<String, Boolean> entry : defaults.attributes.entrySet()) {
                    if (!config.attributes.containsKey(entry.getKey())) {
                        config.attributes.put(entry.getKey(), entry.getValue());
                        needsSave = true;
                    }
                }

                for (Map.Entry<String, Boolean> entry : defaults.systems.entrySet()) {
                    if (!config.systems.containsKey(entry.getKey())) {
                        config.systems.put(entry.getKey(), entry.getValue());
                        needsSave = true;
                    }
                }

                if (needsSave) {
                    config.save();
                }

                return config;
            } catch (IOException e) {
                return new KevsLibraryConfig();
            }
        } else {
            KevsLibraryConfig config = new KevsLibraryConfig();
            config.save();
            return config;
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAttributeEnabled(String attributeName) {

        if (!attributes.getOrDefault(attributeName, false)) {
            return false;
        }


        for (Map.Entry<String, List<String>> entry : SYSTEM_ATTRIBUTES.entrySet()) {
            if (entry.getValue().contains(attributeName)) {
                String systemName = entry.getKey();
                if (!systems.getOrDefault(systemName, false)) {

                    return false;
                }
            }
        }

        return true;
    }


    public boolean isSystemEnabled(String systemName) {

        if (!systems.getOrDefault(systemName, false)) {
            return false;
        }

        List<String> requiredAttrs = SYSTEM_ATTRIBUTES.get(systemName);
        if (requiredAttrs != null) {
            for (String attrName : requiredAttrs) {

                if (!attributes.getOrDefault(attrName, false)) {

                    return false;
                }
            }
        }

        return true;
    }

    public boolean isCritEnabled() {
        return isSystemEnabled("crit");
    }

    public boolean isMultistrikeEnabled() {
        return isSystemEnabled("multistrike");
    }

    public boolean isChainLightningEnabled() {
        return isSystemEnabled("chain_lightning");
    }

    public boolean isFireTornadoEnabled() {
        return isSystemEnabled("fire_tornado");
    }

    public boolean isFrostNovaEnabled() {
        return isSystemEnabled("frost_nova");
    }

    public boolean isSoulLinkEnabled() {
        return isSystemEnabled("soul_link");
    }

    public boolean isArcaneRuptureEnabled() {
        return isSystemEnabled("arcane_rupture");
    }

    public boolean isThornsEnabled() {
        return isSystemEnabled("thorns");
    }

    public boolean isCleaveEnabled() {
        return isSystemEnabled("cleave");
    }

    public boolean isPiercingEnabled() {
        return isSystemEnabled("piercing");
    }

    public boolean isBarrageEnabled() {
        return isSystemEnabled("barrage");
    }

    public boolean isProjectileStormEnabled() {
        return isSystemEnabled("projectile_storm");
    }

    public boolean isPetInheritanceEnabled() {
        return isSystemEnabled("pet_inheritance");
    }

    public boolean isArmorPenetrationEnabled() {
        return isSystemEnabled("armor_penetration");
    }

    public boolean isDamageEnabled() {
        return isSystemEnabled("damage");
    }

    public boolean isTridentEnabled() {
        return isSystemEnabled("trident");
    }

    public boolean isHungerEnabled() {
        return isSystemEnabled("hunger");
    }
    public List<String> getSystemAttributes(String systemName) {
        return SYSTEM_ATTRIBUTES.get(systemName);
    }

}