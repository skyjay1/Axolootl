/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Immutable
public abstract class ModifierCondition implements Predicate<AquariumModifierContext> {

    public static final Codec<ModifierCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.MODIFIER_CONDITION_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ModifierCondition::getCodec, Function.identity());

    public static final Codec<Holder<ModifierCondition>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);
    public static final Codec<HolderSet<ModifierCondition>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);

    public static final Codec<List<ModifierCondition>> LIST_CODEC = AxCodecUtils.listOrElementCodec(DIRECT_CODEC);

    public abstract Codec<? extends ModifierCondition> getCodec();

    /**
     * @return a list of text components that describe this modifier condition
     */
    public abstract List<Component> getDescription();

    /**
     * @param intProvider an int provider
     * @return a component describing the given int provider
     */
    protected static Component createIntDescription(final IntProvider intProvider) {
        final String prefix = "axolootl.modifier_condition.int.";
        if(intProvider.getMaxValue() == intProvider.getMinValue()) {
            return Component.translatable(prefix + "exact", intProvider.getMaxValue());
        }
        if(intProvider.getMinValue() == Integer.MIN_VALUE) {
            return Component.translatable(prefix + "max", intProvider.getMaxValue());
        }
        if(intProvider.getMaxValue() == Integer.MAX_VALUE) {
            return Component.translatable(prefix + "min", intProvider.getMinValue());
        }
        return Component.translatable(prefix + "range", intProvider.getMinValue(), intProvider.getMaxValue());
    }

    /**
     * @param holderSet a holder set
     * @param toText a function to convert from the holder set element type to a component
     * @param <T> the holder set type
     * @return a list of components describing the holder set
     */
    protected static <T> List<Component> createHolderSetDescription(final HolderSet<T> holderSet, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<TagKey<T>, List<Holder<T>>> unwrapped = holderSet.unwrap();
        unwrapped.ifLeft(e -> list.add(Component.translatable("axolootl.modifier_condition.holder_set.tag", Component.literal("#" + e.location()).withStyle(ChatFormatting.GRAY))));
        unwrapped.ifRight(holderList -> {
            for(Holder<T> holder : holderList) {
                list.addAll(createHolderDescription(holder, toText));
            }
        });
        return list;
    }

    /**
     * @param holder a holder
     * @param toText a function to convert from the holder element type to a component
     * @param <T> the holder type
     * @return a list of components describing the holder
     */
    protected static <T> List<Component> createHolderDescription(final Holder<T> holder, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<ResourceKey<T>, T> unwrappedHolder = holder.unwrap();
        unwrappedHolder.ifLeft(id -> list.add(Component.literal(id.location().toString()).withStyle(ChatFormatting.GRAY)));
        unwrappedHolder.ifRight(o -> list.add(toText.apply(o)));
        return list;
    }

    protected static <T> List<Component> createCountedDescription(final String key, final IntProvider count, final HolderSet<T> holderSet, final Function<T, Component> toText) {
        final List<Component> descriptionList = createHolderSetDescription(holderSet, toText);
        // create description for single elements
        if(descriptionList.size() == 1) {
            return ImmutableList.of(Component.translatable(key + ".single", createIntDescription(count), descriptionList.get(0)));
        }
        // create description for multiple elements
        ImmutableList.Builder<Component> builder = ImmutableList.builder();
        builder.add(Component.translatable(key + ".multiple", createIntDescription(count)));
        for(Component c : descriptionList) {
            builder.add(Component.literal("  ").append(c));
        }
        return builder.build();
    }

    /**
     * @param access the registry access
     * @return the modifier condition registry
     */
    public static Registry<ModifierCondition> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.MODIFIER_CONDITIONS);
    }
}
