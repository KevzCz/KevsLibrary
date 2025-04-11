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
import net.spell_power.api.SpellSchools;

import java.util.*;

public class SoulLinkHandler {

    private static final int BASE_LINK_DURATION_TICKS = 200; // Doubled from 100 → 200 (10s)
    private static final int MAX_LINKS = 4;
    private static final double LINK_RADIUS = 8.0;
    private static final int PULL_INTERVAL_TICKS = 10; // Faster pull frequency (used to be 20)
    private static final Set<UUID> linkedEntities = new HashSet<>();

    public static void triggerSoulLink(LivingEntity attacker, LivingEntity primaryTarget) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        if (SoulLinkTracker.getGroup(primaryTarget).isPresent()) return;

        List<LivingEntity> found = world.getEntitiesByClass(
                LivingEntity.class,
                primaryTarget.getBoundingBox().expand(LINK_RADIUS),
                e -> isValidTarget(e, attacker) && !linkedEntities.contains(e.getUuid())
        );

        if (found.isEmpty()) return;
        Collections.shuffle(found);

        List<LivingEntity> linked = new ArrayList<>();
        linked.add(primaryTarget);
        linkedEntities.add(primaryTarget.getUuid());

        for (LivingEntity mob : found) {
            linked.add(mob);
            linkedEntities.add(mob.getUuid());
            if (linked.size() >= MAX_LINKS) break;
        }

        playLinkParticles(world, primaryTarget.getPos());
        world.playSound(null, primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8f, 1.2f);

        float soulPower = (float) SpellPower.getSpellPower(SpellSchools.SOUL, attacker).baseValue();

