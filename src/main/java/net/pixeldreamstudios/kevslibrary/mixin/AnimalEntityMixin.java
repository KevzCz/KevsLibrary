package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;
import net.pixeldreamstudios.kevslibrary.util.AttributeInheritanceUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin {

    @Inject(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V",
            at = @At("TAIL"))
    private void onBreed(ServerWorld world, AnimalEntity other, @Nullable PassiveEntity baby, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;
        if (baby == null) return;

        AnimalEntity parent1 = (AnimalEntity)(Object)this;

        UUID ownerUuid = null;

        if (parent1 instanceof TameableEntity tameable1 && tameable1.isTamed()) {
            ownerUuid = tameable1.getOwnerUuid();
        } else if (parent1 instanceof UniversalTameable universalTameable1 && universalTameable1.kevslib$isTamed()) {
            ownerUuid = universalTameable1.kevslib$getOwnerUuid();
        }

        if (ownerUuid == null && other instanceof TameableEntity tameable2 && tameable2.isTamed()) {
            ownerUuid = tameable2.getOwnerUuid();
        } else if (ownerUuid == null && other instanceof UniversalTameable universalTameable2 && universalTameable2.kevslib$isTamed()) {
            ownerUuid = universalTameable2.kevslib$getOwnerUuid();
        }

        if (ownerUuid == null) return;

        if (baby instanceof TameableEntity tameableBaby) {
            tameableBaby.setOwnerUuid(ownerUuid);
            tameableBaby.setTamed(true, false);

            var owner = world.getPlayerByUuid(ownerUuid);
            if (owner != null && KevsLibrary.PET_INHERITANCE_RATIO != null) {
                AttributeContext context = new AttributeContext(owner);
                double ratioValue = context.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
                double ratio = (ratioValue - 100.0) / 100.0;

                if (ratio > 0 && baby instanceof UniversalTameable universalBaby) {
                    var inheritanceData = AttributeInheritanceUtil.apply(
                            owner,
                            tameableBaby,
                            universalBaby.kevslib$getInheritanceData(),
                            ratio
                    );
                    universalBaby.kevslib$setInheritanceData(inheritanceData);
                }
            }
        } else if (baby instanceof UniversalTameable universalBaby) {
            universalBaby.kevslib$setOwnerUuid(ownerUuid);

            var owner = world.getPlayerByUuid(ownerUuid);
            if (owner != null && KevsLibrary.PET_INHERITANCE_RATIO != null) {
                AttributeContext context = new AttributeContext(owner);
                double ratioValue = context.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
                double ratio = (ratioValue - 100.0) / 100.0;

                if (ratio > 0) {
                    var inheritanceData = AttributeInheritanceUtil.apply(
                            owner,
                            baby,
                            universalBaby.kevslib$getInheritanceData(),
                            ratio
                    );
                    universalBaby.kevslib$setInheritanceData(inheritanceData);
                }
            }
        }
    }
}