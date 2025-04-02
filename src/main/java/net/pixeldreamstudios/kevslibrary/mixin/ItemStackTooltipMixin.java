package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    private static final Map<String, String> EFFECT_COLORS = Map.of(
            "Multistrike", "§6",
            "Fire Tornado", "§c",
            "Chain Lightning", "§b",
            "Frost Nova", "§3"
    );

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void modifyTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> original = cir.getReturnValue();
        List<Text> modified = new ArrayList<>();

        for (Text line : original) {
            String str = line.getString();

            if (str.startsWith("+")) {
                try {
                    double value = extractRawValue(str);
                    boolean isPercent = value > 0.0 && value <= 1.0;
                    String numColor = isPercent ? "§a" : "§b";

                    String afterValue = str.substring(str.indexOf(" ") + 1).trim();
                    String effectName = extractEffectName(afterValue);
                    String effectColor = EFFECT_COLORS.getOrDefault(effectName, "§f");

                    String valueStr = isPercent ? (int) (value * 100) + "%" : ((value % 1 == 0) ? Integer.toString((int) value) : String.format("%.2f", value));
                    String formatted = "§7+" + numColor + valueStr + " §7" + colorEffectWords(afterValue, effectColor);

                    modified.add(Text.literal(formatted));
                    continue;
                } catch (Exception ignored) {
                }
            }

            modified.add(line);
        }

        cir.setReturnValue(modified);
    }

    private double extractRawValue(String str) {
        int plusIndex = str.indexOf("+");
        int spaceIndex = str.indexOf(" ", plusIndex);
        String numStr = str.substring(plusIndex + 1, spaceIndex);
        return Double.parseDouble(numStr);
    }

    private String extractEffectName(String full) {
        for (String key : EFFECT_COLORS.keySet()) {
            if (full.contains(key)) return key;
        }
        return "";
    }

    private String colorEffectWords(String line, String effectColor) {
        for (String key : EFFECT_COLORS.keySet()) {
            if (line.contains(key)) {
                return line.replaceAll("(?i)\\b" + key + "s?\\b", effectColor + "$0§7");

            }
        }
        return line;
    }
}
