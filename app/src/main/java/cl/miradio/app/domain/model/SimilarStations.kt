package cl.miradio.app.domain.model

/**
 * Elige la radio más parecida a [target] para ofrecerla cuando [target] no transmite:
 * pesa la misma categoría, la misma región y los tags en común; desempata por votos.
 */
object SimilarStations {

    fun pick(target: Station, candidates: List<Station>): Station? {
        val targetCategories = StationCategory.entries.filter { it != StationCategory.ALL && it.matches(target) }
        val targetRegion = target.region
        val targetTags = target.tags.toSet()

        return candidates
            .asSequence()
            .filter { it.id != target.id && !it.isUnavailable }
            .map { candidate -> candidate to score(candidate, targetCategories, targetRegion, targetTags) }
            .filter { (_, score) -> score > 0 }
            .maxWithOrNull(compareBy<Pair<Station, Int>> { it.second }.thenBy { it.first.votes })
            ?.first
    }

    private fun score(
        candidate: Station,
        targetCategories: List<StationCategory>,
        targetRegion: ChileRegion?,
        targetTags: Set<String>,
    ): Int {
        var score = 0
        if (targetCategories.any { it.matches(candidate) }) score += 3
        if (targetRegion != null && candidate.region == targetRegion) score += 2
        score += candidate.tags.count { it in targetTags }.coerceAtMost(3)
        return score
    }
}