        LinkedGroup group = new LinkedGroup(attacker, linked, soulPower);
        group.start();
        SoulLinkTracker.linkGroup(linked, attacker, soulPower, group);
    }

    private static void playLinkParticles(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 / 24) * i;
            double radius = 2.0;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.5;
            world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.01, 0.01, 0.01, 0.01);
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

    public static void tryExtendLink(LivingEntity attacker, LivingEntity fromEntity) {
        var opt = SoulLinkTracker.getGroup(fromEntity);
        if (opt.isEmpty()) return;

        var groupData = opt.get();
        List<LivingEntity> group = groupData.group();

        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                fromEntity.getBoundingBox().expand(LINK_RADIUS),
                entity -> isValidTarget(entity, attacker) &&
                        !group.contains(entity) &&
                        !linkedEntities.contains(entity.getUuid())
        );

        LivingEntity nearest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(fromEntity)))
                .orElse(null);

        if (nearest == null) return;

        group.add(nearest);
        linkedEntities.add(nearest.getUuid());

        // ✅ Fully update the SoulLinkTracker with the new group state
        SoulLinkTracker.linkGroup(group, groupData.attacker(), groupData.soulPower(), groupData.handler());

        groupData.handler().extendLifetime();

        playLinkParticles(world, nearest.getPos());
        world.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.6f, 1.0f);
    }

    public static void handleLinkedDamage(LivingEntity attacker, LivingEntity damaged, float originalDamage, List<LivingEntity> linked, float soulPower) {
        Set<LivingEntity> alreadyHit = new HashSet<>();

        for (LivingEntity entity : linked) {
            if (!entity.isAlive()) continue;

            SpellPower.Result soul = SpellPower.getSpellPower(SpellSchools.SOUL, attacker);
            float spreadDamage = (float) (originalDamage * (0.75f + soul.baseValue() * 0.1f));

            boolean isCrit = attacker.getRandom().nextFloat() < soul.criticalChance();
            if (isCrit) spreadDamage *= soul.criticalDamage();

            EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
            if (dmgAttr != null) spreadDamage *= dmgAttr.getValue();

            if (alreadyHit.contains(entity)) continue;
            alreadyHit.add(entity);

            entity.damage(attacker.getDamageSources().magic(), spreadDamage);
            Vec3d pos = entity.getPos();
            ((ServerWorld) attacker.getWorld()).spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    public static void handleDeathOverload(LivingEntity attacker, LivingEntity dead, float maxHealth) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        SoulLinkTracker.getGroup(dead).ifPresent(linkData -> {
            Set<UUID> hitAlready = new HashSet<>();

            SpellPower.Result result = SpellPower.getSpellPower(SpellSchools.SOUL, attacker);
            float soulPower = (float) result.baseValue();
            float explosionDamage = maxHealth * (0.3f + soulPower * 0.1f);

            EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
            if (dmgAttr != null) explosionDamage *= dmgAttr.getValue();

            for (LivingEntity linked : linkData.group()) {
                if (!linked.isAlive() || linked.equals(dead)) continue;
                if (!hitAlready.add(linked.getUuid())) continue;

                linked.damage(attacker.getDamageSources().magic(), explosionDamage);
                Vec3d pos = linked.getPos();
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.0, pos.z, 12, 0.3, 0.3, 0.3, 0.03);
            }

            Vec3d epicenter = dead.getPos();

            for (int i = 0; i < 32; i++) {
                double angle = i * (2 * Math.PI / 32);
                double dx = Math.cos(angle) * 2.5;
                double dz = Math.sin(angle) * 2.5;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, epicenter.x + dx, epicenter.y + 1, epicenter.z + dz, 2, 0.1, 0.1, 0.1, 0.02);
            }

            world.spawnParticles(ParticleTypes.SOUL, epicenter.x, epicenter.y + 1.0, epicenter.z, 30, 0.6, 0.6, 0.6, 0.03);
            world.spawnParticles(ParticleTypes.EXPLOSION, epicenter.x, epicenter.y + 0.5, epicenter.z, 3, 0.2, 0.2, 0.2, 0);

            DelayedExecutor.runLater(() -> {
                for (int i = 0; i < 16; i++) {
                    double angle = i * (2 * Math.PI / 16);
                    double dx = Math.cos(angle) * 3.5;
                    double dz = Math.sin(angle) * 3.5;
                    world.spawnParticles(ParticleTypes.SMOKE, epicenter.x + dx, epicenter.y + 1, epicenter.z + dz, 2, 0.1, 0.1, 0.1, 0.01);
                }
                world.playSound(null, epicenter.x, epicenter.y, epicenter.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 0.85f);
            }, 20);

            world.playSound(null, epicenter.x, epicenter.y, epicenter.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1f, 0.65f);
        });
    }

    public static class LinkedGroup {
        private final List<LivingEntity> members;
        private final LivingEntity attacker;
        private final float soulPower;
        private int ticks = 0;
        private float currentExtension = 20f;

        LinkedGroup(LivingEntity attacker, List<LivingEntity> members, float soulPower) {
            this.attacker = attacker;
            this.members = members;
            this.soulPower = soulPower;
        }

        void start() {
            tick();
        }

        void extendLifetime() {
            ticks -= (int) currentExtension;
            if (ticks < 0) ticks = 0;
            currentExtension *= 0.9f;
            if (currentExtension < 2) currentExtension = 2;
        }

        void tick() {
            DelayedExecutor.runLater(() -> {
                if (ticks >= BASE_LINK_DURATION_TICKS) {
                    members.forEach(e -> linkedEntities.remove(e.getUuid()));
                    SoulLinkTracker.clearLinks(members);
                    return;
                }

                if (ticks % PULL_INTERVAL_TICKS == 0) {
                    pullEntities();
                }

                drawArcs();
                ticks++;
                tick();
            }, 1);
        }

        void pullEntities() {
            if (members.isEmpty()) return;

            LivingEntity anchor = members.get(0);
            Vec3d center = anchor.getPos();

            for (LivingEntity entity : members) {
                if (!entity.isAlive() || entity.equals(anchor)) continue;

                Vec3d pull = center.subtract(entity.getPos()).normalize().multiply(0.3); // Stronger pull
                entity.addVelocity(pull.x, 0.1, pull.z); // Higher vertical pull
            }
        }

        void drawArcs() {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            Vec3d a = members.get(0).getPos();

            for (LivingEntity b : members) {
                if (!b.isAlive() || b == members.get(0)) continue;

                Vec3d bPos = b.getPos();
                Vec3d diff = bPos.subtract(a);
                for (int i = 0; i <= 10; i++) {
                    Vec3d point = a.add(diff.multiply(i / 10.0));
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 1.0, point.z, 1, 0, 0, 0, 0.001);
                }
            }
        }
    }
}
