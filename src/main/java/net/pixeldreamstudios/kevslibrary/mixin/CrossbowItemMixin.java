package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.handler.BarrageHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    private static final ThreadLocal<ItemStack> KEVSLIB_LAST_CROSSBOW_PROJECTILE = new ThreadLocal<>();

    @Inject(
            method = "shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD")
    )
    private void kevslib$captureCrossbowProjectile(World world,
                                                   LivingEntity shooter,
                                                   Hand hand,
                                                   ItemStack stack,
                                                   float speed,
                                                   float divergence,
                                                   @Nullable LivingEntity target,
                                                   CallbackInfo ci) {
        if (world.isClient()) return;
        ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged != null && !charged.isEmpty()) {
            List<ItemStack> list = charged.getProjectiles();
            KEVSLIB_LAST_CROSSBOW_PROJECTILE.set(list.isEmpty() ? ItemStack.EMPTY : list.get(0).copy());
        } else {
            KEVSLIB_LAST_CROSSBOW_PROJECTILE.set(ItemStack.EMPTY);
        }
    }

    @Inject(
            method = "shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("TAIL")
    )
    private void kevslib$barrageOnCrossbowShoot(World world,
                                                LivingEntity shooter,
                                                Hand hand,
                                                ItemStack stack,
                                                float speed,
                                                float divergence,
                                                @Nullable LivingEntity target,
                                                CallbackInfo ci) {
        if (world.isClient()) return;
        ItemStack projectileTemplate = KEVSLIB_LAST_CROSSBOW_PROJECTILE.get();
        KEVSLIB_LAST_CROSSBOW_PROJECTILE.remove();

        float baseSpeed = speed > 0.0f ? speed : BarrageHandler.crossbowBaseSpeed();
        BarrageHandler.tryBarrage(shooter, stack, projectileTemplate, baseSpeed, divergence);
    }
}
