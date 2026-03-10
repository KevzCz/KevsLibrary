package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.handler.BarrageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    private static final ThreadLocal<ItemStack> KEVSLIB_LAST_BOW_PROJECTILE = new ThreadLocal<>();

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void kevslib$captureBowProjectile(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!KevsLibraryConfig.getInstance().isBarrageEnabled()) return;

        if (user instanceof PlayerEntity p) {
            KEVSLIB_LAST_BOW_PROJECTILE.set(p.getProjectileType(stack).copy());
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void kevslib$barrageOnBowRelease(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!KevsLibraryConfig.getInstance().isBarrageEnabled()) return;

        int used = ((BowItem)(Object)this).getMaxUseTime(stack, user) - remainingUseTicks;
        float pull = BowItem.getPullProgress(used);
        float speed = BarrageHandler.bowSpeedFromPull(pull);
        if (speed <= 0.1f) {
            KEVSLIB_LAST_BOW_PROJECTILE.remove();
            return;
        }

        ItemStack projectileTemplate = KEVSLIB_LAST_BOW_PROJECTILE.get();
        KEVSLIB_LAST_BOW_PROJECTILE.remove();
        if (projectileTemplate == null) projectileTemplate = ItemStack.EMPTY;

        BarrageHandler.getInstance().tryBarrage(user, stack, projectileTemplate, speed, 1.0f);
    }
}