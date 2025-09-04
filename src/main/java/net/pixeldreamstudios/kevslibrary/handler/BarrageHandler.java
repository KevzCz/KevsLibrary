package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;

public final class BarrageHandler {
    private BarrageHandler() {}
    private static final int EXTRA_SHOTS = 2;
    private static final int TICKS_BETWEEN_SHOTS = 3;

    public static void tryBarrage(LivingEntity shooter, ItemStack weaponStack, ItemStack projectileTemplate,
                                  float baseSpeed, float baseDivergence) {
        if (!(shooter.getWorld() instanceof ServerWorld world)) return;

        EntityAttributeInstance chanceAttr = shooter.getAttributeInstance(KevsLibrary.BARRAGE_CHANCE);
        double chance = chanceAttr != null ? chanceAttr.getValue() : 0.0;
        if (chance <= 0.0) return;
        if (world.getRandom().nextDouble() > chance) return;

        if (projectileTemplate == null || projectileTemplate.isEmpty()) {
            projectileTemplate = new ItemStack(Items.ARROW);
        }

        for (int i = 0; i < EXTRA_SHOTS; i++) {
            final int delay = (i + 1) * TICKS_BETWEEN_SHOTS;
            final ItemStack projCopy = projectileTemplate.copy();
            DelayedExecutor.runLater(() -> spawnFromTemplate(world, shooter, weaponStack, projCopy, baseSpeed, baseDivergence), delay);
        }
    }

    private static void spawnFromTemplate(ServerWorld world, LivingEntity shooter, ItemStack weaponStack,
                                          ItemStack projectileStack, float speed, float divergence) {
        if (!shooter.isAlive()) return;

        if (projectileStack.isOf(Items.FIREWORK_ROCKET)) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    world, projectileStack, shooter,
                    shooter.getX(), shooter.getEyeY() - 0.15D, shooter.getZ(), true
            );
            Vec3d dir = shooter.getRotationVec(1.0F);
            rocket.setVelocity(dir.x, dir.y, dir.z, 1.6F, divergence);
            world.spawnEntity(rocket);
            return;
        }

        if (projectileStack.getItem() instanceof ArrowItem arrowItem) {
            PersistentProjectileEntity arrow = arrowItem.createArrow(world, projectileStack, shooter, weaponStack);

            arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;

            int power = getLevelOn(shooter, Enchantments.POWER, weaponStack);
            if (power > 0) arrow.setDamage(arrow.getDamage() + 0.5D * power + 0.5D);

            if (getLevelOn(shooter, Enchantments.FLAME, weaponStack) > 0) {
                arrow.setOnFireFor(100);
            }

            float pitch = shooter.getPitch();
            float yaw   = shooter.getYaw();
            arrow.setVelocity(shooter, pitch, yaw, 0.0F, speed, divergence);

            if (speed >= 2.9F && arrow instanceof ArrowEntity ae) {
                ae.setCritical(true);
            }

            world.spawnEntity(arrow);
        }
    }

    private static int getLevelOn(LivingEntity ctx, RegistryKey<Enchantment> key, ItemStack stack) {
        return ctx.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack))
                .orElse(0);
    }

    public static float bowSpeedFromPull(float pullProgress) {
        return MathHelper.clamp(pullProgress, 0.0F, 1.0F) * 3.0F;
    }

    public static float crossbowBaseSpeed() {
        return 3.15F;
    }
}
