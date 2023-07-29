package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import java.util.List;

public class AndForgeCondition extends ForgeCondition {

    public static final Codec<AndForgeCondition> CODEC = LIST_OR_SINGLE_CODEC
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
