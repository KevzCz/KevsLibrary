package net.pixeldreamstudios.kevslibrary.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

import java.util.HashSet;
import java.util.Set;

public class AttributeInheritanceUtil {

    public static Set<RegistryEntry<EntityAttribute>> getInheritableAttributes() {
        Set<RegistryEntry<EntityAttribute>> set = new HashSet<>();

        set.add(EntityAttributes.GENERIC_MAX_HEALTH);
        set.add(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        set.add(KevsLibrary.PET_DAMAGE_BONUS);
        set.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        set.add(EntityAttributes.GENERIC_ARMOR);
        set.add(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        // Try to get spell power attributes (if they exist)
        addIfPresent(set, "fire");
        addIfPresent(set, "frost");
        addIfPresent(set, "arcane");
        addIfPresent(set, "air");
        addIfPresent(set, "earth");
        addIfPresent(set, "water");
        addIfPresent(set, "lightning");
        addIfPresent(set, "soul");
        addIfPresent(set, "healing");
        addIfPresent(set, "critical_chance");
        addIfPresent(set, "critical_damage");
        addIfPresent(set, "haste");
        return set;
    }

    private static void addIfPresent(Set<RegistryEntry<EntityAttribute>> set, String type) {
        Registries.ATTRIBUTE.getEntry(Identifier.of("spell_power", type)).ifPresent(set::add);
    }

    public static NbtCompound apply(PlayerEntity owner, LivingEntity pet, NbtCompound previousData, double ratio) {
        boolean isReapplying = previousData != null && !previousData.isEmpty();
        NbtCompound baseAttrTag = isReapplying && previousData.contains("petBaseAttributes")
                ? previousData.getCompound("petBaseAttributes")
                : new NbtCompound();

        NbtCompound newInheritance = new NbtCompound();
        NbtCompound newBaseAttrTag = new NbtCompound();

        for (RegistryEntry<EntityAttribute> attribute : getInheritableAttributes()) {
            String key = attribute.getKey().map(id -> id.getValue().toString()).orElse(null);
            if (key == null) continue;

            EntityAttributeInstance ownerAttr = owner.getAttributeInstance(attribute);
            EntityAttributeInstance petAttr = pet.getAttributeInstance(attribute);
            if (ownerAttr == null || petAttr == null) continue;

            double baseValue;
            if (isReapplying && baseAttrTag.contains(key)) {
                baseValue = baseAttrTag.getDouble(key);
            } else {
                baseValue = petAttr.getBaseValue();
                newBaseAttrTag.putDouble(key, baseValue);
            }

            double bonus;

            if (key.equals("minecraft:generic.attack_damage")) {
                bonus = ownerAttr.getValue() * ratio;

                EntityAttributeInstance extraBonusAttr = owner.getAttributeInstance(KevsLibrary.PET_DAMAGE_BONUS);
                if (extraBonusAttr != null) {
                    bonus += extraBonusAttr.getValue(); // 100% inheritance
                }

            } else if (key.equals("kevslibrary:pet_damage_bonus")) {
                continue;
            } else {
                bonus = ownerAttr.getValue() * ratio;
            }


            if (attribute.value().equals(EntityAttributes.GENERIC_MOVEMENT_SPEED)) {
                bonus = Math.min(bonus, 0.2);
            }
            petAttr.setBaseValue(baseValue + bonus);
            newInheritance.putDouble(key, bonus);

            if (attribute.value().equals(EntityAttributes.GENERIC_MAX_HEALTH)) {
                pet.setHealth((float) pet.getAttributeValue(attribute));
            }
        }

        if (!newBaseAttrTag.isEmpty()) {
            newInheritance.put("petBaseAttributes", newBaseAttrTag);
        }
        newInheritance.putBoolean("petAttributesInherited", true);
        return newInheritance;
    }
}
