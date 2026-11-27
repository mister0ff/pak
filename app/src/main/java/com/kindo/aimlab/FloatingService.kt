package com.kindo.aimlab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Switch
import android.widget.Toast

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var menuView: View? = null
    private var espView: View? = null
    private lateinit var espOverlay: ESPOverlay
    
    private var isEspEnabled = true
    private var isStealthEnabled = false
    private var isMinimized = false
    private var isDragging = false

    private var espParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START" || intent == null) {
            if (menuView == null) {
                initEspLayer()
                initMenuLayer()
            }
        } else if (intent?.action == "STOP") {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeViews()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("AimLabChannel", "AimLab Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, "AimLabChannel")
            .setContentTitle("AimLab Active")
            .setContentText("Overlay is running. Use the floating menu to adjust settings.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initEspLayer() {
        if (espView != null) return

        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        espView = layoutInflater.inflate(R.layout.esp_overlay, null)
        espOverlay = espView!!.findViewById(R.id.espView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        espParams = params
        windowManager.addView(espView, params)
    }

    private fun initMenuLayer() {
        if (menuView != null) return

        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        menuView = layoutInflater.inflate(R.layout.floating_menu, null)

        val switchEsp = menuView!!.findViewById<Switch>(R.id.switchEsp)
        val switchStealth = menuView!!.findViewById<Switch>(R.id.switchStealth)
        val btnMinimize = menuView!!.findViewById<Button>(R.id.btnMinimize)
        val btnClose = menuView!!.findViewById<Button>(R.id.btnClose)

        // Configurações Iniciais
        switchEsp.isChecked = isEspEnabled
        switchStealth.isChecked = isStealthEnabled

        switchEsp.setOnCheckedChangeListener { _, isChecked ->
            isEspEnabled = isChecked
            espOverlay.setEspEnabled(isChecked)
            Toast.makeText(this, "ESP ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchStealth.setOnCheckedChangeListener { _, isChecked ->
            isStealthEnabled = isChecked
            espOverlay.setStealthMode(isChecked)
            Toast.makeText(this, "Stealth Mode ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        btnMinimize.setOnClickListener {
            isMinimized = !isMinimized
            updateMenuState()
        }

        btnClose.setOnClickListener {
            stopSelf()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        // Posição inicial: canto superior direito
        params.x = resources.displayMetrics.widthPixels - 200
        params.y = 100

        menuParams = params

        // Lógica de Arrastar (Drag)
        menuView!!.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    menuParams!!.x = event.rawX.toInt() - v.left
                    menuParams!!.y = event.rawY.toInt() - v.top
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        menuParams!!.x = event.rawX.toInt() - v.left
                        menuParams!!.y = event.rawY.toInt() - v.top
                        windowManager.updateViewLayout(v, menuParams)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        windowManager.addView(menuView, params)
    }

    private fun updateMenuState() {
        if (menuView == null) return
        if (isMinimized) {
            menuView!!.visibility = View.INVISIBLE
        } else {
            menuView!!.visibility = View.VISIBLE
        }
        windowManager.updateViewLayout(menuView, menuParams)
    }

    private fun removeViews() {
        if (menuView != null) {
            windowManager.removeView(menuView)
            menuView = null
        }
        if (espView != null) {
            windowManager.removeView(espView)
            espView = null
        }
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
    }
}