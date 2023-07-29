package axolootl.data.axolootl_variant.condition;

import net.minecraft.core.RegistryAccess;

public class ForgeConditionContext {

    private final RegistryAccess registryAccess;

    public ForgeConditionContext(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    //// GETTERS ////

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }
}
