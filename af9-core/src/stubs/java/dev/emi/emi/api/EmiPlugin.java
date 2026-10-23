package dev.emi.emi.api;

/** Compile-time stand-in for EMI's plugin interface (not shipped). */
public interface EmiPlugin {

    void register(EmiRegistry registry);
}
