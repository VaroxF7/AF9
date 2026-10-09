# Auxiliary power

Some machines have to be kept at a temperature to run a recipe at all (the Sanguinite Hearth Furnace is the model). Their
recipes ask for **auxiliary power**, a second kind of power besides the recipe's own (primary) EU/t.

## The hatches

Every energy **input** hatch (GT's, in every tier and amperage, and the pack's own such as the Micro Universe hatch) has a
power mode. Right-click it with a **screwdriver** to switch it between

* **Primary** (the default): it feeds the recipes of the machine it is in, as always;
* **Auxiliary**: it is no part of the primary power any more (no recipe draws on it) and its energy only goes to the
  machine's auxiliary demand.

The machine collects its hatches again at once, no rebuilding. Laser hatches and the wireless receivers have no such mode.

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
`WorkableMultiblockMachine` (demand, draw, collecting the handlers again), `PowerManagementFancyTab`, `GTRecipeWidget` (the
tooltip). Texts: `af9.power.*` in the AF9 Core lang files.
