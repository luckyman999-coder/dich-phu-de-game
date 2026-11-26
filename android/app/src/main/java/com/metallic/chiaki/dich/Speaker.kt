// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - giọng đọc tiếng Việt, mỗi nhân vật một giọng

package com.metallic.chiaki.dich

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class Speaker(context: Context, private val prefs: DichPrefs)
{
	companion object
	{
		private const val TAG = "DichSpeaker"
		private val VI = Locale("vi", "VN")
	}

	private var ready = false
	private var viVoices: List<Voice> = emptyList()
	private val speakerVoice = HashMap<String, Pair<Voice?, Float>>()

	/** Có giọng tiếng Việt trên máy không (để báo người dùng cài) */
	var hasVietnamese = true
		private set

	private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
		if(status == TextToSpeech.SUCCESS)
			onInit()
		else
			Log.e(TAG, "TTS init thất bại: $status")
	}

	private fun onInit()
	{
		val r = tts.setLanguage(VI)
		hasVietnamese = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
		viVoices = try
		{
			tts.voices?.filter { it.locale.language == "vi" && !it.isNetworkConnectionRequired }
				?.sortedBy { it.name } ?: emptyList()
		}
		catch(e: Exception) { emptyList() }
		tts.setAudioAttributes(AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_ASSISTANT)
			.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
			.build())
		ready = true
		Log.i(TAG, "TTS sẵn sàng, ${viVoices.size} giọng tiếng Việt")
	}

	fun speak(t: Translation)
	{
		if(!ready || !prefs.speak) return
		val (voice, pitch) = voiceFor(t.speaker, t.gender)
		if(voice != null)
			tts.setVoice(voice)
		else
			tts.setLanguage(VI)
		tts.setPitch(pitch)
		tts.setSpeechRate(prefs.speechRate)
		// Câu mới thay câu cũ để không bị đọc dồn
		tts.speak(t.vietnamese, TextToSpeech.QUEUE_FLUSH, null, t.original.hashCode().toString())
	}

	fun stop()
	{
		if(ready) tts.stop()
	}

	fun shutdown()
	{
		tts.shutdown()
	}

	/** Nam giọng trầm, nữ giọng cao; nếu máy có nhiều giọng Việt thì mỗi nhân vật một giọng khác nhau */
	private fun voiceFor(speaker: String, gender: Gender): Pair<Voice?, Float>
	{
		val key = speaker.lowercase().ifEmpty { "_" + gender.name }
		return speakerVoice.getOrPut(key) {
			val h = (key.hashCode() and 0x7fffffff)
			val voice = if(viVoices.isEmpty()) null else viVoices[h % viVoices.size]
			val jitter = ((h / 7) % 5) * 0.03f // nhân vật cùng giới vẫn hơi khác nhau
			val pitch = when(gender)
			{
				Gender.MALE -> 0.78f + jitter
				Gender.FEMALE -> 1.15f + jitter
				Gender.UNKNOWN -> 1.0f
			}
			voice to pitch
		}
	}
}
