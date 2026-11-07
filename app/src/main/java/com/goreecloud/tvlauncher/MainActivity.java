package com.goreecloud.tvlauncher;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Privacy-first, D-pad-native TV Home.
 * The media-like layout is original. Only installed app icons and backgrounds
 * explicitly chosen through the system document picker appear in the UI.
 * No network, tracking, overlays, accessibility services, or privileged APIs.
 */
public final class MainActivity extends Activity {
    private static final int PICK_BACKGROUND = 801;
    private static final int REQUEST_HOME = 700;
    private static final String FAVORITES = "favorites";
    private static final String BACKGROUND_URI = "background_uri";

    private static final int FOREGROUND = Color.rgb(246, 249, 252);
    private static final int MUTED = Color.rgb(188, 205, 218);
    private static final int GLASS = 0xD5212A38;
    private static final int TILE = 0xAA1A2738;
    private static final int FOCUS = Color.rgb(249, 253, 255);
    private static final int ACCENT = Color.rgb(132, 224, 214);
    private static final int BORDER = 0x77D5E9EF;

    private final List<AppEntry> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private ImageView backgroundImage;
    private LinearLayout dockRow;
    private LinearLayout allAppsRows;
    private EditText search;
    private TextView clock;
    private TextView featuredApp;
    private TextView dockHeading;
    private TextView subtitle;
    private TextView countLabel;
    private View initialTile;
    private View wallpaperAction;
    private boolean receiverRegistered;

