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
import net.pixeldreamstudios.kevslibrary.entity.IcicleProjectileEntity;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class FrostNovaHandler {
    private static final double RADIUS = 5.0;
    private static final float BASE_DAMAGE = 3.0f;
    private static final int SLOW_DURATION = 60;
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

    private static void doFrostNova(LivingEntity attacker, int waveIndex, boolean overloaded, List<LivingEntity> targets) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        SpellPower.Result result = SpellPower.getSpellPower(SpellSchools.FROST, attacker);
        SpellPower.Result.Value rawResult = result.nonCritical();

        float base = BASE_DAMAGE + (float) result.baseValue();
        float critChance = (float) result.criticalChance();
        float critMultiplier = (float) result.criticalDamage();

        float dmgMult = 1.0f;
        EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgAttr != null) dmgMult = (float) dmgAttr.getValue();

        if (overloaded) {
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                    40, 0.7, 0.6, 0.7, 0.04);
            world.spawnParticles(ParticleTypes.SNOWFLAKE, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                    30, 0.4, 0.3, 0.4, 0.01);
            world.spawnParticles(ParticleTypes.CLOUD, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                    20, 0.4, 0.1, 0.4, 0.02);
            world.playSound(null, attacker.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.1f, 0.8f);
        }

        double radius = RADIUS + waveIndex * 1.5;

        float pitch = 0.9f + waveIndex * 0.05f;
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.7f, pitch);

        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2;
            double x = attacker.getX() + Math.cos(angle) * radius;
            double z = attacker.getZ() + Math.sin(angle) * radius;
            double y = attacker.getY() + 1.2;

            world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            if (overloaded) {
                world.spawnParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 1, 0.05, 0.05, 0.05, 0.005);
            }
        }

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
            SpellPower.Vulnerability vuln = SpellPower.getVulnerability(target, SpellSchools.FROST);

            float raw = base * (1.0f + vuln.powerBaseMultiplier());
            boolean isCrit = attacker.getRandom().nextDouble() < (critChance + vuln.criticalChanceBonus());
            float critApplied = isCrit ? raw * (critMultiplier + vuln.criticalDamageBonus()) : raw;

            float finalDamage = critApplied * dmgMult;
            if (overloaded) finalDamage *= 1.5f;

            target.damage(attacker.getDamageSources().magic(), finalDamage);
            float finalDamage1 = finalDamage;
            SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                SoulLinkHandler.handleLinkedDamage(linkData.attacker(), target, finalDamage1, linkData.group(), linkData.soulPower());
            });

            Vec3d knock = target.getPos().subtract(attacker.getPos()).normalize().multiply(0.4 + 0.1 * waveIndex);
            target.addVelocity(knock.x, 0.2, knock.z);
            target.setFrozenTicks(SLOW_DURATION);

            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.01);

            if (isCrit) {
                world.spawnParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 1.2, target.getZ(),
                        8, 0.3, 0.2, 0.3, 0.01);
                world.spawnParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 1.4, target.getZ(),
                        8, 0.25, 0.25, 0.25, 0.01);
                world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
            }

            if (overloaded) {
                spawnIcicleBurst(world, attacker, target, finalDamage * 0.75f);
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
        if (!(entity instanceof PlayerEntity) && entity.getType().getSpawnGroup().isPeaceful()) return false;
        if (entity instanceof TameableEntity tameable && tameable.isTamed()) return false;
        return true;
    }
}
