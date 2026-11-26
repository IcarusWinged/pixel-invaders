package com.icarus.pixelinvaders.ui

/** Where the app's latest APK is published (a GitHub Release asset, rebuilt by CI on every push to main). */
object AppLinks {
    const val REPO_URL = "https://github.com/IcarusWinged/pixel-invaders"
    const val RELEASES_URL = "$REPO_URL/releases/latest"
    const val APK_URL = "$REPO_URL/releases/latest/download/pixel-invaders-debug.apk"
}
