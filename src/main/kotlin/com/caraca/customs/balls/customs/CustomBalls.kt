package com.caraca.customs.balls.customs

import com.caraca.customs.balls.customs.ancient.CustomAncientBalls
import com.caraca.customs.balls.customs.ancient.CustomAncientUltraBalls
import com.caraca.customs.balls.customs.regular.CustomRegularBalls
import com.caraca.customs.balls.customs.regular.CustomRegularUltraBalls
import com.caraca.customs.balls.modules.CustomBallProperties

/**
 * Aggregator component serving as the unified registry collection for all custom Poké Ball definitions.
 *
 * Combines categorized declarations from regular and ancient Poké Ball registries into a single,
 * immutable collection to streamline initialization in [com.caraca.customs.balls.CustomBallRegistry].
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBalls {

    /**
     * Immutable collection containing the concatenated list of all custom Poké Ball property definitions.
     *
     * Dynamically aggregates definitions from:
     * - [CustomRegularBalls.ALL] (Standard custom Poké Balls)
     * - [CustomRegularUltraBalls.ALL] (Ultra variant custom Poké Balls)
     * - [CustomAncientBalls.ALL] (Hisuian/Ancient custom Poké Balls)
     * - [CustomAncientUltraBalls.ALL] (Ancient Ultra variant custom Poké Balls)
     */
    val ALL: List<CustomBallProperties> by lazy {
        buildList {
            addAll(CustomRegularBalls.ALL)
            addAll(CustomRegularUltraBalls.ALL)
            addAll(CustomAncientBalls.ALL)
            addAll(CustomAncientUltraBalls.ALL)
        }
    }
}