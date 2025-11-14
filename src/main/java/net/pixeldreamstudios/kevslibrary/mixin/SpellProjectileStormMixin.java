package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.spell_engine.entity.SpellProjectile;
import net.pixeldreamstudios.kevslibrary.handler.ProjectileStormHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellProjectile.class)
public abstract class SpellProjectileStormMixin {

    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void kevslib$stormOnEntity(EntityHitResult hit, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isProjectileStormEnabled()) return;

        SpellProjectile self = (SpellProjectile) (Object) this;
        ProjectileStormHandler.tryTrigger(self, hit.getPos(), hit.getEntity() instanceof LivingEntity l ? l : null);
    }

    @Inject(method = "onBlockHit", at = @At("TAIL"))
    private void kevslib$stormOnBlock(BlockHitResult hit, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isProjectileStormEnabled()) return;

        SpellProjectile self = (SpellProjectile) (Object) this;
        ProjectileStormHandler.tryTrigger(self, hit.getPos(), null);
    }
}