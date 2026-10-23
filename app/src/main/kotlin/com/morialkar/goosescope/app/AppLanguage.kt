package com.morialkar.goosescope.app

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import com.morialkar.goosescope.core.catalog.Lang

/**
 * Langue de l'app par la langue par app d'Android (ARCHITECTURE §1.4): le système la mémorise
 * lui-même et recrée l'activité quand elle change. Anglais par défaut (SPEC F17).
 */
class AppLanguage(context: Context) {
    private val manager = context.getSystemService(LocaleManager::class.java)

    fun current(): Lang = if (manager.applicationLocales.get(0)?.language == "fr") Lang.FR else Lang.EN

    /** Liste vide = jamais choisie: on impose l'anglais plutôt que de suivre la langue du système. */
    fun ensureDefault() {
        if (manager.applicationLocales.isEmpty) set(Lang.EN)
    }

    fun set(lang: Lang) {
        manager.applicationLocales = LocaleList.forLanguageTags(if (lang == Lang.FR) "fr" else "en")
    }

    fun toggle() = set(if (current() == Lang.FR) Lang.EN else Lang.FR)
}
