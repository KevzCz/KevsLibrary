package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcaneRuptureTracker {
    private static final Map<UUID, Long> activeRuptures = new HashMap<>();
    private static final long RUPTURE_DURATION_TICKS = 60L;

    public static boolean hasBeenRuptured(LivingEntity entity) {
        long now = System.currentTimeMillis();
        return activeRuptures.getOrDefault(entity.getUuid(), 0L) > now;
    }

    public static void markRuptured(LivingEntity entity, long durationTicks) {
        long expiryTime = System.currentTimeMillis() + (durationTicks * 50);
        activeRuptures.put(entity.getUuid(), expiryTime);
    }

    public static void clearRupture(LivingEntity entity) {
        activeRuptures.remove(entity.getUuid());
    }

    public static void clearAll() {
        activeRuptures.clear();
    }
}
