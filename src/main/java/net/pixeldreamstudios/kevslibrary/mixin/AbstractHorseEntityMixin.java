package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin {

    private static final Set<RegistryEntry<EntityAttribute>> INHERITABLE_ATTRIBUTES = Set.of(
            EntityAttributes.GENERIC_MAX_HEALTH,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributes.GENERIC_SCALE,
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
            EntityAttributes.GENERIC_ARMOR,
            EntityAttributes.GENERIC_MOVEMENT_SPEED
    );
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;

        if (horse.isTame() && horse.getOwnerUuid() != null && horse.getWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(horse.getOwnerUuid());
            if (owner != null) {
                applyAttributeInheritance(owner, horse);
            }
        }
    }

    @Inject(method = "bondWithPlayer", at = @At("TAIL"))
    private void onBondWithPlayer(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
            applyAttributeInheritance(player, horse);
        }
    }

    private void applyAttributeInheritance(PlayerEntity owner, AbstractHorseEntity horse) {
        double ratio = owner.getAttributeValue(KevsLibrary.PET_INHERITANCE_RATIO);
        StringBuilder debug = new StringBuilder();
        debug.append("🐴 ").append(horse.getName().getString())
                .append(" [").append(horse.getUuidAsString()).append("] inherited attributes from ")
                .append(owner.getName().getString()).append(" (ratio: ").append(ratio).append(")\n");

        boolean inheritedAny = false;

        for (RegistryEntry<EntityAttribute> attribute : INHERITABLE_ATTRIBUTES) {
            EntityAttributeInstance ownerAttr = owner.getAttributeInstance(attribute);
            if (ownerAttr == null) continue;

            double inheritedValue = ownerAttr.getValue() * ratio;

            EntityAttributeInstance horseAttr = horse.getAttributeInstance(attribute);
            if (horseAttr != null) {
                double original = horseAttr.getBaseValue();
                horseAttr.setBaseValue(original + inheritedValue);

                if (attribute.value().equals(EntityAttributes.GENERIC_MAX_HEALTH)) {
                    horse.setHealth((float) horse.getAttributeValue(attribute));
                }

                debug.append("  ➤ ")
                        .append(attribute.getKey().map(key -> key.getValue().toString()).orElse("unknown"))
                        .append(": ").append(original)
                        .append(" ➝ ").append(horseAttr.getBaseValue())
                        .append(" (+").append(inheritedValue).append(")\n");

                inheritedAny = true;
            }
        }

        if (inheritedAny) {
            System.out.println(debug);
        } else {
            System.out.println("⚠️ No attributes were inherited by " + horse.getName().getString());
        }
    }
}
