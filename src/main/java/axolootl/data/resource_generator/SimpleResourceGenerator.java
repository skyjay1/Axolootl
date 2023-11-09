/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public abstract class SimpleResourceGenerator extends ResourceGenerator {

    private final Set<ResourceType> resourceType;

    public SimpleResourceGenerator(final ResourceType resourceType) {
        super();
        this.resourceType = ImmutableSet.of(resourceType);
    }

    @Override
    public Set<ResourceType> getResourceTypes() {
        return resourceType;
    }
}
