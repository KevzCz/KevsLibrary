package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"), cancellable = true)
    private static void injectCustomAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
                cir.getReturnValue()
                        .add(KevsLibrary.CRIT_CHANCE, 0.0)
                        .add(KevsLibrary.CRIT_DAMAGE, 1)
                        .add(KevsLibrary.MULTISTRIKE_CHANCE, 0.0)
                        .add(KevsLibrary.MULTISTRIKE_COUNT, 1.0)
                        .add(KevsLibrary.MULTISTRIKE_DAMAGE, 0.5)
                        .add(KevsLibrary.DAMAGE, 1)
                        .add(KevsLibrary.CHAIN_LIGHTNING_CHANCE, 0)
                        .add(KevsLibrary.CHAIN_LIGHTNING_COUNT, 3.0)
                        .add(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE, 0.0)
                        .add(KevsLibrary.FIRE_TORNADO_CHANCE, 0)
                        .add(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE, 0.0)
                        .add(KevsLibrary.FROST_NOVA_CHANCE, 0)
                        .add(KevsLibrary.FROST_NOVA_COUNT, 1)
                        .add(KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE, 0.0)
                        .add(KevsLibrary.PET_INHERITANCE_RATIO, 0)



        );
    }
}
