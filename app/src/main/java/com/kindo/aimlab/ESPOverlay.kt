package com.kindo.aimlab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
    
    // Pincéis para diferentes elementos
    private val paintBox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val paintHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    
    private val paintCrosshair = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Crosshair Config
    private val crosshairSize = 50f

    // Handler para atualizar a UI em tempo real
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isEspEnabled && width > 0 && height > 0) {
                refreshFromNative()
                invalidate()
            }
            // 60 FPS update rate
            postDelayed(this, 16)
        }
    }

    // Cache de jogadores atuais (evita realocar objetos a cada frame)
    private val currentPlayers = mutableListOf<JSONObject>()

    init {
        // Garante que a view seja redimensionada para a tela cheia
        setWillNotDraw(false)
    }

    // ========================================================================
    // FUNÇÕES NATIVAS (JNI)
    // Estas funções chamam o código C++ em native.cpp
    // ========================================================================
    external fun nativeStart()
    external fun nativeStop()
    external fun nativeGetStatus(): String
    external fun nativeGetProjectedPlayers(screenWidth: Int, screenHeight: Int): Array<out Any?>?
    external fun nativeForceUpdate()

    companion object {
        init {
            try {
                System.loadLibrary("aimlab")
                android.util.Log.d("AimLab", "Native library 'aimlab' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("AimLab", "Failed to load native library: " + e.message)
            }
        }
    }

    // ========================================================================
    // MÉTODOS PÚBLICOS (Chamados pelo FloatingService)
    // ========================================================================

    fun setEspEnabled(enabled: Boolean) {
        if (isEspEnabled == enabled) return
        isEspEnabled = enabled
        
        if (enabled) {
            startEngine()
            post(updateRunnable)
        } else {
            stopEngine()
            removeCallbacks(updateRunnable)
            currentPlayers.clear()
            invalidate()
        }
    }

    fun setStealthMode(enabled: Boolean) {
        isStealth = enabled
        // Em Stealth Mode, reduzimos a opacidade e mudamos as cores para "neon"
        if (enabled) {
            paintBox.color = Color.CYAN
            paintHead.color = Color.MAGENTA
            paintText.color = Color.CYAN
            paintCrosshair.color = Color.CYAN
        } else {
            paintBox.color = Color.GREEN
            paintHead.color = Color.RED
            paintText.color = Color.WHITE
            paintCrosshair.color = Color.RED
        }
        invalidate()
    }

    private fun startEngine() {
        try {
            nativeStart()
            android.util.Log.d("AimLab", "ESP Engine Started via JNI.")
        } catch (e: Exception) {
            android.util.Log.e("AimLab", "Error starting native engine: " + e.message)
        }
    }

    private fun stopEngine() {
        try {
            nativeStop()
            android.util.Log.d("AimLab", "ESP Engine Stopped via JNI.")
        } catch (e: Exception) {
            android.util.Log.e("AimLab", "Error stopping native engine: " + e.message)
        }
    }

    // Busca dados atualizados da memória simulada/nativa
    private fun refreshFromNative() {
        try {
            // Chama o C++ para obter a lista de jogadores projetados na tela
            val result = nativeGetProjectedPlayers(width, height)
            
            currentPlayers.clear()
            if (result != null) {
                for (item in result) {
                    if (item is JSONObject) {
                        currentPlayers.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AimLab", "Error fetching players from native: " + e.message)
        }
    }

    // ========================================================================
    // RENDERIZAÇÃO
    // ========================================================================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isEspEnabled) return

        val cx = width / 2f
        val cy = height / 2f

        // 1. Desenhar Crosshair Central (Aim)
        drawCrosshair(canvas, cx, cy)

        // 2. Desenhar Caixas de Jogadores Detectados
        for (player in currentPlayers) {
            try {
                val px = player.getDouble("x").toFloat()
                val py = player.getDouble("y").toFloat()
                val hp = player.getInt("hp")
                val name = player.getString("name")
                
                drawPlayerBox(canvas, px, py, hp, name)
            } catch (e: Exception) {
                // Ignora erros de parsing de um jogador específico
            }
        }
        
        // 3. HUD de Status (Canto Inferior Esquerdo)
        drawHud(canvas)
    }

    private fun drawCrosshair(canvas: Canvas, x: Float, y: Float) {
        paintCrosshair.color = if (isStealth) Color.CYAN else Color.RED
        paintCrosshair.strokeWidth = 3f

        // Linha Horizontal
        canvas.drawLine(x - crosshairSize, y, x + crosshairSize, y, paintCrosshair)
        // Linha Vertical
        canvas.drawLine(x, y - crosshairSize, x, y + crosshairSize, paintCrosshair)
        
        // Ponto Central
        paintCrosshair.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 6f, paintCrosshair)
        paintCrosshair.style = Paint.Style.STROKE
    }

    private fun drawPlayerBox(canvas: Canvas, x: Float, y: Float, hp: Int, name: String) {
        // Dimensões da Caixa (Body Box)
        val boxWidth = 120f
        val boxHeight = 180f
        val headSize = 45f
        
        // Ajuste da cor com base na vida (Verde -> Amarelo -> Vermelho)
        val healthPercent = hp / 100f
        val healthColor = if (healthPercent > 0.6f) Color.GREEN 
                          else if (healthPercent > 0.3f) Color.YELLOW 
                          else Color.RED

        // 1. Corpo (Retângulo)
        paintBox.color = healthColor
        paintBox.style = Paint.Style.STROKE
        paintBox.strokeWidth = 4f
        canvas.drawRect(
            x - boxWidth / 2,
            y - boxHeight / 2,
            x + boxWidth / 2,
            y + boxHeight / 2,
            paintBox
        )

        // 2. Head (Quadrado no topo)
        paintHead.color = healthColor
        paintHead.style = Paint.Style.STROKE
        paintHead.strokeWidth = 4f
        canvas.drawRect(
            x - headSize / 2,
            y - boxHeight / 2 - headSize,
            x + headSize / 2,
            y - boxHeight / 2,
            paintHead
        )
        
        // 3. Barra de Vida (Opcional, desenhada abaixo do nome)
        paintBox.style = Paint.Style.FILL
        paintBox.color = Color.BLACK // Fundo
        canvas.drawRect(x - 50f, y - boxHeight / 2 - headSize - 40f, x + 50f, y - boxHeight / 2 - headSize - 25f, paintBox)
        
        paintBox.color = healthColor // Preenchimento
        val barWidth = 100f * healthPercent
        canvas.drawRect(x - 50f, y - boxHeight / 2 - headSize - 40f, x - 50f + barWidth, y - boxHeight / 2 - headSize - 25f, paintBox)

        // 4. Nome e ID
        paintText.color = if (isStealth) Color.CYAN else Color.WHITE
        paintText.textSize = 24f
        canvas.drawText(name, x, y - boxHeight / 2 - headSize - 50f, paintText)
        
        // 5. Distsance (Simulada baseada na posição Y da tela)
        val distance = (height - y) / 10
        paintText.textSize = 20f
        paintText.color = Color.GRAY
        canvas.drawText("${distance.toInt()}m", x, y + boxHeight / 2 + 30f, paintText)
    }

    private fun drawHud(canvas: Canvas) {
        // Texto de Status no canto inferior esquerdo
        paintText.color = if (isStealth) Color.CYAN else Color.GREEN
        paintText.textSize = 32f
        paintText.textAlign = Paint.Align.LEFT
        
        val statusText = if (isStealth) "AIMLAB | STEALTH" else "AIMLAB | ACTIVE"
        val playerCount = "${currentPlayers.size} ENTITIES"
        
        canvas.drawText(statusText, 20f, height - 60f, paintText)
        canvas.drawText(playerCount, 20f, height - 20f, paintText)
    }

    // ========================================================================
    // CICLO DE VIDA DA VIEW
    // ========================================================================

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Inicia o loop de atualização se o ESP estiver habilitado
        if (isEspEnabled) {
            startEngine()
            post(updateRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Para o motor nativo e limpa o handler
        stopEngine()
        removeCallbacks(updateRunnable)
    }
}