// src/main/java/.../mixin/PersistentProjectileSelfGuardMixin.java
package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.pixeldreamstudios.kevslibrary.handler.ProjectileStormHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileStormMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void kevslib$stormNoSelfHit(EntityHitResult hit, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (!ProjectileStormHandler.isStormTag(self)) return;

        if (self.getOwner() instanceof LivingEntity owner && hit.getEntity() == owner) {
            self.discard();
            ci.cancel();
        }
    }
}
