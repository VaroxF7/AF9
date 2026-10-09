// AF9 - Photolithography Line and Orbital Lithography Station recipes (machines and materials:
// startup_scripts/gtceu/photolithography.js; items: AF9 Core, registry/AF9Items). Spec and numbers:
// docs/semiconductor-factory.md

// Shared with the other server scripts (server scripts share one scope). Must stay in sync with
// com.af9.core.litho.LithoMode and the item tables of com.af9.core.registry.AF9Items in af9-core.
const AF9_WAFERS = (() => {
    // The nine substrates, lowest first. blank = the substrate wafer, tier = voltage of its lithography mode,
    // yield = silicon-class chip wafers one print gives (GT's laser engraving: 1 / 4 / 8 / 16 on silicon / phosphorus /
    // naquadah / neutronium; the new substrates fill in and go on)
    const substrates = [
        { id: 'silicon', yield: 1, blank: 'gtceu:silicon_wafer', tier: GTValues.MV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'phosphorus', yield: 4, blank: 'gtceu:phosphorus_wafer', tier: GTValues.HV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'naquadah', yield: 8, blank: 'gtceu:naquadah_wafer', tier: GTValues.EV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'trinium', yield: 10, blank: 'af9:trinium_wafer', tier: GTValues.IV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'naquadria', yield: 12, blank: 'af9:naquadria_wafer', tier: GTValues.LuV, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'neutronium', yield: 16, blank: 'gtceu:neutronium_wafer', tier: GTValues.ZPM, reclaim: 'gtceu:small_silicon_dust' },
        { id: 'transmuted_neutronium', yield: 24, blank: 'af9:transmuted_neutronium_wafer', tier: GTValues.UV,
            reclaim: 'gtceu:small_silicon_dust' },
        { id: 'strange_matter', yield: 32, blank: 'af9:strange_matter_wafer', tier: GTValues.UHV,
            reclaim: 'gtceu:small_silicon_dust' },
        { id: 'chromodynium', yield: 64, blank: 'af9:chromodynium_wafer', tier: GTValues.UHV,
            reclaim: 'gtceu:small_chromodynium_dust' }
    ]
    substrates.forEach((s, index) => s.index = index)

    // One mode per substrate. baseBreak: break chance at a clean vacuum, of 10000 (= LithoMode.baseBreak; shown as the
    // chanced broken wafer, the break roll itself is AF9 Core's). light: the exposure tool the real node used; resist:
    // the photoresist made for that light; laserGas: the excimer premix; immersion: water film under the lens; highK:
    // HfO2 gate dielectric from hafnium tetrachloride; euv: tin-plasma source (molten tin + hydrogen buffer gas);
    // barc: the bottom anti-reflective coat under the resist (every ArF and KrF node, 200 to 50 nm); the immersion nodes
    // (65, 50 nm) also get the top coat TARC, both in the Coater Track.
    const modes = [
        { id: '350nm', substrate: 0, resist: 'gtceu:photoresist', baseBreak: 200 },
        { id: '200nm', substrate: 1, resist: 'gtceu:krf_photoresist', laserGas: 'gtceu:krf_excimer_gas', barc: true,
            baseBreak: 300 },
        { id: '100nm', substrate: 2, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', barc: true,
            baseBreak: 500 },
        { id: '80nm', substrate: 3, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', barc: true,
            laser: 'af9:arf_excimer_laser', baseBreak: 700 },
        { id: '65nm', substrate: 4, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', immersion: true,
            barc: true, laser: 'af9:arf_excimer_laser', baseBreak: 900 },
        { id: '50nm', substrate: 5, resist: 'gtceu:arf_photoresist', laserGas: 'gtceu:arf_excimer_gas', immersion: true,
            highK: true, barc: true, baseBreak: 1200 },
        { id: '20nm', substrate: 6, resist: 'gtceu:euv_photoresist', euv: true, highK: true, baseBreak: 1800 },
        { id: '7nm', substrate: 7, resist: 'gtceu:euv_photoresist', euv: true, highK: true, baseBreak: 2500 }
    ]
    modes.forEach((m, index) => m.index = index)
    const orbital = { id: '1nm', substrate: 8, baseBreak: 3500 }

    function own(id, native, lens, dies) {
        return { id: id, native: native, reticle: id, lens: lens, chip: `af9:${id}_chip`,
            wafer: `af9:${id}_wafer`, dies: dies }
    }
    // Every GT chip wafer. native = index of the chip's own substrate (GT's: silicon, phosphorus, naquadah for ASoC,
    // neutronium for HASoC). reticle: the photomask (derived wafers come from GT's Chemical Reactor recipes instead);
    // lens: the GT lens colour that engraves the chip's own reticle (native mask class, see maskClass: the colours of one
    // class's chips differ); chip: the chip GT's cutter makes of the wafer.
    const chips = [
        { id: 'ilc', native: 0, reticle: 'ilc', lens: 'red', engrave: 'engrave_ilc', chip: 'gtceu:ilc_chip' },
        { id: 'ram', native: 0, reticle: 'ram', lens: 'green', engrave: 'engrave_ram', chip: 'gtceu:ram_chip' },
        { id: 'cpu', native: 0, reticle: 'cpu', lens: 'light_blue', engrave: 'engrave_cpu', chip: 'gtceu:cpu_chip' },
        { id: 'ulpic', native: 0, reticle: 'ulpic', lens: 'blue', engrave: 'engrave_ulpic', chip: 'gtceu:ulpic_chip' },
        { id: 'lpic', native: 0, reticle: 'lpic', lens: 'orange', engrave: 'engrave_lpic', chip: 'gtceu:lpic_chip' },
        { id: 'simple_soc', native: 0, reticle: 'simple_soc', lens: 'cyan', engrave: 'engrave_ssoc', chip: 'gtceu:simple_soc' },
        { id: 'nand_memory', native: 1, reticle: 'nand', lens: 'gray', engrave: 'engrave_nand', chip: 'gtceu:nand_memory_chip' },
        { id: 'nor_memory', native: 1, reticle: 'nor', lens: 'pink', engrave: 'engrave_nor', chip: 'gtceu:nor_memory_chip' },
        { id: 'mpic', native: 1, reticle: 'mpic', lens: 'brown', engrave: 'engrave_pic', chip: 'gtceu:mpic_chip' },
        { id: 'soc', native: 1, reticle: 'soc', lens: 'yellow', engrave: 'engrave_soc', chip: 'gtceu:soc' },
        { id: 'advanced_soc', native: 2, reticle: 'advanced_soc', lens: 'orange', engrave: 'engrave_asoc', chip: 'gtceu:advanced_soc' },
        { id: 'highly_advanced_soc', native: 5, reticle: 'highly_advanced_soc', lens: 'black', engrave: 'engrave_hasoc', chip: 'gtceu:highly_advanced_soc' },
        { id: 'nano_cpu', native: 0, from: 'cpu', chip: 'gtceu:nano_cpu_chip' },
        { id: 'qbit_cpu', native: 0, from: 'nano_cpu', chip: 'gtceu:qbit_cpu_chip' },
        { id: 'hpic', native: 1, from: 'mpic', chip: 'gtceu:hpic_chip' },
        { id: 'uhpic', native: 1, from: 'hpic', chip: 'gtceu:uhpic_chip' },
        // AF9's own chips (af9-core, registry/AF9Items), af9: items: wafer = their chip wafer, dies = chips the
        // Cutter makes of one
        own('rf_transceiver', 0, 'lime', 8),
        own('apu', 0, 'magenta', 6),
        own('mcu', 0, 'white', 16),
        own('asic', 1, 'light_gray', 8),
        own('edram', 3, 'green', 16),
        own('mram', 3, 'blue', 16),
        own('feram', 3, 'yellow', 16),
        own('vpu', 4, 'purple', 6),
        own('tpu', 6, 'orange', 4),
        // the new families (chips.js)
        own('saw_filter', 2, 'red', 8),
        own('photonic_ic', 3, 'cyan', 6),
        own('spin_logic', 4, 'lime', 8),
        own('tmd_logic', 5, 'pink', 6),
        own('memristor', 6, 'cyan', 8),
        own('quantum_dot_ic', 7, 'yellow', 4),
        own('qram', 7, 'magenta', 8),
        own('qlos', 7, 'lime', 4)
    ]
    const chip = id => {
        const found = chips.filter(c => c.id === id)[0]
        if (!found) throw new Error(`AF9_WAFERS: unknown chip '${id}'`)
        return found
    }
    // The chip wafer a substrate (index) prints: GT's own wafer item (AF9's for its own chips), null below the chip's
    // own substrate
    const waferOf = c => c.wafer || `gtceu:${c.id}_wafer`
    const printed = (substrateIndex, c) => substrateIndex < c.native ? null : waferOf(c)
    // Chip wafers per print, like GT's engraving: 1 on the chip's own substrate; above it the substrate's yield,
    // divided by the chip class's divisor (silicon chips 1, phosphorus chips 2, ASoC 8, HASoC 16: GT's numbers;
    // AF9's trinium, naquadria and transmuted neutronium chips: their substrate's yield)
    const CLASS_DIVISOR = { 0: 1, 1: 2, 2: 8, 3: 10, 4: 12, 5: 16, 6: 24, 7: 32 }
    const yieldOf = (substrateIndex, c) => {
        if (substrateIndex < c.native) return 0
        if (substrateIndex === c.native) return 1
        return Math.max(1, Math.floor(substrates[substrateIndex].yield / CLASS_DIVISOR[c.native]))
    }
    // Every wafer item of a substrate, for the contamination tags: the blank, the coated wafer (all but the last substrate's) and
    // GT's chip wafers of that substrate
    const wafersOf = substrateIndex => [substrates[substrateIndex].blank]
        .concat(substrateIndex < substrates.length - 1 ? [`af9:coated_${substrates[substrateIndex].id}_wafer`] : [])
        .concat(chips.filter(c => c.native === substrateIndex).map(waferOf))
    // Plain chip stack by chip or old reticle id (the circuit scripts use 'nand', 'nor' ...)
    const chipStack = (id, count) => {
        const found = chips.filter(c => c.id === id || c.reticle === id)[0]
        if (!found) throw new Error(`AF9_WAFERS: unknown chip '${id}'`)
        return `${count || 1}x ${found.chip}`
    }
    // Mask classes: the photomask has to fit the light. Chrome-on-quartz (binary) masks for 350 and 200 nm, MoSi attenuated
    // phase-shift masks for 100 to 65 nm, reflective Mo/Si EUV multilayer masks from 50 nm (the orbital station's nodes).
    // A chip has a reticle of its own (native) class and one of every finer class: the finer one is written from the
    // native one (same layout, new blank). af9-core registers them (registry/AF9Items, RETICLES).
    const CLASSES = ['chrome', 'psm', 'euv']
    const BLANKS = { chrome: 'af9:photomask_blank', psm: 'af9:phase_shift_mask_blank', euv: 'af9:euv_mask_blank' }
    const maskClass = substrateIndex => CLASSES[substrateIndex <= 1 ? 0 : substrateIndex <= 4 ? 1 : 2]
    // af9:<chip>_reticle in the chip's native class, af9:<chip>_<class>_reticle in a finer one
    const reticleItem = (c, cls) => cls === maskClass(c.native) ? `af9:${c.reticle}_reticle` :
        `af9:${c.reticle}_${cls}_reticle`
    // every reticle: the chip, its class, the item, the blank it is written on, the native reticle it is written from
    const reticles = []
    // (a callback per class, not a for loop: Rhino keeps a const declared in a loop's body at its first value)
    chips.filter(c => c.reticle).forEach(c => {
        CLASSES.slice(CLASSES.indexOf(maskClass(c.native))).forEach(cls => {
            reticles.push({ chip: c, cls: cls, item: reticleItem(c, cls), blank: BLANKS[cls],
                master: cls === maskClass(c.native) ? null : reticleItem(c, maskClass(c.native)), lens: c.lens })
        })
    })
    return { substrates: substrates, modes: modes, orbital: orbital, chips: chips, chip: chip, printed: printed,
        yieldOf: yieldOf, wafersOf: wafersOf, chipStack: chipStack, classes: CLASSES, maskClass: maskClass,
        reticleItem: reticleItem, reticles: reticles }
})()

