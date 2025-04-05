package net.pixeldreamstudios.kevslibrary.client;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimap.*; // optionally this if you need to explicitly type it

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE;

public class AttributePanelDrawable implements Drawable, Element, Selectable {
    private static final Identifier NAME_BG = Identifier.of("minecraft", "textures/block/light_gray_concrete.png");
    private static final Identifier VALUE_BG = Identifier.of("minecraft", "textures/block/gray_concrete.png");

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int x, y, width;
    private int height;

    private boolean expanded = false;
    private final List<StatEntry> cachedStats = new ArrayList<>();

    private int currentPage = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_ROWS = 6;
    private static final int PADDING = 2;

    private boolean showOnlyChanged = true;

    public AttributePanelDrawable(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void setHeightFromInventory(int inventoryHeight) {
        this.height = inventoryHeight;
    }

    public void toggle() {
        expanded = !expanded;
        currentPage = 0;
        if (expanded) cacheStats();
    }

    public void tick() {
        if (expanded) {
            cacheStats();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void cacheStats() {
        cachedStats.clear();
        PlayerEntity player = client.player;
        if (player == null) return;

        for (RegistryEntry<EntityAttribute> entry : Registries.ATTRIBUTE.streamEntries().toList()) {
            EntityAttribute attr = entry.value();
            EntityAttributeInstance instance = player.getAttributeInstance(entry);
            if (instance == null) continue;

            boolean isPercent = attr.getTranslationKey().contains("resistance")
                    || attr.getTranslationKey().contains("movement_speed")
                    || attr.getTranslationKey().contains("fire_tornado_chance")
                    || attr.getTranslationKey().contains("fire_tornado_overload_chance")
                    || attr.getTranslationKey().contains("chain_lightning_chance")
                    || attr.getTranslationKey().contains("chain_lightning_overload_chance")
                    || attr.getTranslationKey().contains("frost_nova_chance")
                    || attr.getTranslationKey().contains("frost_nova_overload_chance");

            double base = instance.getBaseValue();
            double value = instance.getValue();

            if (showOnlyChanged && Math.abs(base - value) < 0.001) continue;

            cachedStats.add(new StatEntry(
                    Text.translatable(attr.getTranslationKey()),
                    base,
                    value,
                    isPercent,
                    entry // ✅ This is RegistryEntry<EntityAttribute>
            ));

        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!expanded) return;

        TextRenderer tr = client.textRenderer;
        int visibleRows = MAX_ROWS;
        int totalPages = (int) Math.ceil(cachedStats.size() / (float) visibleRows);
        currentPage = Math.min(currentPage, Math.max(totalPages - 1, 0));

        int startIndex = currentPage * visibleRows;
        int endIndex = Math.min(startIndex + visibleRows, cachedStats.size());
        int rowY = y + PADDING;

        int hoverIndex = -1;

        for (int i = startIndex; i < endIndex; i++) {
            StatEntry stat = cachedStats.get(i);
            int rowIndex = i - startIndex;
            int yOffset = rowY + rowIndex * ROW_HEIGHT;

            String statName = stat.name().getString();
            int maxNameWidth = width / 2 - 8;

            String topLine, bottomLine = null;
            if (tr.getWidth(statName) <= maxNameWidth) {
                topLine = statName;
            } else {
                topLine = tr.trimToWidth(statName, maxNameWidth);
                String remainder = statName.substring(topLine.length()).trim();

                if (!remainder.isEmpty()) {
                    bottomLine = tr.trimToWidth(remainder, maxNameWidth);
                    if (tr.getWidth(remainder) > maxNameWidth) {
                        while (tr.getWidth(bottomLine + "...") > maxNameWidth && bottomLine.length() > 0) {
                            bottomLine = bottomLine.substring(0, bottomLine.length() - 1);
                        }
                        bottomLine += "...";
                    }
                }
            }

            context.drawTexture(NAME_BG, x, yOffset, 0, 0, width / 2, ROW_HEIGHT, 16, 16);
            context.drawTexture(VALUE_BG, x + width / 2, yOffset, 0, 0, width / 2, ROW_HEIGHT, 16, 16);

            if (bottomLine == null) {
                int nameY = yOffset + (ROW_HEIGHT - 8) / 2;
                context.drawText(tr, topLine, x + 4, nameY, 0xFFFFFF, false);
            } else {
                context.drawText(tr, topLine, x + 4, yOffset + 4, 0xFFFFFF, false);
                context.drawText(tr, bottomLine, x + 4, yOffset + 14, 0xCCCCCC, false);
            }

            String valueStr = stat.percent()
                    ? String.format("%d%%", (int) (stat.current() * 100))
                    : String.format("%.2f", stat.current());

            int color = Formatting.GRAY.getColorValue();
            if (stat.isChanged()) {
                color = stat.current() > stat.base() ? Formatting.GREEN.getColorValue() : Formatting.RED.getColorValue();
                valueStr += stat.current() > stat.base() ? " ↑" : " ↓";
            }

            int valueY = yOffset + (ROW_HEIGHT - 8) / 2;
            context.drawText(tr, valueStr, x + width - 4 - tr.getWidth(valueStr), valueY, color, false);
            context.fill(x, yOffset + ROW_HEIGHT - 1, x + width, yOffset + ROW_HEIGHT, 0xFF444444);

            if (mouseX >= x && mouseX <= x + width && mouseY >= yOffset && mouseY <= yOffset + ROW_HEIGHT) {
                hoverIndex = i;
            }
        }

        if (hoverIndex != -1 && hoverIndex < cachedStats.size()) {
            StatEntry stat = cachedStats.get(hoverIndex);

            int rowIndex = hoverIndex % MAX_ROWS;
            int hoverY = y + PADDING + rowIndex * ROW_HEIGHT;
            int midX = x + width / 2;

            boolean onName = mouseX >= x && mouseX <= midX;
            boolean onValue = mouseX > midX && mouseX <= x + width;

            if (onName) {
                context.drawTooltip(tr, List.of(stat.name()), mouseX, mouseY);
            } else if (onValue) {
                List<Text> lines = new ArrayList<>();
                lines.add(Text.literal("Base: " + String.format("%.2f", stat.base())));

                EntityAttributeInstance instance = client.player.getAttributeInstance(stat.attribute());
                if (instance != null && !instance.getModifiers().isEmpty()) {
                    lines.add(Text.empty());
                    lines.add(Text.literal("Modifiers:").formatted(Formatting.YELLOW));
                    for (EntityAttributeModifier mod : instance.getModifiers()) {
                        String op = switch (mod.operation()) {
                            case ADD_VALUE -> "+" + String.format("%.2f", mod.value());
                            case ADD_MULTIPLIED_BASE -> "× base × " + String.format("%.2f", mod.value());
                            case ADD_MULTIPLIED_TOTAL -> "× total × " + String.format("%.2f", mod.value());
                        };

                        // Try to find the item that added this modifier
                        String source = null;

                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            ItemStack stack = client.player.getEquippedStack(slot);
                            if (!stack.isEmpty()) {
                                var modifiersComponent = stack.getItem().getAttributeModifiers();
                                for (var modifierEntry : modifiersComponent.modifiers()) {
                                    if (modifierEntry.modifier().id().equals(mod.id())) {
                                        String displayName = stack.getName().getString();
                                        String id = stack.getItem().getTranslationKey(); // fallback name key like "item.minecraft.diamond_sword"

                                        if (displayName.toLowerCase().contains("minecraft:") || displayName.matches("^[a-z0-9_]+:[a-z0-9_/.]+$")) {
                                            // fallback to translation key if it's just an ID
                                            displayName = Text.translatable(id).getString();
                                        }

                                        source = displayName;
                                        // "Diamond Sword", etc.
                                        break;
                                    }
                                }
                            }
                            if (source != null) break;
                        }


                        String idString = mod.id().toString();
                        String label = source != null
                                ? source + " (" + idString + ")"
                                : idString;

                        lines.add(Text.literal("- " + label + " ").append(Text.literal(op).formatted(Formatting.GRAY)));
                    }

                } else {
                    lines.add(Text.literal("No active modifiers").formatted(Formatting.DARK_GRAY));
                }


                context.drawTooltip(tr, lines, mouseX, mouseY);
            }
        }

        int btnY = y + height - 12;

        String prevText = "« Prev";
        int prevX = x + 4;
        int prevW = tr.getWidth(prevText);
        boolean hoveringPrev = mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= btnY && mouseY <= btnY + 10;
        context.drawText(tr, prevText, prevX, btnY, hoveringPrev ? 0xFFFFFF : 0xAAAAAA, false);

        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";
        int checkX = x + (width / 2) - (tr.getWidth(checkLabel) / 2);
        context.drawText(tr, checkLabel, checkX, btnY, 0xAAAAAA, false);

        String nextText = "Next »";
        int nextW = tr.getWidth(nextText);
        int nextX = x + width - 4 - nextW;
        boolean hoveringNext = mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= btnY && mouseY <= btnY + 10;
        context.drawText(tr, nextText, nextX, btnY, hoveringNext ? 0xFFFFFF : 0xAAAAAA, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!expanded) return false;

        int visibleRows = MAX_ROWS;
        int totalPages = (int) Math.ceil(cachedStats.size() / (float) visibleRows);
        int btnY = y + height - 12;

        String prevText = "« Prev";
        int prevX = x + 4;
        int prevW = client.textRenderer.getWidth(prevText);
        if (mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= btnY && mouseY <= btnY + 10) {
            if (currentPage > 0) currentPage--;
            return true;
        }

        String nextText = "Next »";
        int nextW = client.textRenderer.getWidth(nextText);
        int nextX = x + width - 4 - nextW;
        if (mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= btnY && mouseY <= btnY + 10) {
            if (currentPage < totalPages - 1) currentPage++;
            return true;
        }

        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";
        int checkX = x + (width / 2) - (client.textRenderer.getWidth(checkLabel) / 2);
        if (mouseX >= checkX && mouseX <= checkX + client.textRenderer.getWidth(checkLabel) &&
                mouseY >= btnY && mouseY <= btnY + 10) {
            showOnlyChanged = !showOnlyChanged;
            currentPage = 0;
            cacheStats();
            return true;
        }

        return false;
    }

    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public SelectionType getType() { return SelectionType.NONE; }
    @Override public void appendNarrations(NarrationMessageBuilder builder) {}
}
