package uk.lgl.modmenu;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View panel;
    private WindowManager.LayoutParams params;
    private boolean espEnabled, aimEnabled, boxesEnabled, linesEnabled, distEnabled, healthEnabled;
    private LinearLayout controls;
    private TextView espState, aimState, configState, detailState;
    private SharedPreferences prefs;
    private ModuleConfig espConfig, aimConfig, overlayConfig;
    private Handler handler = new Handler(Looper.getMainLooper());
    private EspDrawView drawView;
    private WindowManager.LayoutParams drawParams;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("aimlab_local", MODE_PRIVATE);
        espConfig = ModuleConfig.load(this, "esp.yml");
        aimConfig = ModuleConfig.load(this, "aimbot.yml");
        overlayConfig = ModuleConfig.load(this, "overlay.yml");
        espEnabled = prefs.getBoolean("esp_enabled", false);
        aimEnabled = prefs.getBoolean("aim_enabled", false);
        boxesEnabled = prefs.getBoolean("boxes_enabled", espConfig.getBoolean("draw_boxes", true));
        linesEnabled = prefs.getBoolean("lines_enabled", espConfig.getBoolean("draw_snaplines", true));
        distEnabled = prefs.getBoolean("dist_enabled", espConfig.getBoolean("show_distance", true));
        healthEnabled = prefs.getBoolean("health_enabled", espConfig.getBoolean("show_health", true));
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildPanel();
        buildDrawLayer();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, int size) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setPadding(dp(8), dp(5), dp(8), dp(5));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        return b;
    }

    private void saveState() {
        prefs.edit()
                .putBoolean("esp_enabled", espEnabled)
                .putBoolean("aim_enabled", aimEnabled)
                .putBoolean("boxes_enabled", boxesEnabled)
                .putBoolean("lines_enabled", linesEnabled)
                .putBoolean("dist_enabled", distEnabled)
                .putBoolean("health_enabled", healthEnabled)
                .apply();
        refreshStates();
        if (drawView != null) drawView.invalidate();
    }

    private void refreshStates() {
        if (espState != null) espState.setText("ESP: " + (espEnabled ? "● ATIVO" : "○ INATIVO"));
        if (aimState != null) aimState.setText("AIM: " + (aimEnabled ? "● ATIVO" : "○ INATIVO"));
        if (configState != null) {
            configState.setText("YAML: ESP " + (espConfig.getBoolean("enabled", true) ? "OK" : "OFF")
                    + " • AIM " + (aimConfig.getBoolean("enabled", true) ? "OK" : "OFF"));
        }
        if (detailState != null) {
            detailState.setText("Box:" + (boxesEnabled?"ON":"OFF")
                    + "  Line:" + (linesEnabled?"ON":"OFF")
                    + "  Dist:" + (distEnabled?"ON":"OFF")
                    + "  HP:" + (healthEnabled?"ON":"OFF"));
        }
    }

    private void buildDrawLayer() {
        drawView = new EspDrawView(this);
        drawParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        drawParams.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(drawView, drawParams);
        } catch (Exception ignored) {}
    }

    private void buildPanel() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(8), dp(6), dp(8), dp(8));
        shell.setBackgroundColor(0xee0d1526);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("AIMLAB  •  ESP PANEL", 13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xff53e0d0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button("×");
        close.setOnClickListener(v -> stopSelf());
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(38)));
        shell.addView(header);

        TextView note = label("Módulos locais • cena de treino + overlay", 10);
        note.setTextColor(0xff8aa0c0);
        shell.addView(note);

        ScrollView scroll = new ScrollView(this);
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);

        espState = label("", 12);
        aimState = label("", 12);
        configState = label("", 10);
        detailState = label("", 10);
        detailState.setTextColor(0xffa0b8d8);

        Button esp = button("ESP  •  LIGAR / DESLIGAR");
        esp.setOnClickListener(v -> { espEnabled = !espEnabled; saveState(); });

        Button boxes = button("CAIXAS  •  TOGGLE");
        boxes.setOnClickListener(v -> { boxesEnabled = !boxesEnabled; saveState(); });

        Button lines = button("LINHAS  •  TOGGLE");
        lines.setOnClickListener(v -> { linesEnabled = !linesEnabled; saveState(); });

        Button dist = button("DISTÂNCIA  •  TOGGLE");
        dist.setOnClickListener(v -> { distEnabled = !distEnabled; saveState(); });

        Button health = button("VIDA / HP  •  TOGGLE");
        health.setOnClickListener(v -> { healthEnabled = !healthEnabled; saveState(); });

        Button aim = button("AIM  •  LIGAR / DESLIGAR");
        aim.setOnClickListener(v -> { aimEnabled = !aimEnabled; saveState(); });

        controls.addView(esp);
        controls.addView(espState);
        controls.addView(boxes);
        controls.addView(lines);
        controls.addView(dist);
        controls.addView(health);
        controls.addView(detailState);
        controls.addView(aim);
        controls.addView(aimState);
        controls.addView(configState);

        scroll.addView(controls);
        shell.addView(scroll);

        Button minimize = button("MINIMIZAR / EXPANDIR");
        minimize.setOnClickListener(v -> {
            boolean show = controls.getVisibility() != View.VISIBLE;
            controls.setVisibility(show ? View.VISIBLE : View.GONE);
            note.setText(show ? "Módulos locais • cena de treino + overlay" : "Painel minimizado");
        });
        shell.addView(minimize);

        panel = shell;
        int width = overlayConfig.getInt("panel_width_dp", 280);
        params = new WindowManager.LayoutParams(
                dp(Math.max(220, Math.min(width, 360))),
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(overlayConfig.getInt("start_x_dp", 18));
        params.y = dp(overlayConfig.getInt("start_y_dp", 110));

        header.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    touchX = e.getRawX();
                    touchY = e.getRawY();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int)(e.getRawX() - touchX);
                    params.y = startY + (int)(e.getRawY() - touchY);
                    try { windowManager.updateViewLayout(panel, params); } catch (Exception ignored) {}
                    return true;
                }
                return e.getAction() == MotionEvent.ACTION_UP;
            }
        });

        try {
            windowManager.addView(panel, params);
        } catch (Exception ignored) {}
        refreshStates();
    }

    // Simple draw layer for visual feedback (local simulation markers)
    private class EspDrawView extends View {
        private Paint boxPaint, linePaint, textPaint, fillPaint;
        private float[][] demoTargets = {
                {0.25f, 0.35f, 1.0f},
                {0.70f, 0.40f, 0.7f},
                {0.45f, 0.60f, 0.9f},
                {0.15f, 0.70f, 0.5f},
                {0.80f, 0.75f, 0.8f}
        };

        public EspDrawView(android.content.Context ctx) {
            super(ctx);
            boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(dp(2));
            boxPaint.setColor(0xff53e0d0);

            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dp(1.5f));
            linePaint.setColor(0xaa53e0d0);

            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(0x2853e0d0);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(dp(11));
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!espEnabled) return;

            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h;

            for (int i = 0; i < demoTargets.length; i++) {
                float tx = demoTargets[i][0] * w;
                float ty = demoTargets[i][1] * h;
                float hp = demoTargets[i][2];
                float boxW = dp(38);
                float boxH = dp(58);

                if (boxesEnabled) {
                    canvas.drawRect(tx - boxW/2, ty - boxH/2, tx + boxW/2, ty + boxH/2, fillPaint);
                    canvas.drawRect(tx - boxW/2, ty - boxH/2, tx + boxW/2, ty + boxH/2, boxPaint);
                    // corner style extra
                    float cl = dp(8);
                    canvas.drawLine(tx - boxW/2, ty - boxH/2, tx - boxW/2 + cl, ty - boxH/2, boxPaint);
                    canvas.drawLine(tx - boxW/2, ty - boxH/2, tx - boxW/2, ty - boxH/2 + cl, boxPaint);
                    canvas.drawLine(tx + boxW/2, ty - boxH/2, tx + boxW/2 - cl, ty - boxH/2, boxPaint);
                    canvas.drawLine(tx + boxW/2, ty - boxH/2, tx + boxW/2, ty - boxH/2 + cl, boxPaint);
                }

                if (linesEnabled) {
                    canvas.drawLine(cx, cy, tx, ty + boxH/2, linePaint);
                }

                if (distEnabled) {
                    int dist = (int)(20 + Math.hypot(tx - cx, ty - cy) / 12);
                    canvas.drawText(dist + "m", tx - dp(12), ty + boxH/2 + dp(14), textPaint);
                }

                if (healthEnabled) {
                    float barW = boxW;
                    float barH = dp(4);
                    float barY = ty - boxH/2 - dp(8);
                    Paint bg = new Paint();
                    bg.setColor(0x66000000);
                    canvas.drawRect(tx - barW/2, barY, tx + barW/2, barY + barH, bg);
                    Paint hpPaint = new Paint();
                    hpPaint.setColor(hp > 0.5f ? 0xff44ff88 : 0xffff5555);
                    canvas.drawRect(tx - barW/2, barY, tx - barW/2 + barW * hp, barY + barH, hpPaint);
                }

                canvas.drawText("T" + (i+1), tx - dp(8), ty - boxH/2 - dp(12), textPaint);
            }
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (panel == null) buildPanel();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (panel != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
        if (drawView != null && windowManager != null) {
            try { windowManager.removeView(drawView); } catch (Exception ignored) {}
            drawView = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
