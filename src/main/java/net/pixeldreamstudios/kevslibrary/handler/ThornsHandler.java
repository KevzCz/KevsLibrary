package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeScaling;
import net.pixeldreamstudios.kevslibrary.attribute.EffectHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThornsHandler extends EffectHandler {

    private static final ThornsHandler INSTANCE = new ThornsHandler();

    private static final int COOLDOWN_TICKS = 5;
    private final Map<UUID, Long> lastThornsTrigger = new HashMap<>();

    private ThornsHandler() {
        super(
                KevsLibrary.THORNS_CHANCE,
                null,
                AttributeScaling.builder()
                        .addScaling(KevsLibrary.THORNS_AMP, 0.01)
                        .baseRatio(1.0)
                        .build()
        );
    }

    public static ThornsHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected void execute(EffectContext context) {
    }

    public static void tryReflectThorns(LivingEntity defender, LivingEntity attacker, float incomingDamage) {
        INSTANCE.reflect(defender, attacker, incomingDamage);
    }

    private void reflect(LivingEntity defender, LivingEntity attacker, float incomingDamage) {
        if (attacker == null || !attacker.isAlive()) return;
        if (!(defender.getWorld() instanceof ServerWorld world)) return;

        UUID attackerId = attacker.getUuid();
        long currentTick = world.getTime();

        if (currentTick - lastThornsTrigger.getOrDefault(attackerId, 0L) < COOLDOWN_TICKS) return;

        AttributeContext context = new AttributeContext(defender);
        double thornsChance = context.getAttributeAsPercentage(KevsLibrary.THORNS_CHANCE);

        if (defender.getRandom().nextDouble() >= thornsChance) return;

        float armor = defender.getArmor();
        float armorScale = Math.min(armor / 30.0f, 0.8f);

        double ampValue = context.getAttributeValue(KevsLibrary.THORNS_AMP);
        float amp = (float) (ampValue / 100.0);

        float totalMultiplier = armorScale + amp;
        float reflectedDamage = incomingDamage * totalMultiplier;

        boolean isTrueDamage = false;
        double trueDmgChance = context.getAttributeAsPercentage(KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE);
        isTrueDamage = defender.getRandom().nextDouble() < trueDmgChance;

        if (reflectedDamage <= 0f) return;

        if (isTrueDamage) {
            attacker.damage(attacker.getDamageSources().generic(), reflectedDamage);
        } else {
            attacker.damage(defender.getDamageSources().thorns(defender), reflectedDamage);
        }

        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        world.spawnParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.ENCHANT_THORNS_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        lastThornsTrigger.put(attackerId, currentTick);
    }
}