package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;
import net.pixeldreamstudios.kevslibrary.util.AttributeInheritanceUtil;
import net.pixeldreamstudios.kevslibrary.util.UuidsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin implements UniversalTameable {

    @Unique
    private NbtCompound kevslib$petInheritanceData = new NbtCompound();

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        if (nbt.contains("petinheritance")) {
            kevslib$petInheritanceData = nbt.getCompound("petinheritance");
        }

        AbstractHorseEntity horse = (AbstractHorseEntity)(Object)this;
        if (horse.isTame() && horse.getOwnerUuid() != null && horse.getWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(horse.getOwnerUuid());
            if (owner != null && KevsLibrary.PET_INHERITANCE_RATIO != null) {
                kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                        owner,
                        horse,
                        kevslib$petInheritanceData,
                        owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
                );
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void kevslib$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        UUID ownerUuid = kevslib$getOwnerUuid();
        if (kevslib$isTamed() && ownerUuid != null) {
            nbt.putIntArray("Owner", UuidsHelper.toIntArray(ownerUuid));
        }

        if (!kevslib$petInheritanceData.isEmpty()) {
            nbt.put("petinheritance", kevslib$petInheritanceData);
        }
    }

    @Inject(method = "bondWithPlayer", at = @At("TAIL"))
    private void onBondWithPlayer(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        if (cir.getReturnValue() && KevsLibrary.PET_INHERITANCE_RATIO != null) {
            AbstractHorseEntity horse = (AbstractHorseEntity)(Object)this;
            kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                    player,
                    horse,
                    kevslib$petInheritanceData,
                    player.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
            );
        }
    }

    @Unique
    @Override
    public boolean kevslib$isTamed() {
        if ((Object)this instanceof SkeletonHorseEntity || (Object)this instanceof ZombieHorseEntity) {
            return false;
        }
        return ((AbstractHorseEntity)(Object)this).isTame();
    }

    @Unique
    @Override
    public java.util.UUID kevslib$getOwnerUuid() {
        return ((AbstractHorseEntity)(Object)this).getOwnerUuid();
    }

    @Unique
    @Override
    public void kevslib$setInheritanceData(NbtCompound data) {
        this.kevslib$petInheritanceData = data;
    }

    @Unique
    @Override
    public NbtCompound kevslib$getInheritanceData() {
        return this.kevslib$petInheritanceData;
    }
}