package uk.lgl.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 4102;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 42, 28, 28);
        page.setBackgroundColor(0xff0b1020);

        TextView title = new TextView(this);
        title.setText("AIMLAB • PAINEL FLUTUANTE");
        title.setTextColor(0xffffffff);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("\\nO painel fica sobre outros aplicativos. Os botões ESP/AIM são controles demonstrativos locais e não leem nem alteram jogos.");
        info.setTextColor(0xffb8c3dc);
        info.setTextSize(14);
        info.setPadding(0, 22, 0, 22);
        page.addView(info);

        Button enable = new Button(this);
        enable.setText("ATIVAR SOBREPOSIÇÃO");
        page.addView(enable, new LinearLayout.LayoutParams(-1, -2));
        enable.setOnClickListener(v -> enableOverlay());

        Button stop = new Button(this);
        stop.setText("FECHAR PAINEL FLUTUANTE");
        page.addView(stop, new LinearLayout.LayoutParams(-1, -2));
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));

        status = new TextView(this);
        status.setTextColor(0xff8ee6bd);
        status.setTextSize(13);
        status.setPadding(0, 20, 0, 0);
        page.addView(status);
        setContentView(page);
        updateStatus();
    }

    private void enableOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQUEST_OVERLAY);
            return;
        }
        startService(new Intent(this, OverlayService.class));
        status.setText("Painel flutuante iniciado.");
    }

    private void updateStatus() {
        boolean allowed = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
        status.setText(allowed ? "Permissão de sobreposição concedida." :
                "Permissão necessária: permita 'Sobrepor a outros apps'.");
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this))) {
            startService(new Intent(this, OverlayService.class));
            status.setText("Painel flutuante iniciado.");
        } else updateStatus();
    }
}
