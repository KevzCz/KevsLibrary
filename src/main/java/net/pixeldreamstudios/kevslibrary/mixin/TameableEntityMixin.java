package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin extends AnimalEntity {

    protected TameableEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }

     @Inject(method = "setOwner", at = @At("TAIL"))
    private void onSetOwner(PlayerEntity player, CallbackInfo ci) {
        TameableEntity tameable = (TameableEntity) (Object) this;

        if (tameable.isTamed()) {
            applyAttributeInheritance(player, tameable);
        }
    }

     @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        TameableEntity tameable = (TameableEntity)(Object)this;

        if (tameable.isTamed() && tameable.getOwnerUuid() != null && this.getWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(tameable.getOwnerUuid());
            if (owner != null) {
                applyAttributeInheritance(owner, tameable);
            }
        }
    }

    private static final Set<RegistryEntry<EntityAttribute>> INHERITABLE_ATTRIBUTES = Set.of(
            EntityAttributes.GENERIC_MAX_HEALTH,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributes.GENERIC_SCALE,
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
            EntityAttributes.GENERIC_ARMOR,
            EntityAttributes.GENERIC_MOVEMENT_SPEED
    );

    private void applyAttributeInheritance(PlayerEntity owner, TameableEntity pet) {
        double ratio = owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
        StringBuilder debug = new StringBuilder();
        debug.append("🐾 ").append(pet.getName().getString())
                .append(" [").append(pet.getUuidAsString()).append("] inherited attributes from ")
                .append(owner.getName().getString()).append(" (ratio: ").append(ratio).append(")\n");

        boolean inheritedAny = false;

        for (RegistryEntry<EntityAttribute> attribute : INHERITABLE_ATTRIBUTES) {
            EntityAttributeInstance ownerAttr = owner.getAttributeInstance(attribute);
            if (ownerAttr == null) continue;

            double inheritedValue = ownerAttr.getValue() * ratio;

            EntityAttributeInstance petAttr = pet.getAttributeInstance(attribute);
            if (petAttr != null) {
                double original = petAttr.getBaseValue();
                petAttr.setBaseValue(original + inheritedValue);

                if (attribute.value().equals(EntityAttributes.GENERIC_MAX_HEALTH)) {
                    pet.setHealth((float) pet.getAttributeValue(attribute));
                }

                debug.append("  ➤ ")
                        .append(attribute.getKey().map(key -> key.getValue().toString()).orElse("unknown"))
                        .append(": ").append(original)
                        .append(" ➝ ").append(petAttr.getBaseValue())
                        .append(" (+").append(inheritedValue).append(")\n");

                inheritedAny = true;
            }
        }

        if (inheritedAny) {
            System.out.println(debug);
        } else {
            System.out.println("⚠️ No attributes were inherited by " + pet.getName().getString());
        }
    }
}

