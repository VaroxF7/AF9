package com.af9.core.ae2;

import appeng.block.crafting.AbstractCraftingUnitBlock;

/**
 * Crafting CPU Core: AE2 crafting unit block ({@link CpuCoreBlockEntity}) that is the Crafting CPU Array's connection to
 * the ME network. It takes a cable on any face like any AE2 device. Its model shows whether its array is running
 * (AE2's {@code powered} state).
 */
public class CpuCoreBlock extends AbstractCraftingUnitBlock<CpuCoreBlockEntity> {

    public CpuCoreBlock(Properties properties) {
        super(properties, CpuCoreType.CORE);
    }
}
