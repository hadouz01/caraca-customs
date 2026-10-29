package com.caraca.customs.constants;

/**
 * How Exp. Share All hands the active Pokémon's experience to the rest of the party.
 *
 * @author hadouz01
 * @since 1.11.0
 */
public enum ExpDivisionMethod {
    /**
     * Every other member gets a copy of the active Pokémon's experience, scaled by the configured multiplier.
     */
    MULTIPLIED,
    /**
     * The active Pokémon's experience is split between every member, the active Pokémon included.
     */
    SHARED
}
