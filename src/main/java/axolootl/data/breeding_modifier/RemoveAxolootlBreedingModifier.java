/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding_modifier;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class RemoveAxolootlBreedingModifier extends AxolootlBreedingModifier {

    public static final Codec<RemoveAxolootlBreedingModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(WeightedEntryPredicate.CODEC.fieldOf("predicate").forGetter(RemoveAxolootlBreedingModifier::getPredicate))
            .apply(instance, RemoveAxolootlBreedingModifier::new));

    private final WeightedEntryPredicate predicate;

    public RemoveAxolootlBreedingModifier(final ResourceLocation target, WeightedEntryPredicate predicate) {
        super(target);
        this.predicate = predicate;
    }

    @Override
    public void apply(final List<WeightedEntry.Wrapper<Holder<AxolootlVariant>>> list, final Phase phase) {
        // verify phase
        if(phase != Phase.REMOVE) {
            return;
        }
        // remove values
        list.removeIf(predicate);
    }

    @Override
    public Codec<? extends AxolootlBreedingModifier> getCodec() {
        return AxRegistry.AxolootlBreedingModifierReg.REMOVE.get();
    }

    //// GETTERS ////

    public WeightedEntryPredicate getPredicate() {
        return predicate;
    }
}
