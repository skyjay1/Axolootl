package axolootl.client.menu.widget;

import axolootl.AxRegistry;
import axolootl.client.menu.AbstractTabScreen;
import axolootl.client.menu.AxolootlInterfaceScreen;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class AxolootlEntryButton extends ImageButton {

    public static final int WIDTH = 92;
    public static final int HEIGHT = 20;

    public static final int INTERACT_WIDTH = 14;
    public static final int INTERACT_HEIGHT = 14;

    private Font font;
    private AxolootlVariant entry;
    private int count;
    private Component text;
    private Component tooltipText;
    private Component tooltipFailText;

    public AxolootlEntryButton(int pX, int pY, Font font, OnPress pOnPress, OnTooltip pOnTooltip) {
        super(pX, pY, INTERACT_WIDTH, INTERACT_HEIGHT, 242, 143, INTERACT_HEIGHT, AbstractTabScreen.WIDGETS, 256, 256,
                pOnPress, pOnTooltip, Component.empty());
        this.entry = AxolootlVariant.EMPTY;
        this.font = font;
        this.text = Component.empty();
        this.tooltipText = Component.translatable(AxolootlInterfaceScreen.PREFIX + "extract");
        this.tooltipFailText = Component.translatable(AxolootlInterfaceScreen.PREFIX + "extract.fail").withStyle(ChatFormatting.RED);
    }

    public AxolootlVariant getEntry() {
        return entry;
    }

    public void update(final Map.Entry<AxolootlVariant, Integer> entry, final boolean isActive) {
        this.entry = entry.getKey();
        this.count = entry.getValue();
        Component axolootlName = Component.translatable("entity.axolootl.axolootl.description", AxRegistry.EntityReg.AXOLOOTL.get().getDescription(), entry.getKey().getDescription());
        this.tooltipText = Component.translatable(AxolootlInterfaceScreen.PREFIX + "extract_x", axolootlName);
        this.text = Component.translatable(AxolootlInterfaceScreen.PREFIX + "entry", count, entry.getKey().getDescription());
        updateActive(isActive);
    }

    public void updateActive(final boolean isActive) {
        this.active = isActive && this.count > 0 && this.entry != AxolootlVariant.EMPTY;
        this.setMessage(this.active ? this.tooltipText : this.tooltipFailText);
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.font.draw(pPoseStack, text, this.x + INTERACT_WIDTH + 4, this.y + (this.height - font.lineHeight) / 2.0F, 0);
        super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }
}
