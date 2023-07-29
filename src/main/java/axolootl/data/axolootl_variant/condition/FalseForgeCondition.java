package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;

public class FalseForgeCondition extends ForgeCondition {

    public static final FalseForgeCondition INSTANCE = new FalseForgeCondition();

    public static final Codec<FalseForgeCondition> CODEC = Codec.unit(INSTANCE);

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.FALSE.get();
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return false;
    }
}
