package net.pixeldreamstudios.kevslibrary.compat;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.util.Pair;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;

public class TrinketCompat {
    @Nullable
    public static Pair<ItemStack, String> findModifierSource(PlayerEntity player, Identifier modifierId) {
        Optional<TrinketComponent> opt = TrinketsApi.getTrinketComponent(player);
        if (opt.isEmpty()) return null;

        TrinketComponent component = opt.get();
        for (var entry : component.getAllEquipped()) {
            ItemStack stack = entry.getRight();
            if (stack == null || stack.isEmpty() || stack.getItem() == net.minecraft.item.Items.AIR) continue;


            var attributeData = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attributeData != null) {
                boolean[] matched = {false};
                attributeData.applyModifiers((AttributeModifierSlot) null, (attr, mod) -> {
                    if (mod.id().equals(modifierId)) matched[0] = true;
                });
                if (matched[0]) {
                    return new Pair<>(stack.copy(), stack.getName().getString());


                }
            }
        }
        return null;
    }


}
