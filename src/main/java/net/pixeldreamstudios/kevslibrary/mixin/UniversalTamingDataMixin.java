package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.taming.TameableBridge;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;
import net.pixeldreamstudios.kevslibrary.util.UuidsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class UniversalTamingDataMixin implements UniversalTameable, TameableBridge {

    private static final TrackedData<Optional<UUID>> KEVSLIB_OWNER =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private static final Set<RegistryEntry<EntityAttribute>> INHERITABLE_ATTRIBUTES = Set.of(
            EntityAttributes.GENERIC_MAX_HEALTH,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributes.GENERIC_SCALE,
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
            EntityAttributes.GENERIC_ARMOR,
            EntityAttributes.GENERIC_MOVEMENT_SPEED
    );

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
            if (owner != null) applyAttributeInheritance(owner, mob);
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
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void kevslib$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (kevslib$isTamed()) {
            nbt.putIntArray("Owner", UuidsHelper.toIntArray(kevslib$getOwnerUuid()));
        }
    }

    private void applyAttributeInheritance(PlayerEntity owner, MobEntity pet) {
        double ratio = owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
        for (RegistryEntry<EntityAttribute> attribute : INHERITABLE_ATTRIBUTES) {
            EntityAttributeInstance ownerAttr = owner.getAttributeInstance(attribute);
            if (ownerAttr == null) continue;

            double inherited = ownerAttr.getValue() * ratio;
            EntityAttributeInstance petAttr = pet.getAttributeInstance(attribute);
            if (petAttr != null) {
                petAttr.setBaseValue(petAttr.getBaseValue() + inherited);
                if (attribute.value().equals(EntityAttributes.GENERIC_MAX_HEALTH)) {
                    pet.setHealth((float) pet.getAttributeValue(attribute));
                }
            }
        }
    }


}
