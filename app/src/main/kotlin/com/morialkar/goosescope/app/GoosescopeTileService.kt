package com.morialkar.goosescope.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Tuile de réglages rapides (SPEC F27): un appui ouvre le mode direct. Sur un appareil verrouillé,
 * l'activité s'affiche par-dessus l'écran verrouillé (F28), donc pas besoin de déverrouiller.
 */
class GoosescopeTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            // Une tuile qui lance une app n'a pas d'état on/off: elle reste « inactive » comme un bouton.
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            // API 33: la version PendingIntent n'existe pas encore.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
