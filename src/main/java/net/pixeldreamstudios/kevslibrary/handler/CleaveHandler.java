package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeScaling;
import net.pixeldreamstudios.kevslibrary.attribute.DamageScaling;
import net.pixeldreamstudios.kevslibrary.attribute.EffectHandler;
import net.pixeldreamstudios.kevslibrary.entity.CleaveSlashEntity;

import java.util.List;

public class CleaveHandler extends EffectHandler {

    private static final CleaveHandler INSTANCE = new CleaveHandler();

    private CleaveHandler() {
        super(
                KevsLibrary.CLEAVE_CHANCE,
                null,
                AttributeScaling.builder()
                        .addScaling(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER, 0.01)
                        .baseRatio(1.0)
                        .build()
        );
    }

    public static CleaveHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected void execute(EffectContext effectContext) {
        triggerCleave(effectContext);
    }

    private void triggerCleave(EffectContext effectContext) {
        LivingEntity attacker = effectContext.getAttacker();
        ServerWorld world = effectContext.getWorld();
        AttributeContext context = effectContext.getAttackerContext();

        double rangeValue = context.getAttributeValue(KevsLibrary.CLEAVE_RANGE);
        double range = rangeValue;

        Vec3d forward = attacker.getRotationVec(1.0F).normalize().multiply(1.5);
        double yOffset = 0.6;

        int lifespanTicks = Math.max(4, (int)(range * 5));

        CleaveSlashEntity slash = new CleaveSlashEntity(KevsLibrary.CLEAVE_SLASH, world);
        slash.setOwner(attacker);
        slash.setLifespan(lifespanTicks);
        slash.refreshPositionAndAngles(
                attacker.getX() + forward.x,
                attacker.getY() + yOffset,
                attacker.getZ() + forward.z,
                attacker.getYaw(),
                0
        );
        slash.setVelocity(forward.multiply(0.4));

        world.spawnEntity(slash);

        List<LivingEntity> hitTargets = world.getEntitiesByClass(
                LivingEntity.class,
                attacker.getBoundingBox().expand(range),
                target -> {
                    if (target == attacker || !target.isAlive()) return false;
                    Vec3d origin = attacker.getPos();
                    Vec3d toTarget = target.getPos().subtract(origin);
                    if (toTarget.lengthSquared() > range * range) return false;

                    Vec3d look = attacker.getRotationVec(1.0F);
                    double angleBetween = Math.toDegrees(Math.acos(look.dotProduct(toTarget.normalize())));
                    return angleBetween <= 90.0 / 2.0;
                }
        );

        double multiplierValue = context.getAttributeValue(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER);
        float multiplier = (float) ((multiplierValue - 100.0) / 100.0 + 1.0);

        float baseDamage = effectContext.getBaseDamage();
        float cleaveDamage = DamageScaling.applyGlobalDamageScaling(context, baseDamage * multiplier);

        for (LivingEntity target : hitTargets) {
            if (attacker instanceof net.minecraft.entity.player.PlayerEntity player) {
                target.damage(player.getDamageSources().playerAttack(player), cleaveDamage);
            } else {
                target.damage(attacker.getDamageSources().mobAttack(attacker), cleaveDamage);
            }
        }
    }
}