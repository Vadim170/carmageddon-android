package com.dethrace.carmageddon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

/**
 * Entry point behind the "Carmageddon" and "Carmageddon Splat Pack" launcher icons
 * (activity-aliases carrying a game_id). Starts the game straight away; on the first
 * run it first copies the game data out of the APK behind the game's own loading screen.
 */
public class EntryActivity extends Activity {

    private String gameId;
    private LoadingView loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameId = readGameId();
        if (!GameData.needsExtraction(this)) {
            startGame();
            return;
        }
        loadingView = new LoadingView(this, "sp".equals(gameId) ? R.drawable.loading_sp : R.drawable.loading_c1);
        setContentView(loadingView);
        hideSystemBars();
        new Thread(() -> {
            try {
                GameData.extract(this, (done, total) -> loadingView.setProgress(total > 0 ? (float) done / total : 0));
                runOnUiThread(this::startGame);
            } catch (Exception e) {
                android.util.Log.e("dethrace", "Unpacking game data failed", e);
                runOnUiThread(this::finish);
            }
        }, "extract").start();
    }

    private String readGameId() {
        try {
            ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            if (info.metaData != null && info.metaData.getString("game_id") != null) {
                return info.metaData.getString("game_id");
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return "c1";
    }

    private void startGame() {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_GAME_ID, gameId);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private void hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /** The game's LOADSCRN.PIX shown 4:3 with sharp pixels, plus a thin progress bar. */
    private static final class LoadingView extends View {
        private final Bitmap screen;
        private final Paint pixelPaint = new Paint();
        private final Paint barPaint = new Paint();
        private final Rect dst = new Rect();
        private volatile float progress;

        LoadingView(Context ctx, int drawable) {
            super(ctx);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            screen = BitmapFactory.decodeResource(ctx.getResources(), drawable, o);
            pixelPaint.setFilterBitmap(false);
            setBackgroundColor(Color.BLACK);
        }

        void setProgress(float p) {
            if ((int) (p * 1000) != (int) (progress * 1000)) {
                progress = p;
                postInvalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            // 320x200 was displayed on 4:3 monitors
            int iw = Math.min(w, h * 4 / 3);
            int ih = iw * 3 / 4;
            dst.set((w - iw) / 2, (h - ih) / 2, (w + iw) / 2, (h + ih) / 2);
            canvas.drawBitmap(screen, null, dst, pixelPaint);

            // bar in the bottom margin under the picture's caption
            float px = iw / 320f;
            float left = dst.left + 40 * px, right = dst.right - 40 * px;
            float top = dst.bottom - 6 * px, bottom = dst.bottom - 3 * px;
            barPaint.setColor(0xFF3A0000);
            canvas.drawRect(left, top, right, bottom, barPaint);
            barPaint.setColor(0xFFD01010);
            canvas.drawRect(left, top, left + (right - left) * progress, bottom, barPaint);
        }
    }
}
