package com.prime.nobuffer.shields.engine

/** Element-hiding rules (`##sel`, `dom.com##sel`, `dom.com#@#sel`) from filter lists. */
class CosmeticIndex(
    private val generic: List<String>,
    private val genericExcludedOn: List<Pair<String, List<String>>>,
    private val specific: Map<String, List<String>>,
    private val exceptions: Map<String, Set<String>>,
    private val globalExceptions: Set<String>
) {
    val genericCount: Int get() = generic.size

    fun selectorsFor(host: String?): List<String> {
        val h = host?.lowercase().orEmpty()
        val hostChain = domainChain(h)
        val blocked = HashSet<String>(globalExceptions)
        hostChain.forEach { d -> exceptions[d]?.let(blocked::addAll) }

        val out = LinkedHashSet<String>()
        // Domain-specific first so the generic cap never starves them.
        hostChain.forEach { d -> specific[d]?.forEach { if (it !in blocked) out += it } }
        var generics = 0
        for (sel in generic) {
            if (generics >= MAX_GENERIC) break
            if (sel !in blocked) { out += sel; generics++ }
        }
        for ((sel, excl) in genericExcludedOn) {
            if (sel !in blocked && excl.none { DomainUtils.hostMatches(h, it) }) out += sel
        }
        return out.toList()
    }

    private fun domainChain(host: String): List<String> {
        if (host.isEmpty()) return emptyList()
        val chain = ArrayList<String>()
        var current = host
        while (true) {
            chain += current
            val dot = current.indexOf('.')
            if (dot == -1 || current.indexOf('.', dot + 1) == -1) break
            current = current.substring(dot + 1)
        }
        return chain
    }

    class Builder {
        private val generic = LinkedHashSet<String>()
        private val genericExcludedOn = ArrayList<Pair<String, List<String>>>()
        private val specific = HashMap<String, MutableList<String>>()
        private val exceptions = HashMap<String, MutableSet<String>>()
        private val globalExceptions = HashSet<String>()

        fun addHide(selector: String, include: List<String>, exclude: List<String>) {
            when {
                include.isNotEmpty() -> include.forEach { specific.getOrPut(it) { ArrayList() } += selector }
                exclude.isNotEmpty() -> genericExcludedOn += selector to exclude
                else -> generic += selector
            }
        }

        fun addException(selector: String, include: List<String>) {
            if (include.isEmpty()) globalExceptions += selector
            else include.forEach { exceptions.getOrPut(it) { HashSet() } += selector }
        }

        fun build() = CosmeticIndex(generic.toList(), genericExcludedOn, specific, exceptions, globalExceptions)
    }

    companion object {
        const val MAX_GENERIC = 8000
        val EMPTY = CosmeticIndex(emptyList(), emptyList(), emptyMap(), emptyMap(), emptySet())

        /** Packs selectors into `display:none` rules, chunked so one invalid selector only voids its chunk. */
        fun buildCss(selectors: List<String>, chunk: Int = 40): String =
            selectors.chunked(chunk).joinToString("\n") { it.joinToString(",") + "{display:none!important}" }
    }
}
