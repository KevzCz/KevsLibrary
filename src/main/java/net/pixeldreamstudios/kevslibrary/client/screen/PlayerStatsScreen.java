package net.pixeldreamstudios.kevslibrary.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.joml.Quaternionf;

import java.util.*;

public class PlayerStatsScreen extends Screen {

    private static final Identifier FRAME_TEXTURE = Identifier.of("kevslibrary", "textures/gui/outline.png");

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final PlayerEntity player;

    private float modelYaw = 180f;

    private int currentPage = 0;
    private boolean showAll = false;

    private ButtonWidget showAllToggle;
    private ButtonWidget nextPageButton;
    private ButtonWidget prevPageButton;

    private List<List<DisplayStat>> pages = new ArrayList<>();
    private int cachedStatBoxWidth = 160;

    private static final List<RegistryEntry<EntityAttribute>> CORE_ATTRIBUTES = List.of(
            EntityAttributes.GENERIC_MAX_HEALTH,
            EntityAttributes.GENERIC_ARMOR,
            EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributes.GENERIC_LUCK
    );

    public PlayerStatsScreen() {
        super(Text.literal("Player Stats"));
        this.player = MinecraftClient.getInstance().player;
    }

    @Override
    protected void init() {
        generatePages();

        showAllToggle = ButtonWidget.builder(getShowAllText(), btn -> {
            showAll = !showAll;
            showAllToggle.setMessage(getShowAllText());
            currentPage = 0;
            generatePages();
        }).position(0, 0).size(20, 20).build();
        this.addDrawableChild(showAllToggle);

        prevPageButton = ButtonWidget.builder(Text.literal("<"), btn -> {
            if (currentPage > 0) currentPage--;
        }).position(0, 0).size(20, 20).build();

        nextPageButton = ButtonWidget.builder(Text.literal(">"), btn -> {
            if (currentPage < pages.size() - 1) currentPage++;
        }).position(0, 0).size(20, 20).build();

        this.addDrawableChild(prevPageButton);
        this.addDrawableChild(nextPageButton);
    }

    private Text getShowAllText() {
        return Text.literal(showAll ? "[✔]" : "[ ]");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int windowWidth = client.getWindow().getScaledWidth();
        int windowHeight = client.getWindow().getScaledHeight();

        float targetScale = Math.min(4.0f,
                Math.min((float) windowWidth / 360f, (float) windowHeight / 230f));
        int panelX = (int) ((windowWidth / targetScale - 360) / 2f);
        int panelY = (int) ((windowHeight / targetScale - 230) / 2f);

        context.getMatrices().push();
        context.getMatrices().scale(targetScale, targetScale, 1.0f);

        // --- Frame Sizing ---
        int statLineHeight = 14;
        int statPadding = 20;
        int statBoxInnerPadding = 10; // extra inner padding for the content
        int statBoxWidth = cachedStatBoxWidth;
        int statBoxHeight = (6 * statLineHeight) + statPadding;

        int minFrameWidth = 320;
        int minFrameHeight = 220;

        int framePixelSize = 800;
        float minFrameScale = Math.max(
                (float) minFrameWidth / framePixelSize,
                (float) minFrameHeight / framePixelSize
        );

        float frameScale = Math.max(
                minFrameScale,
                Math.max(
                        (float) statBoxWidth / (framePixelSize * 0.6f),
                        (float) statBoxHeight / (framePixelSize * 0.6f)
                )
        );

        int frameWidth = (int) (framePixelSize * frameScale);
        int frameHeight = (int) (framePixelSize * frameScale);
        int frameX = panelX + (360 - frameWidth) / 2;
        int frameY = panelY + (230 - frameHeight) / 2;

        // Draw Frame
        context.getMatrices().push();
        context.getMatrices().translate(frameX, frameY, 0);
        context.getMatrices().scale(frameScale, frameScale, 1.0f);
        context.drawTexture(FRAME_TEXTURE, 0, 0, 0, 0, framePixelSize, framePixelSize, framePixelSize, framePixelSize);
        context.getMatrices().pop();

        // Stat box position inside frame
        int statsOffsetX = frameX + (frameWidth - statBoxWidth) / 2 + statBoxInnerPadding;
        int statsOffsetY = frameY + (frameHeight - statBoxHeight) / 2 + statBoxInnerPadding;

        // Title
        String title = "§lPlayer Attributes";
        int titleWidth = textRenderer.getWidth(title);
        int titleX = frameX + (frameWidth - titleWidth) / 2;
        context.drawTextWithShadow(textRenderer, title, titleX, statsOffsetY - 20, 0xFFFFFF);

        // Player model (moved more to the left)
        renderPlayerModel(context, frameX + frameWidth * 0.12f, frameY + frameHeight / 2f, 80f);

        // Stats
        renderStats(context, statsOffsetX, statsOffsetY, mouseX, mouseY);

        // Buttons under stat box
        int navY = statsOffsetY + statBoxHeight + 6;
        int buttonRowWidth = 80;
        int buttonStartX = statsOffsetX + (statBoxWidth - statBoxInnerPadding * 2 - buttonRowWidth) / 2;

        prevPageButton.setPosition(buttonStartX, navY);
        nextPageButton.setPosition(buttonStartX + 30, navY);
        showAllToggle.setPosition(buttonStartX + 60, navY);

        context.getMatrices().pop();
    }

