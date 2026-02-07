package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Unique
    private PlayerEntity kevslibrary$player;

    @Inject(method = "update", at = @At("HEAD"))
    private void capturePlayer(PlayerEntity player, CallbackInfo ci) {
        this.kevslibrary$player = player;
    }

    @ModifyVariable(
            method = "addExhaustion",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float modifyExhaustion(float exhaustion) {
        if (!KevsLibraryConfig.getInstance().isHungerEnabled()) {
            return exhaustion;
        }

        if (kevslibrary$player != null) {
            AttributeContext context = new AttributeContext(kevslibrary$player);
            double hungerValue = context.getAttributeValue(KevsLibrary.HUNGER_CONSUMPTION);
            float multiplier = (float) ((hungerValue - 100.0) / 100.0 + 1.0);
            return exhaustion * multiplier;
        }

        return exhaustion;
    }
}