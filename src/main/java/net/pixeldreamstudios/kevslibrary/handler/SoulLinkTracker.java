package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;

import java.util.*;

public class SoulLinkTracker {

    private static final Map<UUID, LinkedGroupData> linkMap = new HashMap<>();

    public static void linkGroup(List<LivingEntity> group, LivingEntity attacker, float soulPower, SoulLinkHandler.LinkedGroup handler) {
        LinkedGroupData data = new LinkedGroupData(group, attacker, soulPower, handler);
        for (LivingEntity e : group) {
            linkMap.put(e.getUuid(), data);
        }
    }

    public static void clearLinks(List<LivingEntity> group) {
        for (LivingEntity e : group) {
            linkMap.remove(e.getUuid());
        }
    }

    public static Optional<LinkedGroupData> getGroup(LivingEntity entity) {
        return Optional.ofNullable(linkMap.get(entity.getUuid()));
    }

    public record LinkedGroupData(List<LivingEntity> group, LivingEntity attacker, float soulPower, SoulLinkHandler.LinkedGroup handler) { }
}
