/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.aquarium_modifier.condition;

import axolootl.AxRegistry;
import axolootl.data.aquarium_modifier.AquariumModifierContext;
import axolootl.util.AxCodecUtils;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.MinMaxBounds;
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
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Immutable
public abstract class ModifierCondition implements Predicate<AquariumModifierContext> {

    public static final Codec<ModifierCondition> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.MODIFIER_CONDITION_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(ModifierCondition::getCodec, Function.identity());

    public static final Codec<Holder<ModifierCondition>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);
    public static final Codec<HolderSet<ModifierCondition>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.MODIFIER_CONDITIONS, DIRECT_CODEC);

    public static final Codec<List<ModifierCondition>> LIST_CODEC = AxCodecUtils.listOrElementCodec(DIRECT_CODEC);

    private final List<Component> description = new ArrayList<>();
    private final List<Component> descriptionView = Collections.unmodifiableList(description);

    /**
     * @return the serializer for this implementation
     */
    public abstract Codec<? extends ModifierCondition> getCodec();

    /**
     * @param registryAccess the registry access
     * @return a list of text components that describe this modifier condition
     */
    public final List<Component> getDescription(final RegistryAccess registryAccess) {
        if(description.isEmpty()) {
            description.addAll(createDescription(registryAccess));
        }
        return descriptionView;
    }

    /**
     * @param registryAccess the registry access
     * @return a list of text components that describe this modifier condition
     */
    protected abstract List<Component> createDescription(final RegistryAccess registryAccess);

    /**
     * @param intProvider an int provider
     * @return a component describing the given int provider
     */
    protected static Component createIntDescription(final MinMaxBounds.Ints intProvider) {
        final String prefix = "axolootl.modifier_condition.int.";
        if(intProvider.getMin() != null && intProvider.getMax() != null && intProvider.getMin().equals(intProvider.getMax())) {
            return Component.translatable(prefix + "exact", intProvider.getMax());
        }
        if(intProvider.getMin() != null && intProvider.getMax() != null && (intProvider.getMin() > 0) && (intProvider.getMax() < Integer.MAX_VALUE)) {
            return Component.translatable(prefix + "range", intProvider.getMin(), intProvider.getMax());
        }
        if(intProvider.getMax() != null) {
            return Component.translatable(prefix + "max", intProvider.getMax());
        }
        if(intProvider.getMin() != null) {
            return Component.translatable(prefix + "min", intProvider.getMin());
        }
        return Component.empty();
    }

    /**
     * @param holderSet a holder set
     * @param toText a function to convert from the holder set element type to a component
     * @param <T> the holder set type
     * @return a list of components describing the holder set
     */
    protected static <T> List<Component> createHolderSetDescription(final Registry<T> registry, final HolderSet<T> holderSet, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<TagKey<T>, List<Holder<T>>> unwrapped = holderSet.unwrap();
        unwrapped.ifLeft(e -> list.add(Component.translatable("axolootl.modifier_condition.holder_set.tag", Component.literal("#" + e.location()).withStyle(ChatFormatting.GRAY))));
        unwrapped.ifRight(holderList -> {
            for(Holder<T> holder : holderList) {
                list.addAll(createHolderDescription(registry, holder, toText));
            }
        });
        return list;
    }

    /**
     * @param registry the registry
     * @param holder a holder
     * @param toText a function to convert from the holder element type to a component
     * @param <T> the holder type
     * @return a list of components describing the holder
     */
    protected static <T> List<Component> createHolderDescription(final Registry<T> registry, final Holder<T> holder, final Function<T, Component> toText) {
        final List<Component> list = new ArrayList<>();
        Either<ResourceKey<T>, T> unwrappedHolder = holder.unwrap();
        unwrappedHolder.ifLeft(id -> list.add(toText.apply(registry.get(id))));
        unwrappedHolder.ifRight(o -> list.add(toText.apply(o)));
        return list;
    }

    /**
     * @param resourceKey a resource key
     * @param <T> the resource key type
     * @return the text representation of the resource key location
     */
    protected static <T> Component createResourceKeyDescription(ResourceKey<T> resourceKey) {
        // TODO improve resource key description
        return Component.literal(resourceKey.location().toString()).withStyle(ChatFormatting.GRAY);
    }

    protected static <T> List<Component> createCountedDescription(final String key, final MinMaxBounds.Ints count, final Registry<T> registry, final HolderSet<T> holderSet, final Function<T, Component> toText) {
        final List<Component> descriptionList = createHolderSetDescription(registry, holderSet, toText);
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
