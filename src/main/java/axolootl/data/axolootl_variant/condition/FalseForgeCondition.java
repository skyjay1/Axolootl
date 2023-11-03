/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

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
