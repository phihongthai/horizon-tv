package app.horizontv.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    /** Apps pinned to the front of the row (in order) when installed. */
    private static final String[] FAVORITES = {
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.google.android.youtube.tvmusic",
            "com.spotify.tv.android",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.google.android.videos",
            "com.mxtech.videoplayer.ad",
            "org.xbmc.kodi",
            "com.plexapp.android",
    };

    private static final long WALLPAPER_INTERVAL_MS = 60000;
    /** After this long without a key press the app row fades out, leaving only the photo. */
    private static final long IDLE_MS = 12000;

    private static class Item {
        String label;
        String pkg;
        Intent intent;
        Drawable image;
        boolean banner;
    }

    private float density;
    private HorizontalScrollView scroll;
    private LinearLayout row;
    private TextView timeView;
    private TextView dateView;
    private TextView lunarView;
    private TextView creditView;
    private ImageView wallFront;
    private ImageView wallBack;
    private View scrim;
    private boolean nextToTop = true;
    private boolean idle = false;
    private boolean hasPhoto = false;
    private final Runnable goIdle = new Runnable() {
        @Override
        public void run() {
            idle = true;
            scroll.animate().alpha(0f).setDuration(1500).start();
            scrim.animate().alpha(hasPhoto ? 0.45f : 0f).setDuration(1500).start();
        }
    };
    private PhotoWall photoWall;
    private String signature = "";
    private int focusedIndex = 0;
    private final Handler handler = new Handler();
    private final SimpleDateFormat time24 = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat time12 = new SimpleDateFormat("h:mm", Locale.US);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH);

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            boolean h24 = android.text.format.DateFormat.is24HourFormat(MainActivity.this);
            timeView.setText((h24 ? time24 : time12).format(now));
            dateView.setText(dateFmt.format(now));
            java.util.Calendar c = java.util.Calendar.getInstance();
            lunarView.setText(LunarCalendar.describe(
                    c.get(java.util.Calendar.DAY_OF_MONTH),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.YEAR)));
            handler.postDelayed(this, 10000);
        }
    };

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    /** Soft drop shadow so text stays readable on any photo. */
    private void shadow(TextView t, float radiusDp, float dyDp, int color) {
        t.setShadowLayer(dp(radiusDp), 0, dp(dyDp), color);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF080C1E, 0xFF191343, 0xFF0A2B3E});
        root.setBackground(bg);

        // Wallpaper layers (two stacked images for cross-fading) plus a scrim for legibility
        wallBack = new ImageView(this);
        wallBack.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallBack.setAlpha(0f);
        wallFront = new ImageView(this);
        wallFront.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallFront.setAlpha(0f);
        root.addView(wallBack, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(wallFront, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim = new View(this);
        scrim.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3000000, 0x1A000000, 0xD9000000}));
        scrim.setAlpha(0f);
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Clock, top right
        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.END);
        timeView = new TextView(this);
        timeView.setTextColor(Color.WHITE);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76);
        timeView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timeView.setGravity(Gravity.END);
        timeView.setLetterSpacing(0.02f);
        shadow(timeView, 14, 3, 0x99000000);
        dateView = new TextView(this);
        dateView.setTextColor(0xE6FFFFFF);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        dateView.setGravity(Gravity.END);
        dateView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        shadow(dateView, 8, 2, 0x99000000);
        lunarView = new TextView(this);
        lunarView.setTextColor(0xFFF0CF7A);
        lunarView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        lunarView.setGravity(Gravity.END);
        shadow(lunarView, 8, 2, 0x99000000);
        clock.addView(timeView);
        clock.addView(dateView);
        clock.addView(lunarView);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        clp.topMargin = dp(32);
        clp.rightMargin = dp(56);
        root.addView(clock, clp);

        // Photo credit, top left
        creditView = new TextView(this);
        creditView.setTextColor(0xB3FFFFFF);
        creditView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        creditView.setSingleLine(true);
        creditView.setEllipsize(TextUtils.TruncateAt.END);
        creditView.setAlpha(0f);
        shadow(creditView, 3, 1, 0xCC000000);
        FrameLayout.LayoutParams crp = new FrameLayout.LayoutParams(
                dp(640), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        crp.topMargin = dp(40);
        crp.leftMargin = dp(56);
        root.addView(creditView, crp);

        // App row along the bottom edge
        scroll = new HorizontalScrollView(this) {
            @Override
            protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
                return 0; // centering is done manually with smoothScrollTo
            }
        };
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(64), dp(40), dp(64), dp(24));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        scroll.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        setContentView(root);
        tick.run();

    }

    private void resetIdle() {
        handler.removeCallbacks(goIdle);
        handler.postDelayed(goIdle, IDLE_MS);
    }

    private void wakeUi() {
        idle = false;
        scroll.animate().alpha(1f).setDuration(300).start();
        scrim.animate().alpha(hasPhoto ? 1f : 0f).setDuration(300).start();
        resetIdle();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (idle) {
            // The first key press only brings the app row back; it does not activate a tile
            if (e.getAction() == KeyEvent.ACTION_DOWN) wakeUi();
            return true;
        }
        resetIdle();
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        tick.run();
        idle = false;
        scroll.setAlpha(1f);
        resetIdle();
        if (photoWall == null) {
            photoWall = new PhotoWall(this, WALLPAPER_INTERVAL_MS, new PhotoWall.Listener() {
                @Override
                public void onPhoto(Bitmap bitmap, String credit) {
                    showWallpaper(bitmap, credit);
                }
            });
        }
        photoWall.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Item> items = loadApps();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populate(items);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(goIdle);
        if (photoWall != null) photoWall.stop();
    }

    /** Cross-fades to the new photo with a slow zoom. */
    private void showWallpaper(Bitmap bitmap, String credit) {
        creditView.setText(credit);
        creditView.animate().alpha(credit.length() > 0 ? 1f : 0f).setDuration(1800).start();
        // wallBack sits below wallFront (fixed z-order). The new photo either fades in on the top
        // layer, or the top layer fades out to reveal the new photo underneath.
        ImageView target = nextToTop ? wallFront : wallBack;
        target.animate().cancel();
        target.setImageBitmap(bitmap);
        target.setScaleX(1f);
        target.setScaleY(1f);
        target.animate().scaleX(1.08f).scaleY(1.08f)
                .setDuration(WALLPAPER_INTERVAL_MS + 3000)
                .setInterpolator(new android.view.animation.LinearInterpolator()).start();
        if (nextToTop) {
            wallFront.setAlpha(0f);
            wallFront.animate().alpha(1f).setDuration(1800).start();
        } else {
            wallBack.setAlpha(1f);
            wallFront.animate().cancel();
            wallFront.animate().alpha(0f).setDuration(1800).start();
        }
        nextToTop = !nextToTop;
        hasPhoto = true;
        scrim.animate().alpha(idle ? 0.45f : 1f).setDuration(1800).start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed: show the UI again and jump back to the first tile
        wakeUi();
        focusedIndex = 0;
        if (row.getChildCount() > 0) {
            scroll.smoothScrollTo(0, 0);
            View first = tileOf(row.getChildAt(0));
            if (first != null) first.requestFocus();
        }
    }

    @Override
    public void onBackPressed() {
        // A launcher never exits on Back
    }

    private List<Item> loadApps() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> infos = new ArrayList<ResolveInfo>();
        infos.addAll(pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        infos.addAll(pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));

        Set<String> seen = new HashSet<String>();
        List<Item> items = new ArrayList<Item>();
        for (ResolveInfo ri : infos) {
            ActivityInfo ai = ri.activityInfo;
            if (ai.packageName.equals(getPackageName()) || !seen.add(ai.packageName)) continue;
            try {
                Item it = new Item();
                it.label = String.valueOf(ri.loadLabel(pm));
                it.pkg = ai.packageName;
                it.intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(ai.packageName, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Drawable banner = ai.loadBanner(pm);
                it.banner = banner != null;
                it.image = banner != null ? banner : ri.loadIcon(pm);
                items.add(it);
            } catch (Exception ignored) {
            }
        }

        final List<String> fav = java.util.Arrays.asList(FAVORITES);
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                int fa = fav.indexOf(a.pkg);
                int fb = fav.indexOf(b.pkg);
                if (fa >= 0 || fb >= 0) {
                    if (fa < 0) return 1;
                    if (fb < 0) return -1;
                    return fa - fb;
                }
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        Item settings = new Item();
        settings.label = "Settings";
        settings.pkg = "settings";
        settings.intent = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settings.image = new GearDrawable();
        settings.banner = false;
        items.add(settings);
        return items;
    }

    private void populate(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) sb.append(it.pkg).append('|').append(it.label).append(';');
        String sig = sb.toString();
        if (sig.equals(signature)) return;
        signature = sig;

        row.removeAllViews();
        for (Item it : items) row.addView(makeCell(it));

        final int idx = Math.min(focusedIndex, row.getChildCount() - 1);
        row.post(new Runnable() {
            @Override
            public void run() {
                if (idx >= 0 && idx < row.getChildCount()) {
                    View t = tileOf(row.getChildAt(idx));
                    if (t != null) t.requestFocus();
                }
            }
        });
    }

    private View tileOf(View cell) {
        return ((ViewGroup) cell).getChildAt(0);
    }

    private View makeCell(final Item it) {
        final LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setClipChildren(false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                dp(168), ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.rightMargin = dp(24);
        cell.setLayoutParams(cp);

        final FrameLayout tile = new FrameLayout(this);
        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setCornerRadius(dp(14));
        tileBg.setColor(it.banner ? 0xFF1B1F33 : 0x2EFFFFFF);
        tile.setBackground(tileBg);
        tile.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        tile.setClipToOutline(true);
        tile.setElevation(dp(3));
        tile.setFocusable(true);
        tile.setClickable(true);

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(it.image);
        if (it.banner) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(iv, new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER));
        }
        cell.addView(tile, new LinearLayout.LayoutParams(dp(168), dp(95)));

        final TextView label = new TextView(this);
        label.setText(it.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setAlpha(0f);
        shadow(label, 6, 1, 0xCC000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        cell.addView(label, lp);

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                float s = has ? 1.14f : 1f;
                v.animate().scaleX(s).scaleY(s).translationZ(has ? dp(18) : 0)
                        .setDuration(170).start();
                label.animate().alpha(has ? 1f : 0f).setDuration(170).start();
                if (has) {
                    focusedIndex = row.indexOfChild(cell);
                    int target = row.getLeft() + cell.getLeft() + cell.getWidth() / 2 - scroll.getWidth() / 2;
                    scroll.smoothScrollTo(Math.max(0, target), 0);
                }
            }
        });
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(it.intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Can\u2019t open " + it.label,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        return cell;
    }

    /** Simple gear icon for the Settings tile. */
    private static class GearDrawable extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        GearDrawable() {
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            float w = b.width();
            float r = w * 0.26f;
            p.setStrokeWidth(w * 0.09f);
            c.drawCircle(cx, cy, r, p);
            p.setStrokeWidth(w * 0.11f);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                float x1 = cx + (float) Math.cos(a) * r * 1.35f;
                float y1 = cy + (float) Math.sin(a) * r * 1.35f;
                float x2 = cx + (float) Math.cos(a) * r * 1.7f;
                float y2 = cy + (float) Math.sin(a) * r * 1.7f;
                c.drawLine(x1, y1, x2, y2, p);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            p.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            p.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
