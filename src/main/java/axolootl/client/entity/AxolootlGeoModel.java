/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.client.entity;

import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import axolootl.entity.IAxolootl;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LerpingModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector3f;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;

public class AxolootlGeoModel<T extends LivingEntity & LerpingModel & IAxolootl & GeoAnimatable> extends DefaultedEntityGeoModel<T> {

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

    private CoreGeoBone tail;
    private CoreGeoBone leftHindLeg;
    private CoreGeoBone rightHindLeg;
    private CoreGeoBone leftFrontLeg;
    private CoreGeoBone rightFrontLeg;
    private CoreGeoBone body;
    private CoreGeoBone head;
    private CoreGeoBone topGills;
    private CoreGeoBone leftGills;
    private CoreGeoBone rightGills;

    public AxolootlGeoModel() {
        super(new ResourceLocation(Axolootl.MODID, "axolootl"), false);
    }

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
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if(EMPTY_ANIMATIONS.equals(getAnimationResource(entity))) {
            setAxolotlAnimations(entity, instanceId, animationState);
        }
    }

    public void setAxolotlAnimations(T entity, long uniqueID, AnimationState<?> event) {
        // calculate values
        final float partialTick = event.getPartialTick();
        final float ageInTicks = entity.tickCount + partialTick;
        final Vec2 headRotations = getHeadRotations(event);
        final float headPitch = headRotations.x;
        final float netHeadYaw = headRotations.y;
        // populate bones
        this.tail = getAnimationProcessor().getBone(TAIL);
        this.leftHindLeg = getAnimationProcessor().getBone(LEFT_HIND_LEG);
        this.rightHindLeg = getAnimationProcessor().getBone(RIGHT_HIND_LEG);
        this.leftFrontLeg = getAnimationProcessor().getBone(LEFT_FRONT_LEG);
        this.rightFrontLeg = getAnimationProcessor().getBone(RIGHT_FRONT_LEG);
        this.body = getAnimationProcessor().getBone(BODY);
        this.head = getAnimationProcessor().getBone(HEAD);
        this.topGills = getAnimationProcessor().getBone(TOP_GILLS);
        this.leftGills = getAnimationProcessor().getBone(LEFT_GILLS);
        this.rightGills = getAnimationProcessor().getBone(RIGHT_GILLS);
        // verify non-null
        if(null == tail || null == leftHindLeg || null == rightHindLeg || null == leftFrontLeg || null == rightFrontLeg
                || null == body || null == head || null == leftGills || null == rightGills) {
            return;
        }
        // set up animations
        this.setupInitialAnimationValues(entity, netHeadYaw, headPitch);
        //if(true) return; // TODO debug
        if (entity.isEntityPlayingDead()) {
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
        body.setPosX(0.0F);
        body.setPosY(0.0F);
        head.setPosY(0.0F);
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

    protected Vector3f getRotationVector(CoreGeoBone bone) {
        return new Vector3f(bone.getRotX(), bone.getRotY(), bone.getRotZ());
    }

    protected void setRotationFromVector(CoreGeoBone bone, Vector3f vec) {
        setRotations(bone, vec.x(), vec.y(), vec.z());
    }


    protected float lerpTo(float start, float end) {
        return this.lerpTo(0.05F, start, end);
    }

    protected float lerpTo(float delta, float start, float end) {
        return Mth.rotLerp(delta, start, end);
    }

    protected void lerpPart(CoreGeoBone bone, float deltaX, float deltaY, float deltaZ) {
        setRotations(bone, this.lerpTo(bone.getRotX(), deltaX), this.lerpTo(bone.getRotY(), deltaY), this.lerpTo(bone.getRotZ(), deltaZ));
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

    protected void setRotations(final CoreGeoBone bone, final float x, final float y, final float z) {
        bone.setRotX(x);
        bone.setRotY(y);
        bone.setRotZ(z);
    }

    /**
     * @param event the animation event
     * @return the head pitch and net head yaw of the entity in radians
     */
    protected Vec2 getHeadRotations(AnimationState<?> event) {
        final EntityModelData data = event.getData(DataTickets.ENTITY_MODEL_DATA);
        return new Vec2(data.headPitch(), data.netHeadYaw()).scale(Mth.DEG_TO_RAD);
        
    }

    protected void setupLayStillOnGroundAnimation(float ageInTicks, float netHeadYaw) {
        float scaledAgeInTicks = ageInTicks * 0.09F;
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks);
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float f3 = sinAgeInTicks * sinAgeInTicks - 2.0F * sinAgeInTicks;
        float f4 = cosAgeInTicks * cosAgeInTicks - 3.0F * sinAgeInTicks;
        this.head.setRotX(this.lerpTo(this.head.getRotX(), -0.09F * f3));
        this.head.setRotY(this.lerpTo(this.head.getRotY(), 0.0F));
        this.head.setRotZ(this.lerpTo(this.head.getRotZ(), -0.2F));
        this.tail.setRotY(this.lerpTo(this.tail.getRotY(), -0.1F + 0.1F * f3));
        this.topGills.setRotX(this.lerpTo(this.topGills.getRotX(), 0.6F + 0.05F * f4));
        this.leftGills.setRotY(this.lerpTo(this.leftGills.getRotY(), -this.topGills.getRotX()));
        this.rightGills.setRotY(this.lerpTo(this.rightGills.getRotY(), -this.leftGills.getRotY()));
        this.lerpPart(this.leftHindLeg, 1.1F, 1.0F, 0.0F);
        this.lerpPart(this.leftFrontLeg, 0.8F, 2.3F, -0.5F);
        this.applyMirrorLegRotations();
        this.body.setRotX(this.lerpTo(0.2F, this.body.getRotX(), 0.0F));
        this.body.setRotY(this.lerpTo(this.body.getRotY(), netHeadYaw * (Mth.PI / 180F)));
        this.body.setRotZ(this.lerpTo(this.body.getRotZ(), 0.0F));
    }

    protected void setupGroundCrawlingAnimation(float ageInTicks, float netHeadYaw) {
        float scaledAgeInTicks = ageInTicks * 0.11F;
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float f2 = (cosAgeInTicks * cosAgeInTicks - 2.0F * cosAgeInTicks) / 5.0F;
        float scaledCosAgeInTicks = 0.7F * cosAgeInTicks;
        this.head.setRotX(this.lerpTo(this.head.getRotX(), 0.0F));
        this.head.setRotY(this.lerpTo(this.head.getRotY(), 0.09F * cosAgeInTicks));
        this.head.setRotZ(this.lerpTo(this.head.getRotZ(), 0.0F));
        this.tail.setRotY(this.lerpTo(this.tail.getRotY(), this.head.getRotY()));
        this.topGills.setRotX(this.lerpTo(this.topGills.getRotX(), 0.6F - 0.08F * (cosAgeInTicks * cosAgeInTicks + 2.0F * Mth.sin(scaledAgeInTicks))));
        this.leftGills.setRotY(this.lerpTo(this.leftGills.getRotY(), -this.topGills.getRotX()));
        this.rightGills.setRotY(this.lerpTo(this.rightGills.getRotY(), -this.leftGills.getRotY()));
        this.lerpPart(this.leftHindLeg, 0.9424779F, 1.5F - f2, 0.1F);
        this.lerpPart(this.leftFrontLeg, 1.0995574F, (Mth.PI / 2F) - scaledCosAgeInTicks, 0.0F);
        this.lerpPart(this.rightHindLeg, this.leftHindLeg.getRotX(), -1.0F - f2, 0.0F);
        this.lerpPart(this.rightFrontLeg, this.leftFrontLeg.getRotX(), (-Mth.PI / 2F) - scaledCosAgeInTicks, 0.0F);
        this.body.setRotX(this.lerpTo(0.2F, this.body.getRotX(), 0.0F));
        this.body.setRotY(this.lerpTo(this.body.getRotY(), netHeadYaw * (Mth.PI / 180F)));
        this.body.setRotZ(this.lerpTo(this.body.getRotZ(), 0.0F));
    }

    protected void setupWaterHoveringAnimation(float ageInTicks) {
        float scaledAgeInTicks = ageInTicks * 0.075F;
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks) * 0.15F;
        this.body.setRotX(this.lerpTo(this.body.getRotX(), -0.15F + 0.075F * cosAgeInTicks));
        // TODO this.body.setPosY(this.body.getPosY() - sinAgeInTicks);
        this.head.setRotX(this.lerpTo(this.head.getRotX(), -this.body.getRotX()));
        this.topGills.setRotX(this.lerpTo(this.topGills.getRotX(), 0.2F * cosAgeInTicks));
        this.leftGills.setRotY(this.lerpTo(this.leftGills.getRotY(), -0.3F * cosAgeInTicks - 0.19F));
        this.rightGills.setRotY(this.lerpTo(this.rightGills.getRotY(), -this.leftGills.getRotY()));
        this.lerpPart(this.leftHindLeg, -(2.3561945F - cosAgeInTicks * 0.11F), -0.47123894F, 1.7278761F);
        this.lerpPart(this.leftFrontLeg, -((Mth.PI / 4F) - cosAgeInTicks * 0.2F), -2.042035F, 0.0F);
        this.applyMirrorLegRotations();
        this.tail.setRotY(this.lerpTo(this.tail.getRotY(), 0.5F * cosAgeInTicks));
        this.head.setRotY(this.lerpTo(this.head.getRotY(), 0.0F));
        this.head.setRotZ(this.lerpTo(this.head.getRotZ(), 0.0F));
    }

    protected void setupSwimmingAnimation(float ageInTicks, float headPitch) {
        float scaledAgeInTicks = ageInTicks * 0.33F;
        float sinAgeInTicks = Mth.sin(scaledAgeInTicks);
        float cosAgeInTicks = Mth.cos(scaledAgeInTicks);
        float scaledSinAgeInTicks = 0.13F * sinAgeInTicks;
        this.body.setRotX(this.lerpTo(0.1F, this.body.getRotX(), headPitch * (Mth.PI / 180F) + scaledSinAgeInTicks));
        this.head.setRotX(-scaledSinAgeInTicks * 1.8F);
        // TODO this.body.setPosY(this.body.getPosY() - 0.45F * cosAgeInTicks);
        this.topGills.setRotX(this.lerpTo(this.topGills.getRotX(), -0.5F * sinAgeInTicks - 0.8F));
        this.leftGills.setRotY(this.lerpTo(this.leftGills.getRotY(), 0.3F * sinAgeInTicks + 0.9F));
        this.rightGills.setRotY(this.lerpTo(this.rightGills.getRotY(), -this.leftGills.getRotY()));
        this.tail.setRotY(this.lerpTo(this.tail.getRotY(), 0.3F * Mth.cos(scaledAgeInTicks * 0.9F)));
        this.lerpPart(this.leftHindLeg, -1.8849558F, -0.4F * sinAgeInTicks, (Mth.PI / 2F));
        this.lerpPart(this.leftFrontLeg, -1.8849558F, -0.2F * cosAgeInTicks - 0.1F, (Mth.PI / 2F));
        this.applyMirrorLegRotations();
        this.head.setRotY(this.lerpTo(this.head.getRotY(), 0.0F));
        this.head.setRotZ(this.lerpTo(this.head.getRotZ(), 0.0F));
    }

    protected void setupPlayDeadAnimation(float netHeadYaw) {
        this.lerpPart(this.leftHindLeg, 1.4137167F, 1.0995574F, (Mth.PI / 4F));
        this.lerpPart(this.leftFrontLeg, (Mth.PI / 4F), 2.042035F, 0.0F);
        this.body.setRotX(this.lerpTo(this.body.getRotX(), -0.15F));
        this.body.setRotZ(this.lerpTo(this.body.getRotZ(), 0.35F));
        this.applyMirrorLegRotations();
        this.body.setRotY(this.lerpTo(this.body.getRotY(), netHeadYaw * (Mth.PI / 180F)));
        this.head.setRotX(this.lerpTo(this.head.getRotX(), 0.0F));
        this.head.setRotY(this.lerpTo(this.head.getRotY(), 0.0F));
        this.head.setRotZ(this.lerpTo(this.head.getRotZ(), 0.0F));
        this.tail.setRotY(this.lerpTo(this.tail.getRotY(), 0.0F));
        this.lerpPart(this.topGills, 0.0F, 0.0F, 0.0F);
        this.lerpPart(this.leftGills, 0.0F, 0.0F, 0.0F);
        this.lerpPart(this.rightGills, 0.0F, 0.0F, 0.0F);
    }

    protected void applyMirrorLegRotations() {
        this.lerpPart(this.rightHindLeg, this.leftHindLeg.getRotX(), -this.leftHindLeg.getRotY(), -this.leftHindLeg.getRotZ());
        this.lerpPart(this.rightFrontLeg, this.leftFrontLeg.getRotX(), -this.leftFrontLeg.getRotY(), -this.leftFrontLeg.getRotZ());
    }

}
