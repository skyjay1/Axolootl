/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding_modifier;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.data.breeding.AxolootlBreeding;
import axolootl.util.AxCodecUtils;
import com.mojang.datafixers.Products;
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

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.function.Function;

// TODO test to make sure ADD and REMOVE work as expected
@Immutable
public abstract class AxolootlBreedingModifier {

    public static final Codec<AxolootlBreedingModifier> DIRECT_CODEC = ExtraCodecs.lazyInitializedCodec(() -> AxRegistry.AXOLOOTL_BREEDING_MODIFIERS_SERIALIZERS_SUPPLIER.get().getCodec())
            .dispatch(AxolootlBreedingModifier::getCodec, Function.identity());

    public static final Codec<Holder<AxolootlBreedingModifier>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_BREEDING_MODIFIERS, DIRECT_CODEC);
    public static final Codec<HolderSet<AxolootlBreedingModifier>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_BREEDING_MODIFIERS, DIRECT_CODEC);

    public static final Codec<List<AxolootlBreedingModifier>> DIRECT_LIST_CODEC = AxCodecUtils.listOrElementCodec(DIRECT_CODEC);

    /** The predicate for the {@link AxolootlBreeding} object to target **/
    private final ResourceLocation target;

    public AxolootlBreedingModifier(ResourceLocation target) {
        this.target = target;
    }

    /**
     * Applies the breeding modifier
     * @param list the mutable list of weighted entries
     * @param phase the modifier phase
     */
    public abstract void apply(final List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> list, final Phase phase);

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

        public static final Codec<Phase> CODEC = Codec.STRING.xmap(Phase::getByName, Phase::getSerializedName);

        private final String name;

        private Phase(String name) {
            this.name = name;
        }

        public static Phase getByName(final String name) {
            for(Phase phase : values()) {
                if(phase.getSerializedName().equals(name)) {
                    return phase;
                }
            }
            return PRE;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

}
