package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThornsHandler {
    private static final int COOLDOWN_TICKS = 5;
    private static final Map<UUID, Long> LAST_THORNS_TRIGGER = new HashMap<>();

    public static void tryReflectThorns(LivingEntity defender, LivingEntity attacker, float incomingDamage) {
        if (attacker == null || !attacker.isAlive()) return;
        if (!(defender.getWorld() instanceof ServerWorld world)) return;

        UUID attackerId = attacker.getUuid();
        long currentTick = world.getTime();

        if (currentTick - LAST_THORNS_TRIGGER.getOrDefault(attackerId, 0L) < COOLDOWN_TICKS) return;

        EntityAttributeInstance thornsChanceAttr = defender.getAttributeInstance(KevsLibrary.THORNS_CHANCE);
        double thornsChance = thornsChanceAttr != null ? thornsChanceAttr.getValue() : 0.0;

        if (defender.getRandom().nextDouble() >= thornsChance) return;

        float armor = defender.getArmor();
        float armorScale = Math.min(armor / 30.0f, 0.8f);

        EntityAttributeInstance ampAttr = defender.getAttributeInstance(KevsLibrary.THORNS_AMP);
        double amp = ampAttr != null ? ampAttr.getValue() : 0.0;

        float totalMultiplier = armorScale + (float) amp;
        float reflectedDamage = incomingDamage * totalMultiplier;

        boolean isTrueDamage = false;
        EntityAttributeInstance trueDmgAttr = defender.getAttributeInstance(KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE);
        if (trueDmgAttr != null) {
            double chance = trueDmgAttr.getValue();
            isTrueDamage = defender.getRandom().nextDouble() < chance;
        }

        if (reflectedDamage <= 0f) return;

        if (isTrueDamage) {
            attacker.damage(attacker.getDamageSources().generic(), reflectedDamage);
        } else {
            attacker.damage(defender.getDamageSources().thorns(defender), reflectedDamage);
        }
//        System.out.println("[THORNS] Pre-damage: " + incomingDamage + ", armor: " + armor + ", amp: " + amp + ", total: " + reflectedDamage);

        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        world.spawnParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.ENCHANT_THORNS_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        LAST_THORNS_TRIGGER.put(attackerId, currentTick);
    }
}
