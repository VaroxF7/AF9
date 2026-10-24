# Auxiliary power

Some machines have to be kept at a temperature to run a recipe at all (the Sanguinite Hearth Furnace is the model). Their
recipes ask for **auxiliary power**, a second kind of power besides the recipe's own (primary) EU/t.

## The hatches

Every energy **input** hatch (GT's, in every tier and amperage, and the pack's own such as the Micro Universe hatch) and every
**Wireless Energy Receiver** has a
power mode. Right-click it with a **screwdriver** to switch it between

* **Primary** (the default): it feeds the recipes of the machine it is in, as always;
* **Auxiliary**: it is no part of the primary power any more (no recipe draws on it) and its energy only goes to the
  machine's auxiliary demand.

The machine collects its hatches again at once, no rebuilding. A receiver in Auxiliary shows blue like GT's hatches and its
screen says the mode. Laser hatches have no such mode.

**Orbital Array Mk2:** its primary power comes only from wireless receivers and laser hatches. Plain energy input hatches
in the Mk2 are forced to Auxiliary (wireless receivers stay as they are set) (`OrbitalLithographyMachine.forceAuxiliaryHatches`, set again every tick, so the
screwdriver cannot switch them back). The basic Orbital Station is unchanged.

## The machine

* A recipe names its auxiliary demand in its data: `.addData('af9_aux_eut', 128)` in KubeJS (EU/t, a long). The recipe
  viewers show it in the tooltip of the recipe's EU line, under the slots.
* A recipe with a demand **starts only when** the auxiliary hatches hold at least that much, and **draws it every tick** while
  it runs. When the hatches run dry mid-run, the machine has cooled down and the run is lost.
* A machine class can add a demand of its own that holds whatever it runs: `WorkableMultiblockMachine.getBaseAuxiliaryEUt()`.
* The Sanguinite Hearth Furnace's heaters (4 A of LuV, as before) draw on the auxiliary hatches when it has any, else on the
  primary ones.

## The screen

Every multiblock has a **Power Management** tab on the left of its screen (GT's own, and AF9's: the Orbital Lithography
Station, the Space Elevator and its modules): the primary supply and what the running recipe draws, the auxiliary hatches, their
stored EU and what is needed.

## Where it is

The fork's patch `0005` (`gtceu-fork/patches`): `EnergyHatchPartMachine` (mode, screwdriver),
`WorkableMultiblockMachine` (demand, draw, collecting the handlers again; `getAuxiliaryParts` / `hasAuxiliary` over every
`IAuxiliaryPowerPart`, patch `0025`: GT's energy hatches and `WirelessReceiverHatch`), `PowerManagementFancyTab`, `GTRecipeWidget` (the
tooltip). Texts: `af9.power.*` in the AF9 Core lang files.
