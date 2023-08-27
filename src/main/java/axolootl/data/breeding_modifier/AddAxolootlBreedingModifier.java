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
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class AddAxolootlBreedingModifier extends AxolootlBreedingModifier {

    public static final Codec<AddAxolootlBreedingModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(WeightedEntry.Wrapper.codec(AxolootlVariant.HOLDER_CODEC).listOf().fieldOf("values").forGetter(AddAxolootlBreedingModifier::getValues))
            .apply(instance, AddAxolootlBreedingModifier::new));

    private final List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> values;

    public AddAxolootlBreedingModifier(final ResourceLocation target, List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> values) {
        super(target);
        this.values = ImmutableList.copyOf(values);
    }

    @Override
    public void apply(final List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> list, final Phase phase) {
        // verify phase
        if(phase != Phase.ADD) {
            return;
        }
        // add values
        list.addAll(getValues());
    }

    @Override
    public Codec<? extends AxolootlBreedingModifier> getCodec() {
        return AxRegistry.AxolootlBreedingModifierReg.ADD.get();
    }

    //// GETTERS ////

    public List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> getValues() {
        return values;
    }
}
