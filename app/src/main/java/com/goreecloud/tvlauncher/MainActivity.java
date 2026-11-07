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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Original, permission-minimal TV HOME activity.
 * No network, usage access, accessibility service, device admin, or privileged APIs.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 17, 31);
    private static final int PANEL = Color.rgb(25, 39, 58);
    private static final int PANEL_FOCUSED = Color.rgb(34, 65, 78);
    private static final int FG = Color.rgb(242, 249, 250);
    private static final int MUTED = Color.rgb(162, 182, 197);
    private static final int ACCENT = Color.rgb(103, 226, 209);
    private static final int PREF_MODE = Context.MODE_PRIVATE;
    private static final String FAVORITES = "favorites";

    private final List<AppEntry> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private LinearLayout favoriteRow;
    private LinearLayout appRows;
    private TextView clock;
    private TextView countLabel;
    private EditText search;
    private boolean receiverRegistered;

    private final BroadcastReceiver clockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateClock(); }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        SharedPreferences prefs = getPreferences(PREF_MODE);
        favorites.addAll(prefs.getStringSet(FAVORITES, new HashSet<>()));
        buildLayout();
        refreshApps();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshApps();
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

    private void buildLayout() {
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        scroller.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.rgb(12, 39, 54), BG, Color.rgb(13, 18, 38) }));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(52), dp(38), dp(52), dp(42));
        scroller.addView(content);
        setContentView(scroller);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView identity = label("G  /  GOREECLOUD TV", 16, ACCENT, Typeface.BOLD);
        identity.setLetterSpacing(0.12f);
        top.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        clock = label("", 16, MUTED, Typeface.NORMAL);
        top.addView(clock);
        content.addView(top);
        updateClock();

        TextView title = label("Make yourself at home.", 36, FG, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(26);
        content.addView(title, titleParams);
        TextView tagline = label("Your favorite entertainment, organized your way.", 17, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams taglineParams = new LinearLayout.LayoutParams(-1, -2);
        taglineParams.topMargin = dp(6);
        content.addView(tagline, taglineParams);

        LinearLayout utility = new LinearLayout(this);
        utility.setOrientation(LinearLayout.HORIZONTAL);
        utility.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams utilityParams = new LinearLayout.LayoutParams(-1, -2);
        utilityParams.topMargin = dp(24);
        content.addView(utility, utilityParams);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setHint("Search installed apps");
        search.setHintTextColor(MUTED);
        search.setTextColor(FG);
        search.setPadding(dp(20), 0, dp(20), 0);
        search.setBackground(panel(false, true));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setContentDescription("Search installed apps");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, dp(54), 1);
        searchParams.rightMargin = dp(14);
        utility.addView(search, searchParams);
        focusOutline(search, true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderApps(); }
            @Override public void afterTextChanged(Editable editable) { }
        });

        Button defaults = action("Set default Home");
        utility.addView(defaults, new LinearLayout.LayoutParams(dp(194), dp(54)));
        defaults.setOnClickListener(v -> requestHomeRole());

        LinearLayout favHeading = sectionHeading("Pinned apps", "Hold OK or press Menu to pin or unpin");
        LinearLayout.LayoutParams favHeadParams = new LinearLayout.LayoutParams(-1, -2);
        favHeadParams.topMargin = dp(32);
        content.addView(favHeading, favHeadParams);
        favoriteRow = new LinearLayout(this);
        favoriteRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams favoriteParams = new LinearLayout.LayoutParams(-1, -2);
        favoriteParams.topMargin = dp(12);
        content.addView(favoriteRow, favoriteParams);

        LinearLayout allHeading = sectionHeading("Your apps", "OK to launch  •  Hold OK / Menu to pin");
        LinearLayout.LayoutParams allParams = new LinearLayout.LayoutParams(-1, -2);
        allParams.topMargin = dp(34);
        content.addView(allHeading, allParams);
        appRows = new LinearLayout(this);
        appRows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(10);
        content.addView(appRows, gridParams);

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(-1, dp(65));
        footParams.topMargin = dp(20);
        content.addView(foot, footParams);
        content.setFocusableInTouchMode(true);
        content.requestFocus();
        countLabel = label("", 14, MUTED, Typeface.NORMAL);
        foot.addView(countLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Button refresh = action("Refresh apps");
        foot.addView(refresh, new LinearLayout.LayoutParams(dp(168), dp(48)));
        refresh.setOnClickListener(v -> refreshApps());
        Button settingsButton = action("TV settings");
        LinearLayout.LayoutParams settingParams = new LinearLayout.LayoutParams(dp(164), dp(48));
        settingParams.leftMargin = dp(10);
        foot.addView(settingsButton, settingParams);
        settingsButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException ex) { toast("TV settings are unavailable"); }
        });
    }

    private LinearLayout sectionHeading(String heading, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = label(heading, 23, FG, Typeface.BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView sub = label(hint, 13, MUTED, Typeface.NORMAL);
        row.addView(sub);
        return row;
    }

    private void refreshApps() {
        if (appRows == null) return;
        View active = getCurrentFocus();
        String previouslyFocused = active == null ? null : (active.getTag() instanceof String ? (String) active.getTag() : null);
        LinkedHashMap<String, AppEntry> found = new LinkedHashMap<>();
        collectApps(found, Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        collectApps(found, Intent.CATEGORY_LAUNCHER, false);
        apps.clear();
        apps.addAll(found.values());
        java.util.Collections.sort(apps, (a, b) -> a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT)));
        renderApps();
        restoreTileFocus(previouslyFocused);
    }

    private void collectApps(LinkedHashMap<String, AppEntry> found, String category, boolean tv) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> matches = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo == null || info.activityInfo.packageName == null
                    || info.activityInfo.name == null) continue;
            if (getPackageName().equals(info.activityInfo.packageName)) continue;
            ComponentName component = new ComponentName(
                    info.activityInfo.packageName, info.activityInfo.name);
            String key = component.flattenToString();
            if (found.containsKey(key)) continue;
            CharSequence loaded = info.loadLabel(pm);
            String label = loaded == null ? info.activityInfo.packageName : loaded.toString();
            found.put(key, new AppEntry(component, label, category, tv));
        }
    }

    private void renderApps() {
        if (appRows == null || favoriteRow == null) return;
        favoriteRow.removeAllViews();
        appRows.removeAllViews();
        String query = search == null ? "" : search.getText().toString();
        List<AppEntry> pinned = new ArrayList<>();
        List<AppEntry> filtered = new ArrayList<>();
        for (AppEntry app : apps) {
            if (favorites.contains(app.key())) pinned.add(app);
            if (AppFilter.matches(app.label, query)) filtered.add(app);
        }
        if (pinned.isEmpty()) {
            TextView empty = label("☆  No pinned apps yet. Hold OK on a tile to pin it here.",
                    15, MUTED, Typeface.NORMAL);
            empty.setPadding(dp(18), dp(22), dp(18), dp(22));
            empty.setBackground(panel(false, false));
            favoriteRow.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else {
            int shown = Math.min(6, pinned.size());
            for (int i = 0; i < shown; i++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(138), 1);
                lp.rightMargin = dp(12);
                favoriteRow.addView(card(pinned.get(i), true), lp);
            }
            if (pinned.size() > shown) {
                TextView more = label("+" + (pinned.size() - shown) + " more", 15, MUTED, Typeface.BOLD);
                favoriteRow.addView(more);
            }
        }
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidthDp = (int) (metrics.widthPixels / metrics.density);
        int columns = Math.max(3, Math.min(6, (screenWidthDp - 104) / 178));
        for (int start = 0; start < filtered.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < columns; j++) {
                int index = start + j;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(140), 1);
                lp.setMargins(dp(0), dp(7), dp(13), dp(7));
                if (index < filtered.size()) row.addView(card(filtered.get(index), false), lp);
                else {
                    View spacer = new View(this);
                    row.addView(spacer, lp);
                }
            }
            appRows.addView(row);
        }
        if (filtered.isEmpty()) {
            TextView none = label("No matching apps found. Try a different search.",
                    16, MUTED, Typeface.NORMAL);
            none.setPadding(dp(18), dp(26), 0, dp(26));
            appRows.addView(none);
        }
        countLabel.setText(getString(R.string.app_count, apps.size(), pinned.size()));
    }

    private View card(AppEntry app, boolean compact) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(10), dp(12), dp(10), dp(12));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setTag(app.key());
        tile.setBackground(panel(false, false));
        tile.setContentDescription(app.label + (favorites.contains(app.key()) ? ", pinned" : "")
                + ". Press OK to open; hold OK or press Menu to change pin.");
        ImageView icon = new ImageView(this);
        try {
            Drawable image = getPackageManager().getActivityIcon(app.component);
            icon.setImageDrawable(image);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView name = label(app.label, compact ? 14 : 15, FG, Typeface.BOLD);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(9);
        tile.addView(name, nameParams);
        if (favorites.contains(app.key())) {
            TextView pinned = label("★", 14, ACCENT, Typeface.NORMAL);
            tile.addView(pinned);
        }
        focusOutline(tile, false);
        tile.setOnClickListener(v -> launch(app));
        tile.setOnLongClickListener(v -> { toggleFavorite(app); return true; });
        tile.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
                toggleFavorite(app);
                return true;
            }
            return keyCode == KeyEvent.KEYCODE_MENU;
        });
        return tile;
    }

    private void toggleFavorite(AppEntry app) {
        if (!favorites.add(app.key())) favorites.remove(app.key());
        getPreferences(PREF_MODE).edit().putStringSet(FAVORITES, new HashSet<>(favorites)).apply();
        toast(favorites.contains(app.key()) ? app.label + " pinned" : app.label + " unpinned");
        renderApps();
        restoreTileFocus(app.key());
    }

    private void restoreTileFocus(String key) {
        if (key == null || appRows == null) return;
        for (int rowIndex = 0; rowIndex < appRows.getChildCount(); rowIndex++) {
            View row = appRows.getChildAt(rowIndex);
            if (!(row instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) row;
            for (int i = 0; i < group.getChildCount(); i++) {
                View candidate = group.getChildAt(i);
                if (key.equals(candidate.getTag())) {
                    candidate.requestFocus();
                    return;
                }
            }
        }
    }

    private void launch(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(app.category);
        intent.setComponent(app.component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            toast("Unable to open " + app.label + ". Try refreshing apps.");
        }
    }

    private void requestHomeRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(RoleManager.ROLE_HOME)) { toast("Already the default Home app"); return; }
                try { startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 700); return; }
                catch (ActivityNotFoundException | SecurityException ignored) { }
            }
        }
        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException e) {
            toast("This TV controls the Home app in system settings. Your original launcher remains installed.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 700)
            toast(resultCode == RESULT_OK ? "Home launcher selected" : "Home selection was not changed");
    }

    private Button action(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(FG);
        button.setBackground(panel(false, false));
        button.setFocusable(true);
        button.setPadding(dp(12), 0, dp(12), 0);
        focusOutline(button, false);
        return button;
    }

    private void focusOutline(View view, boolean input) {
        view.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(panel(focused, input));
            v.setScaleX(focused ? 1.025f : 1.0f);
            v.setScaleY(focused ? 1.025f : 1.0f);
        });
    }

    private GradientDrawable panel(boolean focused, boolean input) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? PANEL_FOCUSED : PANEL);
        shape.setCornerRadius(dp(input ? 16 : 18));
        shape.setStroke(dp(focused ? 3 : 1), focused ? ACCENT : Color.rgb(50, 68, 84));
        return shape;
    }

    private TextView label(String text, int sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        return view;
    }

    private void updateClock() {
        if (clock != null) clock.setText(DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(new Date()));
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    private static final class AppEntry {
        final ComponentName component;
        final String label;
        final String category;
        final boolean tv;

        AppEntry(ComponentName component, String label, String category, boolean tv) {
            this.component = component;
            this.label = label;
            this.category = category;
            this.tv = tv;
        }
        String key() { return component.flattenToString(); }
    }
}