    private void renderStats(DrawContext context, int x, int y, int mouseX, int mouseY) {
        if (currentPage >= pages.size()) return;

        for (DisplayStat stat : pages.get(currentPage)) {
            Text text = statDisplayText(stat);
            context.drawText(textRenderer, text, x, y, 0xAAAAAA, false);

            int width = textRenderer.getWidth(text);
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 12) {
                String descKey = stat.translationKey() + ".desc";
                if (Language.getInstance().hasTranslation(descKey)) {
                    context.drawTooltip(textRenderer, Text.translatable(descKey), mouseX, mouseY + 10);
                }
            }

            y += 14;
        }
    }

    private Text statDisplayText(DisplayStat stat) {
        String baseStr = format(stat.base(), stat.percent());
        String currentStr = format(stat.current(), stat.percent());

        Text name = stat.name(); // already translatable
        Text arrow;
        Text currentText;

        if (Math.abs(stat.current() - stat.base()) < 0.001) {
            // No change: just show base value
            return Text.literal("§f").append(name).append(": ").append(Text.literal("§7" + currentStr));
        } else {
            boolean boosted = stat.current() > stat.base();
            arrow = Text.literal(boosted ? " ↑" : " ↓").formatted(boosted ? Formatting.GREEN : Formatting.RED);
            currentText = Text.literal(currentStr).formatted(boosted ? Formatting.GREEN : Formatting.RED);

            return Text.literal("§f").append(name)
                    .append(": §7").append(baseStr)
                    .append(" §8➜ ")
                    .append(currentText)
                    .append(arrow);
        }
    }


    private void renderPlayerModel(DrawContext context, float x, float y, float scale) {
        if (player == null) return;

        float yaw = modelYaw + 50f;
        float pitch = -35f;
        double offset = 30f;

        context.getMatrices().push();
        context.getMatrices().translate(x, y + offset, 100.0);
        context.getMatrices().scale(-scale, -scale, scale);
        context.getMatrices().multiply(new Quaternionf().rotateY((float) Math.toRadians(180.0F)));
        context.getMatrices().translate(0.0f, -1.5f, 0.0f);

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);
        dispatcher.render(player, 0.0, 0.0, 0.0, 0.0f, 1.0f, context.getMatrices(), context.getVertexConsumers(), 0xF000F0);
        dispatcher.setRenderShadows(true);

        context.getMatrices().pop();
    }

    private void generatePages() {
        pages.clear();

        List<DisplayStat> coreStats = new ArrayList<>();
        for (var attr : CORE_ATTRIBUTES) {
            var instance = player.getAttributeInstance(attr);
            if (instance != null) {
                coreStats.add(new DisplayStat(
                        Text.translatable(getAttributeKey(attr)),
                        instance.getBaseValue(),
                        instance.getValue(),
                        false,
                        getAttributeKey(attr)
                ));
            }
        }
        pages.add(coreStats);

        List<DisplayStat> extraStats = new ArrayList<>();
        for (EntityAttribute attr : Registries.ATTRIBUTE) {
            RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(attr);
            if (CORE_ATTRIBUTES.contains(entry)) continue;

            var instance = player.getAttributeInstance(entry);
            if (instance == null) continue;

            double base = instance.getBaseValue();
            double current = instance.getValue();
            boolean changed = Math.abs(current - base) > 0.001;

            if (showAll || changed) {
                extraStats.add(new DisplayStat(
                        Text.translatable(attr.getTranslationKey()),
                        base,
                        current,
                        false,
                        getAttributeKey(entry)
                ));
            }
        }

        for (int i = 0; i < extraStats.size(); i += 6) {
            pages.add(extraStats.subList(i, Math.min(i + 6, extraStats.size())));
        }

        // Calculate max stat box width
        List<DisplayStat> allStats = new ArrayList<>(coreStats);
        allStats.addAll(extraStats);
        cachedStatBoxWidth = allStats.stream()
                .map(stat -> textRenderer.getWidth(statDisplayText(stat)))
                .max(Integer::compareTo)
                .orElse(160) + 20;
    }

    private String format(double value, boolean percent) {
        return percent
                ? (int) (value * 100) + "%"
                : (value % 1 == 0 ? Integer.toString((int) value) : String.format("%.2f", value));
    }

    private String getAttributeKey(RegistryEntry<EntityAttribute> attr) {
        Identifier id = Registries.ATTRIBUTE.getId(attr.value());
        return "attribute." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record DisplayStat(Text name, double base, double current, boolean percent, String translationKey) {}
}
