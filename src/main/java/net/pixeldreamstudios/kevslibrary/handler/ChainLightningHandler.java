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
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ChainLightningHandler extends EffectHandler {

    private static final ChainLightningHandler INSTANCE = new ChainLightningHandler();
    private static final double MAX_DISTANCE = 8.0;

    private ChainLightningHandler() {
        super(
                KevsLibrary.CHAIN_LIGHTNING_CHANCE,
                KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE,
                AttributeScaling.builder()
                        .baseRatio(1.0)
                        .build()
        );
    }

    public static ChainLightningHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected void execute(EffectContext context) {
        spawnChainLightning(context, false);
    }

    @Override
    protected void executeOverload(EffectContext context) {
        spawnChainLightning(context, true);
    }

    private void spawnChainLightning(EffectContext effectContext, boolean isOverload) {
        LivingEntity attacker = effectContext.getAttacker();
        LivingEntity initialTarget = effectContext.getTarget();
        ServerWorld world = effectContext.getWorld();
        AttributeContext context = effectContext.getAttackerContext();

        double countValue = context.getAttributeValue(KevsLibrary.CHAIN_LIGHTNING_COUNT);
        int bounceCount = (int) countValue;

        SpellPower.Result spellResult = SpellPower.getSpellPower(SpellSchools.LIGHTNING, attacker);
        float baseDamage = 3.0f + (float) spellResult.baseValue();

        Set<LivingEntity> visited = new HashSet<>();
        visited.add(attacker);
        visited.add(initialTarget);

        spawnArcParticles(world, attacker, initialTarget);
        world.playSound(null, initialTarget.getX(), initialTarget.getY(), initialTarget.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.2f, 0.2f);

        boolean isCrit = attacker.getRandom().nextDouble() < spellResult.criticalChance();
        float critApplied = isCrit ? baseDamage * (float) spellResult.criticalDamage() : baseDamage;
        float damage = DamageScaling.applyGlobalDamageScaling(context, critApplied);

        initialTarget.damage(attacker.getDamageSources().magic(), damage);
        if (isCrit) {
            world.spawnParticles(ParticleTypes.CRIT, initialTarget.getX(), initialTarget.getY() + 1.0, initialTarget.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
        }

        if (isOverload) {
            spawnOverloadVisuals(world, initialTarget.getPos());
            triggerOverloadDoT(attacker, initialTarget, damage * 0.2f, bounceCount);
        }

        Queue<LivingEntity> queue = new LinkedList<>();
        queue.add(initialTarget);
        int jumps = 0;

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

            boolean nextCrit = attacker.getRandom().nextDouble() < spellResult.criticalChance();
            float nextCritApplied = nextCrit ? baseDamage * (float) spellResult.criticalDamage() : baseDamage;
            float nextDamage = DamageScaling.applyGlobalDamageScaling(context, nextCritApplied);

            next.damage(attacker.getDamageSources().magic(), nextDamage);

            SoulLinkTracker.getGroup(next).ifPresent(linkData -> {
                if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                SoulLinkHandler.handleLinkedDamage(linkData.attacker(), next, nextDamage, linkData.group(), linkData.soulPower());
            });

            if (nextCrit) {
                world.spawnParticles(ParticleTypes.CRIT, next.getX(), next.getY() + 1.0, next.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            }

            if (isOverload) {
                spawnOverloadVisuals(world, next.getPos());
                triggerOverloadDoT(attacker, next, nextDamage * 0.2f, bounceCount);
            }
        }
    }

    private boolean isValidBounceTarget(LivingEntity entity, LivingEntity attacker, Set<LivingEntity> visited) {
        if (!entity.isAlive()) return false;
        if (entity.equals(attacker)) return false;
        if (visited.contains(entity)) return false;
        if (entity.isTeammate(attacker)) return false;
        if (!(entity instanceof PlayerEntity) && entity.getType().getSpawnGroup().isPeaceful()) return false;
        if (entity.getType().getSpawnGroup().isPeaceful()) return false;
        return !(entity instanceof TameableEntity tameable) || !tameable.isTamed();
    }

    private void spawnArcParticles(ServerWorld world, LivingEntity from, LivingEntity to) {
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

    private void spawnOverloadVisuals(ServerWorld world, Vec3d pos) {
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.8f);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 10, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.01);
    }

    private void triggerOverloadDoT(LivingEntity attacker, LivingEntity target, float tickDamage, int ticks) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}

                if (!target.isAlive()) return;

                Vec3d pos = target.getPos();
                world.getServer().execute(() -> {
                    world.spawnParticles(ParticleTypes.CRIT,
                            pos.x, pos.y + 1.0, pos.z,
                            3, 0.2, 0.3, 0.2, 0.01);
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.x, pos.y + 0.5, pos.z,
                            2, 0.1, 0.2, 0.1, 0.01);
                });
            }

            for (int i = 0; i < ticks; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}

                if (!target.isAlive()) return;

                world.getServer().execute(() -> {
                    target.timeUntilRegen = 0;
                    target.hurtTime = 0;
                    target.damage(attacker.getDamageSources().magic(), tickDamage);
                    SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                        if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                        SoulLinkHandler.handleLinkedDamage(linkData.attacker(), target, tickDamage, linkData.group(), linkData.soulPower());
                    });

                    world.spawnParticles(ParticleTypes.END_ROD,
                            target.getX(), target.getY() + 1.2, target.getZ(),
                            6, 0.3, 0.4, 0.3, 0.01);
                });
            }
        }).start();
    }
}