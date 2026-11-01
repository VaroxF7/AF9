package com.af9.core.compat.jei;

import com.af9.core.AF9Core;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * What the client KubeJS scripts did for the recipe viewer (mud_coolant_info, space_elevator_info, transistors_info,
 * steam_hide, parallel_cap_hide): information pages on items, and items hidden because they can no longer be made.
 * The hidden ids duplicate the removals in recipes/gen (create_steam_age, parallel_cap): change both.
 */
@JeiPlugin
public class AF9JeiPlugin implements IModPlugin {

    private static final String[] MUD = {
            "Drilling Fluid is a water-based mud, made in six steps and nothing above HV: soda ash softens water (mixer); bentonite and soda ash in that water make sodium bentonite slurry (mixer); sawdust, lye and water make alkali cellulose (mixer); acetic acid and chlorine make chloroacetic acid (chemical reactor); alkali cellulose and chloroacetic acid make a carboxymethyl cellulose solution (chemical reactor); slurry, CMC solution, salt, calcite and lye blend to 5000 mB of mud (mixer).",
            "Nothing in it needs oil, asteroids or the Void Miner. Soda ash gives back calcite, the CMC step gives back salt, so a line feeds itself with bentonite, sawdust, lye, soda ash, acetic acid and chlorine.",
            "The Void Miner drinks 2000 mB of it per run." };

    private static final String[] COOLANT = {
            "PCB Coolant is an askarel, the dielectric of old transformers: polychlorinated biphenyl cut with trichlorobenzene, dried and stabilised.",
            "Biphenyl is chlorinated twice over ferric chloride (reactor to dichlorobiphenyl, then the EV Large Chemical Reactor to the PCB); dichlorobenzene takes a third chlorine to trichlorobenzene. 600 mB PCB and 400 mB trichlorobenzene mix to askarel (HV mixer); calcium chloride dries it (reactor); 900 mB of dry askarel and 100 mB epichlorohydrin make 1000 mB of coolant.",
            "Every chlorination gives HCl: it makes the epichlorohydrin from glycerol and the calcium chloride from calcite, so the acid is used up again. The first user is the HPCA." };

    private static final String[] ELEVATOR = {
            "The Space Elevator flies expeditions with its Mining Modules (MK-1 to MK-3), as many at once as the motors power.",
            "Each module needs a Mining Drone of its tier (not used up), a drill head and a crate for the cargo (both used up), hydrogen and the supercooled coolant of the drone, and the energy: all of it comes through the hatches of the tower and the module.",
            "The tower also needs computation: data hatches on an optical cable from a computation source. Its screen says how much the modules in the slots need (MK-1 20, MK-2 60, MK-3 120 CWU/t each) and how much hydrogen and coolant they use up a second. Without it no module flies.",
            "Energy hatches, wireless energy receivers and laser hatches (up to 2) all power the tower; an energy hatch switched to Auxiliary does not.",
            "The ore comes home in the output buses; the ores of each drone tier are on the recipe pages." };

    private static final String[] TRANSISTORS = {
            "Transistors are no item in AF9, they are a virtual price. A silicon chip wafer carries a fixed number of transistors, and printing it uses up the fluids that many transistors cost, through the auxiliary fluid hatches of the lithography machine.",
            "Every 100 transistors cost 12 mB Polyethylene (the insulator), 1 mB Silicon and 2 mB Electrum (the metal). The ready-made numbers are in the recipe of every print.",
            "Transistors per wafer: CPU 12000, APU 12000, MCU 8200, RAM 8000, Simple SoC 6700, LPIC 5000, ILC 4500, ULPIC 3540, RF Transceiver 1200.",
            "A transistor gets dearer with every node, as in a real fab: the price rises 20% per node, from 1x at 350 nm to about 3.6x at 7 nm and 4.3x at 1 nm. And a finer node prints more wafers at once, which are all billed.",
            "Example: a CPU wafer at 350 nm is 12000 transistors = 1440 mB Polyethylene, 120 mB Silicon, 240 mB Electrum.",
            "Only the silicon chips cost transistors for now." };

    private static final String[] STEAM_MACHINES = {
            "gtceu:lp_steam_solid_boiler", "gtceu:hp_steam_solid_boiler",
            "gtceu:lp_steam_liquid_boiler", "gtceu:hp_steam_liquid_boiler",
            "gtceu:lp_steam_solar_boiler", "gtceu:hp_steam_solar_boiler",
            "gtceu:lp_steam_extractor", "gtceu:hp_steam_extractor",
            "gtceu:lp_steam_macerator", "gtceu:hp_steam_macerator",
            "gtceu:lp_steam_compressor", "gtceu:hp_steam_compressor",
            "gtceu:lp_steam_forge_hammer", "gtceu:hp_steam_forge_hammer",
            "gtceu:lp_steam_furnace", "gtceu:hp_steam_furnace",
            "gtceu:lp_steam_alloy_smelter", "gtceu:hp_steam_alloy_smelter",
            "gtceu:lp_steam_rock_crusher", "gtceu:hp_steam_rock_crusher",
            "gtceu:lp_steam_miner", "gtceu:hp_steam_miner",
            "gtceu:primitive_pump",
            "gtceu:charcoal_pile_igniter" };

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AF9Core.MOD_ID, "info");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, MUD, "gtceu:drilling_fluid_bucket", "gtceu:void_miner");
        info(registration, COOLANT, "gtceu:pcb_coolant_bucket");
        info(registration, ELEVATOR, "gtceu:space_elevator", "gtceu:space_mining_module_mk1", "gtceu:space_mining_module_mk2",
                "gtceu:space_mining_module_mk3", "gtceu:space_pumping_module");
        info(registration, TRANSISTORS, "gtceu:photolithography_line", "gtceu:photolithography_scanner",
                "gtceu:orbital_lithography_station", "gtceu:cpu_wafer", "gtceu:ram_wafer", "gtceu:ilc_wafer",
                "gtceu:simple_soc_wafer", "gtceu:ulpic_wafer", "gtceu:lpic_wafer", "af9:rf_transceiver_wafer",
                "af9:apu_wafer", "af9:mcu_wafer");
    }

    private static void info(IRecipeRegistration registration, String[] paragraphs, String... itemIds) {
        List<Component> text = new ArrayList<>();
        for (String p : paragraphs) text.add(Component.literal(p));
        for (String id : itemIds) {
            ItemStack stack = stack(id);
            if (!stack.isEmpty()) registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK, text.toArray(new Component[0]));
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        // the removed UV Parallel Control Hatch (256 parallels) and the removed steam-age machines
        add(hidden, "gtceu:uv_parallel_hatch");
        for (String id : STEAM_MACHINES) add(hidden, id);
        if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }

    private static void add(List<ItemStack> into, String id) {
        ItemStack stack = stack(id);
        if (!stack.isEmpty()) into.add(stack);
    }

    @SuppressWarnings("removal")
    private static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
