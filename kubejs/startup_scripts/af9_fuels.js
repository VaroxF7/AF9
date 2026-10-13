// AF9 - furnace fuels: spent filter cake (the filtration plant's waste dust) burns like coal,
// asphalt bottoms burn like coke.
ItemEvents.modification(event => {
    event.modify('gtceu:filter_cake_dust', item => {
        item.burnTime = 1600
    })
    event.modify('gtceu:asphalt_dust', item => {
        item.burnTime = 3200
    })
})
