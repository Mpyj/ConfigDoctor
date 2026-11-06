package com.configdoctor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = ConstraintLayout(this)
        val textView = TextView(this).apply {
            text = "دستیار هوشمند کانفیگ\n\nنسخه 0.1.0\n\nبیلد اول موفق شد ✅"
            textSize = 20f
            setPadding(64, 64, 64, 64)
        }
        layout.addView(textView)
        setContentView(layout)
    }
}