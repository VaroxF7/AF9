package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.IForceFieldMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The DTPF's force field: a tube of light round the beam, from the front ring to the back ring, with a beam from every
 * injection casing into it, in one of four styles
 * ({@link ForceFieldGeometry}: 0 hoops, 1 plasma sheath, 2 helix, 3 lattice). A hall that merely stands glows low; a
 * recipe running makes it burn hard (the change is eased over a second or two).
 * <p>
 * Placed relative to the controller: the axis runs {@code start} to {@code end} blocks behind the controller's centre
 * (the controller sits in the middle of the front plate, at the beam's height), {@code radius} blocks wide, so the model
 * turns with the controller's facing. Drawn as see-through light that tests depth but never writes it
 * ({@link AF9RenderTypes}); with a shader pack ({@link IrisCompat}) in lightning's shader, which the packs light up, and
 * not into the shadow map. Model side: {@link com.af9.core.machine.AF9MachineModels#workableCasingWithForceField}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class ForceFieldRender extends DynamicRender<IForceFieldMachine, ForceFieldRender> {

    // spotless:off
    public static final Codec<ForceFieldRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("style").forGetter(render -> render.style),
            Codec.FLOAT.fieldOf("radius").forGetter(render -> render.radius),
            Codec.FLOAT.fieldOf("start").forGetter(render -> render.start),
            Codec.FLOAT.fieldOf("end").forGetter(render -> render.end),
            Codec.STRING.fieldOf("injectors").forGetter(render -> render.injectorSpec)
    ).apply(instance, ForceFieldRender::new));
    // spotless:on
    public static final DynamicRenderType<IForceFieldMachine, ForceFieldRender> TYPE = new DynamicRenderType<>(CODEC);

    /** How hard the field burns while the hall stands, and while a recipe runs; how fast it moves between (per frame). */
    private static final float IDLE = 0.4F, BURNING = 1F, EASE = 0.04F;
    /** How fast the injectors ramp up and down (per frame). */
    private static final float FEED_EASE = 0.03F;

    /** The burn and the injectors' feed of each machine on screen, eased towards their targets; machines that unload drop out. */
    private static final Map<MetaMachine, float[]> POWER = new WeakHashMap<>();

    private final int style;
    private final float radius;
    private final float start;
    private final float end;
    private final String injectorSpec;
    private final float[] injectors;

    public ForceFieldRender(int style, float radius, float start, float end, String injectors) {
        this.style = Mth.clamp(style, 0, ForceFieldGeometry.STYLES - 1);
        this.radius = radius;
        this.start = start;
        this.end = end;
        this.injectorSpec = injectors;
        this.injectors = parse(injectors);
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(int style, float radius, float start, float end, String injectors) {
        return new ForceFieldRender(style, radius, start, end, injectors);
    }

    /** "x,y,z;x,y,z" to a flat array; a malformed entry is skipped. */
    private static float[] parse(String spec) {
        String[] entries = spec.isBlank() ? new String[0] : spec.split(";");
        float[] out = new float[entries.length * 3];
        int n = 0;
        for (String entry : entries) {
            String[] parts = entry.trim().split(",");
            if (parts.length != 3) continue;
            try {
                out[n] = Float.parseFloat(parts[0].trim());
                out[n + 1] = Float.parseFloat(parts[1].trim());
                out[n + 2] = Float.parseFloat(parts[2].trim());
                n += 3;
            } catch (NumberFormatException e) {
                // skipped
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "force_field"), TYPE);
    }

    @Override
    public DynamicRenderType<IForceFieldMachine, ForceFieldRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    @Override
    public boolean shouldRender(IForceFieldMachine machine, Vec3 cameraPos) {
        if (!machine.isFieldFormed()) return false;
        MetaMachine self = machine.self();
        Direction back = back(self);
        Vec3 middle = Vec3.atCenterOf(self.getPos()).add(back.getStepX() * (start + end) / 2,
                back.getStepY() * (start + end) / 2, back.getStepZ() * (start + end) / 2);
        return middle.closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(IForceFieldMachine machine) {
        return true;
    }

    /** The whole tube. */
    @Override
    public AABB getRenderBoundingBox(IForceFieldMachine machine) {
        MetaMachine self = machine.self();
        Direction back = back(self);
        Vec3 from = Vec3.atCenterOf(self.getPos());
        Vec3 to = from.add(back.getStepX() * end, back.getStepY() * end, back.getStepZ() * end);
        return new AABB(from, to).inflate(radius + 2);
    }

    @Override
    public void render(IForceFieldMachine machine, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.isFieldFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        MetaMachine self = machine.self();
        // the field burns with its strength: faint while it builds, hard while a recipe runs
        float target = (machine.isFieldBurning() ? BURNING : IDLE) * (0.12F + 0.88F * machine.getFieldLevel());
        float feedTarget = machine.isFieldInjecting() ? 1F : 0F;
        float[] eased = POWER.computeIfAbsent(self, key -> new float[] { target, feedTarget });
        eased[0] += (target - eased[0]) * EASE;
        eased[1] += (feedTarget - eased[1]) * FEED_EASE;
        float power = eased[0];
        float time = self.getOffsetTimer() + partialTick;

        boolean pack = IrisCompat.shaderPackInUse();
        VertexConsumer shell = buffer.getBuffer(pack ? AF9RenderTypes.SHADER_RING : AF9RenderTypes.LIGHT_RING);
        VertexConsumer glow = buffer.getBuffer(pack ? AF9RenderTypes.SHADER_RING : AF9RenderTypes.LIGHTNING);

        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction left = RelativeDirection.LEFT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction up = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction back = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);

        poseStack.pushPose();
        // the middle of the controller block: the field's axis runs back from here, through the beam
        poseStack.translate(0.5, 0.5, 0.5);
        Matrix4f mat = poseStack.last().pose();
        new ForceFieldGeometry((layer, v) -> {
            VertexConsumer out = layer == ForceFieldGeometry.SHELL ? shell : glow;
            for (int i = 0; i < 4; i++) {
                int o = i * 7;
                float x = v[o], y = v[o + 1], z = v[o + 2];
                out.vertex(mat, x * left.getStepX() + y * up.getStepX() + z * back.getStepX(),
                        x * left.getStepY() + y * up.getStepY() + z * back.getStepY(),
                        x * left.getStepZ() + y * up.getStepZ() + z * back.getStepZ())
                        .color(v[o + 3], v[o + 4], v[o + 5], v[o + 6]).endVertex();
            }
        }, radius, start, end, time, power).injectors(injectors, eased[1]).emit(style);
        poseStack.popPose();
    }

    private static Direction back(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        return RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
    }
}
