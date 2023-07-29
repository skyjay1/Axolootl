package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import java.util.List;

public class OrForgeCondition extends ForgeCondition {

    public static final Codec<OrForgeCondition> CODEC = NON_EMPTY_LIST_CODEC
            .xmap(OrForgeCondition::new, OrForgeCondition::getChildren).fieldOf("values").codec();

    private final List<ForgeCondition> children;

    public OrForgeCondition(List<ForgeCondition> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<ForgeCondition> getChildren() {
        return children;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        for(ForgeCondition child : children) {
            if(child.test(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.OR.get();
    }
}
