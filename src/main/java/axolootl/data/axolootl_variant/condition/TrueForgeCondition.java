package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;

public class TrueForgeCondition extends ForgeCondition {

    public static final TrueForgeCondition INSTANCE = new TrueForgeCondition();

    public static final Codec<TrueForgeCondition> CODEC = Codec.unit(INSTANCE);

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.TRUE.get();
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return true;
    }
}
