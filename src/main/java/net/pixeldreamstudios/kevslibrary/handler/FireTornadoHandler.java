package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class FireTornadoHandler {
    private static final int BASE_TICKS = 60;
    private static final double BASE_RADIUS = 4.0;
    private static final float BASE_DAMAGE = 3.0f;
    private static final int FIRE_DURATION = 3 * 20; // 3 seconds
    private static final Map<UUID, Long> LAST_TORNADO_CAST = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20;

    public static void spawnFireTornado(LivingEntity attacker, LivingEntity centerEntity) {
        ServerWorld world = (ServerWorld) centerEntity.getWorld();
        Vec3d center = centerEntity.getPos();
        UUID attackerId = attacker.getUuid();
        long currentTime = world.getTime();

        if (currentTime - LAST_TORNADO_CAST.getOrDefault(attackerId, 0L) < COOLDOWN_TICKS) return;
        LAST_TORNADO_CAST.put(attackerId, currentTime);

        // Determine if overloaded
        boolean isOverloaded;
        double overloadChance = attacker.getAttributeInstance(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE) != null
                ? attacker.getAttributeValue(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE)
                : 0.0;

        if (attacker.getRandom().nextDouble() < overloadChance) {
            isOverloaded = true;
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, attacker.getSoundCategory(), 1.0f, 1.4f);
        } else {
            isOverloaded = false;
        }

        // Spell power scaling
        double firePower = SpellSchools.FIRE.getValue(
                SpellSchool.Trait.POWER,
                new SpellSchool.QueryArgs(attacker)
        );

        float damage = (float) (BASE_DAMAGE);
        EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgAttr != null) {
            damage += (float) dmgAttr.getValue();
        }
        if (isOverloaded) damage *= 2;

        int ticks = isOverloaded ? BASE_TICKS * 2 : BASE_TICKS;
        double radius = isOverloaded ? BASE_RADIUS * 2 : BASE_RADIUS;
        double baseInwardStrength = isOverloaded ? 0.16 : 0.08;

        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ITEM_FIRECHARGE_USE, attacker.getSoundCategory(), 1.0f, 1.0f);

        for (int tick = 0; tick < ticks; tick++) {
            final int currentTick = tick;

            float finalDamage = damage;
            DelayedExecutor.runLater(() -> {
                Vec3d tornadoCenter = center.add(0, 0.5, 0);
                double baseY = center.y;
                double topY = baseY + 3.0;

                List<LivingEntity> affected = world.getEntitiesByClass(
                        LivingEntity.class,
                        centerEntity.getBoundingBox().expand(radius + 2.0),
                        e -> isValidTarget(e, attacker)
                );

                for (LivingEntity target : affected) {
                    Vec3d targetPos = target.getPos();
                    Vec3d toCenter = tornadoCenter.subtract(targetPos);
                    double distance = toCenter.length();

                    Vec3d pull = new Vec3d(toCenter.x, 0, toCenter.z).normalize();
                    Vec3d velocity;

                    if (distance > radius) {
                        velocity = pull.multiply(0.07);
                    } else {
                        double inwardStrength = baseInwardStrength + (1.0 - (distance / radius)) * 0.1;
                        inwardStrength = Math.min(inwardStrength, 0.25);

                        Vec3d orbit = new Vec3d(-pull.z, 0, pull.x).normalize().multiply(0.05);
                        Vec3d inward = pull.multiply(inwardStrength);
                        velocity = orbit.add(inward);

                        double speedLimit = 0.3;
                        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                        if (horizontalSpeed > speedLimit) {
                            double scale = speedLimit / horizontalSpeed;
                            velocity = new Vec3d(velocity.x * scale, velocity.y, velocity.z * scale);
                        }

                        double currentY = targetPos.y;
                        double verticalMotion;
                        if (currentY < topY - 0.3) {
                            verticalMotion = 0.07 + (Math.random() * 0.03);
                        } else if (currentY > topY) {
                            verticalMotion = -0.1;
                        } else {
                            verticalMotion = Math.sin((target.age + currentTick) * 0.3) * 0.05;
                        }

                        velocity = new Vec3d(velocity.x, verticalMotion, velocity.z);

                        target.setOnFireFor(FIRE_DURATION / 20);
                        target.damage(attacker.getDamageSources().inFire(), finalDamage);


                    }

                    target.addVelocity(velocity.x, velocity.y, velocity.z);
                }

                // Spiral particle effect
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18 + (currentTick * 15));
                    double spiralRadius = 1.0 + 0.05 * currentTick;
                    double x = center.x + Math.cos(angle) * spiralRadius;
                    double y = center.y + 0.1 * currentTick;
                    double z = center.z + Math.sin(angle) * spiralRadius;

                    world.spawnParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0, 0, 0);
                    world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 0, 0, 0, 0, 0.01);

                    if (isOverloaded) {
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.1, z, 1, 0, 0, 0, 0.01);
                        world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.2, z, 1, 0, 0, 0, 0.01);
                    }
                }
            }, currentTick);
        }
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
