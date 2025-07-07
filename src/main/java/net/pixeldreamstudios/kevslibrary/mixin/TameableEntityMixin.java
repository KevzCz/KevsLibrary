package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;
import net.pixeldreamstudios.kevslibrary.util.AttributeInheritanceUtil;
import net.pixeldreamstudios.kevslibrary.util.UuidsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
@SuppressWarnings("unused")
@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin extends AnimalEntity implements UniversalTameable {

    @Unique
    private NbtCompound kevslib$petInheritanceData = new NbtCompound();

    protected TameableEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void onSetOwner(PlayerEntity player, CallbackInfo ci) {
        TameableEntity tameable = (TameableEntity) (Object) this;
        if (tameable.isTamed()) {
            this.kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                    player,
                    tameable,
                    this.kevslib$petInheritanceData,
                    player.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
            );
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readInheritanceData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("petinheritance")) {
            kevslib$petInheritanceData = nbt.getCompound("petinheritance");
        }

        TameableEntity tameable = (TameableEntity)(Object)this;
        if (tameable.isTamed() && tameable.getOwnerUuid() != null && this.getWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(tameable.getOwnerUuid());
            if (owner != null) {
                this.kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                        owner,
                        tameable,
                        this.kevslib$petInheritanceData,
                        owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
                );
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void kevslib$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID ownerUuid = kevslib$getOwnerUuid();
        if (kevslib$isTamed() && ownerUuid != null) {
            nbt.putIntArray("Owner", UuidsHelper.toIntArray(ownerUuid));
        }

        if (!kevslib$petInheritanceData.isEmpty()) {
            nbt.put("petinheritance", kevslib$petInheritanceData);
        }
    }

    @Unique
    @Override
    public boolean kevslib$isTamed() {
        if ((Object)this instanceof SkeletonHorseEntity || (Object)this instanceof ZombieHorseEntity) {
            return false;
        }
        return ((TameableEntity)(Object)this).isTamed();
    }

    @Unique
    @Override
    public java.util.UUID kevslib$getOwnerUuid() {
        return ((TameableEntity)(Object)this).getOwnerUuid();
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
