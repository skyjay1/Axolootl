/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import java.util.List;

public class AndForgeCondition extends ForgeCondition {

    public static final Codec<AndForgeCondition> CODEC = LIST_CODEC
            .xmap(AndForgeCondition::new, AndForgeCondition::getChildren).fieldOf("values").codec();

    private final List<ForgeCondition> children;

    public AndForgeCondition(List<ForgeCondition> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<ForgeCondition> getChildren() {
        return children;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        for(ForgeCondition child : children) {
            if(!child.test(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.AND.get();
    }
}
