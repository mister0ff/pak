package uk.lgl.modmenu;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View panel;
    private WindowManager.LayoutParams params;
    private boolean espEnabled, aimEnabled;
    private LinearLayout controls;
    private TextView espState, aimState, configState;
    private SharedPreferences prefs;
    private ModuleConfig espConfig, aimConfig, overlayConfig;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("aimlab_local", MODE_PRIVATE);
        espConfig = ModuleConfig.load(this, "esp.yml");
        aimConfig = ModuleConfig.load(this, "aimbot.yml");
        overlayConfig = ModuleConfig.load(this, "overlay.yml");
        espEnabled = prefs.getBoolean("esp_enabled", false);
        aimEnabled = prefs.getBoolean("aim_enabled", false);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildPanel();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, int size) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(size);
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private void saveState() {
        prefs.edit().putBoolean("esp_enabled", espEnabled)
                .putBoolean("aim_enabled", aimEnabled).apply();
        refreshStates();
    }

    private void refreshStates() {
        if (espState != null) espState.setText("ESP local: " + (espEnabled ? "ATIVO" : "INATIVO"));
        if (aimState != null) aimState.setText("AIM local: " + (aimEnabled ? "ATIVO" : "INATIVO"));
        if (configState != null) {
            configState.setText("YAML: ESP " + (espConfig.getBoolean("enabled", true) ? "OK" : "OFF")
                    + " • AIM " + (aimConfig.getBoolean("enabled", true) ? "OK" : "OFF"));
        }
    }

    private void buildPanel() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(10), dp(8), dp(10), dp(10));
        shell.setBackgroundColor(0xee111a2e);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("AIMLAB  •  FLUTUANTE", 14);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button("×");
        close.setOnClickListener(v -> stopSelf());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(44)));
        shell.addView(header);

        TextView note = label("Módulos locais • cena de treino", 11);
        note.setTextColor(0xffaebbd3);
        shell.addView(note);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        espState = label("", 13);
        aimState = label("", 13);
        configState = label("", 10);
        Button esp = button("ESP • ATIVAR / DESATIVAR");
        esp.setOnClickListener(v -> { espEnabled = !espEnabled; saveState(); });
        Button aim = button("AIM • ATIVAR / DESATIVAR");
        aim.setOnClickListener(v -> { aimEnabled = !aimEnabled; saveState(); });
        controls.addView(esp); controls.addView(espState);
        controls.addView(aim); controls.addView(aimState);
        controls.addView(configState);
        shell.addView(controls);
        refreshStates();

        Button minimize = button("MINIMIZAR / EXPANDIR");
        minimize.setOnClickListener(v -> {
            boolean show = controls.getVisibility() != View.VISIBLE;
            controls.setVisibility(show ? View.VISIBLE : View.GONE);
            note.setText(show ? "Módulos locais • cena de treino" : "Painel minimizado • toque para expandir");
        });
        shell.addView(minimize);

        panel = shell;
        int width = overlayConfig.getInt("panel_width_dp", 270);
        params = new WindowManager.LayoutParams(
                dp(Math.max(220, Math.min(width, 360))), WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(overlayConfig.getInt("start_x_dp", 18));
        params.y = dp(overlayConfig.getInt("start_y_dp", 110));
        header.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY; private float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y;
                    touchX = e.getRawX(); touchY = e.getRawY(); return true;
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
        windowManager.addView(panel, params);
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
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
