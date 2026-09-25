package com.af9.core.common;

/**
 * A multiblock that only starts a recipe its energy (or laser) hatches can actually supply: GT's own voltage check
 * would let hatches of the tier below start a 4A recipe and then starve it.
 */
public interface IPowerGated {

    /** EU/t the input hatches can deliver: voltage x amperage of all of them together, 0 while not formed. */
    long getAvailableEUt();
}
