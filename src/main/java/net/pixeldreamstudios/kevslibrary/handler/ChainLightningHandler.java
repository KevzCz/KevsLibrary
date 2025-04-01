package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class ChainLightningHandler {

    private static final double MAX_DISTANCE = 8.0;

    public static void spawnChainLightning(LivingEntity attacker, LivingEntity initialTarget) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        int bounceCount = attacker.getAttributeInstance(KevsLibrary.CHAIN_LIGHTNING_COUNT) != null
                ? (int) attacker.getAttributeValue(KevsLibrary.CHAIN_LIGHTNING_COUNT)
                : 3;

        double overloadChance = attacker.getAttributeInstance(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE) != null
                ? attacker.getAttributeValue(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE)
                : 0.0;

        double lightningPower = SpellSchools.LIGHTNING.getValue(
                SpellSchool.Trait.POWER,
                new SpellSchool.QueryArgs(attacker)
        );

        float damage = (float) (4.0 + lightningPower);

        EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgAttr != null) {
            damage *= (float) dmgAttr.getValue();
        }
        Set<LivingEntity> visited = new HashSet<>();
        visited.add(attacker);
        visited.add(initialTarget);

        // 🎯 Hit the original target FIRST (not a bounce)
        spawnArcParticles(world, attacker, initialTarget);
        world.playSound(null, initialTarget.getX(), initialTarget.getY(), initialTarget.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.2f, 0.2f);
        initialTarget.damage(attacker.getDamageSources().magic(), damage);

        if (attacker.getRandom().nextDouble() < overloadChance) {
            spawnOverloadVisuals(world, initialTarget.getPos());
            triggerOverloadDoT(attacker, initialTarget, damage * 0.2f, bounceCount);
        }

        // 🌩️ Start bouncing from the initial target
        Queue<LivingEntity> queue = new LinkedList<>();
        queue.add(initialTarget);

        int jumps = 0; // ✅ Zero bounces so far

        while (!queue.isEmpty() && jumps < bounceCount) {
            LivingEntity current = queue.poll();

            List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class,
                    current.getBoundingBox().expand(MAX_DISTANCE),
                    e -> isValidBounceTarget(e, attacker, visited));

            if (candidates.isEmpty()) break;

            LivingEntity next = candidates.get(0);
            visited.add(next);
            queue.add(next);
            jumps++;

            spawnArcParticles(world, current, next);
            world.playSound(null, next.getX(), next.getY(), next.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.2f, 0.2f);
            next.damage(attacker.getDamageSources().magic(), damage);

            if (attacker.getRandom().nextDouble() < overloadChance) {
                spawnOverloadVisuals(world, next.getPos());
                triggerOverloadDoT(attacker, next, damage * 0.2f, bounceCount);
            }
        }
    }


    private static boolean isValidBounceTarget(LivingEntity entity, LivingEntity attacker, Set<LivingEntity> visited) {
        if (!entity.isAlive()) return false;
        if (entity.equals(attacker)) return false;
        if (visited.contains(entity)) return false;
        if (entity.isTeammate(attacker)) return false;
        if (entity instanceof PlayerEntity) return false;
        if (entity.getType().getSpawnGroup().isPeaceful()) return false;
        if (entity instanceof TameableEntity tameable && tameable.isTamed()) return false;
        return true;
    }

    private static void spawnArcParticles(ServerWorld world, LivingEntity from, LivingEntity to) {
        Vec3d start = from.getPos().add(0, from.getHeight() * 0.6, 0);
        Vec3d end = to.getPos().add(0, to.getHeight() * 0.6, 0);
        Vec3d diff = end.subtract(start);
        int steps = 10;

        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3d point = start.add(diff.multiply(progress));
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0.01);
        }
    }

    private static void spawnOverloadVisuals(ServerWorld world, Vec3d pos) {
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.8f);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 10, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.01);
    }

    private static void triggerOverloadDoT(LivingEntity attacker, LivingEntity target, float tickDamage, int ticks) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        new Thread(() -> {
            // ✨ Delay before damage starts
            for (int i = 0; i < 20; i++) { // 20 iterations = ~2 seconds (20 × 100ms)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}

                if (!target.isAlive()) return;

                Vec3d pos = target.getPos();
                world.getServer().execute(() -> {
                    // 💫 Pre-detonation "charged" particles
                    world.spawnParticles(ParticleTypes.CRIT,
                            pos.x, pos.y + 1.0, pos.z,
                            3, 0.2, 0.3, 0.2, 0.01);

                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.x, pos.y + 0.5, pos.z,
                            2, 0.1, 0.2, 0.1, 0.01);
                });
            }

            // ⚡ Begin actual damage ticks
            for (int i = 0; i < ticks; i++) {
                try {
                    Thread.sleep(100); //
                } catch (InterruptedException ignored) {}

                if (!target.isAlive()) return;

                world.getServer().execute(() -> {
                    // Bypass i-frames
                    target.timeUntilRegen = 0;
                    target.hurtTime = 0;

                    target.damage(attacker.getDamageSources().magic(), tickDamage);

                    // 💥 Zap visuals per tick
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 0.4f, 1.8f);

                    world.spawnParticles(ParticleTypes.END_ROD,
                            target.getX(), target.getY() + 1.2, target.getZ(),
                            6, 0.3, 0.4, 0.3, 0.01);
                });
            }
        }).start();
    }

}
