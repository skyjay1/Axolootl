/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.breeding_modifier;

import axolootl.AxRegistry;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.util.AxCodecUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Immutable
public class SequenceAxolootlBreedingModifier extends AxolootlBreedingModifier {

    public static final Codec<SequenceAxolootlBreedingModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(AxCodecUtils.listOrElementCodec(WeightedEntry.Wrapper.codec(AxolootlVariant.RESOURCE_KEY_CODEC)).fieldOf(Phase.ADD.getSerializedName()).forGetter(SequenceAxolootlBreedingModifier::getAdd))
            .and(AxCodecUtils.listOrElementCodec(WeightedEntryPredicate.CODEC).fieldOf(Phase.REMOVE.getSerializedName()).forGetter(SequenceAxolootlBreedingModifier::getRemove))
            .apply(instance, SequenceAxolootlBreedingModifier::new));

    private final List<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> add;
    private final List<WeightedEntryPredicate> remove;

    private final Map<Phase, List<AxolootlBreedingModifier>> values;

    public SequenceAxolootlBreedingModifier(final ResourceLocation target, List<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> add, List<WeightedEntryPredicate> remove) {
        super(target);
        this.add = add;
        this.remove = remove;
        this.values = Collections.unmodifiableMap(buildValues());
    }

    private Map<Phase, List<AxolootlBreedingModifier>> buildValues() {
        final EnumMap<Phase, List<AxolootlBreedingModifier>> builder = new EnumMap<>(Phase.class);
        for(Phase phase : Phase.values()) {
            builder.put(phase, buildModifiers(phase));
        }
        return builder;
    }

    private List<AxolootlBreedingModifier> buildModifiers(final Phase phase) {
        switch (phase) {
            case ADD: return ImmutableList.of(new AddAxolootlBreedingModifier(this.getTarget(), this.add));
            case REMOVE: return ImmutableList.copyOf(this.remove.stream().map(p -> new RemoveAxolootlBreedingModifier(this.getTarget(), p)).toList());
            case PRE: case POST: default: break;
        }
        return new ArrayList<>();
    }

    @Override
    public void apply(final List<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> list, final Phase phase) {
        getValues().getOrDefault(phase, ImmutableList.of()).forEach(entry -> entry.apply(list, phase));
    }

    @Override
    public Codec<? extends AxolootlBreedingModifier> getCodec() {
        return AxRegistry.AxolootlBreedingModifierReg.SEQUENCE.get();
    }

    //// GETTERS ////


    public List<WeightedEntry.Wrapper<ResourceKey<AxolootlVariant>>> getAdd() {
        return add;
    }

    public List<WeightedEntryPredicate> getRemove() {
        return remove;
    }

    public Map<Phase, List<AxolootlBreedingModifier>> getValues() {
        return values;
    }
}
