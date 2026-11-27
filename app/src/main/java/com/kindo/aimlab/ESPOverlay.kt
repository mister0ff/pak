package com.kindo.aimlab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import org.json.JSONObject

class ESPOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isEspEnabled = true
    private var isStealth = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Lista de jogadores detectados pelo motor nativo
    private val detectedPlayers = mutableListOf<JSONObject>()
    
    // Crosshair
    private val crosshairSize = 40f
    private val crosshairColor = Color.RED
    private val crosshairWidth = 3f

    // Handler para atualizar a UI em intervalos (60fps)
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isEspEnabled && width > 0 && height > 0) {
                refreshFromNative()
                invalidate()
            }
            postDelayed(this, 16) // ~60 FPS
        }
    }

    init {
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
    }

    // --- Funções Nativas (JNI) ---
    external fun nativeStart()
    external fun nativeStop()
    external fun nativeGetStatus(): String
    external fun nativeGetProjectedPlayers(screenWidth: Int, screenHeight: Int): Array<out Any?>?
    external fun nativeForceUpdate()

    companion object {
        init {
            try {
                System.loadLibrary("aimlab")
                android.util.Log.d("AimLab", "Native library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("AimLab", "Failed to load native library: " + e.message)
            }
        }
    }

    // Métodos Públicos para o Service
    fun setEspEnabled(enabled: Boolean) {
        isEspEnabled = enabled
        if (enabled) {
            startEngine()
        } else {
            stopEngine()
        }
        invalidate()
    }

    fun setStealthMode(enabled: Boolean) {
        isStealth = enabled
        paint.alpha = if (enabled) 150 else 255
        invalidate()
    }

    private fun startEngine() {
        nativeStart()
    }

    private fun stopEngine() {
        nativeStop()
    }

    // Atualiza a lista de jogadores a partir do Native
    private fun refreshFromNative() {
        try {
            val result = nativeGetProjectedPlayers(width, height)
            detectedPlayers.clear()
            if (result != null) {
                for (item in result) {
                    if (item is JSONObject) {
                        detectedPlayers.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AimLab", "Error refreshing from native: " + e.message)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isEspEnabled) return

        val cx = width / 2f
        val cy = height / 2f

        // 1. Desenhar Crosshair Central
        drawCrosshair(canvas, cx, cy)

        // 2. Desenhar Caixas de Jogadores Detectados
        for (player in detectedPlayers) {
            try {
                val px = player.getDouble("x").toFloat()
                val py = player.getDouble("y").toFloat()
                val hp = player.getInt("hp")
                val name = player.getString("name")
                drawPlayerBox(canvas, px, py, hp, name)
            } catch (e: Exception) {
                // Ignora erros de parsing individual
            }
        }
        
        // 3. HUD de Status (Stealth Indicator)
        if (isStealth) {
            paint.color = Color.YELLOW
            paint.textSize = 30f
            paint.style = Paint.Style.FILL
            canvas.drawText("STEALTH MODE ACTIVE | ${detectedPlayers.size} ENTITIES", 20f, height - 20f, paint