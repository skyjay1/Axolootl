/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;

public class NotForgeCondition extends ForgeCondition {

    public static final Codec<NotForgeCondition> CODEC = DIRECT_CODEC
            .xmap(NotForgeCondition::new, NotForgeCondition::getChild).fieldOf("value").codec();

    private final ForgeCondition child;

    public NotForgeCondition(ForgeCondition child) {
        this.child = child;
    }

    public ForgeCondition getChild() {
        return child;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return !child.test(context);
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.NOT.get();
    }
}
