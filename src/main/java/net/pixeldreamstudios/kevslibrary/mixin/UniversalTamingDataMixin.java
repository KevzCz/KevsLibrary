package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.taming.TameableBridge;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;
import net.pixeldreamstudios.kevslibrary.util.AttributeInheritanceUtil;
import net.pixeldreamstudios.kevslibrary.util.UuidsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class UniversalTamingDataMixin implements UniversalTameable, TameableBridge {

    @Unique
    private NbtCompound kevslib$petInheritanceData = new NbtCompound();
    @Override
    public void kevslib$setInheritanceData(NbtCompound tag) {
        kevslib$petInheritanceData = tag;
    }
    @Unique
    private static final TrackedData<Optional<UUID>> KEVSLIB_OWNER =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    @Override
    public NbtCompound kevslib$getInheritanceData() {
        return kevslib$petInheritanceData;
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void kevslib$initTrackedData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(KEVSLIB_OWNER, Optional.empty());
    }

    @Override
    public UUID kevslib$getOwnerUuid() {
        return ((MobEntity)(Object)this).getDataTracker().get(KEVSLIB_OWNER).orElse(null);
    }

    @Override
    public void kevslib$setOwnerUuid(UUID uuid) {
        MobEntity mob = (MobEntity)(Object)this;
        mob.getDataTracker().set(KEVSLIB_OWNER, Optional.ofNullable(uuid));

        if (uuid != null && mob.getWorld() instanceof ServerWorld sw) {
            PlayerEntity owner = sw.getPlayerByUuid(uuid);
            if (owner != null) {
                kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                        owner,
                        mob,
                        kevslib$petInheritanceData,
                        owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
                );
            }
        }
    }

    @Override
    public boolean kevslib$isTamed() {
        return kevslib$getOwnerUuid() != null;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void kevslib$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("Owner", NbtElement.INT_ARRAY_TYPE)) {
            kevslib$setOwnerUuid(UuidsHelper.fromIntArray(nbt.getIntArray("Owner")));
        }

        if (nbt.contains("petinheritance")) {
            kevslib$petInheritanceData = nbt.getCompound("petinheritance");
        }

        MobEntity mob = (MobEntity)(Object)this;
        if (kevslib$isTamed() && mob.getWorld() instanceof ServerWorld sw) {
            PlayerEntity owner = sw.getPlayerByUuid(kevslib$getOwnerUuid());
            if (owner != null) {
                kevslib$petInheritanceData = AttributeInheritanceUtil.apply(
                        owner,
                        mob,
                        kevslib$petInheritanceData,
                        owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO)
                );
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void kevslib$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID ownerUuid = kevslib$getOwnerUuid();
        if (ownerUuid != null) {
            nbt.putIntArray("Owner", UuidsHelper.toIntArray(ownerUuid));
        }
        if (!kevslib$petInheritanceData.isEmpty()) {
            nbt.put("petinheritance", kevslib$petInheritanceData);
        }
    }

}
