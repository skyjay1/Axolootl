/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.entity.IAxolootl;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AxolootlGeoRenderer<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends GeoEntityRenderer<T> {

    public AxolootlGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new AxolootlGeoModel<>());
        this.addRenderLayer(new PrimaryLayer<>(this));
        this.addRenderLayer(new SecondaryLayer<>(this));
        this.addRenderLayer(new FermiLayer<>(this));
        this.addRenderLayer(new AkxolootlLayer<>(this, new AkxolootlGeoModel<>()));
    }
}
