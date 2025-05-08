package net.pixeldreamstudios.kevslibrary.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.compat.TrinketCompat;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.stream.Collectors;

public class AttributePanelDrawable implements Drawable, Element, Selectable {
    private static final Identifier BOOK_TEXTURE = Identifier.of("minecraft", "textures/gui/book.png");
    private static final Identifier NAME_BG = Identifier.of("minecraft", "textures/block/light_gray_concrete.png");
    private static final Identifier VALUE_BG = Identifier.of("minecraft", "textures/block/gray_concrete.png");

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int x, y, width;
    private int height;
    @Unique
    private List<Text> queuedTooltip = null;
    @Unique
    private int tooltipX, tooltipY;

    private boolean expanded = false;
    private final List<StatEntry> cachedStats = new ArrayList<>();
    private int currentPage = 0;

    private static final int MAX_ROWS = 6;
    private boolean showOnlyChanged = true;
    private List<ItemStack> queuedTooltipIcons = new ArrayList<>();
    private static final String DESCRIPTION_PREFIX = "description.";
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
    public void renderTooltip(DrawContext context) {
        if (queuedTooltip == null || queuedTooltip.isEmpty()) return;

        TextRenderer tr = client.textRenderer;
        int zOffset = 400;
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, zOffset);

        int maxWidth = 0;
        for (Text line : queuedTooltip) {
            maxWidth = Math.max(maxWidth, tr.getWidth(line.getString()));
        }

        int tooltipWidth = maxWidth + 28;
        int tooltipHeight = queuedTooltip.size() * (tr.fontHeight + 4) + 12;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int tooltipX = this.tooltipX + 12;
        int tooltipY = this.tooltipY + 12;

        if (tooltipX + tooltipWidth > screenWidth) {
            tooltipX = screenWidth - tooltipWidth - 8;
        }
        if (tooltipY + tooltipHeight > screenHeight) {
            tooltipY = screenHeight - tooltipHeight - 8;
        }

        tooltipX = Math.max(tooltipX, 4);
        tooltipY = Math.max(tooltipY, 4);


        int backgroundColor = 0xF0131313;
        int borderColorStart = 0xFF5A5A5A;
        int borderColorEnd = 0xFFAAAAAA;

        context.fillGradient(tooltipX - 4, tooltipY - 4, tooltipX + tooltipWidth + 4, tooltipY + tooltipHeight,
                backgroundColor, backgroundColor);
        context.drawBorder(tooltipX - 4, tooltipY - 4, tooltipWidth + 8, tooltipHeight + 1, borderColorStart);

        for (int i = 0; i < queuedTooltip.size(); i++) {
            int lineY = tooltipY + i * (tr.fontHeight + 4);

            ItemStack icon = (queuedTooltipIcons != null && i < queuedTooltipIcons.size()) ? queuedTooltipIcons.get(i) : ItemStack.EMPTY;

            int iconOffset = 0;
            if (!icon.isEmpty()) {
                context.getMatrices().push();
                context.getMatrices().translate(tooltipX, lineY, 0);
                context.getMatrices().scale(0.85f, 0.85f, 1f);
                context.drawItem(icon, 0, 0);
                context.getMatrices().pop();
                iconOffset = 18;
            }

            int textX = tooltipX + iconOffset;
            context.drawText(tr, queuedTooltip.get(i), textX, lineY + 2, 0xFFFFFF, false);
        }

        context.getMatrices().pop();

        queuedTooltip = null;
        if (queuedTooltipIcons != null) queuedTooltipIcons.clear();
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
                    || attr.getTranslationKey().contains("arcane_rupture_chance")
                    || attr.getTranslationKey().contains("arcane_rupture_damage")
                    || attr.getTranslationKey().contains("arcane_rupture_overload_chance")
                    || attr.getTranslationKey().contains("ratio")
                    || attr.getTranslationKey().contains("crit_chance")
                    || attr.getTranslationKey().contains("crit_damage")
                    || attr.getTranslationKey().contains("soul_link_damage")
                    || attr.getTranslationKey().contains("soul_link_chance")
                    || attr.getTranslationKey().contains("damage_multiplier");

            double base = instance.getBaseValue();
            double value = instance.getValue();

            if (showOnlyChanged) {
                if (Double.isNaN(value) || Math.abs(base - value) < 0.001) continue;
            }

