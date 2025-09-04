package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.handler.ProjectileStormHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void kevslib$stormRemoveOnBlockHit(BlockHitResult hit, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        if (self instanceof PersistentProjectileEntity ppe && ProjectileStormHandler.isStormTag(ppe)) {
            ppe.discard();
            ci.cancel();
        }
    }
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void kevslib$stormOnEntityHit(EntityHitResult hit, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        if (!(self instanceof PersistentProjectileEntity ppe)) return;
        if (!(self.getWorld().isClient())) {
            if (!ProjectileStormHandler.isStormTag(ppe)) {
                LivingEntity victim = hit.getEntity() instanceof LivingEntity le ? le : null;
                if (victim != null) {
                    ProjectileStormHandler.tryTrigger(ppe, victim.getPos(), victim);
                }
            }
        }
    }
    @Inject(method = "onBlockHit", at = @At("TAIL"))
    private void kevslib$stormOnBlockHit(BlockHitResult hit, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        if (!(self instanceof PersistentProjectileEntity ppe)) return;
        if (!(self.getWorld().isClient())) {
            if (!ProjectileStormHandler.isStormTag(ppe)) {
                Vec3d center = Vec3d.ofCenter(hit.getBlockPos());
                ProjectileStormHandler.tryTrigger(ppe, center, null);
            }
        }
    }
}
