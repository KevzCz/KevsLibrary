package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

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

        FireTornadoHandler.getInstance().tryTrigger(attacker, target, world, damage);
        FrostNovaHandler.getInstance().tryTrigger(attacker, target, world, damage);
        ChainLightningHandler.getInstance().tryTrigger(attacker, target, world, damage);
        SoulLinkHandler.getInstance().tryTrigger(attacker, target, world, damage);
    }
}