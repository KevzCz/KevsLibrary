package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
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

    @Unique
    private static final TrackedData<Optional<UUID>> KEVSLIB_OWNER =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    @Override
    public void kevslib$setInheritanceData(NbtCompound tag) {
        kevslib$petInheritanceData = tag;
    }

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
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        MobEntity mob = (MobEntity)(Object)this;
        mob.getDataTracker().set(KEVSLIB_OWNER, Optional.ofNullable(uuid));

        if (uuid != null && mob.getWorld() instanceof ServerWorld sw && KevsLibrary.PET_INHERITANCE_RATIO != null) {
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
        if ((Object)this instanceof SkeletonHorseEntity || (Object)this instanceof ZombieHorseEntity) {
            return false;
        }
        return kevslib$getOwnerUuid() != null;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void kevslib$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        UUID parsed = null;

        if (nbt.contains("Owner", NbtElement.INT_ARRAY_TYPE)) {
            parsed = UuidsHelper.fromIntArray(nbt.getIntArray("Owner"));
        }
        else if (nbt.contains("OwnerUUID", NbtElement.INT_ARRAY_TYPE)) {
            parsed = UuidsHelper.fromIntArray(nbt.getIntArray("OwnerUUID"));
        }
        else if (nbt.contains("OwnerUUID", NbtElement.STRING_TYPE)) {
            try {
                parsed = java.util.UUID.fromString(nbt.getString("OwnerUUID"));
            } catch (IllegalArgumentException ignored) { }
        } else if (nbt.contains("Owner", NbtElement.STRING_TYPE)) {
            try {
                parsed = java.util.UUID.fromString(nbt.getString("Owner"));
            } catch (IllegalArgumentException ignored) { }
        }

        if (parsed != null) {
            kevslib$setOwnerUuid(parsed);
        }

        if (nbt.contains("petinheritance")) {
            kevslib$petInheritanceData = nbt.getCompound("petinheritance");
        }

        MobEntity mob = (MobEntity)(Object)this;
        if (kevslib$isTamed() && mob.getWorld() instanceof ServerWorld sw && KevsLibrary.PET_INHERITANCE_RATIO != null) {
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
        if (!KevsLibraryConfig.getInstance().isPetInheritanceEnabled()) return;

        UUID ownerUuid = kevslib$getOwnerUuid();
        if (kevslib$isTamed() && ownerUuid != null) {
            nbt.putIntArray("Owner", UuidsHelper.toIntArray(ownerUuid));
        }

        if (!kevslib$petInheritanceData.isEmpty()) {
            nbt.put("petinheritance", kevslib$petInheritanceData);
        }
    }
}