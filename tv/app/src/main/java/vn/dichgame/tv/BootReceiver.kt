// SPDX-License-Identifier: AGPL-3.0-only
package vn.dichgame.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Bật TV là dịch vụ phụ đề tự chạy, không cần mở app */
class BootReceiver: BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
		if(Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return
		try { SubtitleService.start(context) } catch(_: Exception) {}
	}
}
