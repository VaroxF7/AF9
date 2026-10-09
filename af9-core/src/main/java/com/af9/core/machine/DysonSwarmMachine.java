package com.af9.core.machine;

import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Dyson Swarm (structure in KubeJS: {@code startup_scripts/gtceu/dyson_swarm.js}; the structure and the
 * three-part layout of receiver, deployment unit and command centre are GTNH Intergalactic's). Sails put into the
 * input buses are sent up and stay in the swarm for good (no collisions); the receiver turns the light they catch into
 * power for the Dyson Output Hatches.
 * <p>
 * Power: every sail adds its own yield to the total ({@link DysonSails}: a star matter sail is one amp of UHV, the
 * alloy sail 4/7 of it, an Allthemodium sail 2/7), so a full swarm of star matter sails (10,000) is 10,000 A of UHV and
 * a mixed one is the sum of what it holds. The swarm needs a star ({@link DysonStars}) and one swarm runs on each: the
 * Sun and Alpha Centauri, found from the dimension it stands in. A cycle is an hour of generating on supercooled
 * hydrogen; it only runs while the hatches take the power (like a generator), so a full buffer pauses it instead of
 * restarting it, and the hydrogen is paid per hour of power actually made. A plunger on the controller takes the sails
 * back (what the player can carry, all of them while sneaking: the rest drops), like GTNH's.
 */
public class DysonSwarmMachine extends ProcessMachine implements IInteractedMachine, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(DysonSwarmMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    public static final int MAX_SAILS = 10000;
    /** EU/t of the recipe the swarm's output is scaled from (its output is the modifier's, not the recipe's). */
    public static final long BASE_EUT = GTValues.V[GTValues.UHV];
    private static final int PERIODIC = 20;
    private static final int COLOR = 0xFFFFC857;

    /**
     * Starts a cycle only with sails in the swarm and the star to itself, and gives it the power of the sails: the
     * recipe's EU/t is {@link #BASE_EUT}, scaled to what they catch.
     */
    public static final RecipeModifier SWARM = (machine, recipe) -> {
        if (!(machine instanceof DysonSwarmMachine swarm)) {
            return RecipeModifier.nullWrongType(DysonSwarmMachine.class, machine);
        }
        long output = swarm.outputEUt();
        if (output <= 0 || !swarm.claimStar()) return ModifierFunction.NULL;
        return ModifierFunction.builder().eutMultiplier((double) output / BASE_EUT).build();
    };

    /** Sails in the swarm by tier (see {@link DysonSails}). */
    @Persisted
    private int lowSails, midSails, highSails;
    /** EU/t of the running cycle, 0 when none. */
    @Persisted
    private long cycleEUt;

    private TickableSubscription periodicSubs;

    public DysonSwarmMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.dyson_swarm.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLOR;
    }

    //////////////////////////////////////
    // ************ Sails ************//
    //////////////////////////////////////

    public int sails(int tier) {
        return switch (tier) {
            case 0 -> lowSails;
            case 1 -> midSails;
            default -> highSails;
        };
    }

    private void setSails(int tier, int count) {
        switch (tier) {
            case 0 -> lowSails = count;
            case 1 -> midSails = count;
            default -> highSails = count;
        }
    }

    public int totalSails() {
        return lowSails + midSails + highSails;
    }

    /** EU/t of the swarm as it is now: the sum of the yields of its sails. */
    public long outputEUt() {
        long weighted = 0;
        for (int tier = 0; tier < DysonSails.IDS.length; tier++) {
            weighted += (long) sails(tier) * DysonSails.PERCENT[tier];
        }
        return DysonSails.euPerTick(weighted);
    }

    /** Ticks: takes the sails out of the input buses into the swarm. */
    private void absorb() {
        boolean changed = false;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (!(handler instanceof IItemHandlerModifiable inventory)) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                int tier = sailTier(stack);
                int room = MAX_SAILS - totalSails();
                if (tier < 0 || room <= 0) continue;
                ItemStack taken = inventory.extractItem(slot, Math.min(stack.getCount(), room), false);
                if (taken.isEmpty()) continue;
                setSails(tier, sails(tier) + taken.getCount());
                changed = true;
            }
        }
        if (changed) markDirty();
    }

    /**
     * A plunger on the controller recalls sails, lowest tier first, as many as the plunger has uses left (a plunger
     * without durability: all of them). Sneaking drops what does not fit the inventory on the ground; otherwise what
     * does not fit stays in the swarm.
     */
    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        ItemStack tool = player.getItemInHand(hand);
        if (!tool.is(CustomTags.PLUNGERS) || totalSails() <= 0) return InteractionResult.PASS;
        if (world.isClientSide) return InteractionResult.SUCCESS;
        int budget = tool.isDamageableItem() ? tool.getMaxDamage() - tool.getDamageValue() : Integer.MAX_VALUE;
        int taken = 0;
        boolean sneaking = player.isShiftKeyDown();
        for (int tier = 0; tier < DysonSails.IDS.length && budget > 0; tier++) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("af9", DysonSails.IDS[tier]));
            int count = Math.min(sails(tier), budget);
            while (count > 0 && item != null) {
                ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
                int size = stack.getCount();
                // Inventory.add takes what fits and leaves the rest in the stack (true when any of it went in)
                player.getInventory().add(stack);
                int left = stack.getCount();
                if (left > 0 && sneaking) {
                    player.drop(stack.copy(), false);
                    left = 0;
                }
                int moved = size - left;
                if (moved <= 0) break;
                count -= moved;
                budget -= moved;
                taken += moved;
                setSails(tier, sails(tier) - moved);
                if (left > 0) break;
            }
        }
        if (taken > 0) {
            if (tool.isDamageableItem()) tool.hurtAndBreak(taken, player, p -> p.broadcastBreakEvent(hand));
            markDirty();
        }
        return InteractionResult.CONSUME;
    }

    /** The tier (0-2) of a sail, -1 for anything else. */
    public static int sailTier(ItemStack stack) {
        Item item = stack.getItem();
        for (int tier = 0; tier < DysonSails.IDS.length; tier++) {
            if (item == ForgeRegistries.ITEMS.getValue(new ResourceLocation("af9", DysonSails.IDS[tier]))) return tier;
        }
        return -1;
    }

    /** The console's status: no star here, the star taken by another swarm, or the hatches not taking the power. */
    @Override
    public int getStatus() {
        int status = super.getStatus();
        if (status == ConsoleWidget.STATUS_OFFLINE || status == ConsoleWidget.STATUS_MAINTENANCE ||
                status == ConsoleWidget.STATUS_PAUSED) {
            return status;
        }
        if (star() == null) return ConsoleWidget.STATUS_NO_STAR;
        if (otherHolder() != null) return ConsoleWidget.STATUS_STAR_TAKEN;
        if (getRecipeLogic().isWaiting()) return ConsoleWidget.STATUS_OUTPUT_FULL;
        return status;
    }

    //////////////////////////////////////
    // ************ Star *************//
    //////////////////////////////////////

    /** The star this swarm circles (null: its dimension has none). */
    public ResourceLocation star() {
        return DysonStars.starOf(getLevel());
    }

    private MinecraftServer server() {
        return getLevel() == null ? null : getLevel().getServer();
    }

    /** Takes the star unless another swarm holds it; true when this swarm holds it. */
    public boolean claimStar() {
        MinecraftServer server = server();
        ResourceLocation star = star();
        if (server == null || star == null || !isFormed()) return false;
        return DysonStars.get(server).claim(server, star, getLevel().dimension().location(), getPos());
    }

    /** Who holds this swarm's star: null for nobody, this swarm's own claim included only when it is another's. */
    private DysonStars.Claim otherHolder() {
        MinecraftServer server = server();
        ResourceLocation star = star();
        if (server == null || star == null) return null;
        DysonStars.Claim held = DysonStars.get(server).holder(star);
        if (held == null) return null;
        boolean mine = held.dimension().equals(getLevel().dimension().location()) && held.pos().equals(getPos());
        return mine ? null : held;
    }

    private void releaseStar() {
        MinecraftServer server = server();
        ResourceLocation star = star();
        if (server != null && star != null) {
            DysonStars.get(server).release(star, getLevel().dimension().location(), getPos());
        }
    }

    //////////////////////////////////////
    // *********** Cycles ************//
    //////////////////////////////////////

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        if (!super.beforeWorking(recipe) || !claimStar()) return false;
        cycleEUt = outputEUt();
        markDirty();
        return true;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        cycleEUt = 0;
        markDirty();
    }

    /**
     * The swarm is a generator: when its buffer is full it waits, and the cycle's progress stays where it was (GT's
     * default would start the hour over, so a swarm making more than is used would never finish a cycle).
     */
    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    /** The power is re-scaled to the sails each cycle, and a swarm that lost its last sail does not go on. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    /** An idle swarm tries again every few ticks: the star can fall free, sails and hydrogen can arrive. */
    @Override
    public boolean keepSubscribing() {
        return true;
    }

    /** A swarm in the output hatches' reach is a generator: it needs nothing from them. */
    @Override
    public long getNeededEUt() {
        return 0;
    }

    //////////////////////////////////////
    // ********** Life cycle *********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        periodicSubs = subscribeServerTick(periodicSubs, this::periodic);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribePeriodic();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribePeriodic();
    }

    /** Broken: the star is free again. */
    @Override
    public void onMachineRemoved() {
        if (!isRemote()) releaseStar();
    }

    private void unsubscribePeriodic() {
        if (periodicSubs != null) {
            periodicSubs.unsubscribe();
            periodicSubs = null;
        }
    }

    private void periodic() {
        if (getOffsetTimer() % PERIODIC != 0 || !isFormed()) return;
        absorb();
        // a swarm with no sails and no cycle does not keep the star from the next one
        if (totalSails() <= 0 && !getRecipeLogic().isWorking()) releaseStar();
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.dyson_swarm.console.sails", totalSails(), MAX_SAILS));
        lines.add(Component.translatable("af9.dyson_swarm.console.tiers", lowSails, midSails, highSails));
        ResourceLocation star = star();
        if (star == null) {
            lines.add(Component.translatable("af9.dyson_swarm.console.no_star").withStyle(ChatFormatting.RED));
        } else {
            DysonStars.Claim other = otherHolder();
            if (other == null) {
                lines.add(Component.translatable("af9.dyson_swarm.console.star", DysonStars.starName(star)));
            } else {
                lines.add(Component.translatable("af9.dyson_swarm.console.star_taken", DysonStars.starName(star),
                        other.where()).withStyle(ChatFormatting.RED));
            }
        }
        long eut = getRecipeLogic().isWorking() ? cycleEUt : outputEUt();
        lines.add(Component.translatable("af9.dyson_swarm.console.output", String.format(Locale.ROOT, "%,d", eut),
                String.format(Locale.ROOT, "%,.0f", (double) eut / BASE_EUT)));
        return lines;
    }
}
