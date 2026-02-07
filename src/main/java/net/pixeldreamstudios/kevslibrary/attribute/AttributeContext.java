package net.pixeldreamstudios.kevslibrary.attribute;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.entry.RegistryEntry;

public class AttributeContext {
    private final LivingEntity entity;

    public AttributeContext(LivingEntity entity) {
        this.entity = entity;
    }

    public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        return instance != null ? instance.getValue() : 100.0;
    }

    public double getAttributeAsPercentage(RegistryEntry<EntityAttribute> attribute) {
        double value = getAttributeValue(attribute);
        return (value - 100.0) / 100.0;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}