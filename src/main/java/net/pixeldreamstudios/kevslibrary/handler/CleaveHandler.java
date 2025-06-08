package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.entity.CleaveSlashEntity;

import java.util.List;

public class CleaveHandler {

    public static void triggerCleave(LivingEntity attacker, float baseDamageDealt) {
        World world = attacker.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        double cleaveChance = attacker.getAttributeValue(KevsLibrary.CLEAVE_CHANCE);
        if (attacker.getRandom().nextDouble() > cleaveChance) return;

        double range = attacker.getAttributeValue(KevsLibrary.CLEAVE_RANGE);
        double multiplier = attacker.getAttributeValue(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER);

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

        List<LivingEntity> hitTargets = serverWorld.getEntitiesByClass(
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

        float cleaveDamage = baseDamageDealt * (float) multiplier;

        for (LivingEntity target : hitTargets) {
            if (attacker instanceof net.minecraft.entity.player.PlayerEntity player) {
                target.damage(player.getDamageSources().playerAttack(player), cleaveDamage);
            } else {
                target.damage(attacker.getDamageSources().mobAttack(attacker), cleaveDamage);
            }
        }


    }


}
