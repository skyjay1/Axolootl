/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

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
