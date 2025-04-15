package com.kxxr.sharide.viewmodel

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor() : ViewModel() {
    var nfcValue = mutableStateOf<String?>(null)
        private set

    fun setNfcValue(value: String) {
        nfcValue.value = value
    }

    fun readSector2Block0(tag: Tag): String {
        val mfc = MifareClassic.get(tag)
        return try {
            mfc.connect()
            val sector = 2
            if (mfc.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)) {
                val blockIndex = mfc.sectorToBlock(sector)
                val data = mfc.readBlock(blockIndex)
                String(data).trim { it <= ' ' || it == '\u0000' }
            } else {
                "Auth failed"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Read error"
        } finally {
            try {
                mfc.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
