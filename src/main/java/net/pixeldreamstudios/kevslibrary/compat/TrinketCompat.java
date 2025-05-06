package net.pixeldreamstudios.kevslibrary.compat;

import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.*;

public class TrinketCompat {
    public record TrinketModifierSource(ItemStack stack, EntityAttributeModifier modifier, RegistryEntry<EntityAttribute> id) {}
    public static List<TrinketModifierSource> getTrinketModifierSources(PlayerEntity player) {
        List<TrinketModifierSource> results = new ArrayList<>();
        Optional<TrinketComponent> optComponent = TrinketsApi.getTrinketComponent(player);
        if (optComponent.isEmpty()) return results;

        TrinketComponent component = optComponent.get();

        for (var pair : component.getEquipped(stack -> true)) {
            SlotReference ref = pair.getLeft();
            ItemStack stack = pair.getRight();

            if (stack.isEmpty()) continue;

            Collection<Map.Entry<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> modifiers =
                    TrinketModifiers.get(stack, ref, player).entries();

            for (Map.Entry<RegistryEntry<EntityAttribute>, EntityAttributeModifier> entry : modifiers) {
                results.add(new TrinketModifierSource(stack, entry.getValue(), entry.getKey()));
            }

        }

        return results;
    }


}
