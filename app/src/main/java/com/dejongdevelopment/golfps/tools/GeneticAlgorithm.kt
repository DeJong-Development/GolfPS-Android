package com.dejongdevelopment.golfps.tools

class GeneticAlgorithm<T>(
    private val population: MutableList<T>,
    private val fitness: (T) -> Double,
    private val mutate: (T) -> T,
    private val crossover: (T, T) -> T
) {
    fun evolve(generations: Int, survivors: Int): T? {
        if (population.isEmpty() || generations <= 0 || survivors <= 0) {
            return population.maxByOrNull(fitness)
        }

        repeat(generations) {
            val ranked = population.sortedByDescending(fitness)
            val selected = ranked.take(survivors.coerceAtMost(ranked.count()))

            population.clear()
            population.addAll(selected)

            while (population.count() < ranked.count()) {
                val firstParent = selected.random()
                val secondParent = selected.random()
                population.add(mutate(crossover(firstParent, secondParent)))
            }
        }

        return population.maxByOrNull(fitness)
    }
}
