package net.pixeldreamstudios.kevslibrary.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.config.PetInheritanceConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AttributeInheritanceUtil {

    public static NbtCompound apply(PlayerEntity owner, LivingEntity pet, NbtCompound previousData, double globalRatio) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        PetInheritanceConfig petConfig = config.pet_inheritance;

        if (!petConfig.enabled || !petConfig.ratio_attribute.enabled) {
            return new NbtCompound();
        }

        boolean isReapplying = previousData != null && !previousData.isEmpty();
        NbtCompound baseAttrTag = isReapplying && previousData.contains("petBaseAttributes")
                ? previousData.getCompound("petBaseAttributes")
                : new NbtCompound();

        NbtCompound newInheritance = new NbtCompound();
        NbtCompound newBaseAttrTag = new NbtCompound();

        for (Map.Entry<String, PetInheritanceConfig.AttributeInheritanceSettings> entry :
                petConfig.ratio_attribute.inheritable_attributes.entrySet()) {

            String attributeId = entry.getKey();
            PetInheritanceConfig.AttributeInheritanceSettings settings = entry.getValue();

            if (!settings.enabled) continue;

            Optional<RegistryEntry.Reference<EntityAttribute>> attrOpt =
                    Registries.ATTRIBUTE.getEntry(Identifier.tryParse(attributeId));

            if (attrOpt.isEmpty()) continue;

            RegistryEntry<EntityAttribute> attribute = attrOpt.get();
            EntityAttributeInstance ownerAttr = owner.getAttributeInstance(attribute);
            EntityAttributeInstance petAttr = pet.getAttributeInstance(attribute);

            if (ownerAttr == null || petAttr == null) continue;

            double baseValue;
            if (isReapplying && baseAttrTag.contains(attributeId)) {
                baseValue = baseAttrTag.getDouble(attributeId);
            } else {
                baseValue = petAttr.getBaseValue();
                newBaseAttrTag.putDouble(attributeId, baseValue);
            }

            double ownerValue = ownerAttr.getValue();
            double actualOwnerValue = settings.convertFromBase(ownerValue);
            double bonus = actualOwnerValue * globalRatio * settings.ratio;

            if (attributeId.equals("minecraft:generic.attack_damage") &&
                    petConfig.damage_bonus_attribute.enabled) {

                Map<String, Double> damageBonus = calculateDamageBonuses(owner, petConfig);
                bonus += damageBonus.getOrDefault(attributeId, 0.0);
            }

            bonus = settings.clamp(bonus);

            petAttr.setBaseValue(baseValue + bonus);
            newInheritance.putDouble(attributeId, bonus);

            if (attributeId.equals("minecraft:generic.max_health")) {
                pet.setHealth((float) pet.getAttributeValue(attribute));
            }
        }

        if (!newBaseAttrTag.isEmpty()) {
            newInheritance.put("petBaseAttributes", newBaseAttrTag);
        }
        newInheritance.putBoolean("petAttributesInherited", true);
        return newInheritance;
    }

    private static Map<String, Double> calculateDamageBonuses(PlayerEntity owner, PetInheritanceConfig petConfig) {
        Map<String, Double> bonuses = new HashMap<>();

        if (!petConfig.damage_bonus_attribute.enabled) {
            return bonuses;
        }

        AttributeContext context = new AttributeContext(owner);
        double petDamageBonusValue = context.getAttributeValue(KevsLibrary.PET_DAMAGE_BONUS);

        for (Map.Entry<String, PetInheritanceConfig.AttributeInheritanceSettings> entry :
                petConfig.damage_bonus_attribute.affected_attributes.entrySet()) {

            String attributeId = entry.getKey();
            PetInheritanceConfig.AttributeInheritanceSettings settings = entry.getValue();

            if (!settings.enabled) continue;

            double actualBonusValue = settings.convertFromBase(petDamageBonusValue);
            double bonus = actualBonusValue * settings.ratio;
            bonus = settings.clamp(bonus);

            bonuses.put(attributeId, bonus);
        }

        return bonuses;
    }
}