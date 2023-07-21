/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.function.Function;

@Immutable
public class Bonuses {

    private static final long FALLBACK_DURATION = 2000L;

    public static final Bonuses EMPTY = new Bonuses(0, 0, 0, 0);

    public static final Codec<Bonuses> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("generation", 0.0D).forGetter(Bonuses::getGenerationBonus),
            Codec.DOUBLE.optionalFieldOf("breed", 0.0D).forGetter(Bonuses::getBreedBonus),
            Codec.DOUBLE.optionalFieldOf("feed", 0.0D).forGetter(Bonuses::getFeedBonus),
            Codec.LONG.optionalFieldOf("duration", FALLBACK_DURATION).forGetter(Bonuses::getDuration)
    ).apply(instance, Bonuses::new));

    public static final Codec<Bonuses> CODEC = Codec.either(Codec.DOUBLE, DIRECT_CODEC)
            .xmap(either -> either.map(Bonuses::new, Function.identity()),
                    obj -> obj.isSimple() ? Either.left(obj.getGenerationBonus()) : Either.right(obj));

    private final double generationBonus;
    private final double breedBonus;
    private final double feedBonus;
    private final long duration;

    /**
     * Creates a Bonuses object with the default duration using generationBonus for the given value and 0 for all other values
     * @param generationBonus the generation bonus
     */
    public Bonuses(double generationBonus) {
        this(generationBonus, 0, 0, FALLBACK_DURATION);
    }

    public Bonuses(double generationBonus, double breedBonus, double feedBonus, long duration) {
        this.generationBonus = generationBonus;
        this.breedBonus = breedBonus;
        this.feedBonus = feedBonus;
        this.duration = duration;
    }

    //// GETTERS ////

    public double getGenerationBonus() {
        return generationBonus;
    }
    public double getBreedBonus() {
        return breedBonus;
    }

    public double getFeedBonus() {
        return feedBonus;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isSimple() {
        return !(this.breedBonus > 0) && !(this.feedBonus > 0) && this.duration == FALLBACK_DURATION;
    }

    //// EQUALITY ////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bonuses)) return false;
        Bonuses bonuses = (Bonuses) o;
        return Double.compare(bonuses.generationBonus, generationBonus) == 0
                && Double.compare(bonuses.breedBonus, breedBonus) == 0
                && Double.compare(bonuses.feedBonus, feedBonus) == 0
                && bonuses.duration == duration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(generationBonus, breedBonus, feedBonus, duration);
    }
}
