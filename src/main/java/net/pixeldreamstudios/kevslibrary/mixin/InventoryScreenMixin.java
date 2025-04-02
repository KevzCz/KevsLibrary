package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.client.AttributePanelDrawable;
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
    private static final Identifier IRON_SWORD = Identifier.of("minecraft", "textures/item/iron_sword.png");

    @Unique
    private static final Identifier DIAMOND_SWORD = Identifier.of("minecraft", "textures/item/diamond_sword.png");

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

            // Icon properties
            int iconSize = 8;
            int buttonX = this.x + this.backgroundWidth / 2 - 61;
            int buttonY = this.y + 67;

            Identifier icon = kevslib$attributePanel.isExpanded()
                    ? DIAMOND_SWORD
                    : IRON_SWORD;

            // Draw icon
            context.drawTexture(icon, buttonX, buttonY, 0, 0, iconSize, iconSize, 8, 8);

            // Tooltip
            if (mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize) {
                context.drawTooltip(this.textRenderer, Text.of("Attributes Panel"), mouseX, mouseY);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kevslib$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (kevslib$attributePanel != null) {
            int iconSize = 8;
            int buttonX = this.x + this.backgroundWidth / 2 - 61;
            int buttonY = this.y + 66;

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
