package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_engine.entity.SpellProjectile;

public final class ProjectileStormHandler {
    private ProjectileStormHandler() {}
    private static final String USED_TAG  = "kevslib_storm_used";

    private static final String STORM_TAG = "kevslib_storm_projectile";
    private static final float DAMAGE_RATIO  = 0.15f;
    private static final float FALL_SPEED    = 1.15f;
    private static final int   TELEGRAPH_MIN = 10;
    private static final int   RAIN_INTERVAL = 2;

    public static void tryTrigger(PersistentProjectileEntity projectile, Vec3d impactCenter, LivingEntity targetForHeight) {
        if (!(projectile.getWorld() instanceof ServerWorld world)) return;
        LivingEntity owner = projectile.getOwner() instanceof LivingEntity l ? l : null;
        if (owner == null) return;

        if (isStormTag(projectile) || projectile.getCommandTags().contains(USED_TAG)) return;
        projectile.addCommandTag(USED_TAG);

        double chance = owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_CHANCE);
        if (chance <= 0 || world.getRandom().nextDouble() > chance) return;

        float radius   = (float) owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_RANGE);
        if (radius <= 0) radius = 5.0f;
        int duration   = (int) Math.round(owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_DURATION));
        if (duration <= 0) duration = 30;
        if (projectile instanceof net.minecraft.entity.projectile.TridentEntity) return;


        double originalSpeed = projectile.getVelocity().length();
        float originalFinal  = (float) (originalSpeed * projectile.getDamage());
        float stormDamage    = Math.max(0.1f, originalFinal * DAMAGE_RATIO);

        double spawnY = (targetForHeight != null)
                ? impactCenter.y + targetForHeight.getHeight() + 4.0
                : impactCenter.y + 6.0;

        int teleTicks = Math.min(TELEGRAPH_MIN, Math.max(6, duration / 3));
        telegraphCircle(world, impactCenter, spawnY, radius, teleTicks);
        persistCircle(world, impactCenter, spawnY, radius, teleTicks + duration);

        startRain(world, owner, impactCenter, spawnY, radius, duration, (w, x, y, z) -> {
            Entity copy = net.minecraft.entity.EntityType.ARROW.create(w);
            if (!(copy instanceof PersistentProjectileEntity ppe)) return;

            ppe.setOwner(owner);
            ppe.addCommandTag(STORM_TAG);
            ppe.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;

            ppe.setPosition(x, y, z);
            double wobble = (w.getRandom().nextDouble() - 0.5) * 0.04;
            double vx = wobble, vy = -FALL_SPEED, vz = wobble;
            ppe.setVelocity(vx, vy, vz);

            double fallSpeedLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
            float baseForThisFall = (float) (stormDamage / Math.max(0.1, fallSpeedLen));
            ppe.setDamage(baseForThisFall);

            w.spawnEntity(ppe);
        });

        if (!(projectile instanceof net.minecraft.entity.projectile.TridentEntity) && world.getRandom().nextBoolean()) {
            projectile.discard();
        }
    }
    public static void tryTrigger(SpellProjectile spell, Vec3d impactCenter, LivingEntity targetForHeight) {
        if (!(spell.getWorld() instanceof ServerWorld world)) return;
        LivingEntity owner = spell.getOwner() instanceof LivingEntity l ? l : null;
        if (owner == null) return;

        if (isStormTag(spell) || spell.getCommandTags().contains(USED_TAG)) return;
        spell.addCommandTag(USED_TAG);

        double chance = owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_CHANCE);
        if (chance <= 0 || world.getRandom().nextDouble() > chance) return;

        float radius = (float) owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_RANGE);
        if (radius <= 0) radius = 2.5f;

        int duration = (int) Math.round(owner.getAttributeValue(KevsLibrary.PROJECTILE_STORM_DURATION));
        if (duration <= 0) duration = 30;

        var baseCtx   = spell.getImpactContext();
        var scaledCtx = baseCtx.distance(baseCtx.distance() * DAMAGE_RATIO);

        double spawnY = (targetForHeight != null)
                ? impactCenter.y + targetForHeight.getHeight() + 4.0
                : impactCenter.y + 6.0;

        int teleTicks = Math.min(TELEGRAPH_MIN, Math.max(6, duration / 3));

        telegraphCircle(world, impactCenter, spawnY, radius, teleTicks);
        persistCircle(world, impactCenter, spawnY, radius, teleTicks + duration);

        startRain(world, owner, impactCenter, spawnY, radius, duration, (w, x, y, z) -> {
            SpellProjectile drop = new SpellProjectile(
                    w, owner, x, y, z,
                    spell.getBehaviour(), spell.getSpellEntry(),
                    scaledCtx, spell.mutablePerks().copy()
            );
            drop.addCommandTag(STORM_TAG);
            drop.setVelocity(0, -FALL_SPEED, 0);
            drop.setYaw(0); drop.setPitch(90);
            drop.setSilent(true);
            w.spawnEntity(drop);
        });
    }


    @FunctionalInterface
    private interface Spawner { void spawn(ServerWorld w, double x, double y, double z); }

    private static void telegraphCircle(ServerWorld world, Vec3d center, double y, float radius, int telegraphTicks) {
        for (int t = 0; t < telegraphTicks; t++) {
            final int tick = t;
            DelayedExecutor.runLater(() -> {
                float r = radius * ((tick + 1) / (float) telegraphTicks);
                ring(world, center.x, y, center.z, r, 48, ParticleTypes.ENCHANT);
                disc(world, center.x, y, center.z, r * 0.9f, 5, 16);
            }, t);
        }
        DelayedExecutor.runLater(() ->
                ring(world, center.x, y, center.z, radius, 64, ParticleTypes.GLOW), telegraphTicks);
    }

    private static void persistCircle(ServerWorld world, Vec3d center, double y, float radius, int totalTicks) {
        for (int t = 0; t < totalTicks; t += 2) {
            final int delay = t;
            DelayedExecutor.runLater(() ->
                    disc(world, center.x, y, center.z, radius * 0.95f, 6, 18), delay);
        }
    }


    private static void startRain(ServerWorld world, LivingEntity owner, Vec3d center, double spawnY,
                                  float radius, int durationTicks, Spawner spawner) {
        int startDelay = Math.min(TELEGRAPH_MIN, Math.max(6, durationTicks / 3));
        int ticks = Math.max(1, durationTicks);
        double density = Math.max(1.0, radius * 0.8);
        int perBurst = Math.max(1, (int) Math.floor(density));
        Random r = world.getRandom();

        for (int i = 0; i < ticks; i += RAIN_INTERVAL) {
            final int delay = startDelay + i;
            DelayedExecutor.runLater(() -> {
                if (!owner.isAlive()) return;
                for (int k = 0; k < perBurst; k++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rr = Math.pow(r.nextDouble(), 0.7) * radius;
                    double x = center.x + Math.cos(a) * rr;
                    double z = center.z + Math.sin(a) * rr;
                    spawner.spawn(world, x, spawnY, z);


                }
            }, delay);
        }
    }
    private static void disc(ServerWorld world, double cx, double y, double cz,
                             float radius, int rings, int perRing) {
        long t = world.getTime();
        double swirl = (t % 40) * 0.15;
        for (int r = 1; r <= rings; r++) {
            float rr = radius * (r / (float) rings);
            int points = (int) (perRing * (0.6 + 0.4 * (r / (float) rings)));
            for (int i = 0; i < points; i++) {
                double a = swirl + (i / (double) points) * Math.PI * 2.0;
                double x = cx + Math.cos(a) * rr;
                double z = cz + Math.sin(a) * rr;
                world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0, 0, 0, 0.0);
                if ((i & 3) == 0) {
                    world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.0);
                }
            }
        }
    }
    private static void ring(ServerWorld world, double cx, double y, double cz, float radius, int points, net.minecraft.particle.ParticleEffect type) {
        for (int i = 0; i < points; i++) {
            double a = (i / (double) points) * Math.PI * 2.0;
            double x = cx + Math.cos(a) * radius;
            double z = cz + Math.sin(a) * radius;
            world.spawnParticles(type, x, y, z, 1, 0, 0, 0, 0.0);
        }
    }

    public static boolean isStormTag(Entity e) { return e.getCommandTags().contains(STORM_TAG); }
}
