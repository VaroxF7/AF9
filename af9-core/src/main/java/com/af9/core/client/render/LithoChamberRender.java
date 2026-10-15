package com.af9.core.client.render;

import com.af9.core.AF9Core;
import com.af9.core.machine.ILithoChamberMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

import static net.minecraft.util.FastColor.ARGB32.*;

/**
 * The exposure chamber inside the Mk1 line's stepper and the Mk2 scanner's tube, seen through the glass:
 * <ul>
 * <li>a violet UV fill light (dim on standby, bright while exposing);</li>
 * <li>the wafer on its stage, exposed die by die in the node's colour as the print runs;</li>
 * <li>the exposure head above it with a beam down to the die being written — the laser sweep follows the
 * print's progress, so it finishes when the print does;</li>
 * <li>a robot arm on a slide along the tube: it loads the blank from the front (out of sight), parks while
 * the laser writes, then carries the printed wafer out the back into the output (out of sight).</li>
 * </ul>
 * Placed relative to the controller: {@code up} along its up and {@code back} behind it, centred on the tube.
 * Model side: {@code AF9MachineModels.workableCasingWithChamber}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class LithoChamberRender extends DynamicRender<ILithoChamberMachine, LithoChamberRender> {

    // spotless:off
    public static final Codec<LithoChamberRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("up").forGetter(render -> render.up),
            Codec.FLOAT.fieldOf("back").forGetter(render -> render.back)
    ).apply(instance, LithoChamberRender::new));
    // spotless:on
    public static final DynamicRenderType<ILithoChamberMachine, LithoChamberRender> TYPE = new DynamicRenderType<>(
            CODEC);

    private static final int FULL_BRIGHT = 0xF000F0;
    /** UV violet fill, before the node's colour is mixed in. */
    private static final int UV_R = 0x6B, UV_G = 0x4D, UV_B = 0xFF;
    /**
     * The print's phases, as fractions of its progress (all in the chamber's own space, z from the front to the back):
     * the outer gate of the airlock opens and the blank slides in (0-0.08), the lock pumps down (0.08-0.12), the inner
     * gate opens and the overhead gantry fetches the wafer with its vacuum pad and sets it on the stage (0.12-0.44), the
     * head writes the dies (0.46-0.70), the gantry brings the wafer back to the lock (0.70-0.90), the lock vents and the
     * wafer leaves through the outer gate (0.90-1.0).
     */
    private static final float EXPOSE_START = 0.46F, EXPOSE_END = 0.70F;
    /** Chamber box half extents: 0.9 wide/high, 1.8 long. */
    private static final float HALF_W = 0.45F, HALF_H = 0.45F, HALF_L = 0.9F;
    /** The tube is 1 x 1: floor and ceiling plates, the rail under the ceiling, the airlock at the front. */
    private static final float FLOOR_TOP = -0.46F, CEILING_BOTTOM = 0.46F, RAIL_BOTTOM = 0.38F;
    /** The wafer: radius, thickness, resting height (on the stage and on the lock's pedestal), carried height. */
    private static final float WAFER_R = 0.21F, WAFER_T = 0.035F, REST_Y = -0.2925F, CARRY_Y = -0.20F;
    /** Along the chamber: outer and inner gate, the pedestal in the lock, the stage, the gantry's parking, the head's. */
    private static final float GATE_OUT = -0.90F, GATE_IN = -0.38F, LOCK_Z = -0.645F, STAGE_Z = 0.30F,
            PARK_Z = -0.15F, HEAD_PARK_Z = 0.78F;
    private static final float PAD_R = 0.09F, PAD_H = 0.02F;
    private static final int DIES = 4;

    private final float up;
    private final float back;

    public LithoChamberRender(float up, float back) {
        this.up = up;
        this.back = back;
    }

    /** Typed as GT's base class, so common code that builds a model never loads this client class. */
    public static DynamicRender<?, ?> create(float up, float back) {
        return new LithoChamberRender(up, back);
    }

    /** Client, before the models are built (mod construction). */
    public static void register() {
        DynamicRenderManager.register(new ResourceLocation(AF9Core.MOD_ID, "litho_chamber"), TYPE);
    }

    @Override
    public DynamicRenderType<ILithoChamberMachine, LithoChamberRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    @Override
    public boolean shouldRender(ILithoChamberMachine machine, Vec3 cameraPos) {
        if (!machine.isChamberFormed()) return false;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        return centre.closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(ILithoChamberMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(ILithoChamberMachine machine) {
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        Vec3 centre = Vec3.atLowerCornerOf(self.getPos()).add(frame.x, frame.y, frame.z);
        return new AABB(centre, centre).inflate(1.5);
    }

    @Override
    public void render(ILithoChamberMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!machine.isChamberFormed()) return;
        if (IrisCompat.renderingShadowPass()) return;
        MetaMachine self = machine.self();
        Frame frame = frame(self);
        float progress = machine.getChamberProgress(partialTick);
        boolean working = machine.isChamberLit() && progress >= 0;
        int color = machine.getChamberColor();
        double time = self.getOffsetTimer() + partialTick;

        poseStack.pushPose();
        poseStack.translate(frame.x, frame.y, frame.z);

        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(AF9RenderTypes.WHITE));
        VertexConsumer glow = buffer.getBuffer(AF9RenderTypes.LIGHT_RING);
        VertexConsumer beam = buffer.getBuffer(AF9RenderTypes.LIGHTNING);
        Matrix4f mat = poseStack.last().pose();

        // --- UV fill: dim violet standby, bright + node colour while exposing ---
        float pulse = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 50));
        int cr = red(color), cg = green(color), cb = blue(color);
        int fr, fg, fb;
        float fillAlpha;
        if (!working) {
            fr = UV_R;
            fg = UV_G;
            fb = UV_B;
            fillAlpha = 0.10F + 0.02F * pulse;
        } else {
            float expose = progress < EXPOSE_START ? 0.3F : progress < EXPOSE_END ? 1F : 0.5F;
            fr = Math.round(Mth.lerp(0.35F * expose, UV_R, cr));
            fg = Math.round(Mth.lerp(0.35F * expose, UV_G, cg));
            fb = Math.round(Mth.lerp(0.35F * expose, UV_B, cb));
            fillAlpha = 0.16F + 0.12F * expose + 0.03F * pulse * expose;
        }
        fillBox(glow, mat, frame, -HALF_W, -HALF_H, -HALF_L, HALF_W, HALF_H, HALF_L, fr, fg, fb, fillAlpha);

        // --- the print's state from its progress; idle: gates shut, gantry and head parked, no wafer ---
        float p = working ? progress : 0F;
        float outerOpen = p < 0.06F ? ramp(p, 0F, 0.04F) : p < 0.96F ? 1F - ramp(p, 0.06F, 0.08F) :
                ramp(p, 0.96F, 0.98F);
        float innerOpen = p < 0.12F ? 0F : p < 0.44F ? ramp(p, 0.12F, 0.14F) :
                p < 0.46F ? 1F - ramp(p, 0.44F, 0.46F) : p < 0.70F ? 0F : p < 0.93F ? ramp(p, 0.70F, 0.72F) :
                1F - ramp(p, 0.93F, 0.95F);
        float gantryZ = p < 0.14F ? PARK_Z : p < 0.20F ? Mth.lerp(ramp(p, 0.14F, 0.20F), PARK_Z, LOCK_Z) :
                p < 0.24F ? LOCK_Z : p < 0.34F ? Mth.lerp(ramp(p, 0.24F, 0.34F), LOCK_Z, STAGE_Z) :
                p < 0.39F ? STAGE_Z : p < 0.44F ? Mth.lerp(ramp(p, 0.39F, 0.44F), STAGE_Z, PARK_Z) :
                p < 0.72F ? PARK_Z : p < 0.76F ? Mth.lerp(ramp(p, 0.72F, 0.76F), PARK_Z, STAGE_Z) :
                p < 0.80F ? STAGE_Z : p < 0.86F ? Mth.lerp(ramp(p, 0.80F, 0.86F), STAGE_Z, LOCK_Z) :
                p < 0.90F ? LOCK_Z : Mth.lerp(ramp(p, 0.90F, 0.93F), LOCK_Z, PARK_Z);
        // how far the pad is down on the wafer (0: at carrying height)
        float down = p < 0.20F ? 0F : p < 0.22F ? ramp(p, 0.20F, 0.22F) : p < 0.24F ? 1F - ramp(p, 0.22F, 0.24F) :
                p < 0.34F ? 0F : p < 0.36F ? ramp(p, 0.34F, 0.36F) : p < 0.37F ? 1F :
                p < 0.39F ? 1F - ramp(p, 0.37F, 0.39F) : p < 0.76F ? 0F : p < 0.78F ? ramp(p, 0.76F, 0.78F) :
                p < 0.80F ? 1F - ramp(p, 0.78F, 0.80F) : p < 0.86F ? 0F : p < 0.88F ? ramp(p, 0.86F, 0.88F) :
                p < 0.90F ? 1F - ramp(p, 0.88F, 0.90F) : 0F;
        float padBottom = Mth.lerp(down, CARRY_Y + WAFER_T / 2, REST_Y + WAFER_T / 2);
        // the wafer: slides in through the outer gate, rests on the pedestal, is carried to the stage, exposed there,
        // carried back, rests on the pedestal and slides out
        boolean waferShown = working && p >= 0.02F;
        boolean held = (p >= 0.22F && p < 0.36F) || (p >= 0.78F && p < 0.88F);
        float waferY = REST_Y, waferZ, waferAlpha = 1F;
        if (p < 0.22F) {
            waferZ = Mth.lerp(ramp(p, 0.02F, 0.07F), GATE_OUT - 0.1F, LOCK_Z);
            waferAlpha = ramp(p, 0.02F, 0.04F);
        } else if (p < 0.36F) {
            waferZ = gantryZ;
        } else if (p < 0.78F) {
            waferZ = STAGE_Z;
        } else if (p < 0.88F) {
            waferZ = gantryZ;
        } else {
            waferZ = Mth.lerp(ramp(p, 0.97F, 1.0F), LOCK_Z, GATE_OUT - 0.1F);
            waferAlpha = 1F - ramp(p, 0.98F, 1.0F);
        }
        if (held) waferY = padBottom - WAFER_T / 2;
        float exposed = p < EXPOSE_START ? 0F : p < EXPOSE_END ? (p - EXPOSE_START) / (EXPOSE_END - EXPOSE_START) : 1F;
        boolean beamOn = working && p >= EXPOSE_START && p < EXPOSE_END;
        float headMix = p < 0.70F ? ramp(p, 0.44F, 0.46F) : 1F - ramp(p, 0.70F, 0.72F);
        float[] spot = dieSpot(exposed);
        float headX = spot[0] * headMix;
        float headZ = Mth.lerp(headMix, HEAD_PARK_Z, STAGE_Z + spot[1]);

        // --- the tube's inside: floor and ceiling plates, the rail, the stage, the back wall ---
        SolidBox.box(solid, mat, frame, -0.495F, -0.5F, -HALF_L, 0.495F, FLOOR_TOP, HALF_L, 0x1C2433);
        SolidBox.box(solid, mat, frame, -0.495F, CEILING_BOTTOM, -HALF_L, 0.495F, 0.5F, HALF_L, 0x232C3D);
        SolidBox.box(solid, mat, frame, -0.495F, -0.5F, HALF_L, 0.495F, 0.5F, HALF_L + 0.03F, 0x151B28); // back wall
        quad(glow, mat, frame, -0.30F, CEILING_BOTTOM - 0.002F, -0.8F, -0.24F, CEILING_BOTTOM - 0.002F, 0.8F, 170, 190,
                255, 0.55F);   // ceiling light strips
        quad(glow, mat, frame, 0.24F, CEILING_BOTTOM - 0.002F, -0.8F, 0.30F, CEILING_BOTTOM - 0.002F, 0.8F, 170, 190,
                255, 0.55F);
        SolidBox.box(solid, mat, frame, -0.03F, RAIL_BOTTOM + 0.04F, -0.86F, 0.03F, CEILING_BOTTOM, 0.86F, 0x4A5568); // rail
        // the stage: a chuck on a column, the wafer's resting place
        SolidBox.box(solid, mat, frame, -0.10F, FLOOR_TOP, STAGE_Z - 0.10F, 0.10F, -0.37F, STAGE_Z + 0.10F, 0x2A3346);
        SolidBox.disc(solid, mat, frame, 0F, REST_Y - WAFER_T / 2 - 0.03F, STAGE_Z, WAFER_R + 0.03F, 0.06F, 0x3B4A5A, 1F);
        // the lock's pedestal, the same height
        SolidBox.box(solid, mat, frame, -0.08F, FLOOR_TOP, LOCK_Z - 0.08F, 0.08F, -0.37F, LOCK_Z + 0.08F, 0x2A3346);
        SolidBox.disc(solid, mat, frame, 0F, REST_Y - WAFER_T / 2 - 0.03F, LOCK_Z, WAFER_R + 0.005F, 0.06F, 0x3B4A5A, 1F);
        // the head's parking pocket at the back, under the rail
        SolidBox.box(solid, mat, frame, -0.30F, 0.30F, HEAD_PARK_Z + 0.10F, 0.30F, CEILING_BOTTOM, HALF_L, 0x1E2636);

        // --- the airlock: two gates, a frame at each; a gate sinks into the floor to open (its slot has a notch for
        // the rail, so a shut gate never touches it) ---
        drawGate(solid, glow, mat, frame, GATE_OUT, outerOpen, cr, cg, cb);
        drawGate(solid, glow, mat, frame, GATE_IN, innerOpen, cr, cg, cb);
        // the lock's lamp: amber while it pumps down or vents, green with a gate open, dim otherwise
        boolean cycling = (p >= 0.08F && p < 0.12F) || (p >= 0.94F && p < 0.97F);
        float blink = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 14));
        if (cycling) {
            quad(glow, mat, frame, 0.36F, 0.30F, GATE_IN - 0.045F, 0.42F, 0.36F, GATE_IN - 0.045F, 255, 190, 40,
                    0.5F + 0.5F * blink);
        } else if (outerOpen > 0.5F || innerOpen > 0.5F) {
            quad(glow, mat, frame, 0.36F, 0.30F, GATE_IN - 0.045F, 0.42F, 0.36F, GATE_IN - 0.045F, 80, 255, 140, 0.9F);
        } else {
            quad(glow, mat, frame, 0.36F, 0.30F, GATE_IN - 0.045F, 0.42F, 0.36F, GATE_IN - 0.045F, 120, 130, 150,
                    0.35F);
        }

        // --- the gantry: a carriage on the rail, a column down to the vacuum pad ---
        float padTop = padBottom + PAD_H;
        SolidBox.box(solid, mat, frame, -0.07F, RAIL_BOTTOM, gantryZ - 0.08F, 0.07F, CEILING_BOTTOM - 0.02F,
                gantryZ + 0.08F, 0x8A94A6);
        SolidBox.box(solid, mat, frame, -0.012F, padTop, gantryZ - 0.012F, 0.012F, RAIL_BOTTOM, gantryZ + 0.012F,
                0x6B7688);
        SolidBox.box(solid, mat, frame, -PAD_R, padBottom, gantryZ - PAD_R, PAD_R, padTop, gantryZ + PAD_R, 0xB8C4D4);
        if (held) {
            quad(glow, mat, frame, -0.03F, padTop + 0.001F, gantryZ - 0.03F, 0.03F, padTop + 0.003F, gantryZ + 0.03F,
                    cr, cg, cb, 0.6F * waferAlpha);
        }
        if (waferShown && waferAlpha > 0.01F) {
            drawWafer(solid, glow, mat, frame, 0F, waferY, waferZ, exposed, beamOn, cr, cg, cb, waferAlpha, time);
        }

        // --- the exposure head: hangs from the rail on its own carriage and a cross beam, parked at the back ---
        if (working && headMix > 0.001F) {
            SolidBox.box(solid, mat, frame, -0.06F, RAIL_BOTTOM, headZ - 0.06F, 0.06F, CEILING_BOTTOM - 0.02F,
                    headZ + 0.06F, 0x8A94A6);
            SolidBox.box(solid, mat, frame, -0.012F, 0.30F, headZ - 0.012F, 0.012F, RAIL_BOTTOM, headZ + 0.012F,
                    0x6B7688);
            SolidBox.box(solid, mat, frame, -0.16F, 0.30F, headZ - 0.02F, 0.16F, 0.33F, headZ + 0.02F, 0x4A5568);
            SolidBox.box(solid, mat, frame, headX - 0.07F, 0.20F, headZ - 0.07F, headX + 0.07F, 0.30F, headZ + 0.07F,
                    0x1A2233);
            SolidBox.box(solid, mat, frame, headX - 0.04F, 0.16F, headZ - 0.04F, headX + 0.04F, 0.20F, headZ + 0.04F,
                    0xB8C4D4);
            if (beamOn) {
                float top = REST_Y + WAFER_T / 2;
                quad(glow, mat, frame, headX - 0.04F, 0.159F, headZ - 0.04F, headX + 0.04F, 0.161F, headZ + 0.04F, cr,
                        cg, cb, 0.9F);
                // beam: coloured sheath + white-hot core, lens down to the wafer
                quadVertical(beam, mat, frame, headX, 0.16F, headX, top, headZ, 0.028F, cr, cg, cb, 0.5F);
                quadVertical(beam, mat, frame, headX, 0.16F, headX, top, headZ, 0.010F, 255, 255, 255, 0.9F);
                // spot + glare on the die
                quad(glow, mat, frame, headX - 0.05F, top + 0.002F, headZ - 0.05F, headX + 0.05F, top + 0.004F,
                        headZ + 0.05F, 255, 255, 255, 0.85F);
                quad(glow, mat, frame, headX - 0.09F, top + 0.001F, headZ - 0.09F, headX + 0.09F, top + 0.003F,
                        headZ + 0.09F, cr, cg, cb, 0.55F);
                quadCone(beam, mat, frame, headX, 0.16F, headX, top, headZ, 0.10F, cr, cg, cb, 0.18F);
            }
        }

        poseStack.popPose();
    }

    /** Chamber centre in block-local coords (0.5 + up/back along the controller's axes). */
    private Frame frame(MetaMachine self) {
        boolean flipped = self instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction upDir = RelativeDirection.UP.getRelative(self.getFrontFacing(), self.getUpwardsFacing(), flipped);
        Direction backDir = RelativeDirection.BACK.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        Direction leftDir = RelativeDirection.LEFT.getRelative(self.getFrontFacing(), self.getUpwardsFacing(),
                flipped);
        float x = 0.5F + upDir.getStepX() * up + backDir.getStepX() * back;
        float y = 0.5F + upDir.getStepY() * up + backDir.getStepY() * back;
        float z = 0.5F + upDir.getStepZ() * up + backDir.getStepZ() * back;
        return new Frame(x, y, z, leftDir, upDir, backDir);
    }

    /** 0 before {@code from}, 1 after {@code to}, smooth between. */
    private static float ramp(float p, float from, float to) {
        return smooth((p - from) / (to - from));
    }

    /**
     * An airlock gate across the tube at {@code z}: a frame (two posts and a sill), and the gate itself, which sinks
     * into the floor by {@code open}. The gate is two leaves with a notch between them where the rail runs.
     */
    private static void drawGate(VertexConsumer solid, VertexConsumer glow, Matrix4f mat, Frame frame, float z,
                                 float open, int cr, int cg, int cb) {
        float lift = -open * 0.95F;
        // frame: posts on both walls, the lintel stops short of the rail
        SolidBox.box(solid, mat, frame, -0.495F, FLOOR_TOP, z - 0.035F, -0.46F, CEILING_BOTTOM, z + 0.035F, 0x3B4A5A);
        SolidBox.box(solid, mat, frame, 0.46F, FLOOR_TOP, z - 0.035F, 0.495F, CEILING_BOTTOM, z + 0.035F, 0x3B4A5A);
        // the leaves, from floor to ceiling; the middle only up to under the rail
        float y0 = FLOOR_TOP + lift, y1 = CEILING_BOTTOM + lift;
        SolidBox.box(solid, mat, frame, -0.46F, y0, z - 0.015F, -0.04F, y1, z + 0.015F, 0x5C6A80);
        SolidBox.box(solid, mat, frame, 0.04F, y0, z - 0.015F, 0.46F, y1, z + 0.015F, 0x5C6A80);
        SolidBox.box(solid, mat, frame, -0.04F, y0, z - 0.015F, 0.04F, RAIL_BOTTOM - 0.06F + lift, z + 0.015F,
                0x5C6A80);
        if (open < 0.99F) {
            // a seal line along the shut gate
            quad(glow, mat, frame, -0.44F, y0 + 0.02F, z - 0.0165F, -0.42F, y1 - 0.02F, z - 0.0165F, cr, cg, cb,
                    0.25F * (1F - open));
        }
    }

    private static float smooth(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return t * t * (3 - 2 * t);
    }

    /** Die being written for expose fraction e (0-1): serpentine 4x4, returns wafer-local (x, z). */
    private static float[] dieSpot(float e) {
        int total = DIES * DIES;
        float exact = Math.max(0F, Math.min(0.9999F, e)) * total;
        int index = (int) exact;
        float within = exact - index;
        int row = index / DIES;
        int col = index % DIES;
        if ((row & 1) == 1) col = DIES - 1 - col;
        float step = (WAFER_R * 1.35F) / DIES;
        float dx = (col - (DIES - 1) / 2F) * step + (within - 0.5F) * step * 0.8F;
        float dz = (row - (DIES - 1) / 2F) * step;
        return new float[] { dx, dz };
    }

    /** Wafer disc with a flat, exposed dies in the node colour. */
    private static void drawWafer(VertexConsumer solid, VertexConsumer glow, Matrix4f mat, Frame frame, float x,
                                  float y, float z, float exposed, boolean writing, int cr, int cg, int cb,
                                  float alpha, double time) {
        int a = Math.round(255 * Math.max(0F, Math.min(1F, alpha)));
        if (a <= 3) return;
        float fa = alpha;
        // silicon disc: top + side + flat
        SolidBox.disc(solid, mat, frame, x, y, z, WAFER_R, 0.035F, 0x9AA6B8, fa);
        // rim in the node colour
        SolidBox.ring(glow, mat, frame, x, y + 0.019F, z, WAFER_R - 0.02F, WAFER_R + 0.015F, cr, cg, cb,
                0.7F * fa);
        int total = DIES * DIES;
        int done = Math.round(Math.max(0F, Math.min(1F, exposed)) * total);
        float step = (WAFER_R * 1.35F) / DIES;
        float cell = step * 0.72F;
        for (int order = 0; order < total; order++) {
            int row = order / DIES;
            int col0 = order % DIES;
            // serpentine order matches dieSpot
            int col = ((row & 1) == 1) ? DIES - 1 - col0 : col0;
            float dx = (col - (DIES - 1) / 2F) * step;
            float dz = (row - (DIES - 1) / 2F) * step;
            if (order < done) {
                quad(glow, mat, frame, x + dx - cell / 2, y + 0.019F, z + dz - cell / 2, x + dx + cell / 2,
                        y + 0.021F, z + dz + cell / 2, cr, cg, cb, 0.75F * fa);
            } else if (writing && order == done) {
                float p = 0.5F + 0.5F * Mth.sin((float) (time * 2 * Math.PI / 12));
                quad(glow, mat, frame, x + dx - cell / 2, y + 0.019F, z + dz - cell / 2, x + dx + cell / 2,
                        y + 0.021F, z + dz + cell / 2, cr, cg, cb, (0.5F + 0.4F * p) * fa);
            } else {
                quad(glow, mat, frame, x + dx - cell / 2, y + 0.019F, z + dz - cell / 2, x + dx + cell / 2,
                        y + 0.021F, z + dz + cell / 2, 200, 210, 225, 0.16F * fa);
            }
        }
    }

    // ---- low-level quads in chamber-local coords (x=left, y=up, z=back) ----

    private record Frame(float x, float y, float z, Direction left, Direction up, Direction back) {
        /** Local (left/up/back) to pose-space: the pose is already at the chamber centre. */
        float px(float lx, float ly, float lz) {
            return lx * left.getStepX() + ly * up.getStepX() + lz * back.getStepX();
        }

        float py(float lx, float ly, float lz) {
            return lx * left.getStepY() + ly * up.getStepY() + lz * back.getStepY();
        }

        float pz(float lx, float ly, float lz) {
            return lx * left.getStepZ() + ly * up.getStepZ() + lz * back.getStepZ();
        }
    }

    /** Translucent box faces (POSITION_COLOR). */
    private static void fillBox(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1,
                                float y1, float z1, int r, int g, int b, float alpha) {
        float rf = r / 255F, gf = g / 255F, bf = b / 255F;
        // +y / -y
        quadRaw(c, mat, f.px(x0, y1, z0), f.py(x0, y1, z0), f.pz(x0, y1, z0), f.px(x1, y1, z0), f.py(x1, y1, z0),
                f.pz(x1, y1, z0), f.px(x1, y1, z1), f.py(x1, y1, z1), f.pz(x1, y1, z1), f.px(x0, y1, z1),
                f.py(x0, y1, z1), f.pz(x0, y1, z1), rf, gf, bf, alpha);
        quadRaw(c, mat, f.px(x0, y0, z1), f.py(x0, y0, z1), f.pz(x0, y0, z1), f.px(x1, y0, z1), f.py(x1, y0, z1),
                f.pz(x1, y0, z1), f.px(x1, y0, z0), f.py(x1, y0, z0), f.pz(x1, y0, z0), f.px(x0, y0, z0),
                f.py(x0, y0, z0), f.pz(x0, y0, z0), rf, gf, bf, alpha * 0.7F);
        // sides along back
        quadRaw(c, mat, f.px(x0, y0, z0), f.py(x0, y0, z0), f.pz(x0, y0, z0), f.px(x0, y0, z1), f.py(x0, y0, z1),
                f.pz(x0, y0, z1), f.px(x0, y1, z1), f.py(x0, y1, z1), f.pz(x0, y1, z1), f.px(x0, y1, z0),
                f.py(x0, y1, z0), f.pz(x0, y1, z0), rf, gf, bf, alpha);
        quadRaw(c, mat, f.px(x1, y0, z1), f.py(x1, y0, z1), f.pz(x1, y0, z1), f.px(x1, y0, z0), f.py(x1, y0, z0),
                f.pz(x1, y0, z0), f.px(x1, y1, z0), f.py(x1, y1, z0), f.pz(x1, y1, z0), f.px(x1, y1, z1),
                f.py(x1, y1, z1), f.pz(x1, y1, z1), rf, gf, bf, alpha);
        // front / back caps
        quadRaw(c, mat, f.px(x0, y0, z1), f.py(x0, y0, z1), f.pz(x0, y0, z1), f.px(x1, y0, z1), f.py(x1, y0, z1),
                f.pz(x1, y0, z1), f.px(x1, y1, z1), f.py(x1, y1, z1), f.pz(x1, y1, z1), f.px(x0, y1, z1),
                f.py(x0, y1, z1), f.pz(x0, y1, z1), rf, gf, bf, alpha * 0.5F);
        quadRaw(c, mat, f.px(x1, y0, z0), f.py(x1, y0, z0), f.pz(x1, y0, z0), f.px(x0, y0, z0), f.py(x0, y0, z0),
                f.pz(x0, y0, z0), f.px(x0, y1, z0), f.py(x0, y1, z0), f.pz(x0, y1, z0), f.px(x1, y1, z0),
                f.py(x1, y1, z0), f.pz(x1, y1, z0), rf, gf, bf, alpha * 0.5F);
    }

    /** Flat horizontal quad (y constant): two corners. */
    private static void quad(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1,
                             float y1, float z1, int r, int g, int b, float alpha) {
        quadRaw(c, mat, f.px(x0, y0, z0), f.py(x0, y0, z0), f.pz(x0, y0, z0), f.px(x1, y0, z0), f.py(x1, y0, z0),
                f.pz(x1, y0, z0), f.px(x1, y1, z1), f.py(x1, y1, z1), f.pz(x1, y1, z1), f.px(x0, y1, z1),
                f.py(x0, y1, z1), f.pz(x0, y1, z1), r / 255F, g / 255F, b / 255F, alpha);
    }

    /** Vertical beam quad between two points, width w in left axis. */
    private static void quadVertical(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float x1, float y1,
                                     float z, float w, int r, int g, int b, float alpha) {
        float rf = r / 255F, gf = g / 255F, bf = b / 255F;
        quadRaw(c, mat, f.px(x0 - w, y0, z), f.py(x0 - w, y0, z), f.pz(x0 - w, y0, z), f.px(x0 + w, y0, z),
                f.py(x0 + w, y0, z), f.pz(x0 + w, y0, z), f.px(x1 + w, y1, z), f.py(x1 + w, y1, z),
                f.pz(x1 + w, y1, z), f.px(x1 - w, y1, z), f.py(x1 - w, y1, z), f.pz(x1 - w, y1, z), rf, gf, bf,
                alpha);
        // cross quad so the beam shows from the side too
        quadRaw(c, mat, f.px(x0, y0, z - w), f.py(x0, y0, z - w), f.pz(x0, y0, z - w), f.px(x0, y0, z + w),
                f.py(x0, y0, z + w), f.pz(x0, y0, z + w), f.px(x1, y1, z + w), f.py(x1, y1, z + w),
                f.pz(x1, y1, z + w), f.px(x1, y1, z - w), f.py(x1, y1, z - w), f.pz(x1, y1, z - w), rf, gf, bf,
                alpha);
    }

    /** Small additive cone (two crossed quads widening downward). */
    private static void quadCone(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float x1, float y1,
                                 float z, float spread, int r, int g, int b, float alpha) {
        float rf = r / 255F, gf = g / 255F, bf = b / 255F;
        quadRaw(c, mat, f.px(x0 - 0.02F, y0, z), f.py(x0 - 0.02F, y0, z), f.pz(x0 - 0.02F, y0, z),
                f.px(x0 + 0.02F, y0, z), f.py(x0 + 0.02F, y0, z), f.pz(x0 + 0.02F, y0, z),
                f.px(x1 + spread, y1, z), f.py(x1 + spread, y1, z), f.pz(x1 + spread, y1, z),
                f.px(x1 - spread, y1, z), f.py(x1 - spread, y1, z), f.pz(x1 - spread, y1, z), rf, gf, bf, alpha);
    }

    private static void quadRaw(VertexConsumer c, Matrix4f mat, float x0, float y0, float z0, float x1, float y1,
                                float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r,
                                float g, float b, float alpha) {
        if (alpha <= 0.003F) return;
        c.vertex(mat, x0, y0, z0).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x1, y1, z1).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x2, y2, z2).color(r, g, b, alpha).endVertex();
        c.vertex(mat, x3, y3, z3).color(r, g, b, alpha).endVertex();
    }

    /** Opaque boxes + discs (entity shader, full bright, white texture). */
    private static final class SolidBox {

        static void box(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1, float y1,
                        float z1, int rgb) {
            box(c, mat, f, x0, y0, z0, x1, y1, z1, rgb, 1F);
        }

        static void box(VertexConsumer c, Matrix4f mat, Frame f, float x0, float y0, float z0, float x1, float y1,
                        float z1, int rgb, float alpha) {
            float r = red(rgb) / 255F, g = green(rgb) / 255F, b = blue(rgb) / 255F;
            // +y / -y
            face(c, mat, f, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, r, g, b, alpha);
            face(c, mat, f, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, r * 0.6F, g * 0.6F, b * 0.6F,
                    alpha);
            // +x / -x (left axis)
            face(c, mat, f, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, r * 0.8F, g * 0.8F, b * 0.8F,
                    alpha);
            face(c, mat, f, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, r * 0.8F, g * 0.8F, b * 0.8F,
                    alpha);
            // +z / -z (back axis)
            face(c, mat, f, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r * 0.9F, g * 0.9F, b * 0.9F,
                    alpha);
            face(c, mat, f, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r * 0.9F, g * 0.9F, b * 0.9F,
                    alpha);
        }

        static void disc(VertexConsumer c, Matrix4f mat, Frame f, float x, float y, float z, float radius,
                         float thick, int rgb, float alpha) {
            int sides = 20;
            float r = red(rgb) / 255F, g = green(rgb) / 255F, b = blue(rgb) / 255F;
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                float x0 = x + Mth.cos(a0) * radius, z0 = z + Mth.sin(a0) * radius;
                float x2 = x + Mth.cos(a1) * radius, z2 = z + Mth.sin(a1) * radius;
                // top
                tri(c, mat, f, x, y + thick / 2, z, x0, y + thick / 2, z0, x2, y + thick / 2, z2, 0, 1, 0, r, g, b,
                        alpha);
                // side
                face(c, mat, f, x0, y - thick / 2, z0, x2, y - thick / 2, z2, x2, y + thick / 2, z2, x0,
                        y + thick / 2, z0, Mth.cos((a0 + a1) / 2), 0, Mth.sin((a0 + a1) / 2), r * 0.7F, g * 0.7F,
                        b * 0.7F, alpha);
            }
            // flat notch at the front edge
            box(c, mat, f, x - radius * 0.7F, y - thick / 2, z + radius - 0.035F, x + radius * 0.7F, y + thick / 2,
                    z + radius + 0.001F, 0x2A3346, alpha);
        }

        static void ring(VertexConsumer c, Matrix4f mat, Frame f, float x, float y, float z, float inner,
                         float outer, int r, int g, int b, float alpha) {
            int sides = 28;
            float rf = r / 255F, gf = g / 255F, bf = b / 255F;
            for (int i = 0; i < sides; i++) {
                float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
                quadRaw(c, mat, f.px(x + Mth.cos(a0) * inner, y, z + Mth.sin(a0) * inner),
                        f.py(x + Mth.cos(a0) * inner, y, z + Mth.sin(a0) * inner),
                        f.pz(x + Mth.cos(a0) * inner, y, z + Mth.sin(a0) * inner),
                        f.px(x + Mth.cos(a1) * inner, y, z + Mth.sin(a1) * inner),
                        f.py(x + Mth.cos(a1) * inner, y, z + Mth.sin(a1) * inner),
                        f.pz(x + Mth.cos(a1) * inner, y, z + Mth.sin(a1) * inner),
                        f.px(x + Mth.cos(a1) * outer, y, z + Mth.sin(a1) * outer),
                        f.py(x + Mth.cos(a1) * outer, y, z + Mth.sin(a1) * outer),
                        f.pz(x + Mth.cos(a1) * outer, y, z + Mth.sin(a1) * outer),
                        f.px(x + Mth.cos(a0) * outer, y, z + Mth.sin(a0) * outer),
                        f.py(x + Mth.cos(a0) * outer, y, z + Mth.sin(a0) * outer),
                        f.pz(x + Mth.cos(a0) * outer, y, z + Mth.sin(a0) * outer), rf, gf, bf, alpha);
            }
        }

        private static void face(VertexConsumer c, Matrix4f mat, Frame f, float lx0, float ly0, float lz0, float lx1,
                                 float ly1, float lz1, float lx2, float ly2, float lz2, float lx3, float ly3,
                                 float lz3, float nx, float ny, float nz, float r, float g, float b, float alpha) {
            if (alpha <= 0.003F) return;
            // local normal (left/up/back) to world: same basis as positions
            float wx = nx * f.left.getStepX() + ny * f.up.getStepX() + nz * f.back.getStepX();
            float wy = nx * f.left.getStepY() + ny * f.up.getStepY() + nz * f.back.getStepY();
            float wz = nx * f.left.getStepZ() + ny * f.up.getStepZ() + nz * f.back.getStepZ();
            float len = Mth.sqrt(wx * wx + wy * wy + wz * wz);
            if (len > 1e-6F) {
                wx /= len;
                wy /= len;
                wz /= len;
            }
            vertex(c, mat, f, lx0, ly0, lz0, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx1, ly1, lz1, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx2, ly2, lz2, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx3, ly3, lz3, wx, wy, wz, r, g, b, alpha);
        }

        private static void tri(VertexConsumer c, Matrix4f mat, Frame f, float lx0, float ly0, float lz0, float lx1,
                                float ly1, float lz1, float lx2, float ly2, float lz2, float nx, float ny, float nz,
                                float r, float g, float b, float alpha) {
            if (alpha <= 0.003F) return;
            float wx = nx * f.left.getStepX() + ny * f.up.getStepX() + nz * f.back.getStepX();
            float wy = nx * f.left.getStepY() + ny * f.up.getStepY() + nz * f.back.getStepY();
            float wz = nx * f.left.getStepZ() + ny * f.up.getStepZ() + nz * f.back.getStepZ();
            vertex(c, mat, f, lx0, ly0, lz0, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx1, ly1, lz1, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx2, ly2, lz2, wx, wy, wz, r, g, b, alpha);
            vertex(c, mat, f, lx2, ly2, lz2, wx, wy, wz, r, g, b, alpha);
        }

        private static void vertex(VertexConsumer c, Matrix4f mat, Frame f, float lx, float ly, float lz, float nx,
                                   float ny, float nz, float r, float g, float b, float alpha) {
            c.vertex(mat, f.px(lx, ly, lz), f.py(lx, ly, lz), f.pz(lx, ly, lz)).color(r, g, b, alpha).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(nx, ny, nz).endVertex();
        }
    }

}
