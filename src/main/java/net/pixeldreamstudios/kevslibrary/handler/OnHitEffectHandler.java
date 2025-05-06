package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

public class OnHitEffectHandler {
    private static final ThreadLocal<Boolean> isMultistrikeContext = ThreadLocal.withInitial(() -> false);

    public static void withMultistrikeContext(Runnable action) {
        isMultistrikeContext.set(true);
        try {
            action.run();
        } finally {
            isMultistrikeContext.remove();
        }
    }

    public static boolean isInMultistrikeContext() {
        return isMultistrikeContext.get();
    }

    public static void triggerAll(LivingEntity attacker, LivingEntity target, float damage) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        EntityAttributeInstance fireTornadoAttr = attacker.getAttributeInstance(KevsLibrary.FIRE_TORNADO_CHANCE);
        double fireTornadoChance = fireTornadoAttr != null ? fireTornadoAttr.getValue() : 0.0;
        if (attacker.getRandom().nextDouble() < fireTornadoChance) {
            FireTornadoHandler.spawnFireTornado(attacker, target);
        }

        EntityAttributeInstance frostNovaAttr = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_CHANCE);
        double frostNovaChance = frostNovaAttr != null ? frostNovaAttr.getValue() : 0.0;
        if (attacker.getRandom().nextDouble() < frostNovaChance) {
            FrostNovaHandler.triggerFrostNova(attacker);
        }


        EntityAttributeInstance lightningAttr = attacker.getAttributeInstance(KevsLibrary.CHAIN_LIGHTNING_CHANCE);
        double lightningChance = lightningAttr != null ? lightningAttr.getValue() : 0.0;
        if (attacker.getRandom().nextDouble() < lightningChance) {
            ChainLightningHandler.spawnChainLightning(attacker, target);
        }
        EntityAttributeInstance soulLinkAttr = attacker.getAttributeInstance(KevsLibrary.SOUL_LINK_CHANCE);
        double soulLinkChance = soulLinkAttr != null ? soulLinkAttr.getValue() : 0.0;

        if (soulLinkChance > 0.0 && attacker.getRandom().nextDouble() < soulLinkChance) {
            SoulLinkHandler.triggerSoulLink(attacker, target);
        }

    }
}