// Contamination (af9-core WaferContamination): every wafer of a substrate, every chip, the gloves that protect
ServerEvents.tags('item', event => {
    AF9_WAFERS.substrates.forEach(s => {
        const wafers = AF9_WAFERS.wafersOf(s.index)
        event.add(`af9:wafers/${s.id}`, wafers)
        event.add('af9:wafers', wafers)
    })
    event.add('af9:chips', AF9_WAFERS.chips.map(c => c.chip))
    event.add('af9:wafer_gloves', ['gtceu:rubber_gloves', 'gtceu:hazmat_chestpiece'])
})

ServerEvents.recipes(event => {
    const EU_LV = GTValues.VA[GTValues.LV]
    const EU_MV = GTValues.VA[GTValues.MV]
    const VA = GTValues.VA
    const substrates = AF9_WAFERS.substrates
    const modes = AF9_WAFERS.modes
    const chips = AF9_WAFERS.chips
    const printed = AF9_WAFERS.printed
    const yieldOf = AF9_WAFERS.yieldOf

    // ---- Removed GT paths: the chip wafers come from the lithography machines ----
    // Except one bootstrap: MV Energy Hatches need a ULPIC chip and the line needs MV Energy Hatches, so GT's plain
    // ULPIC engraving on silicon stays (like the chip-free Good Electronic Circuit). GT's derived-wafer chemistry
    // (Nano CPU, Qubit CPU, HPIC, UHPIC) and GT's cutter recipes of its own wafers stay as they are.
    const gtSubstrates = ['silicon', 'phosphorus', 'naquadah', 'neutronium']
    chips.filter(c => c.engrave).forEach(c => gtSubstrates.forEach(s => {
        if (c.id === 'ulpic' && s === 'silicon') return
        event.remove({ id: `gtceu:laser_engraver/${c.engrave}_${s}` })
    }))

    // ---- Machines ----
    event.recipes.gtceu.assembler('af9:photolithography_line')
        .itemInputs(
            'gtceu:mv_machine_hull',
            '4x #gtceu:circuits/mv',
            '2x gtceu:mv_emitter',
            '2x gtceu:mv_sensor',
            '2x gtceu:mv_robot_arm',
            '4x gtceu:mv_electric_motor',
            '2x gtceu:mv_electric_pump',
            '4x gtceu:glass_lens',
            '8x gtceu:stainless_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 576))
        .itemOutputs('gtceu:photolithography_line')
        .duration(1200)
        .EUt(EU_MV)

    // Light sources of the line's versions (version 1 uses GT's purple lamp as its mercury lamp). An excimer laser: a
    // discharge chamber filled with the gas premix, a pulsed power supply, UV optics and a gas circulation pump. KrF
    // allows line version 2, ArF up to 6, the EUV source up to 8 (plus the lens slices).
    event.recipes.gtceu.assembler('af9:krf_excimer_laser')
        .itemInputs('gtceu:hv_machine_hull', '2x gtceu:hv_emitter', '4x #gtceu:circuits/hv', '2x gtceu:glass_lens',
            '4x gtceu:stainless_steel_plate', 'gtceu:hv_electric_pump')
        .inputFluids(Fluid.of('gtceu:krf_excimer_gas', 4000))
        .itemOutputs('af9:krf_excimer_laser')
        .duration(1200)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.assembler('af9:arf_excimer_laser')
        .itemInputs('gtceu:ev_machine_hull', '2x gtceu:ev_emitter', '4x #gtceu:circuits/ev', '4x gtceu:glass_lens',
            '4x gtceu:titanium_plate', 'gtceu:ev_electric_pump')
        .inputFluids(Fluid.of('gtceu:arf_excimer_gas', 4000))
        .itemOutputs('af9:arf_excimer_laser')
        .duration(1200)
        .EUt(VA[GTValues.EV])
    // Laser-produced plasma: a CO2 drive laser hits tin droplets 50,000 times a second; a multilayer collector mirror
    // (two Mo/Si mirrors) gathers the 13.5 nm light
    event.recipes.gtceu.assembler('af9:euv_light_source')
        .itemInputs('gtceu:uv_machine_hull', '4x gtceu:uv_emitter', '4x #gtceu:circuits/uv', '8x gtceu:glass_lens',
            '2x gtceu:uv_electric_pump', '8x gtceu:neutronium_plate', '2x af9:mo_si_mirror')
        .inputFluids(Fluid.of('gtceu:tin', 2304))
        .itemOutputs('af9:euv_light_source')
        .duration(2400)
        .EUt(VA[GTValues.UV])

    // Photolithography Scanner (Mk2): 80 and 65 nm, crafted at IV
    event.recipes.gtceu.assembler('af9:photolithography_scanner')
        .itemInputs('gtceu:iv_machine_hull', '4x #gtceu:circuits/iv', '2x gtceu:iv_emitter', '2x gtceu:iv_sensor',
            '2x gtceu:iv_robot_arm', '4x gtceu:iv_electric_motor', '2x gtceu:iv_electric_pump', '8x gtceu:glass_lens',
            '8x gtceu:tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:soldering_alloy', 1152))
        .itemOutputs('gtceu:photolithography_scanner')
        .duration(1200)
        .EUt(VA[GTValues.IV])

    // Orbital Lithography Station (built on the ground, runs only in orbit; its structure is GT and GCYM blocks).
    // It prints from 50 nm (ZPM) on, so it is crafted at ZPM; its VPUs watch the wafer die by die and six Mo/Si mirrors
    // are its projection optics.
    event.recipes.gtceu.assembler('af9:orbital_lithography_station')
        .itemInputs('gtceu:zpm_machine_hull', '4x gtceu:zpm_emitter', '4x gtceu:zpm_field_generator',
            '4x #gtceu:circuits/zpm', '4x gtceu:zpm_sensor', '4x gtceu:zpm_robot_arm', '16x gtceu:naquadah_alloy_plate',
            '8x af9:vpu_chip', '6x af9:mo_si_mirror')
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 4000))
        .itemOutputs('gtceu:orbital_lithography_station')
        .duration(4000)
        .EUt(VA[GTValues.ZPM])

    // Plascrete Pipe Casing: the chemical lines of the MV machines (Photolithography Line, SMC Thermal Processing
    // Furnace); GT's PTFE Pipe Casing only comes at HV. Like GT's pipe casings: plates, pipes, a frame.
    event.shaped('2x af9:plascrete_pipe_casing', ['PIP', 'IFI', 'PIP'], {
        P: 'gtceu:plascrete', I: 'gtceu:polyethylene_normal_fluid_pipe', F: 'gtceu:steel_frame'
    }).id('af9:plascrete_pipe_casing')

    // Plascrete Filter Casing: the ceiling of the lithography machines, and a GT cleanroom filter (ISO 5). GT's Filter
    // Casing parts (item filters behind a grille, an MV fan motor, a steel rotor) in a plascrete frame.
    event.shaped('2x af9:plascrete_filter_casing', ['PBP', 'IMI', 'PRP'], {
        P: 'gtceu:plascrete', B: 'minecraft:iron_bars', I: 'gtceu:item_filter', M: 'gtceu:mv_electric_motor',
        R: 'gtceu:steel_rotor'
    }).id('af9:plascrete_filter_casing')

    // ---- Photomasks ----
    // Mask blanks ship pre-coated with resist; the pattern is then written by a laser mask writer
    event.recipes.gtceu.assembler('af9:photomask_blank')
        .itemInputs('gtceu:quartzite_plate', 'gtceu:chromium_plate')
        .inputFluids(Fluid.of('gtceu:photoresist', 100))
        .itemOutputs('af9:photomask_blank')
        .duration(400)
        .EUt(EU_MV)

    // The finer chips' masks: an attenuated phase-shift blank, a MoSi film that shifts the light half a wave for sharper
    // edges (80 and 65 nm), and an EUV blank, Mo/Si bilayers that reflect 13.5 nm light (EUV masks are mirrors),
    // sputtered in argon; both coated with the resist their writer needs
    event.recipes.gtceu.fab_cvd('af9:phase_shift_mask_blank')
        .itemInputs('gtceu:quartzite_plate', 'gtceu:small_molybdenum_dust', 'gtceu:small_silicon_dust')
        .inputFluids(Fluid.of('gtceu:arf_photoresist', 100))
        .itemOutputs('af9:phase_shift_mask_blank')
        .blastFurnaceTemp(900)
        .duration(600)
        .EUt(VA[GTValues.EV])
        .cleanroom(CleanroomType.CLEANROOM)
    event.recipes.gtceu.fab_cvd('af9:euv_mask_blank')
        .itemInputs('gtceu:quartzite_plate', '2x gtceu:molybdenum_dust', '2x gtceu:silicon_dust')
        .inputFluids(Fluid.of('gtceu:euv_photoresist', 100), Fluid.of('gtceu:argon', 1000))
        .itemOutputs('af9:euv_mask_blank')
        .blastFurnaceTemp(1200)
        .duration(1200)
        .EUt(VA[GTValues.LuV])
        .cleanroom(CleanroomType.CLEANROOM)

    // EUV optics: a ULE glass body (quartz doped with titania, calcined), then forty Mo/Si bilayers sputtered on it in
    // argon: the collector of the light source and the projection mirrors of the orbital station
    event.recipes.gtceu.fab_calcination('af9:ule_glass_substrate')
        .itemInputs('gtceu:quartzite_plate', 'gtceu:rutile_dust')
        .itemOutputs('af9:ule_glass_substrate')
        .blastFurnaceTemp(1800)
        .duration(600)
        .EUt(VA[GTValues.HV])
    event.recipes.gtceu.fab_cvd('af9:mo_si_mirror')
        .itemInputs('af9:ule_glass_substrate', '4x gtceu:molybdenum_dust', '4x gtceu:silicon_dust')
        .inputFluids(Fluid.of('gtceu:argon', 2000))
        .itemOutputs('af9:mo_si_mirror')
        .blastFurnaceTemp(1200)
        .duration(1800)
        .EUt(VA[GTValues.LuV])

    // A reticle: the chip's own one (its native class) is its blank written through the chip's lens; every finer class is
    // written onto that class's blank from the native reticle (the layout, kept). A class costs more voltage: the
    // phase-shift masks are written at EV, the EUV masks at ZPM (a chip's finest ones, the strange-matter chips', are
    // still cut at UV).
    const WRITER = { chrome: EU_MV, psm: VA[GTValues.EV], euv: VA[GTValues.ZPM] }
    AF9_WAFERS.reticles.forEach(r => {
        const native = r.master === null
        const recipe = event.recipes.gtceu.laser_engraver(native ? `af9:${r.chip.reticle}_reticle` :
            `af9:${r.chip.reticle}_${r.cls}_reticle`)
            .itemInputs(r.blank)
        if (native) recipe.notConsumable(`#forge:lenses/${r.lens}`)
        else recipe.notConsumable(r.master)
        recipe.itemOutputs(r.item)
            .duration(1800)
            .EUt(WRITER[r.cls])
    })

    // ---- AF9's own chips: dicing and packaging ----
    // Their chip wafers into dies (GT's Cutter, dicing-saw water); the silicon chips cut anywhere, the rest in a clean
    // room, at their substrate's voltage
    chips.filter(c => c.wafer).forEach(c => {
        const cut = event.recipes.gtceu.cutter(`af9:cut_${c.id}_wafer`)
            .itemInputs(c.wafer)
            .inputFluids(Fluid.of('gtceu:distilled_water', 100))
            .itemOutputs(`${c.dies}x ${c.chip}`)
            .duration(900)
            .EUt(VA[Math.min(substrates[c.native].tier, GTValues.UV)])
        if (c.native > 0) cut.cleanroom(CleanroomType.CLEANROOM)
    })
    // eDRAM beside the processor on one package, the cache chiplet (as on the Xbox 360's GPU): the CPU or SoC die and
    // two eDRAM dies flip-chip bonded to an epoxy laminate, gold wire for the rest
    const packages = [['cpu', 'gtceu:cpu_chip'], ['soc', 'gtceu:soc']]
    packages.forEach(([id, die]) => {
        event.recipes.gtceu.assembler(`af9:edram_${id}_package`)
            .itemInputs(die, '2x af9:edram_chip', 'gtceu:epoxy_plate', '4x gtceu:fine_gold_wire')
            .inputFluids(Fluid.of('gtceu:soldering_alloy', 72))
            .itemOutputs(`af9:edram_${id}_package`)
            .duration(400)
            .EUt(VA[GTValues.IV])
            .cleanroom(CleanroomType.CLEANROOM)
    })

    // ---- Extreme clean dry air (XCDA) ----
    // A fab's clean-dry-air plant, the purification mode of the SMC fab machines: oxidize -> scrub CO2 -> dry ->
    // cryo-cool -> filter.
    // Everything runs at MV, slowly (the expansion cooler recycles 3/4 of its air). HV adds a platinum oxidizer, a
    // caustic scrubber and liquid-air cooling, which together are much faster.
    const EU_HV = GTValues.VA[GTValues.HV]

    // Cu + 2 MnO2 + O -> CuMn2O4
    event.recipes.gtceu.fab_synthesis('af9:hopcalite')
        .itemInputs('gtceu:copper_dust', '6x gtceu:pyrolusite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('7x gtceu:hopcalite_dust')
        .duration(400)
        .EUt(EU_MV)

    // zeolite crystallised with a bentonite binder into sieve beads (clay + distilled water is GT's clay recipe)
    event.recipes.gtceu.fab_wet_processing('af9:molecular_sieve')
        .itemInputs('4x gtceu:zeolite_dust', 'gtceu:bentonite_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 500))
        .itemOutputs('4x af9:molecular_sieve')
        .duration(600)
        .EUt(EU_MV)

    // temperature-swing regeneration
    event.smelting('af9:molecular_sieve', 'af9:saturated_molecular_sieve').id('af9:regenerate_molecular_sieve')

    // 1. catalytic oxidation: CO, H2 and hydrocarbons -> CO2 + H2O
    event.recipes.gtceu.fab_purification('af9:xcda_oxidize_hopcalite')
        .notConsumable('gtceu:hopcalite_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(600)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_purification('af9:xcda_oxidize_platinum')
        .notConsumable('gtceu:platinum_dust')
        .inputFluids(Fluid.of('gtceu:air', 4000))
        .outputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .duration(150)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 2. CO2 scrubbing: Ca(OH)2 + CO2 -> CaCO3 + H2O, or 2 NaOH + CO2 -> Na2CO3 + H2O
    event.recipes.gtceu.fab_purification('af9:xcda_scrub_lime')
        .itemInputs('gtceu:small_calcium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_calcite_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_purification('af9:xcda_scrub_caustic')
        .itemInputs('gtceu:small_sodium_hydroxide_dust')
        .inputFluids(Fluid.of('gtceu:oxidized_air', 4000))
        .itemOutputs('gtceu:small_soda_ash_dust')
        .outputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .duration(100)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 3. drying: the sieve adsorbs the water
    event.recipes.gtceu.fab_purification('af9:xcda_dry')
        .itemInputs('af9:molecular_sieve')
        .inputFluids(Fluid.of('gtceu:decarbonated_air', 4000))
        .itemOutputs('af9:saturated_molecular_sieve')
        .outputFluids(Fluid.of('gtceu:dry_air', 4000))
        .duration(400)
        .EUt(EU_MV)

    // 4. cryogenic cooling. MV: Joule-Thomson expansion, only a quarter gets cold enough, the rest goes round again.
    // HV: pre-cooled with liquid air, which boils back into ordinary air.
    event.recipes.gtceu.fab_purification('af9:xcda_cool_expansion')
        .circuit(1)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 1000), Fluid.of('gtceu:dry_air', 3000))
        .duration(800)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_purification('af9:xcda_cool_liquid_air')
        .circuit(2)
        .inputFluids(Fluid.of('gtceu:dry_air', 4000), Fluid.of('gtceu:liquid_air', 1000))
        .outputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000), Fluid.of('gtceu:air', 1000))
        .duration(200)
        .EUt(EU_HV)
        .cleanroom(CleanroomType.CLEANROOM)

    // 5. re-warmed through a membrane filter; the last traces stayed frozen in the cold box
    event.recipes.gtceu.fab_purification('af9:xcda_filter')
        .notConsumable('gtceu:fluid_filter')
        .inputFluids(Fluid.of('gtceu:cryogenic_supercooled_air', 4000))
        .outputFluids(Fluid.of('gtceu:extreme_clean_dry_air', 4000))
        .duration(200)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_synthesis('af9:trimethylchlorosilane')
        .itemInputs('gtceu:magnesium_dust')
        .inputFluids(Fluid.of('gtceu:dimethyldichlorosilane', 1000), Fluid.of('gtceu:chloromethane', 1000))
        .itemOutputs('3x gtceu:magnesium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:trimethylchlorosilane', 1000))
        .duration(300)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_synthesis('af9:hexamethyldisilazane')
        .inputFluids(Fluid.of('gtceu:trimethylchlorosilane', 2000), Fluid.of('gtceu:ammonia', 3000))
        .itemOutputs('4x gtceu:ammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hexamethyldisilazane', 1000))
        .duration(400)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_blending('af9:hmds_vapor')
        .inputFluids(Fluid.of('gtceu:hexamethyldisilazane', 100), Fluid.of('gtceu:nitrogen', 900))
        .outputFluids(Fluid.of('gtceu:hmds_vapor', 1000))
        .duration(100)
        .EUt(EU_LV)

    // ---- Photoresist (i-line, MUV) ----
    // Formox process: CH3OH + 1/2 O2 -> CH2O + H2O with air over iron molybdate. GT's own formaldehyde recipe (silver
    // catalyst) is HV, which MUV cannot wait for. The catalyst: molybdenite roasted to MoO3, calcined with hematite.
    event.recipes.gtceu.fab_calcination('af9:molybdenum_trioxide')
        .itemInputs('gtceu:molybdenite_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 7000))
        .itemOutputs('gtceu:molybdenum_trioxide_dust')
        .outputFluids(Fluid.of('gtceu:sulfur_dioxide', 2000))
        .blastFurnaceTemp(900)
        .duration(300)
        .EUt(EU_MV)

    // Fe2O3 + 3 MoO3 -> Fe2(MoO4)3
    event.recipes.gtceu.fab_calcination('af9:iron_molybdate')
        .itemInputs('gtceu:hematite_dust', '3x gtceu:molybdenum_trioxide_dust')
        .itemOutputs('gtceu:iron_molybdate_dust')
        .blastFurnaceTemp(800)
        .duration(300)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_synthesis('af9:formaldehyde_formox')
        .notConsumable('gtceu:iron_molybdate_dust')
        .inputFluids(Fluid.of('gtceu:methanol', 1000), Fluid.of('gtceu:air', 3000))
        .outputFluids(Fluid.of('gtceu:formaldehyde', 1000), Fluid.of('minecraft:water', 1000))
        .duration(200)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_synthesis('af9:novolac_resin')
        .inputFluids(Fluid.of('gtceu:phenol', 1000), Fluid.of('gtceu:formaldehyde', 1000))
        .notConsumableFluid(Fluid.of('gtceu:hydrochloric_acid', 100))
        .outputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('minecraft:water', 1000))
        .duration(400)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_synthesis('af9:diazonaphthoquinone')
        .inputFluids(Fluid.of('gtceu:naphthalene', 1000), Fluid.of('gtceu:nitric_acid', 1000), Fluid.of('gtceu:ammonia', 1000))
        .itemOutputs('gtceu:diazonaphthoquinone_dust')
        .outputFluids(Fluid.of('minecraft:water', 2000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(600)
        .EUt(EU_MV)

    event.recipes.gtceu.fab_blending('af9:photoresist')
        .itemInputs('gtceu:diazonaphthoquinone_dust')
        .inputFluids(Fluid.of('gtceu:novolac_resin', 1000), Fluid.of('gtceu:dimethylbenzene', 3000))
        .outputFluids(Fluid.of('gtceu:photoresist', 4000))
        .duration(400)
        .EUt(EU_MV)

    // ---- Developer ----
    event.recipes.gtceu.fab_synthesis('af9:tetramethylammonium_chloride')
        .inputFluids(Fluid.of('gtceu:dimethylamine', 1000), Fluid.of('gtceu:chloromethane', 2000))
        .itemOutputs('gtceu:tetramethylammonium_chloride_dust')
        .outputFluids(Fluid.of('gtceu:hydrochloric_acid', 1000))
        .duration(300)
        .EUt(EU_MV)

    // Membrane electrolysis, as electronic-grade TMAH is made: no potassium or sodium may reach the developer (metal
    // ions shift transistor thresholds). (CH3)4NCl + H2O -> (CH3)4NOH + 1/2 H2 + 1/2 Cl2
    event.recipes.gtceu.fab_blending('af9:tetramethylammonium_chloride_solution')
        .itemInputs('gtceu:tetramethylammonium_chloride_dust')
        .inputFluids(Fluid.of('gtceu:distilled_water', 5000))
        .outputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .duration(100)
        .EUt(EU_LV)

    event.recipes.gtceu.fab_electrolysis('af9:tmah_developer')
        .inputFluids(Fluid.of('gtceu:tetramethylammonium_chloride_solution', 5000))
        .outputFluids(Fluid.of('gtceu:tmah_developer', 5000), Fluid.of('gtceu:chlorine', 1000), Fluid.of('gtceu:hydrogen', 1000))
        .duration(300)
        .EUt(EU_MV)


    // ---- Metal-oxide EUV resist (20 nm, 7 nm) and the orbital station's dry resist ----
    // Sn + 2 Cl2 -> SnCl4
    event.recipes.gtceu.fab_synthesis('af9:tin_tetrachloride')
        .itemInputs('gtceu:tin_dust')
        .inputFluids(Fluid.of('gtceu:chlorine', 4000))
        .outputFluids(Fluid.of('gtceu:tin_tetrachloride', 1000))
        .duration(300)
        .EUt(VA[GTValues.EV])
        .cleanroom(CleanroomType.CLEANROOM)

    // Hydrolysed with methacrylic acid into tin-oxo methacrylate clusters, dissolved in PGMEA
    event.recipes.gtceu.fab_synthesis('af9:euv_photoresist')
        .inputFluids(Fluid.of('gtceu:tin_tetrachloride', 1000), Fluid.of('gtceu:methacrylic_acid', 2000),
            Fluid.of('gtceu:propylene_glycol_methyl_ether_acetate', 4000), Fluid.of('gtceu:ultrapure_water', 1000))
        .outputFluids(Fluid.of('gtceu:euv_photoresist', 4000), Fluid.of('gtceu:hydrochloric_acid', 2000))
        .duration(600)
        .EUt(VA[GTValues.IV])
        .cleanroom(CleanroomType.CLEANROOM)

    // The same clusters as a vapour-deposition source (no spin coating without gravity)
    event.recipes.gtceu.fab_cvd('af9:dry_resist_cartridge')
        .itemInputs('gtceu:tungsten_steel_plate')
        .inputFluids(Fluid.of('gtceu:euv_photoresist', 1000))
        .itemOutputs('af9:dry_resist_cartridge')
        .blastFurnaceTemp(600)
        .duration(400)
        .EUt(VA[GTValues.UV])
        .cleanroom(CleanroomType.CLEANROOM)

    // ---- Coated wafers (Coater Track) ----
    // HMDS prime, the bottom anti-reflective coat (the DUV nodes down to 65 nm), the resist made for the mode's light, the
    // topcoat (the immersion nodes), soft bake: the blank wafer of the substrate becomes its coated wafer, 1.5x the
    // chemicals a node. What the spin spins off is spent solvent (30 mB of 100 mB of resist's worth), distilled back in the
    // SMC machines at 60 %. The orbital station's 1 nm print takes a blank wafer and deposits its resist dry.
    modes.forEach(m => {
        const s = substrates[m.substrate]
        const chemicals = Math.pow(1.5, m.index)
        const coat = [
            Fluid.of('gtceu:hmds_vapor', Math.round(40 * chemicals)),
            Fluid.of(m.resist, Math.round(100 * chemicals))]
        if (m.barc) coat.push(Fluid.of('gtceu:barc', Math.round(60 * chemicals)))
        if (m.immersion) coat.push(Fluid.of('gtceu:tarc', Math.round(60 * chemicals)))
        event.recipes.gtceu.wafer_coating(`af9:coat_${s.id}_wafer`)
            .itemInputs(s.blank)
            .inputFluids(coat)
            .itemOutputs(`af9:coated_${s.id}_wafer`)
            .outputFluids(Fluid.of('gtceu:spent_resist_solvent', Math.round(30 * chemicals)))
            .duration(300)
            .EUt(VA[s.tier])
    })

    // ---- Printed wafers (Photolithography Line) ----
    // Coated wafer in: exposure, PEB, TMAH develop, DI rinse, plasma etch, hard bake, 4A of the mode's tier, each step 1.5x
    // the chemicals. The excimer lasers burn their premix; immersion modes expose through ultrapure water; from 50 nm a
    // high-k HfO2 gate is grown from HfCl4 + water (ALD); the EUV modes burn tin droplets in a hydrogen buffer; from 200 nm
    // on the etch is a plasma of CF4, chlorine, argon and oxygen. The reticle is of the mask class of the node
    // (AF9_WAFERS.maskClass).
    // Output: GT's chip wafer (as many as the substrate yields), and the broken wafer at the mode's base break chance
    // for the recipe viewers. The machine takes the chanced broken wafer out and rolls the real break chance (node,
    // vacuum cleanliness, line version) when the print is done: a broken print gives one broken wafer and no chip wafers
    // (af9-core LithoMachine). The orbital station adds its coolant itself (af9-core OrbitalLithographyMachine), and
    // only takes the reticle from its reticle slot (a reticle in an input bus does not count there).
    // Computation: the 7 nm prints (here) and the 1 nm prints (below) draw CWU/t from a computation hatch.
    modes.forEach(m => {
        const s = substrates[m.substrate]
        const chemicals = Math.pow(1.5, m.index)
        const fluids = [
            Fluid.of('gtceu:tmah_developer', Math.round(200 * chemicals)),
            Fluid.of('gtceu:distilled_water', Math.round(1000 * chemicals)),
            Fluid.of('gtceu:extreme_clean_dry_air', Math.round(1000 * chemicals))]
        // the etch plasma (CF4 and chlorine: HV chemistry) from the 200 nm node on; the 350 nm node is MV and etches wet
        if (m.index >= 1) fluids.push(Fluid.of('gtceu:etch_plasma_gas', Math.round(50 * chemicals)))
        if (m.laserGas) fluids.push(Fluid.of(m.laserGas, Math.round(10 * chemicals)))
        if (m.immersion) fluids.push(Fluid.of('gtceu:ultrapure_water', 1000))
        if (m.euv) {
            fluids.push(Fluid.of('gtceu:tin', 144))
            fluids.push(Fluid.of('gtceu:hydrogen', 1000))
        }
        if (m.highK) fluids.push(Fluid.of('gtceu:hafnium_tetrachloride', 100))
        chips.filter(c => c.reticle && c.native <= m.substrate).forEach(c => {
            const recipe = event.recipes.gtceu[`lithography_${m.id}`](`af9:print_${c.id}_${m.id}`)
                .itemInputs(`af9:coated_${s.id}_wafer`)
                .notConsumable(AF9_WAFERS.reticleItem(c, AF9_WAFERS.maskClass(m.substrate)))
            // the scanner's ArF laser in its laser slot, the orbital station's EUV source in its EUV slot (or either in
            // an input bus)
            if (m.laser) recipe.notConsumable(m.laser)
            if (m.euv) recipe.notConsumable('af9:euv_light_source')
            recipe
                .inputFluids(fluids)
                .itemOutputs(`${yieldOf(m.substrate, c)}x ${printed(m.substrate, c)}`)
                .chancedOutput(`af9:broken_${s.id}_wafer`, m.baseBreak, 0)
                .duration(900)
                .EUt(VA[s.tier], 4)
            // 7 nm: 32 CWU/t
            if (m.id == '7nm') recipe.CWUt(32)
        })
    })

    // ---- Printed wafers (Orbital Lithography Station, 1 nm) ----
    // X-ray FEL, dry resist; 50A of UHV for eight times the line's run time: a hundred times the energy of a 7 nm print.
    // The coolant (500 mB supercooled endion per print) is added by the station; 96 CWU/t.
    // Research, like GT's assembly line: every 1 nm print is unlocked on its own. The Research Station scans the chip's
    // reticle (2A of ZPM, 48 CWU/t, 256000 CWU) into a data orb, which goes in the station's data hatch (or a data
    // bank).
    const chromodynium = substrates[AF9_WAFERS.orbital.substrate]
    chips.filter(c => c.reticle).forEach(c => {
        event.recipes.gtceu.orbital_lithography(`af9:print_${c.id}_1nm`)
            .itemInputs(chromodynium.blank, 'af9:dry_resist_cartridge')
            .notConsumable(AF9_WAFERS.reticleItem(c, AF9_WAFERS.maskClass(chromodynium.index)))
            .itemOutputs(`${yieldOf(chromodynium.index, c)}x ${printed(chromodynium.index, c)}`)
            .chancedOutput(`af9:broken_${chromodynium.id}_wafer`, AF9_WAFERS.orbital.baseBreak, 0)
            .duration(7200)
            .EUt(VA[chromodynium.tier], 50)
            .CWUt(96)
            .stationResearch(b => b.researchStack(Item.of(AF9_WAFERS.reticleItem(c, AF9_WAFERS.maskClass(chromodynium.index))))
                .researchId(`af9_litho_1nm_${c.id}`).EUt(VA[GTValues.ZPM], 2).CWUt(48, 256000))
    })

    // Derived wafers (Nano CPU, Qubit CPU, HPIC, UHPIC) and cutting: GT's own Chemical Reactor and cutter recipes, since
    // every print is GT's own wafer.

    // ---- Broken and contaminated wafers ----
    // Broken wafers are ground for their material; contaminated ones are stripped and RCA-cleaned back into a blank
    // wafer of their substrate (the print is lost).
    substrates.forEach(s => {
        event.recipes.gtceu.macerator(`af9:reclaim_broken_${s.id}_wafer`)
            .itemInputs(`af9:broken_${s.id}_wafer`)
            .itemOutputs(`2x ${s.reclaim}`)
            .duration(100)
            .EUt(EU_LV)
        // RCA clean: SC-1 (particles, organics), a dilute HF dip (the oxide), SC-2 (metal ions)
        event.recipes.gtceu.fab_wet_processing(`af9:clean_contaminated_${s.id}_wafer`)
            .itemInputs(`af9:contaminated_${s.id}_wafer`)
            .inputFluids(Fluid.of('gtceu:sc1_solution', 500), Fluid.of('gtceu:hydrofluoric_acid', 50),
                Fluid.of('gtceu:sc2_solution', 500))
            .itemOutputs(s.blank)
            .duration(200)
            .EUt(VA[Math.min(s.tier, GTValues.LuV)])
            .cleanroom(CleanroomType.CLEANROOM)
        // Rework, as a real fab does with a failed resist layer: piranha strips the baked resist, then the RCA clean;
        // the wafer survives most of the time (the spent piranha is waste)
        event.recipes.gtceu.fab_wet_processing(`af9:rework_broken_${s.id}_wafer`)
            .itemInputs(`af9:broken_${s.id}_wafer`)
            .inputFluids(Fluid.of('gtceu:piranha_solution', 500), Fluid.of('gtceu:sc1_solution', 500),
                Fluid.of('gtceu:sc2_solution', 500))
            .chancedOutput(s.blank, 6000, 0)
            .outputFluids(Fluid.of('gtceu:spent_piranha', 500))
            .duration(300)
            .EUt(VA[Math.min(s.tier, GTValues.LuV)])
            .cleanroom(CleanroomType.CLEANROOM)
    })
    // Contaminated chips: a dilute HF dip and a rinse gives the chip back (MV, no clean room: an MV recipe)
    chips.forEach(c => {
        const path = c.chip.split(':')[1]
        event.recipes.gtceu.fab_wet_processing(`af9:clean_contaminated_${path}`)
            .itemInputs(`af9:contaminated_${path}`)
            .inputFluids(Fluid.of('gtceu:hydrofluoric_acid', 10), Fluid.of('gtceu:distilled_water', 250))
            .itemOutputs(c.chip)
            .duration(60)
            .EUt(EU_MV)
    })
})
