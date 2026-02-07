package net.pixeldreamstudios.kevslibrary.attribute;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

public interface TriggeredEffect {

    ChanceAttribute getChanceAttribute();

    AttributeScaling getScaling();

    void trigger(EffectContext context);

    void triggerOverload(EffectContext context);

    default boolean shouldTrigger(LivingEntity attacker) {
        return getChanceAttribute().rollChance(attacker);
    }

    default boolean shouldOverload(LivingEntity attacker) {
        return getChanceAttribute().rollOverloadChance(attacker);
    }

    default void execute(EffectContext context) {
        if (!shouldTrigger(context.getAttacker())) return;

        boolean overload = shouldOverload(context.getAttacker());
        context.setOverload(overload);

        if (overload) {
            triggerOverload(context);
        } else {
            trigger(context);
        }
    }

    class EffectContext {
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final ServerWorld world;
        private final float baseDamage;
        private boolean overload;

        public EffectContext(LivingEntity attacker, LivingEntity target, ServerWorld world, float baseDamage) {
            this.attacker = attacker;
            this.target = target;
            this.world = world;
            this.baseDamage = baseDamage;
            this.overload = false;
        }

        public LivingEntity getAttacker() { return attacker; }
        public LivingEntity getTarget() { return target; }
        public ServerWorld getWorld() { return world; }
        public float getBaseDamage() { return baseDamage; }
        public boolean isOverload() { return overload; }
        public void setOverload(boolean overload) { this.overload = overload; }

        public AttributeContext getAttackerContext() {
            return new AttributeContext(attacker);
        }

        public AttributeContext getTargetContext() {
            return new AttributeContext(target);
        }
    }
}