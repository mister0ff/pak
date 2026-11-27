package com.kindo.aimlab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: MaterialButton
    private lateinit var tvStatus: MaterialTextView
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aplica o tema escuro
        setTheme(R.style.Theme_AimLab)

        // Cria o layout raiz programaticamente
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 32)
        root.setBackgroundColor(0xFF111111.toInt()) // Fundo escuro

        // Título
        val title = MaterialTextView(this)
        title.text = "AimLab FF - Overlay Controller"
        title.textSize = 24f
        title.setTextColor(0xFFFFFFFF.toInt())
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        // Status
        tvStatus = MaterialTextView(this)
        tvStatus.text = "Status: Inactive"
        tvStatus.textSize = 18f
        tvStatus.setTextColor(0xFFFF0000.toInt())
        tvStatus.setPadding(0, 16, 0, 16)

        // Instruções
        val instructions = MaterialTextView(this)
        instructions.text = "1. Grant overlay permission.\n2. Start Service.\n3. Launch Free Fire.\n4. The menu will float over the game.\n\nFeatures:\n- ESP Toggle\n- Stealth Mode\n- Crosshair\n- Native Memory Simulation"
        instructions.textSize = 14f
        instructions.setTextColor(0xFFFFFFFF.toInt())
        instructions.setPadding(0, 0, 0, 16)

        // Botão de Toggle
        btnToggle = MaterialButton(this)
        btnToggle.text = "Start Overlay"
        btnToggle.setOnClickListener {
            // Verifica se a permissão de overlay foi concedida
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, FloatingService::class.java)
            if (isRunning) {
                serviceIntent.action = "STOP"
                startService(serviceIntent)
                isRunning = false
                btnToggle.text = "Start Overlay"
                tvStatus.text = "Status: Inactive"
                tvStatus.setTextColor(0xFFFF0000.toInt())
            } else {
                serviceIntent.action = "START"
                // Usa startForegroundService para garantir que o serviço não seja matado
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                isRunning = true
                btnToggle.text = "Stop Overlay"
                tvStatus.text = "Status: Active"
                tvStatus.setTextColor(0xFF00FF00.toInt())
            }
        }

        // Adiciona os views ao layout
        root.addView(title)
        root.addView(tvStatus)
        root.addView(instructions)
        root.addView(btnToggle)

        setContentView(root)
    }
}