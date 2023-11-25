/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.item;

import axolootl.AxRegistry;
import axolootl.client.ClientUtil;
import axolootl.client.item.AxolootlBucketItemRenderer;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.AxolootlEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AxolootlBucketItem extends MobBucketItem {

    private static final Map<ResourceLocation, Rarity> RARITY_CACHE = new HashMap<>();

    public AxolootlBucketItem(Supplier<? extends EntityType<?>> entitySupplier, Supplier<? extends Fluid> fluidSupplier, Supplier<? extends SoundEvent> soundSupplier, Properties properties) {
        super(entitySupplier, fluidSupplier, soundSupplier, properties);
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if(this.allowedIn(pCategory)) {
            pItems.addAll(createSubtypes());
        }
    }

    public static List<ItemStack> createSubtypes() {
        final List<ItemStack> list = new ArrayList<>();
        final RegistryAccess access = registryAccess();
        if (access != null) {
            // create list of variants
            List<Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant>> variants = new ArrayList<>(AxolootlVariant.getRegistry(access).entrySet());
            // remove invalid entries
            variants.removeIf(e -> !e.getValue().isEnabled(access));
            // sort by tier, then by name
            Comparator<Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant>> comparator = Comparator.comparingInt(e -> e.getValue().getTier());
            variants.sort(comparator.thenComparing(e -> e.getValue().getDescription().getString()));
            // add each variant to the creative tab
            for (Map.Entry<ResourceKey<AxolootlVariant>, AxolootlVariant> variantId : variants) {
                ItemStack itemStack = new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get());
                // create itemstack with tag for this variant
                CompoundTag tag = new CompoundTag();
                tag.putString(AxolootlEntity.KEY_VARIANT_ID, variantId.getKey().location().toString());
                itemStack.setTag(tag);
                list.add(itemStack);
                // create itemstack with tag for this variant as a baby
                ItemStack babyItemStack = itemStack.copy();
                babyItemStack.getTag().putInt(AxolootlEntity.KEY_AGE, AxolootlEntity.BABY_AGE);
                list.add(babyItemStack);
            }
        } else {
            list.add(new ItemStack(AxRegistry.ItemReg.AXOLOOTL_BUCKET.get()));
        }
        return list;
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        if(isBaby(pStack)) {
            return super.getDescriptionId(pStack) + ".baby";
        }
        return super.getDescriptionId(pStack);
    }

    @Override
    public Rarity getRarity(ItemStack pStack) {
        if(pStack.hasTag() && pStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING)) {
            return getCachedRarity(new ResourceLocation(pStack.getTag().getString(AxolootlEntity.KEY_VARIANT_ID)));
        }
        return super.getRarity(pStack);
    }

    private static Rarity getCachedRarity(final ResourceLocation id) {
        if(!RARITY_CACHE.containsKey(id)) {
            // load registry access
            RegistryAccess access = registryAccess();
            // load axolootl variant
            if(access != null) {
                AxolootlVariant.getRegistry(access).getOptional(id)
                        .ifPresent(variant -> RARITY_CACHE.put(id, variant.getRarity()));
            }
        }
        return RARITY_CACHE.getOrDefault(id, Rarity.COMMON);
    }

    public static void clearRarityCache() {
        RARITY_CACHE.clear();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        final Level level = (pLevel != null) ? pLevel : ClientUtil.getClientLevel().orElse(null);
        if(level != null) {
            // add tooltips for variant
            getVariant(level.registryAccess(), pStack).ifPresentOrElse(a -> {
                final ResourceLocation id = a.getRegistryName(level.registryAccess());
                final Component tier = Component.translatable("item.axolootl.axolootl_bucket.tooltip.tier", a.getTierDescription()).withStyle(ChatFormatting.GRAY);
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
                    pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.primary_color", Integer.toHexString(a.getModelSettings().getPrimaryColor())).withStyle(ChatFormatting.GRAY));
                    pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.secondary_color", Integer.toHexString(a.getModelSettings().getSecondaryColor())).withStyle(ChatFormatting.GRAY));
                }
            }, () -> {
                // add tooltip for unknown variant
                pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.unknown").withStyle(ChatFormatting.RED));
            });
            // add tooltip for age
            if(pIsAdvanced.isAdvanced() && pStack.hasTag() && pStack.getTag().contains(AxolootlEntity.KEY_AGE, Tag.TAG_INT)) {
                pTooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.age", pStack.getTag().getInt(AxolootlEntity.KEY_AGE)).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            final LazyOptional<AxolootlBucketItemRenderer> renderer = LazyOptional.of(() -> new AxolootlBucketItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().getItemRenderer()));
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                final Optional<AxolootlBucketItemRenderer> oRenderer = renderer.resolve();
                if(oRenderer.isPresent()) {
                    return oRenderer.get();
                }
                return IClientItemExtensions.super.getCustomRenderer();
            }
        });
    }

    /**
     * @return the current registry access
     */
    @Nullable
    private static RegistryAccess registryAccess() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server != null) {
            return server.registryAccess();
        } else {
            final Optional<Level> oLevel = ClientUtil.getClientLevel();
            if(oLevel.isPresent()) {
                return oLevel.get().registryAccess();
            }
        }
        return null;
    }

    /**
     * Parses an {@link AxolootlVariant} from an item stack
     * @param registryAccess the registry access
     * @param itemStack the item stack
     * @return the axolootl variant, if any
     * @see #getVariant(RegistryAccess, ItemStack, boolean)
     */
    public static Optional<AxolootlVariant> getVariant(final RegistryAccess registryAccess, final ItemStack itemStack) {
        return getVariant(registryAccess, itemStack, false);
    }

    /**
     * Parses an {@link AxolootlVariant} from an item stack
     * @param registryAccess the registry access
     * @param itemStack the item stack
     * @param includeAll true to include variants that are not enabled
     * @return the axolootl variant, if any
     */
    public static Optional<AxolootlVariant> getVariant(final RegistryAccess registryAccess, final ItemStack itemStack, final boolean includeAll) {
        // verify itemstack has ID
        if(!itemStack.hasTag() || !itemStack.getTag().contains(AxolootlEntity.KEY_VARIANT_ID, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        // load variant ID
        final ResourceLocation id = new ResourceLocation(itemStack.getTag().getString(AxolootlEntity.KEY_VARIANT_ID));
        // validate variant ID
        if(!includeAll && !AxRegistry.AxolootlVariantsReg.isValid(id)) {
            return Optional.empty();
        }
        // load variant
        return AxolootlVariant.getRegistry(registryAccess).getOptional(id);
    }

    /**
     * @param registryAccess the registry access
     * @param itemStack the item stack to modify
     * @param variant the variant to write
     * @return the given item stack with the given variant written to NBT
     */
    public static ItemStack getWithVariant(final RegistryAccess registryAccess, final ItemStack itemStack, final AxolootlVariant variant) {
        return getWithVariant(itemStack, variant.getRegistryName(registryAccess));
    }

    /**
     * @param itemStack the item stack to modify
     * @param variant the variant to write
     * @return the given item stack with the given variant written to NBT
     */
    public static ItemStack getWithVariant(final ItemStack itemStack, final ResourceLocation variant) {
        itemStack.getOrCreateTag().putString(AxolootlEntity.KEY_VARIANT_ID, variant.toString());
        return itemStack;
    }

    public static boolean isBaby(final ItemStack itemStack) {
        return itemStack.hasTag() && itemStack.getTag().contains(AxolootlEntity.KEY_AGE, Tag.TAG_INT) && itemStack.getTag().getInt(AxolootlEntity.KEY_AGE) < 0;
    }
}
