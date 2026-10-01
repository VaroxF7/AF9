// AF9 - The Crafting CPU Array: AE2's autocrafting CPU as a GregTech multiblock. Behaviour: af9-core com.af9.core.cpu
// (CraftingCpuMachine, CpuRackPartMachine, CpuPart) and the Crafting CPU Core block (com.af9.core.ae2, an AE2 crafting
// unit: af9:crafting_cpu_core). Recipes: server_scripts/mods/gtceu/crafting_cpu.js. Spec: docs/crafting-cpu.md
//
// The racks hold HBM Memory Sticks and Stacks (the CPU's bytes) and CPU Clusters (its co-processors); the core block
// in the structure puts the array on the ME network.

const $CraftingCpu = Java.loadClass('com.af9.core.cpu.CraftingCpuMachine')
const $CpuRack = Java.loadClass('com.af9.core.cpu.CpuRackPartMachine')
const $CpuRelativeDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')

// What the racks take. Sticks and Stacks are 8x the RAM and eDRAM cards of the computer racks, the clusters four and
// eight CPUs; the numbers they give are CpuPart's.
StartupEvents.registry('item', event => {
    event.create('hbm_memory_stick')
        .displayName('HBM Memory Stick')
        .tooltip('High-bandwidth memory for a Crafting CPU Array: 4 MiB of crafting storage.')
        .tooltip('Up to 8 in a CPU Rack slot.')
    event.create('hbm_memory_stack')
        .displayName('HBM Memory Stack')
        .tooltip('Stacked high-bandwidth memory for a Crafting CPU Array: 32 MiB of crafting storage.')
        .tooltip('Up to 8 in a CPU Rack slot.')
    event.create('cpu_cluster')
        .displayName('CPU Cluster')
        .tooltip('Four CPUs on one board: 16 co-processors for a Crafting CPU Array.')
        .tooltip('Up to 8 in a CPU Rack slot.')
    event.create('superpositioned_cpu_cluster')
        .displayName('CPU Superpositioned Cluster')
        .tooltip('Eight quantum CPUs in superposition: 64 co-processors for a Crafting CPU Array.')
        .tooltip('Up to 8 in a CPU Rack slot.')
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    // CPU Rack (HV): four slots for the array's parts
    event.create('cpu_rack', 'custom')
        .tiers(GTValues.HV)
        .machine((holder, tier) => new $CpuRack(holder))
        .definition((tier, builder) => {
            builder
                .langValue('CPU Rack')
                .abilities($CpuRack.CPU_RACK)
                ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.cpu_rack.tooltip', 2))
                .rotationState(RotationState.ALL)
                ['overlayTieredHullModel(net.minecraft.resources.ResourceLocation)']('af9:block/machine/part/cpu_rack')
        })

    // Crafting CPU Array: 3 wide, 3 high, 3 to 10 long. The racks sit left and right in the middle row of every slice
    // between the two ends, a heat vent between them; energy hatches on any casing. One casing may be the Crafting
    // CPU Core (the array's AE2 block). Aisles front (controller) -> back, rows bottom -> top, so GT's auto-build puts
    // the structure right behind the controller (it builds casings; the core is swapped in for one of them).
    event.create('crafting_cpu_array', 'multiblock')
        .langValue('Crafting CPU Array')
        .machine(holder => new $CraftingCpu(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.DUMMY_RECIPES])
        .appearanceBlock(GTBlocks.ADVANCED_COMPUTER_CASING)
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.crafting_cpu_array.tooltip', 5))
        .pattern(definition => FactoryBlockPattern.start($CpuRelativeDirection.LEFT, $CpuRelativeDirection.UP,
            $CpuRelativeDirection.BACK)
            .aisle('CCC', 'CSC', 'CCC')                           // front: the controller
            .aisle('CCC', 'RVR', 'CCC').setRepeatable(1, 8)       // two racks around a heat vent
            .aisle('CCC', 'CCC', 'CCC')                           // back
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('R', Predicates.abilities($CpuRack.CPU_RACK))
            .where('V', Predicates.blocks('gtceu:computer_heat_vent'))
            .where('C', Predicates.blocks('gtceu:advanced_computer_casing')
                .or(Predicates.blocks('af9:crafting_cpu_core').setMaxGlobalLimited(1, 1))
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1)))
            .build())
        .workableCasingModel('gtceu:block/casings/hpca/advanced_computer_casing/back', 'gtceu:block/multiblock/hpca')
})
