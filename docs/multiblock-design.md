# AF9 Multiblock Design Guide

> Read this before designing or changing any multiblock shape, model, render or structure pattern.
> Style is fixed: **sci-fi, industrial, brutalist**. New multiblocks **extend the patterns AF9 already has** (Hearth, Litho Mk1/Mk2,
> Logistics Hub, Orbital Array, Space Elevator, Void Miner, Scanner); they do not invent a new visual language.

## Provenance (what this is based on)

Cloud web access was limited (GTNH wiki, Monifactory docs and Star Technology docs were unreachable or empty), so this guide is mostly
distilled from knowledge of the packs, **not from fresh screenshots**. Confidence: GTCEu defaults and GTNH (high), Monifactory/Nomifactory
(medium), Star Technology and GregTech Odyssey (medium-low on specifics; GTO is publicly described as 350+ multiblocks with "structural
modules"). Treat pack-specific claims as design inspiration, not facts to cite. Re-verify in-game or with screenshots if a rule is
load-bearing.

## 1. What the packs actually do

**GTCEu defaults.** Boxes of one casing type, 3x3x3 up to roughly 5x5x7 (EBF, Large Chemical Reactor, Multi Smelter, Assembly Line,
Distillation Tower, Cleanroom). Identity comes from: the casing texture, an **overlay on the controller**, and a **single hero element**
inside: coils in the EBF, a rotor in the Large Turbine (a real animated block-entity render), the glowing ring of the Fusion Reactor,
the tall column of the Distillation Tower. Hatches replace casing blocks on the faces; the pattern says "any casing here".

**GTNH.** Same box language at the low end, then a deliberate jump to **megastructures with a rendered centerpiece**:
Eye of Harmony (a large sphere and ring, tens of blocks wide, animated, with a rendered star), Godforge (a big modular complex: a
central core plus **separate upgrade modules** you add around it), Space Elevator (a tall tower with **module slots** and a cable render),
Mega EBF/VF-style scale-ups, Active Transformer, Quantum Force Transformer. Philosophy: a multiblock that sits at a major progression
gate should be a *project*, with a footprint a player remembers, resources that scale with the build, and a visible effect while it works.

**Monifactory / Nomifactory CEu.** Mostly GT's own boxes plus a handful of custom multis built with MultiblockTweaker/KubeJS. Philosophy
is pragmatism: recognisable GT casings, a clear controller face, and *tier progression by casing swap* (the same footprint gets better with
a higher casing/coil). Little custom rendering.

**Star Technology.** A GTCEu-Modern skyblock aimed at UIV and beyond, with custom multis made through KubeJS/Java and a lot of
late-game scale. Philosophy (inferred): each late stage gets its own named structure rather than "bigger EBF", and space/stellar themes
drive silhouettes (towers, rings, spheres).

**GregTech Odyssey / MBD2-based packs (TREGGECK, Tregicality, Greg-E).** Many custom multis with custom block/OBJ models, some with
bespoke logic (Greg-E's endgame multiblock needs repairs). Philosophy: more machines = narrower jobs; the model and the interaction
(a UI minigame, a repair, a module slot) tell the player what the machine does.

## 2. Cross-pack design principles

1. **Silhouette first.** From 30 blocks away the player should tell a Hearth from a Litho line from an Orbital Array. One dominant
   shape per machine (tower, ring, tube, sphere, crossed truss), not a generic cube with a texture.
2. **One casing family per machine, one hero element.** Casing sets the material (brutalist plate, plascrete, steel); the hero element
   (coil bank, lens column, light ring, rotor, cable) carries the story and is the only thing that glows/animates much.
3. **Scale tracks progression.** Early: 3x3x3 to 5x5x5. Mid: up to ~9x9x12, often repeatable (setRepeatable) so the player chooses
   depth. Late/endgame: 15-35+ blocks with a rendered centerpiece. Never make a mid-tier machine bigger than the endgame one.
4. **Tier shows in the look.** Same footprint upgraded by casing/coil/glass swap, *or* a Mk2 that adds a module/ring/second tower.
   The Mk2 reads as "Mk1 plus", never a redesign.
5. **Modules attach to ports.** Add-on structures (GTNH Godforge modules, Space Elevator module slots, AF9 Logistics Hub/Rack on fiber
   ports) are separate small multis that connect to a host. This keeps the main pattern small and makes upgrades feel physical.
6. **Hatches live on a defined face or deck, not everywhere.** GT's default is "any casing"; good custom multis restrict it (AF9:
   Hearth's top deck is the only place for controller and hatches; Litho: casings `C`/`I` at the front). The controller sits on the
   side the player approaches; it is never buried.
7. **Readability under the hood.** The inside should be legible in a cutaway: process flows front to back (Litho: cassette, coater,
   bake, develop, lens column, light source), so the layout teaches the process.
8. **Animation is a reward, not noise.** Animate only while working (light ring pulses, rotor spins, climber moves, orbital ring spins).
   Fade out on stop. One animated hero per machine; everything else static.
9. **Cheap to render.** Prefer baked JSON models and a few block-entity renders. Per-frame work must tolerate shader packs (see the
   Oculus/Iris handling in `LightRingRender`/`AF9RenderTypes`) and not hide translucent GT frames.
10. **Flexible but bounded structure.** Only maximums for hatches, never required counts (AF9 rule: `setMaxGlobalLimited`). Repeatable
    sections give size choice. Previews must show a valid, auto-buildable minimum.

## 3. AF9 rules (the checklist)

**Style**
- Sci-fi industrial brutalist: slab plates, exposed trusses, hazard stripes, heavy frames, small status lamps. No organic or
  church-like shapes (the first Hearth was rejected for that). Extend the original pattern.
- Use existing AF9 casings/plascrete/steel and GT frames; add a new casing only when a new material identity is needed.

**Shape and scale**
- Pick one silhouette archetype: tower, tube/line, ring/array, box with hero, or dome/sphere (late only).
- Tiers: LV-EV 3x3x3 to 5x5x7; IV-ZPM up to ~9x9x12 or repeatable lines; UV+ 15+ blocks; endgame 25-70 with a rendered centerpiece.
- Mk2 = Mk1 scaled, doubled, or with a side module/ring. Keep the controller face and hatch zone of Mk1.

**Casing and hatches**
- One main casing, one accent casing, one hero block. Glass only where the process must be visible.
- Hatches/controller on a single face or deck; list maximums only; leave the opposite and top faces clean for the silhouette.
- Parts (energy, buses, fluids, coolant) in the places of casings, not extra protrusions.

**Interaction**
- Give each multi a clear UI: a console/screen widget that shows the process, what it lacks, and counters (Litho, Elevator, Scanner
  pattern). Minigames/tuning are bonus-only (see `multiblock-mechanics-research.md`).

**Animation and effects**
- At most one animated hero, tied to the working state; fade on stop; support Iris/Oculus and Shimmer per existing handling.

**Process for a new multiblock**
1. State archetype and tier. 2. Sketch silhouette (render in `/mnt/project-files`-style PNGs, front + cutaway + iso). 3. Choose casing
family and hero. 4. Define the hatch face/deck. 5. Define Mk2/module attachment points up front. 6. Add quest and tooltip.
7. Only then write the pattern/code.

## 4. Existing AF9 references to copy from (approximate; check the renders in the project files)

| Machine | Archetype | Hero | Hatch zone |
|---|---|---|---|
| Sanguinite Hearth | tower | glowing core | top deck |
| Litho Mk1/Mk2 | tube/line | lens column, light source | front casings |
| Logistics Hub / Rack | attachable module | fiber ports | host's fiber ports |
| Orbital Array | ring/array | spinning light ring | deck |
| Space Elevator | tower + modules | cable + climber | base, module slots |
