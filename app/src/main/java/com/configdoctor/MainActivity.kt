package com.configdoctor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.parser.VlessParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "دستیار هوشمند کانفیگ"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        }

        val input = EditText(this).apply {
            hint = "لینک vless:// را اینجا پیست کن"
            minLines = 3
            setPadding(16, 16, 16, 16)
        }

        val button = Button(this).apply {
            text = "تشخیص"
        }

        val output = TextView(this).apply {
            textSize = 16f
            setPadding(0, 32, 0, 0)
            text = "نتیجه اینجا نمایش داده می‌شود..."
        }

        val scroll = ScrollView(this).apply {
            addView(output)
        }

        root.addView(title)
        root.addView(input)
        root.addView(button)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        button.setOnClickListener {
            val link = input.text.toString().trim()
            if (link.isBlank()) {
                output.text = "لطفاً یه لینک وارد کن"
                return@setOnClickListener
            }

            output.text = "در حال تشخیص...\n(ممکنه چند ثانیه طول بکشه)"
            button.isEnabled = false

            lifecycleScope.launch {
                val parsed = VlessParser.parse(link)

                val resultText = parsed.fold(
                    onSuccess = { config ->
                        try {
                            val diag = Diagnostician.diagnose(this@MainActivity, config)
                            diag.summary
                        } catch (e: Exception) {
                            "❌ خطا در تشخیص:\n${e.message}"
                        }
                    },
                    onFailure = { err ->
                        "❌ خطا در پارس لینک:\n${err.message}"
                    }
                )

                withContext(Dispatchers.Main) {
                    output.text = resultText
                    button.isEnabled = true
                }
            }
        }
    }
}