    private final BroadcastReceiver clockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateClock();
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        favorites.addAll(prefs().getStringSet(FAVORITES, new HashSet<>()));
        createScreen();
        refreshApps();
        applyBackground();
        if (initialTile != null) initialTile.requestFocus();
        else if (wallpaperAction != null) wallpaperAction.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dockRow != null) refreshApps();
        updateClock();
        if (!receiverRegistered) {
            registerReceiver(clockReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
            receiverRegistered = true;
        }
    }

    @Override protected void onPause() {
        if (receiverRegistered) {
            unregisterReceiver(clockReceiver);
            receiverRegistered = false;
        }
        super.onPause();
    }

    private SharedPreferences prefs() {
        return getPreferences(Context.MODE_PRIVATE);
    }

    private void createScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0A1725);
        backgroundImage = new ImageView(this);
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImage.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF1B4853, 0xFF15273D, 0xFF090D1B}));
        root.addView(backgroundImage, new FrameLayout.LayoutParams(-1, -1));

        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0x88050914, 0x22060C17, 0x9C080C13, 0xED070C13}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(48), dp(30), dp(48), dp(50));
        scroll.addView(content);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(top, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView brand = text("G O R E E C L O U D   /   T V", 17, FOREGROUND, Typeface.BOLD);
        brand.setLetterSpacing(0.07f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        clock = text("", 15, FOREGROUND, Typeface.BOLD);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(dp(20), dp(9), dp(20), dp(9));
        clock.setBackground(surface(0xB21D293A, dp(28), BORDER, 1));
        top.addView(clock);
        updateClock();

        FrameLayout hero = new FrameLayout(this);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, dp(190));
        heroLp.topMargin = dp(14);
        content.addView(hero, heroLp);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams copyLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
        copyLp.bottomMargin = dp(22);
        hero.addView(copy, copyLp);
        TextView kicker = text("YOUR SPACE. YOUR SCREEN.", 14, ACCENT, Typeface.BOLD);
        kicker.setLetterSpacing(0.16f);
        copy.addView(kicker);
        featuredApp = text("Everything in its place.", 34, FOREGROUND, Typeface.BOLD);
        featuredApp.setMaxLines(1);
        featuredApp.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams featureLp = new LinearLayout.LayoutParams(-1, -2);
        featureLp.topMargin = dp(8);
        copy.addView(featuredApp, featureLp);
        subtitle = text("Press OK to open  •  Hold OK or Menu to pin", 16, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(8);
        copy.addView(subtitle, subLp);

        LinearLayout dockPanel = new LinearLayout(this);
        dockPanel.setOrientation(LinearLayout.VERTICAL);
        dockPanel.setPadding(dp(15), dp(14), dp(15), dp(14));
        dockPanel.setBackground(surface(GLASS, dp(28), 0x668DA7B2, 1));
        dockPanel.setClipChildren(false);
        dockPanel.setClipToPadding(false);
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(-1, -2);
        content.addView(dockPanel, dockLp);
        LinearLayout dockTitleRow = new LinearLayout(this);
        dockTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        dockPanel.addView(dockTitleRow);
        dockHeading = text("QUICK LAUNCH", 12, MUTED, Typeface.BOLD);
        dockHeading.setLetterSpacing(0.14f);
        dockTitleRow.addView(dockHeading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dockHint = text("Hold OK / Menu to customize", 12, MUTED, Typeface.NORMAL);
        dockTitleRow.addView(dockHint);
        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        LinearLayout.LayoutParams dockRowLp = new LinearLayout.LayoutParams(-1, -2);
        dockRowLp.topMargin = dp(9);
        dockPanel.addView(dockRow, dockRowLp);

        LinearLayout libraryHeading = new LinearLayout(this);
        libraryHeading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, dp(48));
        headingLp.topMargin = dp(22);
        content.addView(libraryHeading, headingLp);
        TextView allAppsTitle = text("ALL APPS", 21, FOREGROUND, Typeface.BOLD);
        allAppsTitle.setLetterSpacing(0.06f);
        libraryHeading.addView(allAppsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        countLabel = text("", 14, MUTED, Typeface.NORMAL);
        libraryHeading.addView(countLabel);

        LinearLayout utilities = new LinearLayout(this);
        utilities.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams utilitiesLp = new LinearLayout.LayoutParams(-1, dp(56));
        utilitiesLp.topMargin = dp(4);
        content.addView(utilities, utilitiesLp);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setTextColor(FOREGROUND);
        search.setHintTextColor(MUTED);
        search.setHint("Find an installed app");
        search.setContentDescription("Search installed apps");
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(surface(0xCC1C2939, dp(16), BORDER, 1));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, dp(52), 1);
        searchLp.rightMargin = dp(10);
        utilities.addView(search, searchLp);
        focusDecoration(search, true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApps(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        Button backdropButton = action("Change backdrop");
        wallpaperAction = backdropButton;
        utilities.addView(backdropButton, new LinearLayout.LayoutParams(dp(174), dp(52)));
        backdropButton.setOnClickListener(v -> chooseBackground());
        Button clearButton = action("Reset");
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(dp(94), dp(52));
        clearLp.leftMargin = dp(9);
        utilities.addView(clearButton, clearLp);
        clearButton.setContentDescription("Reset selected backdrop");
        clearButton.setOnClickListener(v -> {
            String old = prefs().getString(BACKGROUND_URI, null);
            prefs().edit().remove(BACKGROUND_URI).apply();
            if (old != null) releaseBackgroundAccess(old);
            applyBackground();
            toast("Backdrop selection reset");
        });

        allAppsRows = new LinearLayout(this);
        allAppsRows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -2);
        gridLp.topMargin = dp(10);
        content.addView(allAppsRows, gridLp);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, dp(76));
        footerLp.topMargin = dp(15);
        content.addView(footer, footerLp);
        TextView footerHint = text("LOCAL BY DESIGN  •  NO ADS OR TRACKING", 13, MUTED, Typeface.BOLD);
        footer.addView(footerHint, new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = action("TV settings");
        footer.addView(settings, new LinearLayout.LayoutParams(dp(140), dp(48)));
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) {
                toast("TV settings are unavailable on this device");
            }
        });
        Button home = action("Choose Home");
        LinearLayout.LayoutParams homeLp = new LinearLayout.LayoutParams(dp(156), dp(48));
        homeLp.leftMargin = dp(10);
        footer.addView(home, homeLp);
        home.setOnClickListener(v -> requestHomeRole());
    }

    private void refreshApps() {
        if (dockRow == null) return;
        View active = getCurrentFocus();
        String key = active == null ? null : active.getTag() instanceof String
            ? (String) active.getTag() : null;
        LinkedHashMap<String, AppEntry> discovered = new LinkedHashMap<>();
        collect(discovered, Intent.CATEGORY_LEANBACK_LAUNCHER);
        collect(discovered, Intent.CATEGORY_LAUNCHER);
        apps.clear();
        apps.addAll(discovered.values());
        Collections.sort(apps, (a, b) -> a.label.toLowerCase(Locale.ROOT)
            .compareTo(b.label.toLowerCase(Locale.ROOT)));
        renderApps(key);
    }

    private void collect(LinkedHashMap<String, AppEntry> found, String category) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> matches = pm.queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(category), 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo == null || info.activityInfo.packageName == null
                || info.activityInfo.name == null
                || getPackageName().equals(info.activityInfo.packageName)) continue;
            ComponentName component = new ComponentName(
                info.activityInfo.packageName, info.activityInfo.name);
            String key = component.flattenToString();
            if (found.containsKey(key)) continue;
            CharSequence title = info.loadLabel(pm);
            found.put(key, new AppEntry(component,
                title == null ? info.activityInfo.packageName : title.toString(), category));
        }
    }

    private void renderApps(String restoreKey) {
        if (dockRow == null || allAppsRows == null) return;
        initialTile = null;
        dockRow.removeAllViews();
        allAppsRows.removeAllViews();
        List<AppEntry> pinned = new ArrayList<>();
        for (AppEntry app : apps) if (favorites.contains(app.key())) pinned.add(app);
        boolean usesSuggestions = pinned.isEmpty();
        if (dockHeading != null) dockHeading.setText(usesSuggestions ? "QUICK LAUNCH" : "FAVORITES");
        List<AppEntry> dockApps = usesSuggestions ? apps : pinned;
        int slots = Math.min(6, dockApps.size());
        for (int i = 0; i < slots; i++) {
            AppEntry app = dockApps.get(i);
            LinearLayout.LayoutParams tileLp = new LinearLayout.LayoutParams(0, dp(132), 1);
            if (i > 0) tileLp.leftMargin = dp(10);
            View tile = tile(app, true);
            dockRow.addView(tile, tileLp);
            if (initialTile == null) initialTile = tile;
        }
        if (slots == 0) {
            TextView empty = text("No installed TV apps found. Check the app list in TV settings.",
                16, MUTED, Typeface.NORMAL);
            empty.setPadding(dp(18), dp(25), dp(18), dp(25));
            dockRow.addView(empty);
        }
        countLabel.setText(getString(R.string.app_count, apps.size(), pinned.size()));

        String query = search == null ? "" : search.getText().toString();
        List<AppEntry> filtered = new ArrayList<>();
        for (AppEntry app : apps) if (AppFilter.matches(app.label, query)) filtered.add(app);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidthDp = Math.round(metrics.widthPixels / metrics.density);
        int columns = Math.max(3, Math.min(6, (screenWidthDp - 96) / 172));
        for (int start = 0; start < filtered.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            for (int j = 0; j < columns; j++) {
                int index = start + j;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(130), 1);
                lp.setMargins(0, dp(6), dp(11), dp(6));
                row.addView(index < filtered.size() ? tile(filtered.get(index), false)
                    : new View(this), lp);
            }
            allAppsRows.addView(row);
        }
        if (filtered.isEmpty()) {
            TextView empty = text("No matching apps found", 16, MUTED, Typeface.NORMAL);
            empty.setPadding(dp(16), dp(25), 0, dp(25));
            allAppsRows.addView(empty);
        }
        if (restoreKey != null) restoreTileFocus(restoreKey);
    }

    private View tile(AppEntry app, boolean dock) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(9), dp(11), dp(9), dp(10));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setLongClickable(true);
        tile.setTag(app.key());
        tile.setBackground(surface(dock ? 0x552D3440 : TILE, dp(20), dock ? 0x338FA9B5 : BORDER, 1));
        tile.setContentDescription(app.label
            + (favorites.contains(app.key()) ? ", pinned. " : ". ")
            + "Press OK to open; hold OK or Menu to pin or unpin.");

        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(getPackageManager().getActivityIcon(app.component)); }
        catch (PackageManager.NameNotFoundException | RuntimeException e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(dock ? 74 : 64), dp(dock ? 74 : 64)));

        TextView name = text(app.label, dock ? 14 : 13, FOREGROUND, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-1, -2);
        nameLp.topMargin = dp(8);
        tile.addView(name, nameLp);

        focusDecoration(tile, false);
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(surface(hasFocus ? 0xCCF0F8FC
                : dock ? 0x552D3440 : TILE, dp(20),
                hasFocus ? FOCUS : dock ? 0x338FA9B5 : BORDER, hasFocus ? 3 : 1));
            name.setTextColor(hasFocus ? 0xFF172532 : FOREGROUND);
            v.setScaleX(hasFocus ? 1.06f : 1f);
            v.setScaleY(hasFocus ? 1.06f : 1f);
            if (hasFocus && featuredApp != null) {
                featuredApp.setText(app.label);
                subtitle.setText(favorites.contains(app.key())
                    ? R.string.app_hint_unpin : R.string.app_hint_pin);
            }
        });
        tile.setOnClickListener(v -> launch(app));
        tile.setOnLongClickListener(v -> {
            toggleFavorite(app);
            return true;
        });
        tile.setOnKeyListener((v, code, event) -> {
            if (code != KeyEvent.KEYCODE_MENU) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) toggleFavorite(app);
            return true;
        });
        return tile;
    }

    private void toggleFavorite(AppEntry app) {
        if (!favorites.add(app.key())) favorites.remove(app.key());
        prefs().edit().putStringSet(FAVORITES, new HashSet<>(favorites)).apply();
        renderApps(app.key());
        toast(app.label + (favorites.contains(app.key()) ? " pinned" : " unpinned"));
    }

    private void restoreTileFocus(String key) {
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View item = dockRow.getChildAt(i);
            if (key.equals(item.getTag()) && item.requestFocus()) return;
        }
        for (int r = 0; r < allAppsRows.getChildCount(); r++) {
            View row = allAppsRows.getChildAt(r);
            if (!(row instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) row;
            for (int j = 0; j < group.getChildCount(); j++) {
                View candidate = group.getChildAt(j);
                if (key.equals(candidate.getTag()) && candidate.requestFocus()) return;
            }
        }
    }

    private void launch(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(app.category);
        intent.setComponent(app.component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            toast("Cannot open " + app.label + ". It may have been uninstalled.");
            refreshApps();
        }
    }

    private void chooseBackground() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, PICK_BACKGROUND); }
        catch (ActivityNotFoundException | SecurityException e) {
            toast("No compatible image picker. Default backdrop remains available.");
        }
    }

    private void applyBackground() {
        if (backgroundImage == null) return;
        String saved = prefs().getString(BACKGROUND_URI, null);
        // The ADB-imported private file is a development-only fallback when the
        // TV does not provide a real ACTION_OPEN_DOCUMENT picker.
        File localImage = new File(getFilesDir(), "backdrop.jpg");
        if (saved == null && !localImage.isFile()) {
            backgroundImage.setImageDrawable(null);
            return;
        }
        try {
            Uri uri = saved == null ? Uri.fromFile(localImage) : Uri.parse(saved);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalStateException("image not found");
                BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth < 1 || bounds.outHeight < 1) throw new IllegalStateException("invalid image");
            int sample = 1;
            while (bounds.outWidth / sample > 2560 || bounds.outHeight / sample > 1440)
                sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalStateException("image not found");
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) throw new IllegalStateException("unreadable image");
            backgroundImage.setImageBitmap(bitmap);
        } catch (Exception | OutOfMemoryError ex) {
            backgroundImage.setImageDrawable(null);
            if (saved != null) prefs().edit().remove(BACKGROUND_URI).apply();
            toast("Backdrop unavailable. Using the built-in background.");
        }
    }

    private void releaseBackgroundAccess(String saved) {
        try {
            getContentResolver().releasePersistableUriPermission(
                Uri.parse(saved), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException | IllegalArgumentException ignored) {
            // The provider may have revoked access or been uninstalled.
        }
    }

    private void requestHomeRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    toast("Already the default Home app");
                    return;
                }
                try {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME);
                    return;
                } catch (ActivityNotFoundException | SecurityException ignored) { }
            }
        }
        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException e) {
            toast("This TV controls Home selection in system settings.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HOME) {
            toast(resultCode == RESULT_OK ? "Home selected" : "Home selection unchanged");
        } else if (requestCode == PICK_BACKGROUND && resultCode == RESULT_OK
            && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String old = prefs().getString(BACKGROUND_URI, null);
                prefs().edit().putString(BACKGROUND_URI, uri.toString()).apply();
                applyBackground();
                if (old != null && !old.equals(uri.toString())) releaseBackgroundAccess(old);
            } catch (SecurityException e) {
                toast("Unable to keep permission to read that background.");
            }
        }
    }

    private Button action(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(FOREGROUND);
        button.setAllCaps(false);
        button.setFocusable(true);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(surface(0xCC1D2B3B, dp(16), BORDER, 1));
        focusDecoration(button, false);
        return button;
    }

    private void focusDecoration(View view, boolean input) {
        view.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(surface(focused ? 0xCCEDF7FC : 0xCC1D2B3B,
                dp(input ? 16 : 18), focused ? FOCUS : BORDER, focused ? 3 : 1));
            if (v instanceof TextView && !input)
                ((TextView) v).setTextColor(focused ? 0xFF152B3B : FOREGROUND);
            v.setScaleX(focused ? 1.04f : 1f);
            v.setScaleY(focused ? 1.04f : 1f);
        });
    }

    private GradientDrawable surface(int fill, int radius, int border, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(strokeDp), border);
        return drawable;
    }

    private TextView text(String value, int sizeSp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setTypeface(Typeface.create("sans-serif", style));
        return text;
    }

    private void updateClock() {
        if (clock != null) clock.setText(
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date()));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class AppEntry {
        final ComponentName component;
        final String label;
        final String category;
        AppEntry(ComponentName component, String label, String category) {
            this.component = component;
            this.label = label;
            this.category = category;
        }
        String key() { return component.flattenToString(); }
    }
}
