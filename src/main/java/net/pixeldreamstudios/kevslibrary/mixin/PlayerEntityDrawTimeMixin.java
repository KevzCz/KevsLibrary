package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.handler.WeaponSwitchHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntity.class, priority = 1500)
public abstract class PlayerEntityDrawTimeMixin {

    @Unique
    private ItemStack kevslibrary$lastTrackedItem = ItemStack.EMPTY;

    @Unique
    private boolean kevslibrary$justSwitched = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void trackItemSwitch(CallbackInfo ci) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        if (!config.isAttributeEnabled("draw_time") && !config.isAttributeEnabled("first_hit_damage_multiplier")) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack currentItem = player.getMainHandStack();

        if (!ItemStack.areItemsEqual(kevslibrary$lastTrackedItem, currentItem)) {
            WeaponSwitchHandler.getInstance().onItemSwitch(player, currentItem);
            kevslibrary$lastTrackedItem = currentItem.copy();
            kevslibrary$justSwitched = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void modifyTickCounter(CallbackInfo ci) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        if (!config.isAttributeEnabled("draw_time") || KevsLibrary.DRAW_TIME == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        WeaponSwitchHandler handler = WeaponSwitchHandler.getInstance();

        if (!handler.canApplyDrawTime(player)) {
            return;
        }

        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
        int currentTicks = accessor.getLastAttackedTicks();

        if (currentTicks > 0) {
            AttributeContext context = new AttributeContext(player);
            double drawTimeValue = context.getAttributeValue(KevsLibrary.DRAW_TIME);
            float drawTimeMultiplier = (float) (drawTimeValue / 100.0);

            if (drawTimeMultiplier < 1.0f) {
                float extraTicksPerTick = (1.0f / drawTimeMultiplier) - 1.0f;
                int ticksToAdd = (int) extraTicksPerTick;

                if (extraTicksPerTick % 1.0f > 0 && player.getRandom().nextFloat() < (extraTicksPerTick % 1.0f)) {
                    ticksToAdd++;
                }

                accessor.setLastAttackedTicks(currentTicks + ticksToAdd);
            } else if (drawTimeMultiplier > 1.0f) {
                if (player.age % (int) Math.ceil(drawTimeMultiplier) != 0) {
                    accessor.setLastAttackedTicks(currentTicks - 1);
                }
            }
        }
    }

    @Inject(method = "resetLastAttackedTicks", at = @At("HEAD"))
    private void consumeDrawTimeOnAttack(CallbackInfo ci) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        if (!config.isAttributeEnabled("draw_time") || KevsLibrary.DRAW_TIME == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;

        float currentProgress = player.getAttackCooldownProgress(0.0F);

        if (!kevslibrary$justSwitched) {
            WeaponSwitchHandler.getInstance().consumeDrawTime(player);
        } else {
            kevslibrary$justSwitched = false;
        }
    }
}