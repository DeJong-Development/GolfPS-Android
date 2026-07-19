package com.dejongdevelopment.golfps.tools

import android.graphics.Bitmap
import java.net.URL

object BitmojiUtility {
    fun getBitmojiURL(completion: (URL?) -> Unit) {
        completion(null)
    }

    fun downloadBitmojiImage(completion: (URL?, Bitmap?) -> Unit) {
        completion(null, null)
    }
}
