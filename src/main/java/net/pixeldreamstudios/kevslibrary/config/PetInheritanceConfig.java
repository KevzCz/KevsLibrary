package net.pixeldreamstudios.kevslibrary.config;

import java.util.HashMap;
import java.util.Map;

public class PetInheritanceConfig {
    public boolean enabled = true;
    public RatioAttributeConfig ratio_attribute = new RatioAttributeConfig();
    public DamageBonusAttributeConfig damage_bonus_attribute = new DamageBonusAttributeConfig();

    public static class RatioAttributeConfig {
        public boolean enabled = true;
        public Map<String, AttributeInheritanceSettings> inheritable_attributes = new HashMap<>();

        public RatioAttributeConfig() {
            inheritable_attributes.put("minecraft:generic.max_health", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("minecraft:generic.attack_damage", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("minecraft:generic.knockback_resistance", new AttributeInheritanceSettings(true, 0.0, 1.0, 1.0));
            inheritable_attributes.put("minecraft:generic.armor", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("minecraft:generic.movement_speed", new AttributeInheritanceSettings(true, 0.0, 0.2, 0.5));
            inheritable_attributes.put("spell_power:fire", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:frost", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:arcane", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:air", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:earth", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:water", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:lightning", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:soul", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
            inheritable_attributes.put("spell_power:healing", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
        }
    }

    public static class DamageBonusAttributeConfig {
        public boolean enabled = true;
        public Map<String, AttributeInheritanceSettings> affected_attributes = new HashMap<>();

        public DamageBonusAttributeConfig() {
            affected_attributes.put("minecraft:generic.attack_damage", new AttributeInheritanceSettings(true, 0.0, -1, 1.0));
        }
    }

    public static class AttributeInheritanceSettings {
        public boolean enabled;
        public double min;
        public double max;
        public double ratio;

        public AttributeInheritanceSettings() {
            this(true, 0.0, -1, 1.0);
        }

        public AttributeInheritanceSettings(boolean enabled, double min, double max, double ratio) {
            this.enabled = enabled;
            this.min = min;
            this.max = max;
            this.ratio = ratio;
        }

        public double clamp(double value) {
            if (value < min) return min;
            if (max >= 0 && value > max) return max;
            return value;
        }
    }
}