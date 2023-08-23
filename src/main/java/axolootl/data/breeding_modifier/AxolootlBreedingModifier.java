/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding_modifier;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.breeding.AxolootlBreeding;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedEntry;

import java.util.List;
import java.util.function.Function;

// TODO test to make sure ADD and REMOVE work as expected
public abstract class AxolootlBreedingModifier {

    public static final Codec<AxolootlBreedingModifier> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(AxolootlBreedingModifier::getCodec, Function.identity());

    public static final Codec<Holder<AxolootlBreedingModifier>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_BREEDING_MODIFIERS, DIRECT_CODEC);
    public static final Codec<HolderSet<AxolootlBreedingModifier>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_BREEDING_MODIFIERS, DIRECT_CODEC);

    public static final Codec<List<AxolootlBreedingModifier>> DIRECT_LIST_CODEC = DIRECT_CODEC.listOf();
    public static final Codec<List<AxolootlBreedingModifier>> DIRECT_LIST_OR_SINGLE_CODEC = Codec.either(DIRECT_CODEC, DIRECT_LIST_CODEC)
            .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    /** The predicate for the {@link AxolootlBreeding} object to target **/
    private final ResourceLocation target;
    /** The phase to run this modifier **/
    private final Phase phase;

    public AxolootlBreedingModifier(ResourceLocation target, Phase phase) {
        this.target = target;
        this.phase = phase;
    }

    /**
     * Applies the breeding modifier
     * @param list the mutable list of weighted entries
     */
    public abstract void apply(final List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> list);

    /**
     * @return the breeding modifier codec
     */
    public abstract Codec<? extends AxolootlBreedingModifier> getCodec();

    /**
     * @param access the registry access
     * @return the axolootl breeding modifier registry
     */
    public static Registry<AxolootlBreedingModifier> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AXOLOOTL_BREEDING_MODIFIERS);
    }

    /**
     * Simplifies codec creation, especially if no other fields are added
     * @param instance the record codec builder with additional parameters, if any
     */
    protected static <T extends AxolootlBreedingModifier> Products.P1<RecordCodecBuilder.Mu<T>, ResourceLocation> codecStart(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(ResourceLocation.CODEC.fieldOf("target").forGetter(AxolootlBreedingModifier::getTarget));
    }

    //// GETTERS ////

    public ResourceLocation getTarget() {
        return target;
    }

    public Phase getPhase() {
        return phase;
    }

    //// CLASSES ////

    public static enum Phase implements StringRepresentable {
        /** Catch-all for anything that needs to run before standard phases **/
        PRE("pre"),
        /** Additional entries **/
        ADD("add"),
        /** Removal of entries **/
        REMOVE("remove"),
        /** Catch-all for anything that needs to run after standard phases **/
        POST("post");

        private final String name;

        private Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

}
