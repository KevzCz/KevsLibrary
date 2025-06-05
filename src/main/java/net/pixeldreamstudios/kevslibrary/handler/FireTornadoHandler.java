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
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class FireTornadoHandler {
    private static final int BASE_TICKS = 60;
    private static final double BASE_RADIUS = 4.0;
    private static final float BASE_DAMAGE = 3.0f;
    private static final int FIRE_DURATION = 3 * 20;
    private static final Map<UUID, Long> LAST_TORNADO_CAST = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20;

    public static void spawnFireTornado(LivingEntity attacker, LivingEntity centerEntity) {
        ServerWorld world = (ServerWorld) centerEntity.getWorld();
        Vec3d center = centerEntity.getPos();
        UUID attackerId = attacker.getUuid();
        long currentTime = world.getTime();

        if (currentTime - LAST_TORNADO_CAST.getOrDefault(attackerId, 0L) < COOLDOWN_TICKS) return;
        LAST_TORNADO_CAST.put(attackerId, currentTime);

        boolean isOverloaded;
        double overloadChance = attacker.getAttributeInstance(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE) != null
                ? attacker.getAttributeValue(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE)
                : 0.0;

        isOverloaded = attacker.getRandom().nextDouble() < overloadChance;

        if (isOverloaded) {
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, attacker.getSoundCategory(), 1.0f, 1.4f);
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ENTITY_BLAZE_AMBIENT, attacker.getSoundCategory(), 0.6f, 0.7f);
        }

        int ticks = isOverloaded ? BASE_TICKS * 2 : BASE_TICKS;
        double radius = isOverloaded ? BASE_RADIUS * 2 : BASE_RADIUS;
        double baseInwardStrength = isOverloaded ? 0.16 : 0.08;

        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ITEM_FIRECHARGE_USE, attacker.getSoundCategory(), 1.0f, 1.0f);

        for (int tick = 0; tick < ticks; tick++) {
            final int currentTick = tick;

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
                    SpellPower.Result result = SpellPower.getSpellPower(SpellSchools.FIRE, attacker);
                    SpellPower.Vulnerability vuln = SpellPower.getVulnerability(target, SpellSchools.FIRE);
                    SpellPower.Result.Value rawResult = result.nonCritical();

                    float base = BASE_DAMAGE + (float) result.baseValue() * (1.0f + vuln.powerBaseMultiplier());
                    boolean isCrit = attacker.getRandom().nextDouble() < (result.criticalChance() + vuln.criticalChanceBonus());
                    float critMultiplier = isCrit ? (float) (result.criticalDamage() + vuln.criticalDamageBonus()) : 1.0f;
                    float critApplied = base * critMultiplier;

                    EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
                    float multiplier = dmgAttr != null ? (float) dmgAttr.getValue() : 1.0f;

                    float damage = critApplied * multiplier;
                    if (isOverloaded) damage *= 2.0f;

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
                        target.damage(attacker.getDamageSources().inFire(), damage);
                        float finalDamage = damage;
                        SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                            if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                            SoulLinkHandler.handleLinkedDamage(linkData.attacker(), target, finalDamage, linkData.group(), linkData.soulPower());
                        });
                        if (isCrit) {
                            world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
                            if (isOverloaded) {
                                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.3, target.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
                            }
                        }
                    }

                    target.addVelocity(velocity.x, velocity.y, velocity.z);
                }

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
        if (!(entity instanceof PlayerEntity) && entity.getType().getSpawnGroup().isPeaceful()) return false;
        if (entity instanceof TameableEntity tameable && tameable.isTamed()) return false;
        return true;
    }
}
