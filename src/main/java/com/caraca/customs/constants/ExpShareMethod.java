package com.caraca.customs.constants;

/**
 * How the {@link ExpDivisionMethod#SHARED} division splits the active Pokémon's experience.
 *
 * @author hadouz01
 * @since 1.11.0
 */
public enum ExpShareMethod {
    /**
     * Every member gets the same slice, the active Pokémon included.
     */
    EQUALLY,
    /**
     * The active Pokémon keeps half and the other members split the rest.
     */
    BALANCED
}