            cachedStats.add(new StatEntry(
                    Text.translatable(attr.getTranslationKey()),
                    base,
                    value,
                    isPercent,
                    entry
            ));
            cachedStats.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));

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
        if (showOnlyChanged && cachedStats.isEmpty()) {
            String noStatsText = "No changed attributes";
            int textWidth = tr.getWidth(noStatsText);
            context.drawText(tr, noStatsText, x + (width - textWidth) / 2, y + 8, Formatting.GRAY.getColorValue(), false);
            drawVanillaButtons(context, tr, mouseX, mouseY, y + height - 12);
            return;
        }
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

        context.drawTexture(BOOK_TEXTURE, x - 25, y, 0, 0, 240, 230, 240, 230);

        int visibleRows = MAX_ROWS;
        int totalPages = (int) Math.ceil(cachedStats.size() / (float) visibleRows);
        currentPage = Math.min(currentPage, Math.max(totalPages - 1, 0));
        if (showOnlyChanged && cachedStats.isEmpty()) {
            String noStatsText = "No changed attributes";
            int textWidth = tr.getWidth(noStatsText);
            drawBookButtons(context, tr, mouseX, mouseY, y + height - 20);
            context.drawText(tr, noStatsText, x + (width - textWidth) / 2 + 5, y + 20, Formatting.DARK_GRAY.getColorValue(), false);return;
        }
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

    private void drawTooltipContent(DrawContext context,
                                    TextRenderer tr,
                                    int hoverIndex,
                                    int mouseX,
                                    int mouseY,
                                    int rowHeight,
                                    int padding) {
        if (hoverIndex == -1 || hoverIndex >= cachedStats.size()) return;

        StatEntry stat = cachedStats.get(hoverIndex);
        int rowIndex = hoverIndex % MAX_ROWS;
        int yOffset = y + padding + rowIndex * rowHeight;
        int midX = x + width / 2;

        boolean onName = mouseX >= x && mouseX <= midX;
        boolean onValue = mouseX > midX && mouseX <= x + width;

        if (onName) {
            AttributeDescriptionProvider.TooltipContents tooltip = AttributeDescriptionProvider.getTooltip(stat.attribute().value());
            this.queuedTooltip = tooltip.lines();
            this.queuedTooltipIcons = tooltip.icons();
            this.tooltipX = mouseX;
            this.tooltipY = mouseY;
            return;
        }

        if (!onValue) return;

        boolean shiftDown = InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                client.options.sneakKey.getDefaultKey().getCode()
        );
        List<Double> flatComponents = new ArrayList<>();
        List<Double> baseMultComponents = new ArrayList<>();
        List<Double> totalMultComponents = new ArrayList<>();
        EntityAttributeInstance instance = client.player.getAttributeInstance(stat.attribute());

        List<Text> tooltipLines = new ArrayList<>();
        List<ItemStack> iconStacks = new ArrayList<>();

        tooltipLines.add(Text.literal("Base: " + String.format("%.2f", stat.base())));
        iconStacks.add(ItemStack.EMPTY);

        if (instance != null) {
            tooltipLines.add(Text.literal("Final: " + String.format("%.2f", instance.getValue())));
            iconStacks.add(ItemStack.EMPTY);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        List<TrinketCompat.TrinketModifierSource> unmatchedTrinketSources = new ArrayList<>();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            unmatchedTrinketSources.addAll(TrinketCompat.getTrinketModifierSources(client.player));
        }

        if (instance != null && !instance.getModifiers().isEmpty()) {
            tooltipLines.add(Text.empty());
            iconStacks.add(ItemStack.EMPTY);
            tooltipLines.add(Text.literal("Modifiers:").formatted(Formatting.YELLOW));
            iconStacks.add(ItemStack.EMPTY);

            for (EntityAttributeModifier mod : instance.getModifiers()) {
                String opText;
                Formatting color;

                switch (mod.operation()) {
                    case ADD_VALUE -> {
                        double value = mod.value();
                        flat += value;
                        flatComponents.add(value);

                        color = value >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = value >= 0 ? "+" : "";
                        opText = sign + String.format("%.2f", value);
                    }

                    case ADD_MULTIPLIED_BASE -> {
                        double value = mod.value();
                        multBase += value;
                        baseMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = percent >= 0 ? "+" : "";

                        opText = sign + percent + "% Base";
                    }

                    case ADD_MULTIPLIED_TOTAL -> {
                        double value = mod.value();
                        multTotal += value;
                        totalMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = percent >= 0 ? "+" : "";

                        opText = sign + percent + "% Total";
                    }

                    default -> {
                        opText = "?";
                        color = Formatting.GRAY;
                    }
                }





                Identifier rawId    = mod.id();
                String     fullPath = rawId.getPath();
                String[]   parts    = fullPath.split("\\.", 2);
                Identifier modId    = Identifier.of(rawId.getNamespace(), parts[0]);
                String     customName = parts.length > 1 ? parts[1] : null;

                ItemStack matchingStack = new ItemStack(Registries.ITEM.get(modId));
                Text displayName       = Text.literal(formatModifierId(modId));
                boolean foundSource    = false;

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = client.player.getEquippedStack(slot);
                    if (stack.isEmpty()) continue;

                    boolean[] matched = {false};
                    var component = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                    if (component != null) {
                        component.applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(modId)) matched[0] = true;
                        });
                    }

                    if (!matched[0]) {
                        stack.getItem().getAttributeModifiers().applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(modId)) matched[0] = true;
                        });
                    }

                    if (matched[0]) {
                        matchingStack = stack;
                        displayName = stack.getName();
                        foundSource = true;
                        break;
                    }
                }

                if (!foundSource && FabricLoader.getInstance().isModLoaded("trinkets")) {
                    for (Iterator<TrinketCompat.TrinketModifierSource> iter = unmatchedTrinketSources.iterator(); iter.hasNext(); ) {
                        var source = iter.next();
                        if (source.modifier().id().equals(mod.id())) {
                            matchingStack = source.stack();
                            displayName = matchingStack.getName().copy().formatted(matchingStack.getRarity().getFormatting());
                            foundSource = true;
                            iter.remove();
                            break;
                        }
                    }
                    if (!foundSource) {
                        for (Iterator<TrinketCompat.TrinketModifierSource> iter = unmatchedTrinketSources.iterator(); iter.hasNext(); ) {
                            var source = iter.next();
                            if (source.modifier().operation() == mod.operation() &&
                                    Math.abs(source.modifier().value() - mod.value()) < 0.0001) {
                                matchingStack = source.stack();
                                displayName = matchingStack.getName().copy().formatted(matchingStack.getRarity().getFormatting());
                                foundSource = true;
                                iter.remove();
                                break;
                            }
                        }
                    }
                }

                if (!foundSource) {
                    String[] pathParts = modId.getPath().split("/");
                    if (pathParts.length > 0) {
                        String itemGuess = pathParts[pathParts.length - 1];
                        Identifier itemId = Identifier.of(modId.getNamespace(), itemGuess);
                        if (Registries.ITEM.containsId(itemId)) {
                            Item item = Registries.ITEM.get(itemId);
                            matchingStack = new ItemStack(item);
                            displayName = matchingStack.getName().copy().formatted(matchingStack.getRarity().getFormatting());
                            foundSource = true;
                        }
                    }
                }

                if (!foundSource) {
                    for (var entry : client.player.getStatusEffects()) {
                        StatusEffect effect = entry.getEffectType().value();
                        int amplifier = entry.getAmplifier();
                        EntityAttribute attr = stat.attribute().value();

                        Map<EntityAttribute, EntityAttributeModifier> effectMods = new HashMap<>();
                        effect.forEachAttributeModifier(amplifier, (attribute, modifier) -> {
                            effectMods.put(attribute.value(), modifier);
                        });

                        if (effectMods.containsKey(attr)) {
                            EntityAttributeModifier potionMod = effectMods.get(attr);

                            if (potionMod.operation() == mod.operation() &&
                                    Math.abs(potionMod.value() - mod.value()) < 0.0001) {
                                displayName = Text.translatable(effect.getTranslationKey());
                                matchingStack = createColoredPotionItem(effect);
                                foundSource = true;
                                break;
                            }
                        }
                    }
                }
                if (customName != null) {
                    String pretty = Arrays.stream(customName.split("_"))
                            .map(s -> s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase())
                            .collect(Collectors.joining(" "));
                    displayName = Text.literal(pretty).formatted(Formatting.LIGHT_PURPLE);
                }
                if (modId.getNamespace().equals("tiered")) {
                    String path = modId.getPath();
                    String[] pathParts = path.split("_");
                    String tierRarity = pathParts.length > 0 ? pathParts[0] : "Tiered";
                    String tierName = tierRarity.substring(0, 1).toUpperCase() + tierRarity.substring(1).toLowerCase();

                    Text tierLine = Text.literal(tierName + " Tier Bonus: ").formatted(Formatting.AQUA)
                            .append(Text.literal(opText).formatted(Formatting.GREEN));

                    tooltipLines.add(tierLine);
                    iconStacks.add(new ItemStack(Items.ANVIL));
                } else {
                    Text displayLine = displayName.copy()
                            .append(" ")
                            .append(Text.literal(opText).formatted(color));

                    tooltipLines.add(displayLine);
                    iconStacks.add(matchingStack);
                }
            }

        } else if (stat.isChanged()) {
            tooltipLines.add(Text.literal("Hold \u21E7 Shift to show calculation").formatted(Formatting.GRAY));
            iconStacks.add(ItemStack.EMPTY);
        }

        tooltipLines.add(Text.empty());
        iconStacks.add(ItemStack.EMPTY);
        if (stat.isChanged()) {
            if (shiftDown) {
                double base = stat.base();
                double flatTotal = flat;

                double tieredMultTotal = 0.0;
                double nonTieredMultTotal = 0.0;

                instance = client.player.getAttributeInstance(stat.attribute());
                if (instance != null && !instance.getModifiers().isEmpty()) {
                    for (EntityAttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                            if (mod.id().getNamespace().equals("tiered")) {
                                tieredMultTotal += mod.value();
                            } else {
                                nonTieredMultTotal += mod.value();
                            }
                        }
                    }
                }

                double basePlusAdditive = base + flatTotal;
                double afterTiered = (tieredMultTotal != 0.0) ? basePlusAdditive * (1.0 + tieredMultTotal) : basePlusAdditive;
                double afterBaseMult = (multBase != 0.0) ? afterTiered * (1.0 + multBase) : afterTiered;
                double finalValue = (nonTieredMultTotal != 0.0) ? afterBaseMult * (1.0 + nonTieredMultTotal) : afterBaseMult;

                tooltipLines.add(Text.literal("Calculated:").formatted(Formatting.DARK_GRAY));
                iconStacks.add(ItemStack.EMPTY);

                if (flatTotal != 0.0) {
                    String flatBreakdown = flatComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");

                    tooltipLines.add(Text.literal(
                                    String.format("⟶ %.2f + (%s) = %.2f", base, flatBreakdown, basePlusAdditive))
                            .formatted(Formatting.GRAY));

                    iconStacks.add(ItemStack.EMPTY);
                } else {
                    tooltipLines.add(Text.literal(
                                    String.format("= %.2f", base))
                            .formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (tieredMultTotal != 0.0) {
                    tooltipLines.add(Text.literal(
                                    String.format("⟶ %.2f × %.2f = %.2f", basePlusAdditive, 1.0 + tieredMultTotal, afterTiered))
                            .formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (multBase != 0.0) {
                    String baseMultBreakdown = baseMultComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");

                    tooltipLines.add(Text.literal(
                                    String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterTiered, baseMultBreakdown, afterBaseMult))
                            .formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (nonTieredMultTotal != 0.0) {

                    List<Double> nonTieredTotalComponents = new ArrayList<>();
                    for (EntityAttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL &&
                                !mod.id().getNamespace().equals("tiered")) {
                            nonTieredTotalComponents.add(mod.value());
                        }
                    }

                    String totalMultBreakdown = nonTieredTotalComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");

                    if (!nonTieredTotalComponents.isEmpty()) {
                        tooltipLines.add(Text.literal(
                                        String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, totalMultBreakdown, finalValue))
                                .formatted(Formatting.GRAY));
                    }

                }

                tooltipLines.add(Text.literal("= " + String.format("%.2f", finalValue))
                        .formatted(Formatting.GREEN));
                iconStacks.add(ItemStack.EMPTY);

            } else {
                tooltipLines.add(Text.literal("Hold \u21E7 Shift to show calculation").formatted(Formatting.GRAY));
                iconStacks.add(ItemStack.EMPTY);
            }
        }

        this.queuedTooltip = tooltipLines;
        this.queuedTooltipIcons = iconStacks;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }


    private ItemStack createColoredPotionItem(StatusEffect effect) {
        ItemStack stack = new ItemStack(Items.POTION);


        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(effect.getTranslationKey()));

         NbtCompound nbt = new NbtCompound();
        nbt.putInt("CustomPotionColor", 0xFF0000);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        return stack;
    }



    private static String formatModifierId(Identifier id) {
        String path = id.getPath();
        if (path.contains("/")) {
            path = path.substring(0, path.indexOf('/'));
        }
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
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

        int spacing = 24;
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

        int spacing = 12;
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
