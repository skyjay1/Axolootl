/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.IAxolootl;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

import java.util.Map;

public class AxolootlGeoModel<T extends LivingEntity & LerpingModel & IAxolootl & IAnimatable> extends AnimatedGeoModel<T> {

    private static final ResourceLocation EMPTY_ANIMATIONS = new ResourceLocation(Axolootl.MODID, "animations/entity/axolootl/axolootl.animation.json");

    private static final String TAIL = "tail";
    private static final String LEFT_HIND_LEG = "left_hind_leg";
    private static final String RIGHT_HIND_LEG = "right_hind_leg";
    private static final String LEFT_FRONT_LEG = "left_front_leg";
    private static final String RIGHT_FRONT_LEG = "right_front_leg";
    private static final String BODY = "body";
    private static final String HEAD = "head";
    private static final String TOP_GILLS = "top_gills";
    private static final String LEFT_GILLS = "left_gills";
    private static final String RIGHT_GILLS = "right_gills";

    private IBone tail;
    private IBone leftHindLeg;
    private IBone rightHindLeg;
    private IBone leftFrontLeg;
    private IBone rightFrontLeg;
    private IBone body;
    private IBone head;
    private IBone topGills;
    private IBone leftGills;
    private IBone rightGills;

    @Override
    public ResourceLocation getModelResource(T entity) {
        return entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings().getEntityGeoModel();
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings().getEntityTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return entity.getAxolootlVariant(entity.level.registryAccess()).orElse(AxolootlVariant.EMPTY).getModelSettings().getOptionalEntityGeoAnimations().orElse(EMPTY_ANIMATIONS);
    }

    @Override
    public void setCustomAnimations(T entity, int instanceId, AnimationEvent animationEvent) {
        super.setCustomAnimations(entity, instanceId, animationEvent);
        if(EMPTY_ANIMATIONS.equals(getAnimationResource(entity))) {
            setAxolotlAnimations(entity, instanceId, animationEvent);
        }
    }

    public void setAxolotlAnimations(T entity, Integer uniqueID, AnimationEvent<?> event) {
        // calculate values
        final float partialTick = event.getPartialTick();
        final float ageInTicks = entity.tickCount + partialTick;
        final Vec2 headRotations = getHeadRotations(entity, uniqueID, event);
        final float headPitch = headRotations.x;
        final float netHeadYaw = headRotations.y;
        // populate bones
        try {
            this.tail = getBone(TAIL);
            this.leftHindLeg = getBone(LEFT_HIND_LEG);
            this.rightHindLeg = getBone(RIGHT_HIND_LEG);
            this.leftFrontLeg = getBone(LEFT_FRONT_LEG);
            this.rightFrontLeg = getBone(RIGHT_FRONT_LEG);
            this.body = getBone(BODY);
            this.head = getBone(HEAD);
            this.topGills = getBone(TOP_GILLS);
            this.leftGills = getBone(LEFT_GILLS);
            this.rightGills = getBone(RIGHT_GILLS);
        } catch (RuntimeException e) {
            return;
        }
        // set up animations
        this.setupInitialAnimationValues(entity, netHeadYaw, headPitch);
        if (entity.isPlayingDead()) {
            this.setupPlayDeadAnimation(netHeadYaw);
            this.saveAnimationValues(entity);
        } else {
            boolean flag = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7D || entity.getXRot() != entity.xRotO || entity.getYRot() != entity.yRotO || entity.xOld != entity.getX() || entity.zOld != entity.getZ();
            if (entity.isInWaterOrBubble()) {
                if (flag) {
                    this.setupSwimmingAnimation(ageInTicks, headPitch);
                } else {
                    this.setupWaterHoveringAnimation(ageInTicks);
                }

                this.saveAnimationValues(entity);
            } else {
                if (entity.isOnGround()) {
                    if (flag) {
                        this.setupGroundCrawlingAnimation(ageInTicks, netHeadYaw);
                    } else {
                        this.setupLayStillOnGroundAnimation(ageInTicks, netHeadYaw);
                    }
                }

                this.saveAnimationValues(entity);
            }
        }
    }

