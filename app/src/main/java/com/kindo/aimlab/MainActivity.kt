package com.kindo.aimlab

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: MaterialButton
    private lateinit var tvStatus: MaterialTextView
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Não vamos criar activity_main.xml, vamos usar programático ou um layout simples.
        // Vamos criar o layout programaticamente para economizar arquivos, ou usar um layout simples.
        // Para simplificar, vou criar um layout XML básico.
        
        // Criando layout programático rápido para não adicionar mais XML
        val root = android.widget.LinearLayout(this)
        root.orientation = android.widget.LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 32)

        val title = MaterialTextView(this)
        title.text = "AimLab FF - Overlay Controller"
        title.textSize = 24f
        title.setTextColor(0xFFFFFFFF.toInt())
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        tvStatus = MaterialTextView(this)
        tvStatus.text = "Status: Inactive"
        tvStatus.textSize = 18f
        tvStatus.setTextColor(0xFF00FF00.toInt())
        tvStatus.setPadding(0, 16, 0, 16)

        val instructions = MaterialTextView(this)
        instructions.text = "1. Grant overlay permission.\n2. Start Service.\n3. Launch Free Fire.\n4. The menu will float over the game.\n\nFeatures:\n- ESP Toggle\n- Stealth Mode (Hides UI from screenshots)\n- Crosshair\n- Touch-based Player Simulation"
        instructions.textSize = 14f
        instructions.setTextColor(0xFFFFFFFF.toInt())
        instructions.setPadding(0, 0, 0, 16)

        btnToggle = MaterialButton(this)
        btnToggle.text = "Start Overlay"
        btnToggle.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                return@setOnClickListener
            }

            val intent = Intent(this, FloatingService::class.java)
            if (isRunning) {
                stopService(intent)
                isRunning = false
                btnToggle.text = "Start Overlay"
                tvStatus.text = "Status: Inactive"
                tvStatus.setTextColor(0xFFFF0000.toInt())
            } else {
                startForegroundService(intent)
                isRunning = true
                btnToggle.text = "Stop Overlay"
                tvStatus.text = "Status: Active"
                tvStatus.setTextColor(0xFF00FF00.toInt())
            }
        }

        root.addView(title)
        root.addView(tvStatus)
        root.addView(instructions)
        root.addView(btnToggle)
        setContentView(root)
    }
}