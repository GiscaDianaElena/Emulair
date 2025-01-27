package com.bigbratan.emulair.ext.managers.saveSync

import android.app.Activity
import android.content.Context
import com.bigbratan.emulair.common.metadata.retrograde.CoreID
import com.bigbratan.emulair.common.managers.saveSync.SaveSyncManager
import com.bigbratan.emulair.common.managers.storage.DirectoriesManager

class SaveSyncManagerImpl(
    private val appContext: Context,
    private val directoriesManager: DirectoriesManager
) : SaveSyncManager {
    override fun getProvider(): String = ""

    override fun getSettingsActivity(): Class<out Activity>? = null

    override fun isSupported(): Boolean = false

    override fun isConfigured(): Boolean = false

    override fun getLastSyncInfo(): String = ""

    override fun getConfigInfo(): String = ""

    override suspend fun sync(cores: Set<CoreID>) {}

    override fun computeSavesSpace() = ""

    override fun computeStatesSpace(coreID: CoreID) = ""
}
