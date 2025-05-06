package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.client.AttributePanelDrawable;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {

    @Unique
    private AttributePanelDrawable kevslib$attributePanel;

    @Unique
    private static final Identifier ATTRIBUTE_BOOK = Identifier.of("kevslibrary", "textures/gui/attribute_book.png");


    public InventoryScreenMixin(PlayerScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void kevslib$onInit(CallbackInfo ci) {
        kevslib$attributePanel = new AttributePanelDrawable(this.x - 130, this.y, 120);
        kevslib$attributePanel.setHeightFromInventory(this.backgroundHeight);
        this.addDrawableChild(kevslib$attributePanel);
        this.addSelectableChild(kevslib$attributePanel);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void kevslib$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (kevslib$attributePanel != null) {
            kevslib$attributePanel.tick();

            int iconSize = 9;
            int buttonX = this.x + this.backgroundWidth / 2 + KevsLibraryConfig.INSTANCE.xOffset;
            int buttonY = this.y + KevsLibraryConfig.INSTANCE.yOffset;

            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize;

            int color = hovered ? 0xFFFFFFFF : 0xFF666666;
            context.setShaderColor(
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    ((color >> 24) & 0xFF) / 255f
            );

            context.drawTexture(ATTRIBUTE_BOOK, buttonX, buttonY, 0, 0, iconSize, iconSize, 9, 9);
            context.setShaderColor(1f, 1f, 1f, 1f);

            if (hovered) {
                context.drawTooltip(this.textRenderer, Text.of("Attributes Panel"), mouseX, mouseY);
            }
        }
    }
    @Inject(method = "render", at = @At("RETURN"))
    private void kevslib$renderTooltipAfterEverything(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (kevslib$attributePanel != null && kevslib$attributePanel.isExpanded()) {
            kevslib$attributePanel.renderTooltip(context);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kevslib$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (kevslib$attributePanel != null) {
            int iconSize = 9;
            int buttonX = this.x + this.backgroundWidth / 2 + KevsLibraryConfig.INSTANCE.xOffset;
            int buttonY = this.y + KevsLibraryConfig.INSTANCE.yOffset;

            if (mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize) {
                kevslib$attributePanel.toggle();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            if (kevslib$attributePanel.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
