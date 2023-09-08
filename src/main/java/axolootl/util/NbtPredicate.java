/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import axolootl.AxRegistry;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;

import javax.annotation.Nullable;
import java.util.Optional;

public class NbtPredicate implements BlockPredicate {

    public static final NbtPredicate ANY = new NbtPredicate(null);

    public static final Codec<NbtPredicate> CODEC = Codec.STRING.flatXmap(NbtPredicate::parse, o -> DataResult.success(o.getTagString()));

    @Nullable
    private final CompoundTag tag;
    @Nullable
    private final String tagString;

    public NbtPredicate(@Nullable CompoundTag pTag) {
        this.tag = pTag;
        this.tagString = (pTag != null) ? pTag.getAsString() : "";
    }

    //// GETTERS ////

    public Optional<CompoundTag> getTag() {
        return Optional.ofNullable(tag);
    }

    //// HELPER METHODS ////

    public boolean matches(BlockEntity blockEntity) {
        return this == ANY || this.matches(blockEntity.saveWithoutMetadata());
    }

    public boolean matches(@Nullable Tag pTag) {
        if (pTag == null) {
            return this == ANY;
        } else {
            return this.tag == null || NbtUtils.compareNbt(this.tag, pTag, true);
        }
    }

    //// METHODS ////

    @Override
    public BlockPredicateType<?> type() {
        return AxRegistry.BlockPredicateTypesReg.TAG.get();
    }

    @Override
    public boolean test(WorldGenLevel level, BlockPos blockPos) {
        final BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if(null == blockEntity) {
            return false;
        }
        return matches(blockEntity);
    }

    //// SERIALIZATION ////

    public String getTagString() {
        return this.tagString;
    }

    public static DataResult<NbtPredicate> parse(@Nullable final String tagString) {
        // check for null or empty
        if(null == tagString || tagString.isEmpty()) {
            return DataResult.success(ANY);
        }
        // parse tag from string
        CompoundTag compoundTag;
        try {
            compoundTag = TagParser.parseTag(tagString);
        } catch (CommandSyntaxException e) {
            return DataResult.error("Invalid nbt tag: " + e.getMessage());
        }
        return DataResult.success(new NbtPredicate(compoundTag));
    }
}
