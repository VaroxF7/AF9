package com.af9.core.droppod.client;

import com.af9.core.AF9Core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The drop pod: an open cage on four thrusters under a canopy and a nose cone, a door that slides up at the front and a
 * restraint bar that swings out of the way when it lands. Built here from boxes (y is up, in pixels, the pod stands on
 * 0; vanilla's flipped convention below); the texture is drawn by {@code tools/textures/drop_pod.py}, whose box table has
 * to match {@link #createLayer}.
 */
public final class DropPodModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(AF9Core.MOD_ID, "drop_pod"), "main");

    private final ModelPart root;
    private final ModelPart door;
    private final ModelPart restraint;
    private final float doorRest;
    private final float restraintRest;

    public DropPodModel(ModelPart root) {
        this.root = root;
        ModelPart pod = root.getChild("pod");
        this.door = pod.getChild("door");
        this.restraint = pod.getChild("restraint");
        this.doorRest = door.y;
        this.restraintRest = restraint.xRot;
    }

    /** The texture is 128 x 192; every box's offset is its place on it. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition pod = mesh.getRoot().addOrReplaceChild("pod", CubeListBuilder.create()
                // the base plate, the back wall, the two side walls
                .texOffs(0, 0).addBox(-13, -8, -13, 26, 3, 26)
                .texOffs(0, 60).addBox(-13, -42, -13, 26, 34, 3)
                .texOffs(60, 60).addBox(-13, -30, -10, 3, 22, 20)
                .texOffs(0, 100).addBox(10, -30, -10, 3, 22, 20)
                // the canopy and the nose cone on it
                .texOffs(0, 30).addBox(-13, -46, -13, 26, 3, 26)
                .texOffs(48, 100).addBox(-10, -52, -10, 20, 6, 20)
                .texOffs(56, 164).addBox(-7, -58, -7, 14, 6, 14)
                .texOffs(84, 128).addBox(-4, -66, -4, 8, 8, 8)
                // the four posts of the cage
                .texOffs(0, 144).addBox(-13, -43, -13, 2, 35, 2)
                .texOffs(0, 144).addBox(11, -43, -13, 2, 35, 2)
                .texOffs(0, 144).addBox(-13, -43, 11, 2, 35, 2)
                .texOffs(0, 144).addBox(11, -43, 11, 2, 35, 2)
                // the four thrusters under the plate
                .texOffs(56, 152).addBox(-12, -5, -12, 6, 5, 6)
                .texOffs(56, 152).addBox(6, -5, -12, 6, 5, 6)
                .texOffs(56, 152).addBox(-12, -5, 6, 6, 5, 6)
                .texOffs(56, 152).addBox(6, -5, 6, 6, 5, 6),
                PartPose.offset(0, 24, 0));
        // the door in front (+z, where the rider looks): slides up
        pod.addOrReplaceChild("door", CubeListBuilder.create().texOffs(10, 144).addBox(-10, -36, 11, 20, 28, 2),
                PartPose.ZERO);
        // the restraint across the rider's chest: swings up and forward on its pivot
        pod.addOrReplaceChild("restraint", CubeListBuilder.create().texOffs(56, 144).addBox(-11, -1.5F, -1.5F, 22, 3, 3),
                PartPose.offset(0, -24, 9));
        return LayerDefinition.create(mesh, 128, 192);
    }

    /** 0 closed .. 1 open. */
    public void setup(float open) {
        // model y is down: up is negative
        door.y = doorRest - 30F * open;
        restraint.xRot = restraintRest - 1.53F * open;
    }

    public void render(PoseStack stack, VertexConsumer consumer, int light, int overlay) {
        root.render(stack, consumer, light, overlay, 1F, 1F, 1F, 1F);
    }
}
