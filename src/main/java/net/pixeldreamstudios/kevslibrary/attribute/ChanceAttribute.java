package net.pixeldreamstudios.kevslibrary.attribute;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;

public class ChanceAttribute {
    private final RegistryEntry<EntityAttribute> chanceAttribute;
    private final RegistryEntry<EntityAttribute> overloadChanceAttribute;

    public ChanceAttribute(RegistryEntry<EntityAttribute> chanceAttribute,
                           RegistryEntry<EntityAttribute> overloadChanceAttribute) {
        this.chanceAttribute = chanceAttribute;
        this.overloadChanceAttribute = overloadChanceAttribute;
    }

    public boolean rollChance(LivingEntity entity) {
        AttributeContext context = new AttributeContext(entity);
        double chance = context.getAttributeAsPercentage(chanceAttribute);
        return entity.getRandom().nextDouble() < chance;
    }

    public boolean rollOverloadChance(LivingEntity entity) {
        if (overloadChanceAttribute == null) return false;
        AttributeContext context = new AttributeContext(entity);
        double overloadChance = context.getAttributeAsPercentage(overloadChanceAttribute);
        return entity.getRandom().nextDouble() < overloadChance;
    }

    public RegistryEntry<EntityAttribute> getChanceAttribute() {
        return chanceAttribute;
    }

    public RegistryEntry<EntityAttribute> getOverloadChanceAttribute() {
        return overloadChanceAttribute;
    }
}