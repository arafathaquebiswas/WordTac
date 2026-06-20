package com.example.ui.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    fun speak(word: String) {
        if (isReady && tts != null) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "WordTac_Spelling_Engine")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
