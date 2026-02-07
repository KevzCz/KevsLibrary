package net.pixeldreamstudios.kevslibrary.attribute;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;

public abstract class EffectHandler {
    protected final RegistryEntry<EntityAttribute> chanceAttribute;
    protected final RegistryEntry<EntityAttribute> overloadChanceAttribute;
    protected final AttributeScaling scaling;

    protected EffectHandler(RegistryEntry<EntityAttribute> chanceAttribute,
                            RegistryEntry<EntityAttribute> overloadChanceAttribute,
                            AttributeScaling scaling) {
        this.chanceAttribute = chanceAttribute;
        this.overloadChanceAttribute = overloadChanceAttribute;
        this.scaling = scaling;
    }

    public void tryTrigger(LivingEntity attacker, LivingEntity target, ServerWorld world, float baseDamage) {
        AttributeContext context = new AttributeContext(attacker);

        double chance = context.getAttributeAsPercentage(chanceAttribute);
        if (attacker.getRandom().nextDouble() > chance) return;

        boolean isOverload = false;
        if (overloadChanceAttribute != null) {
            double overloadChance = context.getAttributeAsPercentage(overloadChanceAttribute);
            isOverload = attacker.getRandom().nextDouble() < overloadChance;
        }

        EffectContext effectContext = new EffectContext(attacker, target, world, baseDamage, isOverload);

        if (isOverload) {
            executeOverload(effectContext);
        } else {
            execute(effectContext);
        }
    }

    protected abstract void execute(EffectContext context);

    protected void executeOverload(EffectContext context) {
        execute(context);
    }

    public static class EffectContext {
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final ServerWorld world;
        private final float baseDamage;
        private final boolean isOverload;

        public EffectContext(LivingEntity attacker, LivingEntity target, ServerWorld world, float baseDamage, boolean isOverload) {
            this.attacker = attacker;
            this.target = target;
            this.world = world;
            this.baseDamage = baseDamage;
            this.isOverload = isOverload;
        }

        public LivingEntity getAttacker() { return attacker; }
        public LivingEntity getTarget() { return target; }
        public ServerWorld getWorld() { return world; }
        public float getBaseDamage() { return baseDamage; }
        public boolean isOverload() { return isOverload; }

        public AttributeContext getAttackerContext() {
            return new AttributeContext(attacker);
        }

        public AttributeContext getTargetContext() {
            return new AttributeContext(target);
        }

        public float applyScaling(AttributeScaling scaling) {
            if (scaling == null) return baseDamage;
            AttributeContext context = getAttackerContext();
            return DamageScaling.applyBoth(context, scaling, baseDamage);
        }
    }
}