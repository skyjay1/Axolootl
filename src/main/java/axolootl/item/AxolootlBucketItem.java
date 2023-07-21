package axolootl.item;

import axolootl.client.ClientUtil;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.AxolootlEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AxolootlBucketItem extends MobBucketItem {

    public AxolootlBucketItem(Supplier<? extends EntityType<?>> entitySupplier, Supplier<? extends Fluid> fluidSupplier, Supplier<? extends SoundEvent> soundSupplier, Properties properties) {
        super(entitySupplier, fluidSupplier, soundSupplier, properties);
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (this.allowedIn(pCategory)) {
            ClientUtil.getClientLevel().ifPresentOrElse(level -> {
                for(ResourceLocation variantId : AxolootlVariant.getRegistry(level.registryAccess()).keySet()) {
                    ItemStack itemStack = new ItemStack(this);
                    CompoundTag tag = new CompoundTag();
                    tag.putString(AxolootlEntity.KEY_VARIANT_ID, variantId.toString());
                    itemStack.setTag(tag);
                    pItems.add(itemStack);
                }
            }, () -> pItems.add(new ItemStack(this)));
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        final Level level = (pLevel != null) ? pLevel : Minecraft.getInstance().level;
        if(level != null) {
            getVariant(level.registryAccess(), pStack).ifPresentOrElse(a -> {
                final ResourceLocation id = a.getRegistryName(level.registryAccess());
                final Component tierLevel = Component.translatable("enchantment.level." + a.getTier());
                final Component tier = Component.translatable("item.axolootl.axolootl_bucket.tooltip.tier", tierLevel).withStyle(ChatFormatting.GRAY);
                final Component description = a.getDescription().copy().withStyle(ChatFormatting.AQUA)
                        .append(" ").append(tier);
                // add variant description
                pTooltipComponents.add(description);
                // add advanced info
                if(pIsAdvanced.isAdvanced()) {
                    // registry name
                    pTooltipComponents.add(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
                    // mob resource flag
                    if(a.hasMobResources()) {
                        pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.mob_resource_generator").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    }
                    // colors
                    pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.primary_color", Integer.toHexString(a.getPrimaryColor())).withStyle(ChatFormatting.GRAY));
                    pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.secondary_color", Integer.toHexString(a.getSecondaryColor())).withStyle(ChatFormatting.GRAY));
                }
            }, () -> {
                // add tooltip for unknown variant
                pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.unknown").withStyle(ChatFormatting.RED));
            });
        }
    }

    public static Optional<AxolootlVariant> getVariant(final RegistryAccess registryAccess, final ItemStack itemStack) {
        // verify itemstack has ID
        if(!itemStack.hasTag() || !itemStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        // load ID
        final ResourceLocation id = new ResourceLocation(itemStack.getTag().getString(AxolootlEntity.KEY_VARIANT_ID));
        // load variant
        return AxolootlVariant.getRegistry(registryAccess).getOptional(id);
    }

    public static ItemStack getWithVariant(final RegistryAccess registryAccess, final ItemStack itemStack, final AxolootlVariant variant) {
        itemStack.getOrCreateTag().putString(AxolootlEntity.KEY_VARIANT_ID, variant.getRegistryName(registryAccess).toString());
        return itemStack;
    }

    public static ItemStack getWithVariant(final ItemStack itemStack, final ResourceLocation variant) {
        itemStack.getOrCreateTag().putString(AxolootlEntity.KEY_VARIANT_ID, variant.toString());
        return itemStack;
    }
}
