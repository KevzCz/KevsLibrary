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
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
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
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        TameableEntity tameable = (TameableEntity) (Object) this;
        if (!tameable.isTamed()) {
            return;
        }

        if (KevsLibrary.PET_INHERITANCE_RATIO == null) return;

        AttributeContext context = new AttributeContext(player);
        double ratioValue = context.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
        double ratio = (ratioValue - 100.0) / 100.0;

        if (ratio == 0.0) {
            return;
        }

        this.kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                player,
                tameable,
                this.kevslib$petInheritanceData,
                ratio
        );
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readInheritanceData(NbtCompound nbt, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        if (nbt.contains("petinheritance")) {
            kevslib$petInheritanceData = nbt.getCompound("petinheritance");
        }

        TameableEntity tameable = (TameableEntity)(Object)this;
        if (tameable.isTamed() && tameable.getOwnerUuid() != null &&
                this.getWorld() instanceof ServerWorld serverWorld &&
                KevsLibrary.PET_INHERITANCE_RATIO != null) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(tameable.getOwnerUuid());
            if (owner != null) {
                AttributeContext context = new AttributeContext(owner);
                double ratioValue = context.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
                double ratio = (ratioValue - 100.0) / 100.0;

                this.kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                        owner,
                        tameable,
                        this.kevslib$petInheritanceData,
                        ratio
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
    public UUID kevslib$getOwnerUuid() {
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