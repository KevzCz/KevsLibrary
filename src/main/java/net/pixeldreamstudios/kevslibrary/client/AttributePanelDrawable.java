package net.pixeldreamstudios.kevslibrary.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;

import java.util.ArrayList;
import java.util.List;

public class AttributePanelDrawable implements Drawable, Element, Selectable {
    private static final Identifier BOOK_TEXTURE = Identifier.of("minecraft", "textures/gui/book.png");
    private static final Identifier NAME_BG = Identifier.of("minecraft", "textures/block/light_gray_concrete.png");
    private static final Identifier VALUE_BG = Identifier.of("minecraft", "textures/block/gray_concrete.png");

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int x, y, width;
    private int height;

    private boolean expanded = false;
    private final List<StatEntry> cachedStats = new ArrayList<>();
    private int currentPage = 0;

    private static final int MAX_ROWS = 6;
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
        if (expanded) cacheStats();
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
                    || attr.getTranslationKey().contains("frost_nova_overload_chance")
                    || attr.getTranslationKey().contains("ratio");

            double base = instance.getBaseValue();
            double value = instance.getValue();

            if (showOnlyChanged && Math.abs(base - value) < 0.001) continue;

            cachedStats.add(new StatEntry(
                    Text.translatable(attr.getTranslationKey()),
                    base,
                    value,
                    isPercent,
                    entry
            ));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!expanded) return;
        if (KevsLibraryConfig.INSTANCE.useBookBackground) {
            renderBookLayout(context, mouseX, mouseY);
        } else {
            renderVanillaLayout(context, mouseX, mouseY);
        }
    }

    private void renderVanillaLayout(DrawContext context, int mouseX, int mouseY) {
        TextRenderer tr = client.textRenderer;
        int rowHeight = 24;
        int padding = 2;

        int visibleRows = MAX_ROWS;
        int totalPages = (int) Math.ceil(cachedStats.size() / (float) visibleRows);
        currentPage = Math.min(currentPage, Math.max(totalPages - 1, 0));

        int startIndex = currentPage * visibleRows;
        int endIndex = Math.min(startIndex + visibleRows, cachedStats.size());
        int rowY = y + padding;

        int hoverIndex = -1;

        for (int i = startIndex; i < endIndex; i++) {
            StatEntry stat = cachedStats.get(i);
            int rowIndex = i - startIndex;
            int yOffset = rowY + rowIndex * rowHeight;

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

            context.drawTexture(NAME_BG, x, yOffset, 0, 0, width / 2, rowHeight, 16, 16);
            context.drawTexture(VALUE_BG, x + width / 2, yOffset, 0, 0, width / 2, rowHeight, 16, 16);

            int nameY = yOffset + (rowHeight - 8) / 2;
            if (bottomLine == null) {
                context.drawText(tr, topLine, x + 4, nameY, 0xFFFFFF, false);
            } else {
                context.drawText(tr, topLine, x + 4, yOffset + 4, 0xFFFFFF, false);
                context.drawText(tr, bottomLine, x + 4, yOffset + 14, 0xCCCCCC, false);
            }

            String valueStr = stat.percent() ? String.format("%d%%", (int) (stat.current() * 100)) : String.format("%.2f", stat.current());
            int color = stat.isChanged() ? (stat.current() > stat.base() ? Formatting.GREEN.getColorValue() : Formatting.RED.getColorValue()) : Formatting.GRAY.getColorValue();
            valueStr += stat.isChanged() ? (stat.current() > stat.base() ? " ↑" : " ↓") : "";

            int valueY = yOffset + (rowHeight - 8) / 2;
            context.drawText(tr, valueStr, x + width - 4 - tr.getWidth(valueStr), valueY, color, false);
            context.fill(x, yOffset + rowHeight - 1, x + width, yOffset + rowHeight, 0xFF444444);

            if (mouseX >= x && mouseX <= x + width && mouseY >= yOffset && mouseY <= yOffset + rowHeight) {
                hoverIndex = i;
            }
        }

        drawVanillaTooltipButton(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);
    }

    private void renderBookLayout(DrawContext context, int mouseX, int mouseY) {
        TextRenderer tr = client.textRenderer;
        int rowHeight = 20;
        int padding = 20;

        // Draw full book background
        context.drawTexture(BOOK_TEXTURE, x - 25, y, 0, 0, 240, 230, 240, 230);

        int visibleRows = MAX_ROWS;
        int totalPages = (int) Math.ceil(cachedStats.size() / (float) visibleRows);
        currentPage = Math.min(currentPage, Math.max(totalPages - 1, 0));

        int startIndex = currentPage * visibleRows;
        int endIndex = Math.min(startIndex + visibleRows, cachedStats.size());
        int rowY = y + padding;

        int hoverIndex = -1;

        for (int i = startIndex; i < endIndex; i++) {

            StatEntry stat = cachedStats.get(i);
            int rowIndex = i - startIndex;
            int yOffset = rowY + rowIndex * rowHeight;

            String statName = stat.name().getString();
            int maxNameWidth = width / 2 - 7;
            context.fill(x + 12, yOffset + rowHeight + 2, x + width - 12, yOffset + rowHeight +1, 0xFFD6C4A3);
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

            int nameX = x + 15;
            int valueX = x + width - 12;
            int nameY = yOffset + (rowHeight - 8) / 2;

            if (bottomLine == null) {
                context.drawText(tr, topLine, nameX, nameY, 0x3A2F23, false);
            } else {
                context.drawText(tr, topLine, nameX, yOffset + 4, 0x3A2F23, false);
                context.drawText(tr, bottomLine, nameX, yOffset + 12, 0x6D5C48, false);
            }

            String valueStr = stat.percent() ? String.format("%d%%", (int) (stat.current() * 100)) : String.format("%.2f", stat.current());
            int color = stat.isChanged() ? (stat.current() > stat.base() ? Formatting.GREEN.getColorValue() : Formatting.RED.getColorValue()) : Formatting.GRAY.getColorValue();
            valueStr += stat.isChanged() ? (stat.current() > stat.base() ? " ↑" : " ↓") : "";

            context.drawText(tr, valueStr, valueX - tr.getWidth(valueStr), nameY, color, false);

            if (mouseX >= x && mouseX <= x + width && mouseY >= yOffset && mouseY <= yOffset + rowHeight) {
                hoverIndex = i;
            }
        }

        drawBookTooltipButton(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);
    }

    private void drawVanillaTooltipButton(DrawContext context, TextRenderer tr, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        drawTooltipContent(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);

        int btnY = y + height - 12;
        drawVanillaButtons(context, tr, mouseX, mouseY, btnY);
    }

    private void drawBookTooltipButton(DrawContext context, TextRenderer tr, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        drawTooltipContent(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);

        int btnY = y + height - 20;
        drawBookButtons(context, tr, mouseX, mouseY, btnY);
    }

    private void drawTooltipContent(DrawContext context, TextRenderer tr, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        if (hoverIndex != -1 && hoverIndex < cachedStats.size()) {
            StatEntry stat = cachedStats.get(hoverIndex);
            int rowIndex = hoverIndex % MAX_ROWS;
            int yOffset = y + padding + rowIndex * rowHeight;
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

                        String source = null;
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            ItemStack stack = client.player.getEquippedStack(slot);
                            if (!stack.isEmpty()) {
                                var modifiers = stack.getItem().getAttributeModifiers();
                                for (var entry : modifiers.modifiers()) {
                                    if (entry.modifier().id().equals(mod.id())) {
                                        String name = stack.getName().getString();
                                        if (name.matches("^[a-z0-9_]+:[a-z0-9_/.]+$"))
                                            name = Text.translatable(stack.getItem().getTranslationKey()).getString();
                                        source = name;
                                        break;
                                    }
                                }
                            }
                            if (source != null) break;
                        }

                        String label = source != null ? source + " (" + mod.id() + ")" : mod.id().toString();
                        lines.add(Text.literal("- " + label).append(Text.literal(" " + op).formatted(Formatting.GRAY)));
                    }
                } else {
                    lines.add(Text.literal("No active modifiers").formatted(Formatting.DARK_GRAY));
                }

                context.drawTooltip(tr, lines, mouseX, mouseY);
            }
        }
    }
    private record ButtonCoords(int prevX, int checkX, int nextX, int prevW, int checkW, int nextW) {}

    private ButtonCoords getBookButtonCoords(TextRenderer tr) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";

        int spacing = 24;

        int prevW = tr.getWidth(prevText);
        int checkW = tr.getWidth(checkLabel);
        int nextW = tr.getWidth(nextText);

        int totalWidth = prevW + spacing + checkW + spacing + nextW;
        int startX = x + (width - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevW + spacing;
        int nextX = checkX + checkW + spacing;

        return new ButtonCoords(prevX, checkX, nextX, prevW, checkW, nextW);
    }

    private void drawBookButtons(DrawContext context, TextRenderer tr, int mouseX, int mouseY, int btnY) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";

        int spacing = 24; // space between buttons
        int buttonHeight = 10;

        int prevWidth = tr.getWidth(prevText);
        int checkWidth = tr.getWidth(checkLabel);
        int nextWidth = tr.getWidth(nextText);

        int totalWidth = prevWidth + spacing + checkWidth + spacing + nextWidth;
        int startX = x + (width - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevWidth + spacing;
        int nextX = checkX + checkWidth + spacing;

        context.drawText(tr, prevText, prevX, btnY,
                mouseIn(mouseX, mouseY, prevX, btnY, prevWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);

        context.drawText(tr, checkLabel, checkX, btnY,
                mouseIn(mouseX, mouseY, checkX, btnY, checkWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);

        context.drawText(tr, nextText, nextX, btnY,
                mouseIn(mouseX, mouseY, nextX, btnY, nextWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
    }

    private void drawVanillaButtons(DrawContext context, TextRenderer tr, int mouseX, int mouseY, int btnY) {
        String prevText = "« Prev";
        String nextText = "Next »";
        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";

        int spacing = 12; // <- Add more spacing here
        int buttonHeight = 10;

        int prevWidth = tr.getWidth(prevText);
        int checkWidth = tr.getWidth(checkLabel);
        int nextWidth = tr.getWidth(nextText);

        int centerX = x + width / 2;

        int prevX = centerX - checkWidth / 2 - spacing - prevWidth;
        int checkX = centerX - checkWidth / 2;
        int nextX = centerX + checkWidth / 2 + spacing;

        context.drawText(tr, prevText, prevX, btnY,
                mouseIn(mouseX, mouseY, prevX, btnY, prevWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);

        context.drawText(tr, checkLabel, checkX, btnY,
                mouseIn(mouseX, mouseY, checkX, btnY, checkWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);

        context.drawText(tr, nextText, nextX, btnY,
                mouseIn(mouseX, mouseY, nextX, btnY, nextWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);
    }

    private boolean mouseIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!expanded) return false;

        int btnY = y + height - (KevsLibraryConfig.INSTANCE.useBookBackground ? 20 : 12);
        String prevText = "« Prev";
        String nextText = "Next »";
        String checkLabel = "[ " + (showOnlyChanged ? "✓" : " ") + " ]";

        int prevX = x + 20;
        int nextW = client.textRenderer.getWidth(nextText);
        int nextX = x + width - 20 - nextW;
        int checkX = x + (width / 2) - (client.textRenderer.getWidth(checkLabel) / 2);
        if (KevsLibraryConfig.INSTANCE.useBookBackground) {
            ButtonCoords coords = getBookButtonCoords(client.textRenderer);

            if (mouseIn((int) mouseX, (int) mouseY, coords.prevX(), btnY, coords.prevW(), 10)) {
                if (currentPage > 0) currentPage--;
                client.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
                return true;
            }

            if (mouseIn((int) mouseX, (int) mouseY, coords.nextX(), btnY, coords.nextW(), 10)) {
                int totalPages = (int) Math.ceil(cachedStats.size() / (float) MAX_ROWS);
                if (currentPage < totalPages - 1) currentPage++;
                client.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
                return true;
            }

            if (mouseIn((int) mouseX, (int) mouseY, coords.checkX(), btnY, coords.checkW(), 10)) {
                showOnlyChanged = !showOnlyChanged;
                currentPage = 0;
                cacheStats();
                client.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
                return true;
            }
        }
        if (!KevsLibraryConfig.INSTANCE.useBookBackground) {
            ButtonCoords coords = getBookButtonCoords(client.textRenderer);
            if (mouseIn((int) mouseX, (int) mouseY, prevX, btnY, client.textRenderer.getWidth(prevText), 10)) {
                if (currentPage > 0) currentPage--;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);

                return true;
            }

            if (mouseIn((int) mouseX, (int) mouseY, nextX, btnY, nextW, 10)) {
                int totalPages = (int) Math.ceil(cachedStats.size() / (float) MAX_ROWS);
                if (currentPage < totalPages - 1) currentPage++;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);
                return true;
            }

            if (mouseIn((int) mouseX, (int) mouseY, checkX, btnY, client.textRenderer.getWidth(checkLabel), 10)) {
                showOnlyChanged = !showOnlyChanged;
                currentPage = 0;
                cacheStats();
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);
                return true;
            }
        }
        return false;
    }

    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public SelectionType getType() { return SelectionType.NONE; }
    @Override public void appendNarrations(NarrationMessageBuilder builder) {}
}