    protected void setupInitialAnimationValues(T entity, float netHeadYaw, float headPitch) {
        body.setPositionX(0.0F);
        head.setPositionY(0.0F);
        body.setPositionY(0.0F);
        Map<String, Vector3f> map = entity.getModelRotationValues();
        if (map.isEmpty()) {
            setRotations(body, headPitch * (Mth.PI / 180F), netHeadYaw * (Mth.PI / 180F), 0.0F);
            setRotations(head, 0.0F, 0.0F, 0.0F);
            setRotations(leftHindLeg, 0.0F, 0.0F, 0.0F);
            setRotations(rightHindLeg, 0.0F, 0.0F, 0.0F);
            setRotations(leftFrontLeg, 0.0F, 0.0F, 0.0F);
            setRotations(rightFrontLeg, 0.0F, 0.0F, 0.0F);
            setRotations(leftGills, 0.0F, 0.0F, 0.0F);
            setRotations(rightGills, 0.0F, 0.0F, 0.0F);
            setRotations(topGills, 0.0F, 0.0F, 0.0F);
            setRotations(tail, 0.0F, 0.0F, 0.0F);
        } else {
            this.setRotationFromVector(body, map.get(BODY));
            this.setRotationFromVector(this.head, map.get(HEAD));
            this.setRotationFromVector(this.leftHindLeg, map.get(LEFT_HIND_LEG));
            this.setRotationFromVector(this.rightHindLeg, map.get(RIGHT_HIND_LEG));
            this.setRotationFromVector(this.leftFrontLeg, map.get(LEFT_FRONT_LEG));
            this.setRotationFromVector(this.rightFrontLeg, map.get(RIGHT_FRONT_LEG));
            this.setRotationFromVector(this.leftGills, map.get(LEFT_GILLS));
            this.setRotationFromVector(this.rightGills, map.get(RIGHT_GILLS));
            this.setRotationFromVector(this.topGills, map.get(TOP_GILLS));
            this.setRotationFromVector(this.tail, map.get(TAIL));
        }

    }

    protected Vector3f getRotationVector(IBone bone) {
        return new Vector3f(bone.getRotationX(), bone.getRotationY(), bone.getRotationZ());
    }

    protected void setRotationFromVector(IBone bone, Vector3f vec) {
        setRotations(bone, vec.x(), vec.y(), vec.z());
    }


    protected float lerpTo(float start, float end) {
        return this.lerpTo(0.05F, start, end);
    }

    protected float lerpTo(float delta, float start, float end) {
        return Mth.rotLerp(delta, start, end);
    }

    protected void lerpPart(IBone bone, float deltaX, float deltaY, float deltaZ) {
        setRotations(bone, this.lerpTo(bone.getRotationX(), deltaX), this.lerpTo(bone.getRotationY(), deltaY), this.lerpTo(bone.getRotationZ(), deltaZ));
    }

    protected void saveAnimationValues(T entity) {
        Map<String, Vector3f> map = entity.getModelRotationValues();
        map.put(BODY, this.getRotationVector(this.body));
        map.put(HEAD, this.getRotationVector(this.head));
        map.put(RIGHT_HIND_LEG, this.getRotationVector(this.rightHindLeg));
        map.put(LEFT_HIND_LEG, this.getRotationVector(this.leftHindLeg));
        map.put(RIGHT_FRONT_LEG, this.getRotationVector(this.rightFrontLeg));
        map.put(LEFT_FRONT_LEG, this.getRotationVector(this.leftFrontLeg));
        map.put(TAIL, this.getRotationVector(this.tail));
        map.put(TOP_GILLS, this.getRotationVector(this.topGills));
        map.put(LEFT_GILLS, this.getRotationVector(this.leftGills));
        map.put(RIGHT_GILLS, this.getRotationVector(this.rightGills));
    }

    protected void setRotations(final IBone bone, final float x, final float y, final float z) {
        bone.setRotationX(x);
        bone.setRotationY(y);
        bone.setRotationZ(z);
    }

    /**
     * @param entity the animatable entity
     * @param instanceId the animation event instance ID
     * @param event the animation event
     * @return the head pitch and net head yaw of the entity in radians
     */
    protected Vec2 getHeadRotations(T entity, int instanceId, AnimationEvent<?> event) {
        EntityModelData extraData = (EntityModelData) event.getExtraDataOfType(EntityModelData.class).get(0);
        AnimationData manager = entity.getFactory().getOrCreateAnimationData(instanceId);
        int unpausedMultiplier = !Minecraft.getInstance().isPaused() || manager.shouldPlayWhilePaused ? 1 : 0;
        return new Vec2(extraData.headPitch, extraData.netHeadYaw).scale(Mth.DEG_TO_RAD * unpausedMultiplier);
    }

