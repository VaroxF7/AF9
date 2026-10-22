package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * The Bio Vat: GTNH's Bacterial Vat (BartWorks {@code MTEBioVat}, 5 x 4 x 5, MIT, in GT5-Unofficial; credits in
 * CREDITS-textures.md) in GTCEu. A clean stainless shell round a 3 x 3 x 2 tank behind glass, which keeps three of the
 * original's rules:
 * <ul>
 * <li><b>Glass tier:</b> every recipe names the glass it needs ({@code glass_tier} in its data); the vat has the tier of
 * the weakest glass pane of its two glass rings (tempered 1, laminated 2, fusion 3), and a recipe above it does not run.</li>
 * <li><b>The fuller the better:</b> the original multiplies a run by how full the output hatch is: nothing at an empty
 * hatch, most at half full, falling off again towards full ({@link #outputBonus}). The multiplier is parallel runs, free
 * of extra EU, as many as the inputs allow.</li>
 * <li><b>The tank is visible:</b> the culture stands in the vat as high as the hatches are full, in the fluid's own
 * colour ({@link com.af9.core.client.render.BioVatRender}). The original places fluid blocks in the world; this draws.</li>
 * </ul>
 * GTNH's radiation hatch (sieverts) and the petri-dish bio cultures are not ported: the cultures here are plain fluids.
 */
public class BioVatMachine extends WorkableElectricMultiblockMachine implements IBioVatMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(BioVatMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** The most the fill bonus multiplies a run by (GTNH: 1000; the inputs set the real limit). */
    public static final int MAX_BONUS = 64;

    /** The tier of each glass the vat takes; the weakest pane of the two rings counts. */
    private static final Map<String, Integer> GLASS_TIERS = new HashMap<>();

    static {
        GLASS_TIERS.put("gtceu:tempered_glass", 1);
        GLASS_TIERS.put("gtceu:laminated_glass", 2);
        GLASS_TIERS.put("gtceu:fusion_glass", 3);
    }

    /** The glass tier gate and the fill bonus; the vat's recipe modifier. */
    public static final RecipeModifier VAT = BioVatMachine::vatModifier;

    @DescSynced
    private int vatFill;
    @DescSynced
    private String vatFluid = "";

    private int glassTier;
    private TickableSubscription fillSubs;

    public BioVatMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- structure ----

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        glassTier = scanGlassTier();
        fillSubs = subscribeServerTick(fillSubs, this::fillTick);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        glassTier = 0;
        vatFill = 0;
        vatFluid = "";
        if (fillSubs != null) {
            fillSubs.unsubscribe();
            fillSubs = null;
        }
    }

    /**
     * The weakest pane of the glass rings: rows 1 and 2 above the controller, the 5 x 5 outline, behind it.
     * (The controller is front-bottom-centre; the pattern accepts the three glasses of {@link #GLASS_TIERS} there.)
     */
    private int scanGlassTier() {
        if (getLevel() == null) return 0;
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction left = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        int tier = Integer.MAX_VALUE;
        for (int row = 1; row <= 2; row++) {
            for (int depth = 0; depth <= 4; depth++) {
                for (int side = -2; side <= 2; side++) {
                    boolean ring = depth == 0 || depth == 4 || side == -2 || side == 2;
                    if (!ring) continue;
                    BlockPos pos = getPos().relative(up, row).relative(back, depth).relative(left, side);
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(getLevel().getBlockState(pos).getBlock());
                    tier = Math.min(tier, id == null ? 0 : GLASS_TIERS.getOrDefault(id.toString(), 0));
                }
            }
        }
        return tier == Integer.MAX_VALUE ? 0 : tier;
    }

    public int getGlassTier() {
        return glassTier;
    }

    // ---- the tank: fill and fluid, for the renderer ----

    private void fillTick() {
        if (!isFormed() || getOffsetTimer() % 10 != 0) return;
        long stored = 0, capacity = 0;
        Map<String, Long> byFluid = new HashMap<>();
        for (IMultiPart part : getParts()) {
            if (!(part instanceof FluidHatchPartMachine hatch)) continue;
            for (int tank = 0; tank < hatch.tank.getTanks(); tank++) {
                capacity += hatch.tank.getTankCapacity(tank);
                FluidStack stack = hatch.tank.getFluidInTank(tank);
                if (stack.isEmpty()) continue;
                stored += stack.getAmount();
                ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
                if (id != null) byFluid.merge(id.toString(), (long) stack.getAmount(), Long::sum);
            }
        }
        int fill = capacity <= 0 ? 0 : (int) Math.min(1000, stored * 1000 / capacity);
        String fluid = byFluid.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        if (fill != vatFill) vatFill = fill;
        if (!fluid.equals(vatFluid)) vatFluid = fluid;
    }

    /**
     * GTNH's {@code calcMod}: with {@code y} half the output capacity and {@code x} what the output hatches hold, the bonus
     * is {@code ceil((y - (x - y)^2 / y) / y * MAX_BONUS)}, clamped to 1..MAX_BONUS: it rises to the most at half full and
     * falls off again towards full (an overfull hatch is a sign to empty it).
     */
    public static int calcBonus(long stored, long outputCapacity) {
        if (outputCapacity <= 0) return 1;
        double y = outputCapacity / 2.0;
        double bonus = Math.ceil((y - (stored - y) * (stored - y) / y) / y * MAX_BONUS);
        return (int) Math.max(1, Math.min(MAX_BONUS, bonus));
    }

    /** The bonus now: what the output hatches hold against what they can. */
    public int outputBonus() {
        long stored = 0, capacity = 0;
        for (IMultiPart part : getParts()) {
            if (!(part instanceof FluidHatchPartMachine hatch) || hatch.tank.getHandlerIO() != IO.OUT) continue;
            for (int tank = 0; tank < hatch.tank.getTanks(); tank++) {
                capacity += hatch.tank.getTankCapacity(tank);
                FluidStack stack = hatch.tank.getFluidInTank(tank);
                if (!stack.isEmpty()) stored += stack.getAmount();
            }
        }
        return calcBonus(stored, capacity);
    }

    // ---- the recipe modifier ----

    public static @NotNull ModifierFunction vatModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof BioVatMachine vat)) {
            return RecipeModifier.nullWrongType(BioVatMachine.class, machine);
        }
        int needed = recipe.data.getInt("glass_tier");
        if (vat.glassTier < needed) {
            return ModifierFunction.cancel(Component.translatable("af9.bio_vat.glass_tier", needed));
        }
        int parallel = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, vat.outputBonus());
        if (parallel <= 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .parallels(parallel)
                .build();
    }

    // ---- the renderer's view ----

    @Override
    public boolean isVatFormed() {
        return isFormed();
    }

    @Override
    public boolean isVatActive() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    @Override
    public int getVatFill() {
        return vatFill;
    }

    @Override
    public String getVatFluid() {
        return vatFluid;
    }
}
