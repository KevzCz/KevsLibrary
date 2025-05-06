package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Box;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.entity.ArcaneShardEntity;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class ArcaneRuptureHandler {

    private static final int SWIRL_DURATION_TICKS = 60;
    private static final int SPIKE_COUNT = 30;
    private static final double SWIRL_RADIUS = 1.5;

    private static final long COOLDOWN_MS = 500;
    private static final WeakHashMap<LivingEntity, Long> cooldowns = new WeakHashMap<>();

    public static void trigger(LivingEntity attacker, LivingEntity center) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        long now = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(attacker, 0L);
        if (now - lastUsed < COOLDOWN_MS) return;
        cooldowns.put(attacker, now);

        if (ArcaneRuptureTracker.hasBeenRuptured(center)) return;
        ArcaneRuptureTracker.markRuptured(center, SWIRL_DURATION_TICKS);

        double pullRadius = 6.0;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                center.getBoundingBox().expand(pullRadius),
                e -> isValidTarget(e, attacker) && !e.equals(center));

        for (LivingEntity target : targets) {
            Vec3d dir = center.getPos().subtract(target.getPos());
            Vec3d horizontalPull = new Vec3d(dir.x, 0, dir.z).normalize().multiply(0.2);
            target.addVelocity(horizontalPull.x, 0.0, horizontalPull.z);
        }

        for (int tick = 0; tick < SWIRL_DURATION_TICKS; tick++) {
            final int currentTick = tick;
            DelayedExecutor.runLater(() -> {
                Vec3d swirlCenter = center.getPos().add(0, 1.0, 0);

                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians((i * 22.5) + (currentTick * 15));
                    double radius = SWIRL_RADIUS - (currentTick * (SWIRL_RADIUS / SWIRL_DURATION_TICKS));
                    double x = swirlCenter.x + Math.cos(angle) * radius;
                    double z = swirlCenter.z + Math.sin(angle) * radius;
                    double y = swirlCenter.y + Math.sin(currentTick * 0.2 + i * 0.1) * 0.3;

                    world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 0, 0, 0, 0, 0.01);
                    world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 0, 0, 0, 0, 0.01);
                }

                List<LivingEntity> swirlTargets = world.getEntitiesByClass(LivingEntity.class,
                        center.getBoundingBox().expand(6.0),
                        e -> isValidTarget(e, attacker) && !e.equals(center));

                for (LivingEntity t : swirlTargets) {
                    Vec3d dir = swirlCenter.subtract(t.getPos());
                    Vec3d horizontalPull = new Vec3d(dir.x, 0, dir.z).normalize().multiply(0.05);
                    t.addVelocity(horizontalPull.x, 0.0, horizontalPull.z);
                }

            }, tick);
        }

        DelayedExecutor.runLater(() -> {
            world.playSound(null, center.getX(), center.getY(), center.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.0f, 1.4f);

            float finalDamage = computeScaledDamage(attacker, center, true);

            for (LivingEntity target : targets) {
                target.timeUntilRegen = 0;
                target.hurtTime = 0;
                target.damage(attacker.getDamageSources().magic(), finalDamage);

                if (finalDamage > 0) {
                    world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
                }
            }

            Vec3d burstCenter = center.getPos().add(0, 1.0, 0);
            spawnSpikes(world, burstCenter, attacker);

        }, SWIRL_DURATION_TICKS);
    }

    private static void spawnSpikes(ServerWorld world, Vec3d pos, LivingEntity attacker) {
        Random rand = world.getRandom();

        SpellPower.Result spellResult = SpellPower.getSpellPower(SpellSchools.ARCANE, attacker);
        float base = 5.0f + (float) spellResult.baseValue();
        float arcaneScale = getAttr(attacker, KevsLibrary.ARCANE_RUPTURE_DAMAGE, 1.0f);
        float globalScale = getAttr(attacker, KevsLibrary.DAMAGE, 1.0f);
        boolean isCrit = attacker.getRandom().nextDouble() < spellResult.criticalChance();
        float finalDamage = isCrit ? base * arcaneScale * globalScale * (float) spellResult.criticalDamage()
                : base * arcaneScale * globalScale;

        for (int r = 0; r < 40; r++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double x = pos.x + Math.cos(angle) * 3.5;
            double z = pos.z + Math.sin(angle) * 3.5;
            double y = pos.y;
            world.spawnParticles(ParticleTypes.WITCH, x, y + rand.nextDouble() * 0.8, z, 2, 0, 0, 0, 0.01);
        }

        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.2f, 0.6f + rand.nextFloat() * 0.4f);

        for (int i = 0; i < SPIKE_COUNT; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double pitch = (rand.nextDouble() * Math.PI / 2) + Math.toRadians(10); // pitch 10°–100°
            Vec3d dir = new Vec3d(Math.cos(angle) * Math.cos(pitch), Math.sin(pitch), Math.sin(angle) * Math.cos(pitch)).normalize();
            int segments = 10;
            double segmentLength = 0.5;

            for (int j = 1; j <= segments; j++) {
                Vec3d point = pos.add(dir.multiply(j * segmentLength));

                world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 2, 0, 0, 0, 0.01);
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, point.x, point.y, point.z, 3, 0, 0, 0, 0.02);
                world.spawnParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0, 0, 0, 0.01);

                if (j == segments) {
                    world.spawnParticles(ParticleTypes.GLOW, point.x, point.y, point.z, 10, 0.2, 0.2, 0.2, 0.03);
                    world.spawnParticles(ParticleTypes.FLASH, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
            }
        }

        List<LivingEntity> hit = world.getEntitiesByClass(LivingEntity.class,
                new Box(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5).expand(3.5),
                e -> isValidTarget(e, attacker));

        for (LivingEntity target : hit) {
            target.timeUntilRegen = 0;
            target.hurtTime = 0;
            target.damage(attacker.getDamageSources().magic(), finalDamage);

            Vec3d pull = pos.subtract(target.getPos()).normalize().multiply(0.15);
            target.addVelocity(pull.x, 0.06, pull.z);

            world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }


        double overloadChance = getAttr(attacker, KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE, 0.0f);
        if (attacker.getRandom().nextDouble() < overloadChance) {
            spawnArcaneShardOverload(world, pos, attacker, finalDamage);
        }
    }

    private static void spawnArcaneShardOverload(ServerWorld world, Vec3d center, LivingEntity attacker, float baseDamage) {
        Random rand = world.getRandom();
        float launchSpeed = 0.6f;

        for (int i = 0; i < 6; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double pitch = Math.toRadians(40 + rand.nextDouble() * 20);

            Vec3d dir = new Vec3d(
                    Math.cos(angle) * Math.cos(pitch),
                    Math.sin(pitch),
                    Math.sin(angle) * Math.cos(pitch)
            ).normalize();


            ArcaneShardEntity shard = ArcaneShardEntity.create(world, attacker, dir, baseDamage);
            shard.setNoGravity(false);
            shard.setPosition(center);


            shard.setVelocity(dir.multiply(launchSpeed));

            world.spawnEntity(shard);
        }

        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 20, 0.3, 0.3, 0.3, 0.01);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 1.3f);
    }


    private static boolean isValidTarget(LivingEntity entity, LivingEntity attacker) {
        return entity.isAlive()
                && !entity.equals(attacker)
                && !entity.isTeammate(attacker)
                && !(entity instanceof PlayerEntity)
                && !entity.getType().getSpawnGroup().isPeaceful()
                && (!(entity instanceof TameableEntity tameable) || !tameable.isTamed());
    }

    private static float getAttr(LivingEntity entity, RegistryEntry<EntityAttribute> attr, float fallback) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attr);
        return instance != null ? (float) instance.getValue() : fallback;
    }

    private static float computeScaledDamage(LivingEntity attacker, LivingEntity target, boolean applyCrit) {
        SpellPower.Result spellResult = SpellPower.getSpellPower(SpellSchools.ARCANE, attacker);
        SpellPower.Vulnerability vuln = SpellPower.getVulnerability(target, SpellSchools.ARCANE);

        float base = 5.0f + (float) spellResult.baseValue();
        float arcaneScale = getAttr(attacker, KevsLibrary.ARCANE_RUPTURE_DAMAGE, 1.0f);
        float globalScale = getAttr(attacker, KevsLibrary.DAMAGE, 1.0f);
        float scaled = base * arcaneScale * globalScale;

        if (applyCrit && attacker.getRandom().nextDouble() < spellResult.criticalChance()) {
            return scaled * (float) spellResult.criticalDamage();
        }

        return scaled;
    }
}
