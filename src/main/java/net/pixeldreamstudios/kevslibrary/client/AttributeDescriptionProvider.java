package net.pixeldreamstudios.kevslibrary.client;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class AttributeDescriptionProvider {
    private static final String DESCRIPTION_PREFIX = "description.";

    public static TooltipContents getTooltip(EntityAttribute attribute) {
        String descKey = DESCRIPTION_PREFIX + attribute.getTranslationKey();
        String desc = I18n.translate(descKey);

        String id = Registries.ATTRIBUTE
                .getKey(attribute)
                .map(key -> key.getValue().toString())
                .orElse("[unregistered]");

        List<Text> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        lines.add(Text.translatable(attribute.getTranslationKey()).formatted(Formatting.BOLD));
        icons.add(ItemStack.EMPTY);

        if (!desc.isEmpty() && !desc.equals(descKey)) {
            lines.add(Text.literal(desc));
            icons.add(ItemStack.EMPTY);
        }

        lines.add(Text.literal(id).formatted(Formatting.DARK_GRAY));
        icons.add(ItemStack.EMPTY);

        return new TooltipContents(lines, icons);
    }

    public record TooltipContents(List<Text> lines, List<ItemStack> icons) {}
}
