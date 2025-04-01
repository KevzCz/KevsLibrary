package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.entity.IcicleProjectileEntity;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FrostNovaHandler {
    private static final double RADIUS = 5.0;
    private static final float BASE_DAMAGE = 3.0f;
    private static final int SLOW_DURATION = 60; // 3 seconds
    private static final int COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST_NOVA = new HashMap<>();

    public static void triggerFrostNova(LivingEntity attacker) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        UUID attackerId = attacker.getUuid();
        long currentTime = world.getTime();

        if (currentTime - LAST_NOVA.getOrDefault(attackerId, 0L) < COOLDOWN_TICKS) return;
        LAST_NOVA.put(attackerId, currentTime);

        int novaCount = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_COUNT) != null
                ? (int) attacker.getAttributeValue(KevsLibrary.FROST_NOVA_COUNT)
                : 1;

        EntityAttributeInstance overloadAttr = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE);
        double overloadChance = overloadAttr != null ? overloadAttr.getValue() : 0.0;

        int overloadedWave = -1;
        if (attacker.getRandom().nextDouble() < overloadChance) {
            overloadedWave = attacker.getRandom().nextInt(novaCount);
        }

        for (int i = 0; i < novaCount; i++) {
            int delay = i * 10;
            int waveIndex = i;
            boolean overloaded = (waveIndex == overloadedWave);

            List<LivingEntity> targetsSnapshot = world.getEntitiesByClass(
                    LivingEntity.class,
                    attacker.getBoundingBox().expand(RADIUS + novaCount * 2),
                    e -> isValidTarget(e, attacker)
            );

            DelayedExecutor.runLater(() -> {
                doFrostNova(attacker, waveIndex, overloaded, targetsSnapshot);
            }, delay);
        }

    }


    private static void doFrostNova(LivingEntity attacker, int waveIndex, boolean overloaded, List<LivingEntity> targets)

    {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        double frostPower = SpellSchools.FROST.getValue(
                SpellSchool.Trait.POWER,
                new SpellSchool.QueryArgs(attacker)
        );
        float damage = (float) (BASE_DAMAGE + frostPower);
        EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgAttr != null) {
            damage *= (float) dmgAttr.getValue();
        }
        if (overloaded) {
            damage *= 1.5f;
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                    30, 0.6, 0.5, 0.6, 0.03);
            world.playSound(null, attacker.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 0.8f);
        }


        // Radius increases with each wave: base 4 + waveIndex * 1.5
        double radius = RADIUS + waveIndex * 1.5;

        // Softer, more mystical sound
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.5f, 1.1f);

        // ❄️ Spiral snowflake burst
        for (int angleDeg = 0; angleDeg < 360; angleDeg += 15) {
            double angle = Math.toRadians(angleDeg);
            double x = attacker.getX() + Math.cos(angle) * radius;
            double z = attacker.getZ() + Math.sin(angle) * radius;
            double y = attacker.getY() + 1.0;

            world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 2, 0.2, 0.2, 0.2, 0.01);
            world.spawnParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        // 💨 Core burst at center
        world.spawnParticles(ParticleTypes.CLOUD, attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                10, 0.5, 0.1, 0.5, 0.01);
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                6, 0.2, 0.3, 0.2, 0.02);

        targets = world.getEntitiesByClass(
                LivingEntity.class,
                attacker.getBoundingBox().expand(radius),
                e -> isValidTarget(e, attacker)
        );

        for (LivingEntity target : targets) {
            target.damage(attacker.getDamageSources().magic(), damage);
            Vec3d knock = target.getPos().subtract(attacker.getPos()).normalize().multiply(0.4 + 0.1 * waveIndex);
            target.addVelocity(knock.x, 0.2, knock.z);
            target.setFrozenTicks(SLOW_DURATION);

            // 🎯 Impact feedback
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.01);

            // ❄️ On overload, launch icicles from the hit target
            if (overloaded) {
                spawnIcicleBurst(world, attacker, target, damage * 0.75f);
            }
        }

    }

    private static void spawnIcicleBurst(ServerWorld world, LivingEntity attacker, LivingEntity origin, float icicleDamage) {
        Vec3d basePos = origin.getPos();
        int count = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_COUNT) != null
                ? (int) attacker.getAttributeValue(KevsLibrary.FROST_NOVA_COUNT) * 2
                : 2;

        for (int i = 0; i < count; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 1.2;
            double offsetZ = (world.random.nextDouble() - 0.5) * 1.2;
            double spawnY = basePos.y + origin.getHeight() + 0.5;

            Vec3d spawnPos = new Vec3d(basePos.x + offsetX, spawnY, basePos.z + offsetZ);
            IcicleProjectileEntity icicle = IcicleProjectileEntity.create(world, attacker, icicleDamage);
            icicle.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);

            // 🔼 Arc upward with randomness
            double upwardPower = 0.6 + world.random.nextDouble() * 0.4;
            double spreadX = (world.random.nextDouble() - 0.5) * 0.3;
            double spreadZ = (world.random.nextDouble() - 0.5) * 0.3;
            icicle.setVelocity(spreadX, upwardPower, spreadZ);

            world.spawnEntity(icicle);
        }

        world.playSound(null, origin.getBlockPos(), SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 0.6f, 1.5f);
        world.spawnParticles(ParticleTypes.SNOWFLAKE,
                basePos.x, basePos.y + origin.getHeight() + 1.0, basePos.z,
                20, 0.4, 0.2, 0.4, 0.01);
    }




    private static boolean isValidTarget(LivingEntity entity, LivingEntity attacker) {
        if (!entity.isAlive()) return false;
        if (entity.equals(attacker)) return false;
        if (entity.isTeammate(attacker)) return false;
        if (entity instanceof PlayerEntity) return false;
        if (entity.getType().getSpawnGroup().isPeaceful()) return false;
        if (entity instanceof TameableEntity tameable && tameable.isTamed()) return false;
        return true;
    }
}
