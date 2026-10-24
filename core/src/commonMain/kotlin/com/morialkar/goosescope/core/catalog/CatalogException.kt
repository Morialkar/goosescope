package com.morialkar.goosescope.core.catalog

/** Catalogue invalide. Liste toutes les violations d'un coup pour corriger en une passe (ARCHITECTURE §5). */
class CatalogException(val violations: List<String>) :
    Exception("Catalogue invalide (${violations.size}):\n" + violations.joinToString("\n") { "- $it" })
