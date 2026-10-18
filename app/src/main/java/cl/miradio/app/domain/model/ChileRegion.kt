package cl.miradio.app.domain.model

import java.text.Normalizer

/**
 * Regiones de Chile, de norte a sur. Radio Browser guarda en `state` lo que cada
 * radio declaró: a veces la región ("Biobío", "Región Metropolitana"), a veces la
 * ciudad ("Concepción", "Santiago"), con o sin tildes. [aliases] cubre ambas formas.
 */
enum class ChileRegion(val displayName: String, private val aliases: List<String>) {
    ARICA("Arica y Parinacota", listOf("arica", "parinacota", "putre")),
    TARAPACA("Tarapacá", listOf("tarapaca", "iquique", "alto hospicio", "pozo almonte")),
    ANTOFAGASTA("Antofagasta", listOf("antofagasta", "calama", "tocopilla", "mejillones", "san pedro de atacama")),
    ATACAMA("Atacama", listOf("atacama", "copiapo", "vallenar", "caldera", "chanaral")),
    COQUIMBO("Coquimbo", listOf("coquimbo", "la serena", "ovalle", "illapel", "vicuna", "los vilos")),
    VALPARAISO(
        "Valparaíso",
        listOf(
            "valparaiso", "vina del mar", "quilpue", "villa alemana", "san antonio", "quillota",
            "los andes", "san felipe", "la calera", "casablanca", "rapa nui", "isla de pascua",
        ),
    ),
    METROPOLITANA(
        "Metropolitana",
        listOf(
            "metropolitana", "santiago", "region metropolitana", "rm", "puente alto", "maipu",
            "la florida", "providencia", "las condes", "nunoa", "san bernardo", "melipilla", "talagante",
        ),
    ),
    OHIGGINS(
        "O'Higgins",
        listOf("ohiggins", "o higgins", "libertador", "rancagua", "san fernando", "santa cruz", "pichilemu", "rengo"),
    ),
    MAULE("Maule", listOf("maule", "talca", "curico", "linares", "constitucion", "cauquenes", "parral")),
    NUBLE("Ñuble", listOf("nuble", "chillan", "san carlos", "bulnes", "quirihue")),
    BIOBIO(
        "Biobío",
        listOf(
            "biobio", "bio bio", "concepcion", "talcahuano", "los angeles", "coronel", "lota",
            "san pedro de la paz", "chiguayante", "tome", "lebu", "canete", "hualpen",
        ),
    ),
    ARAUCANIA(
        "La Araucanía",
        listOf("araucania", "temuco", "padre las casas", "villarrica", "pucon", "angol", "victoria", "lautaro"),
    ),
    LOS_RIOS("Los Ríos", listOf("los rios", "valdivia", "la union", "rio bueno", "panguipulli")),
    LOS_LAGOS(
        "Los Lagos",
        listOf("los lagos", "puerto montt", "osorno", "castro", "ancud", "chiloe", "puerto varas", "quellon"),
    ),
    AYSEN("Aysén", listOf("aysen", "aisen", "coyhaique", "coihaique", "puerto aysen", "chile chico")),
    MAGALLANES(
        "Magallanes",
        listOf("magallanes", "punta arenas", "puerto natales", "porvenir", "puerto williams", "antartica"),
    ),
    ;

    companion object {
        private val diacritics = Regex("""\p{Mn}+""")
        private val nonAlnum = Regex("""[^a-z0-9]+""")

        /** Pares (alias normalizado, región), los más largos primero: "los rios" gana sobre "rio bueno". */
        private val index: List<Pair<String, ChileRegion>> by lazy {
            entries.flatMap { region -> region.aliases.map { normalize(it) to region } }
                .sortedByDescending { it.first.length }
        }

        /** Región que corresponde al `state` de Radio Browser, o `null` si no se reconoce. */
        fun fromState(state: String?): ChileRegion? {
            if (state.isNullOrBlank()) return null
            val padded = " ${normalize(state)} "
            if (padded.isBlank()) return null
            return index.firstOrNull { (alias, _) -> padded.contains(" $alias ") }?.second
        }

        private fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(diacritics, "")
                .replace("'", "")
                .replace(nonAlnum, " ")
                .trim()
    }
}

/** Región de la estación según su `state`, si se reconoce. */
val Station.region: ChileRegion? get() = ChileRegion.fromState(state)
