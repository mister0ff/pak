package com.kindo.aimlab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class ESPOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isEspEnabled = true
    private var isStealth = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Simulação de jogadores: Lista de pontos na tela
    private val playerBoxes = mutableListOf<Point>()
    
    // Crosshair
    private val crosshairSize = 40f
    private val crosshairColor = Color.RED
    private val crosshairWidth = 3f

    init {
        // Configurações iniciais do Paint
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
    }

    fun setEspEnabled(enabled: Boolean) {
        isEspEnabled = enabled
        invalidate()
    }

    fun setStealthMode(enabled: Boolean) {
        isStealth = enabled
        // Em Stealth Mode, podemos mudar a cor ou reduzir a opacidade
        paint.alpha = if (enabled) 150 else 255
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isEspEnabled) return

        val cx = width / 2f
        val cy = height / 2f

        // 1. Desenhar Crosshair Central
        drawCrosshair(canvas, cx, cy)

        // 2. Desenhar Caixas de Jogadores (Simuladas ou Baseadas em Toque)
        if (isEspEnabled) {
            for (player in playerBoxes) {
                drawPlayerBox(canvas, player.x.toFloat(), player.y.toFloat())
            }
        }
        
        // 3. HUD de Status (Stealth Indicator)
        if (isStealth) {
            paint.color = Color.YELLOW
            paint.textSize = 30f
            paint.style = Paint.Style.FILL
            canvas.drawText("STEALTH MODE ACTIVE", 20f, height - 20f, paint)
        }
    }

    private fun drawCrosshair(canvas: Canvas, x: Float, y: Float) {
        paint.color = crosshairColor
        paint.strokeWidth = crosshairWidth
        paint.style = Paint.Style.STROKE

        // Linha Horizontal
        canvas.drawLine(x - crosshairSize, y, x + crosshairSize, y, paint)
        // Linha Vertical
        canvas.drawLine(x, y - crosshairSize, x, y + crosshairSize, paint)
        
        // Ponto Central
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 5f, paint)
        paint.style = Paint.Style.STROKE
    }

    private fun drawPlayerBox(canvas: Canvas, x: Float, y: Float) {
        // Cálculo da caixa (Head/Body Box)
        val boxWidth = 120f
        val boxHeight = 180f
        val headSize = 40f

        // Corpo
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(
            x - boxWidth / 2,
            y - boxHeight / 2,
            x + boxWidth / 2,
            y + boxHeight / 2,
            paint
        )

        // Head (Quadrado menor no topo)
        paint.color = Color.RED
        canvas.drawRect(
            x - headSize / 2,
            y - boxHeight / 2 - headSize,
            x + headSize / 2,
            y - boxHeight / 2,
            paint
        )
        
        // Nome/ID (Simulado)
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.style = Paint.Style.FILL
        canvas.drawText("ENEMY_${Random.nextInt(100)}", x - 40f, y - boxHeight / 2 - headSize - 10f, paint)
    }

    // Captura Toques na Tela para "Injetar" Marcadores ESP
    // Como o overlay é NOT_TOUCHABLE, precisamos usar um BroadcastReceiver 
    // ou um Accessibility Service para capturar toques reais.
    // Para este exemplo "rápido", vamos adicionar um método público 
    // que o FloatingService pode chamar se integrarmos um Accessibility Service,
    // OU vamos simular jogadores aleatórios para demonstrar o visual.
    
    fun addPlayerMarker(x: Float, y: Float) {
        playerBoxes.add(Point(x.toInt(), y.toInt()))
        // Limite de 10 marcadores para performance
        if (playerBoxes.size > 10) {
            playerBoxes.removeAt(0)
        }
        invalidate()
    }

    fun clearMarkers() {
        playerBoxes.clear()
        invalidate()
    }
    
    // Thread de Simulação para demonstrar o ESP sem precisar de memória real
    private val simulationThread = object : Thread() {
        override fun run() {
            while (isAlive) {
                try {
                    Thread.sleep(100)
                    // Adiciona um jogador aleatório a cada 2 segundos (simulação)
                    if (Random.nextBoolean()) {
                        val randomX = Random.nextInt(0, width).toFloat()
                        val randomY = Random.nextInt(0, height).toFloat()
                        post { addPlayerMarker(randomX, randomY) }
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        simulationThread.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        simulationThread.interrupt()
    }
}