package app.horizontv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;


import java.io.File;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Random landscape wallpapers. The photo list is bundled with the app (res/raw/photos.tsv:
 * Wikimedia Commons featured landscape pictures, one "url TAB credit" per line). Photos are
 * downloaded one at a time, cached on the device, and the photographer is credited on screen.
 */
final class PhotoWall {

    interface Listener {
        void onPhoto(Bitmap bitmap, String credit);
    }

    /** Wikimedia asks clients to identify themselves. Put your project URL here. */
    private static final String UA = "HorizonTV/1.0 (Android TV launcher; wallpaper fetcher)";
    private static final int MAX_CACHE_FILES = 40;

    private final Context ctx;
    private final long intervalMs;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Each entry: thumbnail URL + TAB + credit text. */
    private final List<String> entries = new ArrayList<String>();
    private final File cacheDir;

    private HandlerThread thread;
    private Handler bg;
    private int cursor = 0;

    PhotoWall(Context ctx, long intervalMs, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.intervalMs = intervalMs;
        this.listener = listener;
        this.cacheDir = new File(this.ctx.getCacheDir(), "wall");
    }

    synchronized void start() {
        if (thread != null) return;
        cacheDir.mkdirs();
        entries.clear();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    ctx.getResources().openRawResource(R.raw.photos), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.indexOf('\t') > 0) entries.add(line);
            }
            r.close();
        } catch (Exception ignored) {
        }
        Collections.shuffle(entries);
        cursor = 0;
        thread = new HandlerThread("wall");
        thread.start();
        bg = new Handler(thread.getLooper());
        bg.post(step);
    }

    synchronized void stop() {
        if (thread == null) return;
        bg.removeCallbacksAndMessages(null);
        thread.quit();
        thread = null;
        bg = null;
    }

    private final Runnable step = new Runnable() {
        @Override
        public void run() {
            long delay = intervalMs;
            try {
                final String[] pick = next();
                if (pick != null) {
                    final Bitmap bmp = BitmapFactory.decodeFile(pick[0]);
                    final String credit = pick[1];
                    if (bmp != null) {
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onPhoto(bmp, credit);
                            }
                        });
                    }
                } else {
                    delay = 20000; // offline and nothing cached yet: retry soon
                }
            } catch (Throwable ignored) {
                delay = 20000;
            }
            Handler h = bg;
            if (h != null) h.postDelayed(this, delay);
        }
    };

    /** Returns {local file path, credit} for the next photo, downloading it if needed. */
    private String[] next() {
        if (!entries.isEmpty()) {
            for (int tries = 0; tries < 5; tries++) {
                if (cursor >= entries.size()) {
                    cursor = 0;
                    Collections.shuffle(entries);
                }
                String[] e = entries.get(cursor++).split("\t", 2);
                File f = fileFor(e[0]);
                if (!f.exists()) {
                    try {
                        download(e[0], f);
                    } catch (Exception ex) {
                        f.delete();
                        continue;
                    }
                    prune();
                }
                return new String[]{f.getAbsolutePath(), e.length > 1 ? e[1] : ""};
            }
            // Offline: fall back to any entry that is already cached.
            List<String> copy = new ArrayList<String>(entries);
            Collections.shuffle(copy);
            for (String s : copy) {
                String[] e = s.split("\t", 2);
                File f = fileFor(e[0]);
                if (f.exists()) return new String[]{f.getAbsolutePath(), e.length > 1 ? e[1] : ""};
            }
        }
        return null;
    }

    private File fileFor(String url) {
        return new File(cacheDir, Integer.toHexString(url.hashCode()) + ".jpg");
    }

    private void prune() {
        File[] files = cacheDir.listFiles();
        if (files == null || files.length <= MAX_CACHE_FILES) return;
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(a.lastModified(), b.lastModified());
            }
        });
        for (int i = 0; i < files.length - MAX_CACHE_FILES; i++) files[i].delete();
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(40000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        return c;
    }

    private static void download(String url, File dest) throws Exception {
        HttpURLConnection c = open(url);
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream();
            File tmp = new File(dest.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            byte[] buf = new byte[16384];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
                if (total > 8 * 1024 * 1024) throw new Exception("too big");
            }
            out.close();
            in.close();
            if (!tmp.renameTo(dest)) throw new Exception("rename failed");
        } finally {
            c.disconnect();
        }
    }
}
