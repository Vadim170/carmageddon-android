package com.dethrace.carmageddon;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Original game files are shipped as APK assets (assets/games/<id>/...) and
 * copied once to external app storage, where the engine reads them with plain
 * fopen() and writes save games next to them.
 */
final class GameData {

    interface Progress {
        void onProgress(long done, long total);
    }

    private static final String INDEX = "games/index.txt";

    private GameData() {
    }

    static File root(Context ctx) {
        File ext = ctx.getExternalFilesDir(null);
        return new File(ext != null ? ext : ctx.getFilesDir(), "games");
    }

    static File gameDir(Context ctx, String gameId) {
        return new File(root(ctx), gameId);
    }

    static boolean isInstalled(Context ctx, String gameId) {
        return new File(gameDir(ctx, gameId), "DATA/GENERAL.TXT").exists();
    }

    // Keyed by APK install time so a reinstalled APK re-copies its data
    private static File marker(Context ctx) {
        long installed;
        try {
            installed = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).lastUpdateTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            installed = 0;
        }
        return new File(root(ctx), ".extracted-" + installed);
    }

    /** True when the APK carries game data that has not been copied out yet. */
    static boolean needsExtraction(Context ctx) {
        return hasBundledData(ctx) && !marker(ctx).exists();
    }

    static boolean hasBundledData(Context ctx) {
        try (InputStream in = ctx.getAssets().open(INDEX)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static void extract(Context ctx, Progress progress) throws IOException {
        AssetManager assets = ctx.getAssets();
        List<String> paths = new ArrayList<>();
        long total = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(assets.open(INDEX)))) {
            String line;
            while ((line = r.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab <= 0) {
                    continue;
                }
                paths.add(line.substring(0, tab));
                total += Long.parseLong(line.substring(tab + 1));
            }
        }

        File root = root(ctx);
        byte[] buf = new byte[256 * 1024];
        long done = 0;
        for (String path : paths) {
            File out = new File(root, path);
            File parent = out.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Cannot create " + parent);
            }
            // Files not listed in the index (save games, options) are left untouched
            try (InputStream in = assets.open("games/" + path);
                 OutputStream os = new FileOutputStream(out)) {
                int n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    done += n;
                    progress.onProgress(done, total);
                }
            }
        }
        if (!marker(ctx).createNewFile() && !marker(ctx).exists()) {
            throw new IOException("Cannot write marker");
        }
    }
}
