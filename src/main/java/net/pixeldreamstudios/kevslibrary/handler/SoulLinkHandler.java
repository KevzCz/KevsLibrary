package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeScaling;
import net.pixeldreamstudios.kevslibrary.attribute.DamageScaling;
import net.pixeldreamstudios.kevslibrary.attribute.EffectHandler;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.*;

public class SoulLinkHandler extends EffectHandler {

    private static final SoulLinkHandler INSTANCE = new SoulLinkHandler();

    private static final int BASE_LINK_DURATION_TICKS = 200;
    private static final int MAX_LINKS = 4;
    private static final double LINK_RADIUS = 8.0;
    private static final int PULL_INTERVAL_TICKS = 10;

    private final Set<UUID> linkedEntities = new HashSet<>();

    private SoulLinkHandler() {
        super(
                KevsLibrary.SOUL_LINK_CHANCE,
                null,
                AttributeScaling.builder()
                        .addScaling(KevsLibrary.SOUL_LINK_DAMAGE, 0.01)
                        .baseRatio(1.0)
                        .build()
        );
    }

    public static SoulLinkHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected void execute(EffectContext context) {
        triggerSoulLink(context.getAttacker(), context.getTarget());
    }

    public static void triggerSoulLink(LivingEntity attacker, LivingEntity primaryTarget) {
        INSTANCE.trigger(attacker, primaryTarget);
    }

    public static void tryExtendLink(LivingEntity attacker, LivingEntity fromEntity) {
        INSTANCE.extend(attacker, fromEntity);
    }

    public static void handleLinkedDamage(LivingEntity attacker, LivingEntity damaged, float originalDamage, List<LivingEntity> linked, float soulPower) {
        INSTANCE.applyLinkedDamage(attacker, damaged, originalDamage, linked, soulPower);
    }

    private void trigger(LivingEntity attacker, LivingEntity primaryTarget) {
        ServerWorld world = (ServerWorld) attacker.getWorld();
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

    private void extend(LivingEntity attacker, LivingEntity fromEntity) {
        var opt = SoulLinkTracker.getGroup(fromEntity);
        if (opt.isEmpty()) return;

        var groupData = opt.get();
        List<LivingEntity> group = groupData.group();
        ServerWorld world = (ServerWorld) attacker.getWorld();

        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                fromEntity.getBoundingBox().expand(LINK_RADIUS),
                e -> isValidTarget(e, attacker) &&
                        !group.contains(e) &&
                        !linkedEntities.contains(e.getUuid())
        );

        LivingEntity nearest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(fromEntity)))
                .orElse(null);

        if (nearest == null) return;

        group.add(nearest);
        linkedEntities.add(nearest.getUuid());

        SoulLinkTracker.linkGroup(group, groupData.attacker(), groupData.soulPower(), groupData.handler());
        groupData.handler().extendLifetime();

        playLinkParticles(world, nearest.getPos());
        world.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.6f, 1.0f);
    }

    private void applyLinkedDamage(LivingEntity attacker, LivingEntity damaged, float originalDamage, List<LivingEntity> linked, float soulPower) {
        Set<LivingEntity> alreadyHit = new HashSet<>();
        AttributeContext context = new AttributeContext(attacker);

        for (LivingEntity entity : linked) {
            if (!entity.isAlive()) continue;

            SpellPower.Result soul = SpellPower.getSpellPower(SpellSchools.SOUL, attacker);
            float original = originalDamage;
            soulPower = (float) soul.baseValue();

            float base = 0.1f;
            float maxLinear = 0.5f;
            float postLinearCap = 0.7f;
            float multiplier;

            if (soulPower <= 50.0f) {
                multiplier = base + (soulPower / 50.0f) * (maxLinear - base);
            } else {
                float extraPower = soulPower - 50.0f;
                float diminishing = (float) (1 - Math.exp(-extraPower * 0.05f));
                multiplier = maxLinear + diminishing * (postLinearCap - maxLinear);
            }

            float spreadDamage = original * multiplier;
            boolean isCrit = attacker.getRandom().nextFloat() < soul.criticalChance();
            if (isCrit) spreadDamage *= soul.criticalDamage();

            spreadDamage = DamageScaling.applyGlobalDamageScaling(context, spreadDamage);

            if (alreadyHit.contains(entity)) continue;
            alreadyHit.add(entity);

            entity.damage(attacker.getDamageSources().magic(), spreadDamage);
            Vec3d pos = entity.getPos();
            ((ServerWorld) attacker.getWorld()).spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void playLinkParticles(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 / 24) * i;
            double radius = 2.0;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.5;
            world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.01, 0.01, 0.01, 0.01);
        }
    }

    private boolean isValidTarget(LivingEntity entity, LivingEntity attacker) {
        return entity.isAlive()
                && !entity.equals(attacker)
                && !entity.isTeammate(attacker)
                && (!(entity instanceof PlayerEntity) && entity.getType().getSpawnGroup().isPeaceful())
                && (!(entity instanceof TameableEntity tameable) || !tameable.isTamed());
    }

    public class LinkedGroup {
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

        private void tick() {
            DelayedExecutor.runLater(() -> {
                if (ticks >= BASE_LINK_DURATION_TICKS) {
                    members.forEach(e -> linkedEntities.remove(e.getUuid()));
                    SoulLinkTracker.clearLinks(members);
                    return;
                }

                if (ticks % PULL_INTERVAL_TICKS == 0) pullEntities();
                drawArcs();
                ticks++;
                tick();
            }, 1);
        }

        private void pullEntities() {
            if (members.isEmpty()) return;
            LivingEntity anchor = members.get(0);
            Vec3d center = anchor.getPos();

            for (LivingEntity entity : members) {
                if (!entity.isAlive() || entity.equals(anchor)) continue;
                Vec3d pull = center.subtract(entity.getPos()).normalize().multiply(0.5);
                entity.addVelocity(pull.x, 0.1, pull.z);
            }
        }

        private void drawArcs() {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            Vec3d a = members.get(0).getPos();

            for (LivingEntity b : members) {
                if (!b.isAlive() || b == members.get(0)) continue;
                Vec3d bPos = b.getPos();
                Vec3d diff = bPos.subtract(a);
                for (int i = 0; i <= 10; i++) {
                    Vec3d point = a.add(diff.multiply(i / 10.0));
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y + 1.0, point.z, 1, 0, 0, 0, 0.001);
                }
            }
        }
    }
}