package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

import java.util.*;

public class PiercingHandler {
    private static final Map<UUID, Long> activePiercers = new HashMap<>();
    private static final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private static final long DURATION_MILLIS = 3000;
    private static final long COOLDOWN_MILLIS = 250;

    public static void tryActivatePiercing(LivingEntity attacker) {
        double chance = attacker.getAttributeValue(KevsLibrary.PIERCING_CHANCE);
        if (attacker.getRandom().nextDouble() < chance) {
            activePiercers.put(attacker.getUuid(), System.currentTimeMillis());
            if (attacker.getWorld() instanceof ServerWorld serverWorld) {
                Vec3d pos = attacker.getPos().add(0, attacker.getStandingEyeHeight(), 0);
                serverWorld.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0.1);
                serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    public static boolean isPiercingActive(LivingEntity attacker) {
        Long time = activePiercers.get(attacker.getUuid());
        if (time == null) return false;
        boolean expired = (System.currentTimeMillis() - time) > DURATION_MILLIS;
        if (expired) activePiercers.remove(attacker.getUuid());
        return !expired;
    }

    public static void consumePiercing(LivingEntity attacker) {
        activePiercers.remove(attacker.getUuid());
    }

    public static void applyLineDamage(LivingEntity attacker, float baseDamage, float range, float width) {
        if (!(attacker.getWorld() instanceof ServerWorld serverWorld)) return;

        UUID uuid = attacker.getUuid();
        long now = System.currentTimeMillis();


        if (cooldownUntil.containsKey(uuid) && now < cooldownUntil.get(uuid)) return;


        cooldownUntil.put(uuid, now + COOLDOWN_MILLIS);

        consumePiercing(attacker);

        Vec3d origin = attacker.getPos().add(0, attacker.getStandingEyeHeight(), 0);
        Vec3d forward = attacker.getRotationVec(1.0F).normalize();
        Vec3d end = origin.add(forward.multiply(range));

        for (LivingEntity target : serverWorld.getEntitiesByClass(LivingEntity.class,
                new Box(origin, end).expand(width), e -> e != attacker && e.isAlive())) {

            Vec3d toTarget = target.getPos().add(0, target.getHeight() / 2, 0).subtract(origin);
            double distance = forward.dotProduct(toTarget.normalize()) * toTarget.length();

            if (distance > 0 && distance <= range) {
                target.damage(attacker.getDamageSources().mobAttack(attacker), baseDamage);

                Vec3d hitPos = target.getPos().add(0, target.getHeight() / 2, 0);
                serverWorld.spawnParticles(ParticleTypes.FLASH, hitPos.x, hitPos.y, hitPos.z, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 6, 0.1, 0.1, 0.1, 0.05);
            }
        }

        int steps = (int)(range * 12);
        double spiralRadius = 0.25;
        Vec3d loweredOrigin = origin.subtract(0, 0.15, 0);
        Vec3d loweredEnd = end.subtract(0, 0.15, 0);

        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3d point = loweredOrigin.lerp(loweredEnd, progress);


            serverWorld.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0, 0, 0, 0);


            double angle = progress * Math.PI * 16;
            double offsetX = Math.cos(angle) * spiralRadius;
            double offsetZ = Math.sin(angle) * spiralRadius;
            Vec3d spiral = point.add(offsetX, 0, offsetZ);

            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, spiral.x, spiral.y, spiral.z, 1, 0, 0, 0, 0);


            if (i % 4 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.01);
            }
        }


        serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, loweredEnd.x, loweredEnd.y, loweredEnd.z, 1, 0, 0, 0, 0);
        serverWorld.spawnParticles(ParticleTypes.CRIT, loweredEnd.x, loweredEnd.y, loweredEnd.z, 8, 0.2, 0.1, 0.2, 0.1);
    }
}
