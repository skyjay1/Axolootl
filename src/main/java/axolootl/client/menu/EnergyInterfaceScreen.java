/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.menu;

import axolootl.block.entity.ControllerBlockEntity;
import axolootl.block.entity.VoidEnergyStorage;
import axolootl.data.aquarium_modifier.AquariumModifier;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.menu.CyclingMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EnergyInterfaceScreen extends AbstractCyclingScreen<CyclingMenu> {

    // WIDGET CONSTANTS //
    private static final int ENERGY_X = 29;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_WIDTH = 18;
    private static final int ENERGY_HEIGHT = 108;
    private static final int ENERGY_U = 0;
    private static final int ENERGY_V = 50;

    private static final int TEXT_X = 63;
    private static final int TEXT_Y = 17;
    private static final int TEXT_LINE_SPACING = 8;

    // DATA //
    private Map<BlockPos, IEnergyStorage> energyStorage;
    private IEnergyStorage storage;
    private int totalEnergy;
    private float totalEnergyPercent;
    private int totalEnergyHeight;
    private int totalCapacity;
    private int individualEnergy;
    private float individualEnergyPercent;
    private int individualEnergyHeight;
    private int poweredModifiers;
    private int usagePerTick;
    private int poweredAxolootls;
    private int usagePerAxolootl;

    // COMPONENTS //
    public static final String PREFIX = "gui.controller_tab.axolootl.energy_interface.";
    private Component energyCapacityText;
    private Component energyStorageText;
    private Component individualStorageText;
    private Component poweredModifiersText;
    private Component energyUsagePerTickText;
    private Component poweredAxolootlsText;
    private Component energyUsagePerAxolootlText;

    public EnergyInterfaceScreen(CyclingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.energyStorage = new HashMap<>();
        pMenu.getController().ifPresent(c -> energyStorage.putAll(c.resolveEnergyStorage(IEnergyStorage::canExtract)));
        this.storage = this.energyStorage.getOrDefault(pMenu.getBlockPos(), VoidEnergyStorage.INSTANCE);
    }

    @Override
    protected void init() {
        super.init();
        if(getMenu().getController().isEmpty()) {
            return;
        }
        final ControllerBlockEntity controller = getMenu().getController().get();
        // calculate energy usage per tick
        final RegistryAccess registryAccess = getMenu().getInventory().player.level.registryAccess();
        final Collection<AquariumModifier> poweredModifierSet = controller.resolveModifiers(registryAccess, controller.activePredicate.and((b, a) -> a.getSettings().getEnergyCost() > 0)).values();
        this.usagePerTick = 0;
        this.poweredModifiers = poweredModifierSet.size();
        for(AquariumModifier modifier : poweredModifierSet) {
            this.usagePerTick += modifier.getSettings().getEnergyCost();
        }
        // calculate energy usage per axolootl
        final Collection<AxolootlVariant> variantSet = controller.resolveAxolootlVariants(registryAccess).values().stream().filter(a -> a.getEnergyCost() > 0).toList();
        this.usagePerAxolootl = 0;
        this.poweredAxolootls = variantSet.size();
        for(AxolootlVariant variant : variantSet) {
            this.usagePerAxolootl += variant.getEnergyCost();
        }
        // create components
        this.poweredModifiersText = Component.translatable(PREFIX + "powered_modifier_count", poweredModifiers)
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "powered_modifier_count.description"))));
        this.poweredAxolootlsText = Component.translatable(PREFIX + "powered_axolootl_count", poweredAxolootls)
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "powered_axolootl_count.description"))));
        this.energyUsagePerTickText = Component.translatable(PREFIX + "tick_usage", usagePerTick)
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "tick_usage.description"))));
        this.energyUsagePerAxolootlText = Component.translatable(PREFIX + "axolootl_usage", usagePerAxolootl)
                .withStyle(a -> a.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "axolootl_usage.description"))));
        // calculate energy values
        this.containerTick();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // calculate energy storage and capacity
        this.totalEnergy = 0;
        this.totalCapacity = 0;
        for(IEnergyStorage entry : energyStorage.values()) {
            this.totalEnergy += entry.getEnergyStored();
            this.totalCapacity += entry.getMaxEnergyStored();
        }
        this.individualEnergy = storage.getEnergyStored();
        // calculate percentages and values
        this.totalEnergyPercent = (float) this.totalEnergy / (float) this.totalCapacity;
        this.totalEnergyHeight = Mth.floor(this.totalEnergyPercent * ENERGY_HEIGHT);
        this.individualEnergyPercent = (float) this.storage.getEnergyStored() / (float) this.totalCapacity;
        this.individualEnergyHeight = Mth.floor(this.individualEnergyPercent * ENERGY_HEIGHT);
        // create components
        this.energyCapacityText = Component.translatable(PREFIX + "energy_capacity", totalCapacity);
        this.energyStorageText = Component.translatable(PREFIX + "energy_storage", totalEnergy, totalCapacity);
        this.individualStorageText = Component.translatable(PREFIX + "individual_storage", individualEnergy, this.storage.getMaxEnergyStored());
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderEnergyBar(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderDetails(pPoseStack, pMouseX, pMouseY, pPartialTick);
        renderHoverActions(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void renderEnergyBar(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        int x = this.leftPos + ENERGY_X;
        int y = this.topPos + ENERGY_Y;
        // draw overlaid energy bars
        RenderSystem.setShaderTexture(0, WIDGETS);
        // draw background energy bar
        blit(pPoseStack, x, y, ENERGY_U, ENERGY_V, ENERGY_WIDTH, ENERGY_HEIGHT);
        // draw total energy bar
        blit(pPoseStack, x, y + ENERGY_HEIGHT - totalEnergyHeight, ENERGY_U + ENERGY_WIDTH, ENERGY_V, ENERGY_WIDTH, totalEnergyHeight);
        // draw storage energy bar
        blit(pPoseStack, x, y + ENERGY_HEIGHT - individualEnergyHeight, ENERGY_U + ENERGY_WIDTH * 2, ENERGY_V, ENERGY_WIDTH, individualEnergyHeight);

    }

    private void renderDetails(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        int x = this.leftPos + TEXT_X;
        int y = this.topPos + TEXT_Y;
        int deltaY = font.lineHeight + TEXT_LINE_SPACING;
        this.font.draw(pPoseStack, energyStorageText, x, y, 0);
        y += deltaY;
        this.font.draw(pPoseStack, poweredModifiersText, x, y, 0);
        y += deltaY;
        this.font.draw(pPoseStack, energyUsagePerTickText, x, y, 0);
        y += deltaY;
        this.font.draw(pPoseStack, poweredAxolootlsText, x, y, 0);
        y += deltaY;
        this.font.draw(pPoseStack, energyUsagePerAxolootlText, x, y, 0);
    }

    private void renderHoverActions(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        int deltaY = font.lineHeight + TEXT_LINE_SPACING;
        int x = TEXT_X;
        int y = TEXT_Y + deltaY;
        // render detail hover actions
        if(isHovering(x, y, font.width(poweredModifiersText), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, poweredModifiersText.getStyle(), pMouseX, pMouseY);
        }
        y += deltaY;
        if(isHovering(x, y, font.width(energyUsagePerTickText), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, energyUsagePerTickText.getStyle(), pMouseX, pMouseY);
        }
        y += deltaY;
        if(isHovering(x, y, font.width(poweredAxolootlsText), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, poweredAxolootlsText.getStyle(), pMouseX, pMouseY);
        }
        y += deltaY;
        if(isHovering(x, y, font.width(energyUsagePerAxolootlText), font.lineHeight, pMouseX, pMouseY)) {
            renderComponentHoverEffect(pPoseStack, energyUsagePerAxolootlText.getStyle(), pMouseX, pMouseY);
        }
        // render energy bar tooltips
        x = ENERGY_X;
        y = ENERGY_Y;
        if(isHovering(x, y, ENERGY_WIDTH, ENERGY_HEIGHT - totalEnergyHeight - 1, pMouseX, pMouseY)) {
            renderTooltip(pPoseStack, energyCapacityText, pMouseX, pMouseY);
        }
        y += ENERGY_HEIGHT - totalEnergyHeight + 1;
        if(isHovering(x, y, ENERGY_WIDTH, totalEnergyHeight - individualEnergyHeight - 1, pMouseX, pMouseY)) {
            renderTooltip(pPoseStack, energyStorageText, pMouseX, pMouseY);
        }
        y += totalEnergyHeight - individualEnergyHeight + 1;
        if(isHovering(x, y, ENERGY_WIDTH, individualEnergyHeight, pMouseX, pMouseY)) {
            renderTooltip(pPoseStack, individualStorageText, pMouseX, pMouseY);
        }
    }
}