    protected void setupLayStillOnGroundAnimation(float ageInTicks, float netHeadYaw) {
        float scaledAgeInTicks = ageInTicks * 0.09F;
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks);
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float f3 = sinAgeInTicks * sinAgeInTicks - 2.0F * sinAgeInTicks;
        float f4 = cosAgeInTicks * cosAgeInTicks - 3.0F * sinAgeInTicks;
        this.head.setRotationX(this.lerpTo(this.head.getRotationX(), -0.09F * f3));
        this.head.setRotationY(this.lerpTo(this.head.getRotationY(), 0.0F));
        this.head.setRotationZ(this.lerpTo(this.head.getRotationZ(), -0.2F));
        this.tail.setRotationY(this.lerpTo(this.tail.getRotationY(), -0.1F + 0.1F * f3));
        this.topGills.setRotationX(this.lerpTo(this.topGills.getRotationX(), 0.6F + 0.05F * f4));
        this.leftGills.setRotationY(this.lerpTo(this.leftGills.getRotationY(), -this.topGills.getRotationX()));
        this.rightGills.setRotationY(this.lerpTo(this.rightGills.getRotationY(), -this.leftGills.getRotationY()));
        this.lerpPart(this.leftHindLeg, 1.1F, 1.0F, 0.0F);
        this.lerpPart(this.leftFrontLeg, 0.8F, 2.3F, -0.5F);
        this.applyMirrorLegRotations();
        this.body.setRotationX(this.lerpTo(0.2F, this.body.getRotationX(), 0.0F));
        this.body.setRotationY(this.lerpTo(this.body.getRotationY(), netHeadYaw * (Mth.PI / 180F)));
        this.body.setRotationZ(this.lerpTo(this.body.getRotationZ(), 0.0F));
    }

    protected void setupGroundCrawlingAnimation(float ageInTicks, float netHeadYaw) {
        float scaledAgeInTicks = ageInTicks * 0.11F;
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float f2 = (cosAgeInTicks * cosAgeInTicks - 2.0F * cosAgeInTicks) / 5.0F;
        float scaledCosAgeInTicks = 0.7F * cosAgeInTicks;
        this.head.setRotationX(this.lerpTo(this.head.getRotationX(), 0.0F));
        this.head.setRotationY(this.lerpTo(this.head.getRotationY(), 0.09F * cosAgeInTicks));
        this.head.setRotationZ(this.lerpTo(this.head.getRotationZ(), 0.0F));
        this.tail.setRotationY(this.lerpTo(this.tail.getRotationY(), this.head.getRotationY()));
        this.topGills.setRotationX(this.lerpTo(this.topGills.getRotationX(), 0.6F - 0.08F * (cosAgeInTicks * cosAgeInTicks + 2.0F * Mth.sin(scaledAgeInTicks))));
        this.leftGills.setRotationY(this.lerpTo(this.leftGills.getRotationY(), -this.topGills.getRotationX()));
        this.rightGills.setRotationY(this.lerpTo(this.rightGills.getRotationY(), -this.leftGills.getRotationY()));
        this.lerpPart(this.leftHindLeg, 0.9424779F, 1.5F - f2, 0.1F);
        this.lerpPart(this.leftFrontLeg, 1.0995574F, (Mth.PI / 2F) - scaledCosAgeInTicks, 0.0F);
        this.lerpPart(this.rightHindLeg, this.leftHindLeg.getRotationX(), -1.0F - f2, 0.0F);
        this.lerpPart(this.rightFrontLeg, this.leftFrontLeg.getRotationX(), (-Mth.PI / 2F) - scaledCosAgeInTicks, 0.0F);
        this.body.setRotationX(this.lerpTo(0.2F, this.body.getRotationX(), 0.0F));
        this.body.setRotationY(this.lerpTo(this.body.getRotationY(), netHeadYaw * (Mth.PI / 180F)));
        this.body.setRotationZ(this.lerpTo(this.body.getRotationZ(), 0.0F));
    }

    protected void setupWaterHoveringAnimation(float ageInTicks) {
        float scaledAgeInTicks = ageInTicks * 0.075F;
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks) * 0.15F;
        this.body.setRotationX(this.lerpTo(this.body.getRotationX(), -0.15F + 0.075F * cosAgeInTicks));
        this.body.setPositionY(this.body.getPositionY() - sinAgeInTicks);
        this.head.setRotationX(this.lerpTo(this.head.getRotationX(), -this.body.getRotationX()));
        this.topGills.setRotationX(this.lerpTo(this.topGills.getRotationX(), 0.2F * cosAgeInTicks));
        this.leftGills.setRotationY(this.lerpTo(this.leftGills.getRotationY(), -0.3F * cosAgeInTicks - 0.19F));
        this.rightGills.setRotationY(this.lerpTo(this.rightGills.getRotationY(), -this.leftGills.getRotationY()));
        this.lerpPart(this.leftHindLeg, -(2.3561945F - cosAgeInTicks * 0.11F), -0.47123894F, 1.7278761F);
        this.lerpPart(this.leftFrontLeg, -((Mth.PI / 4F) - cosAgeInTicks * 0.2F), -2.042035F, 0.0F);
        this.applyMirrorLegRotations();
        this.tail.setRotationY(this.lerpTo(this.tail.getRotationY(), 0.5F * cosAgeInTicks));
        this.head.setRotationY(this.lerpTo(this.head.getRotationY(), 0.0F));
        this.head.setRotationZ(this.lerpTo(this.head.getRotationZ(), 0.0F));
    }

    protected void setupSwimmingAnimation(float ageInTicks, float headPitch) {
        float scaledAgeInTicks = ageInTicks * 0.33F;
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks);
        float coseAgeInTicks = Mth.cos(scaledAgeInTicks);
        float scaledSinAgeInTicks = 0.13F * sinAgeInTicks;
        this.body.setRotationX(this.lerpTo(0.1F, this.body.getRotationX(), headPitch * (Mth.PI / 180F) + scaledSinAgeInTicks));
        this.head.setRotationX(-scaledSinAgeInTicks * 1.8F);
        this.body.setPositionY(this.body.getPositionY() - 0.45F * coseAgeInTicks);
        this.topGills.setRotationX(this.lerpTo(this.topGills.getRotationX(), -0.5F * sinAgeInTicks - 0.8F));
        this.leftGills.setRotationY(this.lerpTo(this.leftGills.getRotationY(), 0.3F * sinAgeInTicks + 0.9F));
        this.rightGills.setRotationY(this.lerpTo(this.rightGills.getRotationY(), -this.leftGills.getRotationY()));
        this.tail.setRotationY(this.lerpTo(this.tail.getRotationY(), 0.3F * Mth.cos(scaledAgeInTicks * 0.9F)));
        this.lerpPart(this.leftHindLeg, -1.8849558F, -0.4F * sinAgeInTicks, (Mth.PI / 2F));
        this.lerpPart(this.leftFrontLeg, -1.8849558F, -0.2F * coseAgeInTicks - 0.1F, (Mth.PI / 2F));
        this.applyMirrorLegRotations();
        this.head.setRotationY(this.lerpTo(this.head.getRotationY(), 0.0F));
        this.head.setRotationZ(this.lerpTo(this.head.getRotationZ(), 0.0F));
    }

    protected void setupPlayDeadAnimation(float netHeadYaw) {
        this.lerpPart(this.leftHindLeg, 1.4137167F, 1.0995574F, (Mth.PI / 4F));
        this.lerpPart(this.leftFrontLeg, (Mth.PI / 4F), 2.042035F, 0.0F);
        this.body.setRotationX(this.lerpTo(this.body.getRotationX(), -0.15F));
        this.body.setRotationZ(this.lerpTo(this.body.getRotationZ(), 0.35F));
        this.applyMirrorLegRotations();
        this.body.setRotationY(this.lerpTo(this.body.getRotationY(), netHeadYaw * (Mth.PI / 180F)));
        this.head.setRotationX(this.lerpTo(this.head.getRotationX(), 0.0F));
        this.head.setRotationY(this.lerpTo(this.head.getRotationY(), 0.0F));
        this.head.setRotationZ(this.lerpTo(this.head.getRotationZ(), 0.0F));
        this.tail.setRotationY(this.lerpTo(this.tail.getRotationY(), 0.0F));
        this.lerpPart(this.topGills, 0.0F, 0.0F, 0.0F);
        this.lerpPart(this.leftGills, 0.0F, 0.0F, 0.0F);
        this.lerpPart(this.rightGills, 0.0F, 0.0F, 0.0F);
    }

    protected void applyMirrorLegRotations() {
        this.lerpPart(this.rightHindLeg, this.leftHindLeg.getRotationX(), -this.leftHindLeg.getRotationY(), -this.leftHindLeg.getRotationZ());
        this.lerpPart(this.rightFrontLeg, this.leftFrontLeg.getRotationX(), -this.leftFrontLeg.getRotationY(), -this.leftFrontLeg.getRotationZ());
    }

}
