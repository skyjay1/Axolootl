/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.util.AxCodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.Level;

public class AxolootlBreeding {

    public static final Codec<AxolootlBreeding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AxolootlVariant.HOLDER_CODEC.fieldOf("first").forGetter(AxolootlBreeding::getFirst),
            AxolootlVariant.HOLDER_CODEC.fieldOf("second").forGetter(AxolootlBreeding::getSecond),
            AxCodecUtils.weightedListOrElementCodec(AxolootlVariant.HOLDER_CODEC).fieldOf("result").forGetter(AxolootlBreeding::getResult)
    ).apply(instance, AxolootlBreeding::new));

    public static final Codec<Holder<AxolootlBreeding>> HOLDER_CODEC = RegistryFileCodec.create(AxRegistry.Keys.AXOLOOTL_BREEDING, CODEC);
    public static final Codec<HolderSet<AxolootlBreeding>> HOLDER_SET_CODEC = RegistryCodecs.homogeneousList(AxRegistry.Keys.AXOLOOTL_BREEDING, CODEC);

    /** The first axolootl variant **/
    private final Holder<AxolootlVariant> first;
    /** The second axolootl variant **/
    private final Holder<AxolootlVariant> second;
    /** A weighted list to determine the result, if it is empty the first variant is used instead **/
    private final SimpleWeightedRandomList<Holder<AxolootlVariant>> result;

    public AxolootlBreeding(Holder<AxolootlVariant> first, Holder<AxolootlVariant> second,
                                  SimpleWeightedRandomList<Holder<AxolootlVariant>> result) {
        this.first = first;
        this.second = second;
        this.result = result;
    }

    //// METHODS ////

    /**
     * @param access the registry access
     * @return the axolootl variant registry
     */
    public static Registry<AxolootlBreeding> getRegistry(final RegistryAccess access) {
        return access.registryOrThrow(AxRegistry.Keys.AXOLOOTL_BREEDING);
    }

    /**
     * @param level the level
     * @param aFirst the first variant
     * @param aSecond the second variant
     * @return true if both variants match the variants defined in this object
     * @see #getFirst()
     * @see #getSecond()
     */
    public boolean matches(final Level level, final AxolootlVariant aFirst, final AxolootlVariant aSecond) {
        // create holders
        final ResourceLocation idFirst = aFirst.getRegistryName(level.registryAccess());
        final ResourceLocation idSecond = aSecond.getRegistryName(level.registryAccess());
        // check holder sets (order does not matter)
        return (this.first.is(idFirst) && this.second.is(idSecond)
            || (this.first.is(idSecond) && this.second.is(idFirst)));
    }

    //// GETTERS ////

    public Holder<AxolootlVariant> getFirst() {
        return first;
    }

    public Holder<AxolootlVariant> getSecond() {
        return second;
    }

    public SimpleWeightedRandomList<Holder<AxolootlVariant>> getResult() {
        return result;
    }
}
