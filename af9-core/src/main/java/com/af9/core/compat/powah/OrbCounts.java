package com.af9.core.compat.powah;

/**
 * What an Energizing Orb recipe of Powah carries on top of its own data: how many of each ingredient it takes (a recipe's
 * ingredient may say {@code "count": 4}). Mixed into Powah's recipe class ({@code OrbRecipeMixin}); the counts run in step
 * with the recipe's ingredient list, an empty array is "one of each" (Powah's own recipes).
 */
public interface OrbCounts {

    int[] af9Counts();

    void af9SetCounts(int[] counts);

    /** A recipe with an ingredient of more than one: it runs in the Energizing Orb Mk2 only. */
    boolean af9Counted();
}
