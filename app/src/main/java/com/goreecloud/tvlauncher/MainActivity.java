package com.goreecloud.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
 * Native, original, privacy-first TV Home. Built-in art and logo are first-party
 * GoreeCloud assets. App activity cards are app shortcuts, never fabricated
 * streaming history or playable movies. No network, ads or telemetry.
 */
public final class MainActivity extends Activity {
    private static final int PICK_BACKGROUND = 801;
    private static final int REQUEST_HOME = 700;
    private static final int CUSTOM_SCENE = 4;
    private static final String PREF_FAVORITES = "favorites";
    private static final String PREF_SCENE = "scene";
    private static final String PREF_CUSTOM = "background_uri";
    private static final String PREF_RECENT = "recent_launches";
    private static final int TEXT = 0xFFF4F8FF;
    private static final int SOFT = 0xFFCFDBEA;
    private static final int LAVENDER = 0xFFC8B1FF;
    private static final int OUTLINE = 0x99B7C4DB;
    private static final int CARD = 0xCB222837;
    private static final int FROST = 0xCC2B3041;
    private static final int FOCUS = 0xFFE6CDFF;

    private static final String[] SCENE_TITLES = {
        "HORIZON EXPEDITION", "APERTURE CITY", "CLOUDFORGE PEAKS", "NEON HARBOR"
    };
    private static final String[] SCENE_DESCRIPTIONS = {
        "Beyond the clouds, a new horizon awaits.",
        "Discover a world of light and possibility.",
        "Find your way through the skybound wilds.",
        "Where the city lights meet the open water."
    };
    private static final int[] SCENES = {
        R.drawable.wp_horizon, R.drawable.wp_aperture,
        R.drawable.wp_cloudforge, R.drawable.wp_harbor
    };
    private static final int[] TINTS = {
        0xFF8F6DEA, 0xFFD65BAE, 0xFF45C8BF, 0xFF548DEB, 0xFFE67DAD, 0xFF9A7CEC
    };

    private final List<AppEntry> apps = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private FrameLayout root;
    private ImageView background;
    private TextView clock;
    private TextView heroTitle;
    private TextView heroDescription;
    private TextView featuredName;
    private TextView dockTitle;
    private LinearLayout dock;
    private LinearLayout appRows;
    private LinearLayout continueRow;
    private LinearLayout recommendations;
    private TextView continueSubtitle;
    private TextView appCount;
    private EditText search;
    private View firstDockTile;
    private AppEntry focusedApp;
    private boolean receiverRegistered;
    private final BroadcastReceiver ticker = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        favorites.addAll(settings().getStringSet(PREF_FAVORITES, new HashSet<>()));
        buildScreen();
        refreshApps();
        applyScene();
        if (firstDockTile != null) firstDockTile.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshApps();
        updateClock();
        if (!receiverRegistered) {
            registerReceiver(ticker, new IntentFilter(Intent.ACTION_TIME_TICK));
            receiverRegistered = true;
        }
    }

    @Override protected void onPause() {
        if (receiverRegistered) {
            unregisterReceiver(ticker);
            receiverRegistered = false;
        }
        super.onPause();
    }

    private SharedPreferences settings() { return getPreferences(Context.MODE_PRIVATE); }

    private void buildScreen() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF111828);
        background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setContentDescription("Selected GoreeCloud original wallpaper");
        background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        View darken = new View(this);
        darken.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {0x64080D21, 0x3D10162A, 0xC60C1020, 0xEF080D1B}));
        root.addView(darken, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout page = vertical();
        page.setPadding(dp(38), dp(24), dp(38), dp(40));
        scroll.addView(page);
        setContentView(root);

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tv_wordmark);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        logo.setContentDescription("GoreeCloud TV");
        header.addView(logo, new LinearLayout.LayoutParams(dp(295), dp(52)));
        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        clock = text("", 15, TEXT, Typeface.BOLD);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(dp(20), dp(9), dp(20), dp(9));
        clock.setBackground(shape(0xC5242B3A, dp(25), OUTLINE, 1));
        header.addView(clock);
        updateClock();

        // Original featured artwork. These actions navigate locally; the artwork
        // is not represented as playable content or an invented streaming show.
        LinearLayout hero = vertical();
        hero.setGravity(Gravity.BOTTOM | Gravity.START);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, dp(190));
        heroLp.topMargin = dp(8);
        page.addView(hero, heroLp);
        TextView kicker = text("A GOREECLOUD ORIGINAL  /  FOUR WORLDS", 12, SOFT, Typeface.BOLD);
        kicker.setLetterSpacing(.18f);
        hero.addView(kicker);
        heroTitle = text("", 31, TEXT, Typeface.BOLD);
        heroTitle.setLetterSpacing(.13f);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(-1, -2);
        htp.topMargin = dp(4);
        hero.addView(heroTitle, htp);
        heroDescription = text("", 15, SOFT, Typeface.NORMAL);
        LinearLayout.LayoutParams hdp = new LinearLayout.LayoutParams(-1, -2);
        hdp.topMargin = dp(3);
        hero.addView(heroDescription, hdp);

        LinearLayout actions = horizontal();
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(44));
        ap.topMargin = dp(9);
        hero.addView(actions, ap);
        Button browse = button("Explore apps", true);
        actions.addView(browse, new LinearLayout.LayoutParams(dp(151), dp(41)));
        browse.setOnClickListener(v -> {
            if (firstDockTile != null) firstDockTile.requestFocus();
            else if (search != null) search.requestFocus();
        });
        Button scenes = button("Choose wallpaper", false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(178), dp(41));
        slp.leftMargin = dp(9);
        actions.addView(scenes, slp);
        scenes.setOnClickListener(v -> chooseScene());

        LinearLayout indicators = horizontal();
        indicators.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams indicatorsLp = new LinearLayout.LayoutParams(-1, dp(24));
        hero.addView(indicators, indicatorsLp);
        // Dots are controls, not false claims of streaming carousel content.
        for (int i = 0; i < SCENES.length; i++) {
            final int selected = i;
            TextView dot = text("●", 18, 0xE0D3D9EA, Typeface.BOLD);
            dot.setGravity(Gravity.CENTER);
            dot.setContentDescription("Select wallpaper " + SCENE_TITLES[i]);
            dot.setFocusable(true);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(22), dp(24));
            indicators.addView(dot, dlp);
            dot.setOnClickListener(v -> setScene(selected));
            focusStyle(dot, false, 0x00000000, true);
            dot.setTag("scene-dot-" + i);
        }

        LinearLayout dockPanel = vertical();
        dockPanel.setPadding(dp(15), dp(10), dp(15), dp(12));
        dockPanel.setBackground(shape(FROST, dp(26), 0xA1B2C1D9, 1));
        dockPanel.setClipChildren(false);
        dockPanel.setClipToPadding(false);
        page.addView(dockPanel);
        LinearLayout dockHeading = horizontal();
        dockHeading.setGravity(Gravity.CENTER_VERTICAL);
        dockPanel.addView(dockHeading);
        dockTitle = text("QUICK LAUNCH", 12, TEXT, Typeface.BOLD);
        dockTitle.setLetterSpacing(.12f);
        dockHeading.addView(dockTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView hint = text("Menu / hold OK to pin", 12, SOFT, Typeface.NORMAL);
        dockHeading.addView(hint);
        dock = horizontal();
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        LinearLayout.LayoutParams dockRowLp = new LinearLayout.LayoutParams(-1, -2);
        dockRowLp.topMargin = dp(7);
        dockPanel.addView(dock, dockRowLp);

        // Real recently opened applications; never simulate an episode or play-progress state.
        LinearLayout recentHeading = sectionHeader("CONTINUE IN YOUR APPS",
            "Local app shortcuts · not streaming watch history");
        LinearLayout.LayoutParams rhp = new LinearLayout.LayoutParams(-1, -2);
        rhp.topMargin = dp(18);
        page.addView(recentHeading, rhp);
        HorizontalScrollView recentScroll = new HorizontalScrollView(this);
        recentScroll.setHorizontalScrollBarEnabled(false);
        recentScroll.setClipToPadding(false);
        recentScroll.setFillViewport(false);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, dp(119));
        rlp.topMargin = dp(7);
        page.addView(recentScroll, rlp);
        continueRow = horizontal();
        continueRow.setClipChildren(false);
        continueRow.setClipToPadding(false);
        recentScroll.addView(continueRow);

        LinearLayout worldHeading = sectionHeader("EXPLORE GOREECLOUD WORLDS",
            "Four original offline wallpapers");
        LinearLayout.LayoutParams whp = new LinearLayout.LayoutParams(-1, -2);
        whp.topMargin = dp(17);
        page.addView(worldHeading, whp);
        HorizontalScrollView worldScroll = new HorizontalScrollView(this);
        worldScroll.setHorizontalScrollBarEnabled(false);
        worldScroll.setClipToPadding(false);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, dp(127));
        wlp.topMargin = dp(8);
        page.addView(worldScroll, wlp);
        LinearLayout worlds = horizontal();
        worlds.setClipChildren(false);
        worlds.setClipToPadding(false);
        worldScroll.addView(worlds);
        for (int i = 0; i < SCENES.length; i++) worlds.addView(sceneCard(i));

        LinearLayout recommendedHeading = sectionHeader("RECOMMENDED FOR YOU",
            "Installed apps only  ·  No sponsored content or behavioral tracking");
        LinearLayout.LayoutParams recomHeaderLp = new LinearLayout.LayoutParams(-1, -2);
        recomHeaderLp.topMargin = dp(18);
        page.addView(recommendedHeading, recomHeaderLp);
        HorizontalScrollView recomScroll = new HorizontalScrollView(this);
        recomScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams recomLp = new LinearLayout.LayoutParams(-1, dp(107));
        recomLp.topMargin = dp(7);
        page.addView(recomScroll, recomLp);
        recommendations = horizontal();
        recomScroll.addView(recommendations);

        LinearLayout appHeading = sectionHeader("YOUR APPS", "Available on this TV");
        LinearLayout.LayoutParams ah = new LinearLayout.LayoutParams(-1, -2);
        ah.topMargin = dp(20);
        page.addView(appHeading, ah);
        LinearLayout searchRow = horizontal();
        LinearLayout.LayoutParams shp = new LinearLayout.LayoutParams(-1, dp(52));
        shp.topMargin = dp(9);
        page.addView(searchRow, shp);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search installed apps");
        search.setHintTextColor(SOFT);
        search.setTextSize(15);
        search.setTextColor(TEXT);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(shape(0xD2273045, dp(15), OUTLINE, 1));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setContentDescription("Search installed apps");
        LinearLayout.LayoutParams inp = new LinearLayout.LayoutParams(0, dp(48), 1);
        inp.rightMargin = dp(12);
        searchRow.addView(search, inp);
        focusStyle(search, false, 0xD2273045, false);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                renderAllApps();
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        Button change = button("Wallpapers", false);
        searchRow.addView(change, new LinearLayout.LayoutParams(dp(130), dp(48)));
        change.setOnClickListener(v -> chooseScene());

        appRows = vertical();
        LinearLayout.LayoutParams appsLp = new LinearLayout.LayoutParams(-1, -2);
        appsLp.topMargin = dp(8);
        page.addView(appRows, appsLp);
        appCount = text("", 13, SOFT, Typeface.NORMAL);
        page.addView(appCount);

        LinearLayout footer = horizontal();
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, dp(72));
        flp.topMargin = dp(10);
        page.addView(footer, flp);
        TextView local = text("OFFLINE WALLPAPERS  ·  NO ADS OR TELEMETRY", 12, SOFT, Typeface.BOLD);
        footer.addView(local, new LinearLayout.LayoutParams(0, -2, 1));
        Button settingsBtn = button("TV settings", false);
        footer.addView(settingsBtn, new LinearLayout.LayoutParams(dp(127), dp(45)));
        settingsBtn.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) { toast("TV settings unavailable"); }
        });
        Button homeBtn = button("Choose Home", false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(140), dp(45));
        hlp.leftMargin = dp(10);
        footer.addView(homeBtn, hlp);
        homeBtn.setOnClickListener(v -> requestHomeRole());
    }

    private LinearLayout sectionHeader(String title, String subtitle) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, 17, TEXT, Typeface.BOLD);
        name.setLetterSpacing(.04f);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView supporting = text(subtitle, 12, SOFT, Typeface.NORMAL);
        row.addView(supporting);
        return row;
    }

    private View sceneCard(int id) {
        FrameLayout card = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(233), dp(122));
        lp.rightMargin = dp(12);
        card.setLayoutParams(lp);
        card.setFocusable(true);
        card.setClickable(true);
        card.setBackground(shape(CARD, dp(17), OUTLINE, 1));
        card.setClipToOutline(true);
        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setImageResource(SCENES[id]);
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(art, new FrameLayout.LayoutParams(-1, -1));
        View shadow = new View(this);
        shadow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {0x00000000, 0xE7040C18}));
        card.addView(shadow, new FrameLayout.LayoutParams(-1, -1));
        TextView name = text(SCENE_TITLES[id], 15, TEXT, Typeface.BOLD);
        name.setPadding(dp(15), 0, dp(10), dp(12));
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        card.addView(name, np);
        card.setContentDescription("Use " + SCENE_TITLES[id] + " built-in wallpaper");
        focusStyle(card, true, CARD, false);
        card.setOnClickListener(v -> setScene(id));
        return card;
    }

    private void refreshApps() {
        if (dock == null) return;
        View current = getCurrentFocus();
        String key = current != null && current.getTag() instanceof String
            && ((String) current.getTag()).contains("/") ? (String) current.getTag() : null;
        LinkedHashMap<String, AppEntry> found = new LinkedHashMap<>();
        collect(found, Intent.CATEGORY_LEANBACK_LAUNCHER);
        collect(found, Intent.CATEGORY_LAUNCHER);
        apps.clear();
        apps.addAll(found.values());
        Collections.sort(apps, (a,b) ->
            a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT)));
        renderDock();
        renderRecent();
        renderRecommendations();
        renderAllApps();
        if (key != null) restoreFocus(key);
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
                title == null ? info.activityInfo.packageName : title.toString(),category));
        }
    }

    private void renderDock() {
        if (dock == null) return;
        dock.removeAllViews();
        firstDockTile = null;
        List<AppEntry> pinned = new ArrayList<>();
        for (AppEntry app : apps) if (favorites.contains(app.key())) pinned.add(app);
        dockTitle.setText(pinned.isEmpty() ? "QUICK LAUNCH" : "FAVORITES");
        List<AppEntry> shown = pinned.isEmpty() ? apps : pinned;
        for (int i=0;i<Math.min(6,shown.size());i++) {
            AppEntry app = shown.get(i);
            View tile = appTile(app,i,true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(111),1);
            if(i>0)lp.leftMargin=dp(9);
            dock.addView(tile,lp);
            if (i == 0) firstDockTile=tile;
        }
        if (shown.isEmpty()) dock.addView(text("No launchable TV apps found",16,SOFT,Typeface.NORMAL));
        if (appCount!=null) appCount.setText(getString(R.string.app_count, apps.size(), pinned.size()));
    }

    private void renderRecent() {
        if(continueRow==null)return;
        continueRow.removeAllViews();
        List<AppEntry> recent = new ArrayList<>();
        String saved = settings().getString(PREF_RECENT,"");
        for (String key : saved.split("\n")) {
            for (AppEntry app:apps) if(app.key().equals(key) && !recent.contains(app)) {
                recent.add(app); break;
            }
            if (recent.size()>=5)break;
        }
        if (recent.isEmpty()) {
            for (int i=0;i<Math.min(3,apps.size());i++) recent.add(apps.get(i));
        }
        for (int i=0;i<recent.size();i++) {
            AppEntry app=recent.get(i);
            FrameLayout panel=new FrameLayout(this);
            panel.setFocusable(true);
            panel.setClickable(true);
            panel.setTag(app.key());
            panel.setBackground(shape(0xD1283348,dp(19),OUTLINE,1));
            panel.setClipToOutline(true);
            panel.setContentDescription("Open "+app.label+" application, no playback position");
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(264),dp(110));
            lp.rightMargin=dp(13);
            continueRow.addView(panel,lp);
            ImageView backdrop=new ImageView(this);
            backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            backdrop.setImageResource(SCENES[i%SCENES.length]);
            backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            panel.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
            View gradient=new View(this);
            gradient.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x10070F1D,0xE90A1022}));
            panel.addView(gradient,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout details=vertical();
            details.setPadding(dp(13),dp(8),dp(13),dp(10));
            FrameLayout.LayoutParams detLp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
            panel.addView(details,detLp);
            LinearLayout top=horizontal();
            top.setGravity(Gravity.CENTER_VERTICAL);
            details.addView(top);
            top.addView(appIcon(app,35),new LinearLayout.LayoutParams(dp(35),dp(35)));
            TextView label=text(app.label,16,TEXT,Typeface.BOLD);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl=new LinearLayout.LayoutParams(0,-2,1);
            nl.leftMargin=dp(10);
            top.addView(label,nl);
            TextView secondary=text("OPEN APP   ▶",11,SOFT,Typeface.BOLD);
            LinearLayout.LayoutParams sec=new LinearLayout.LayoutParams(-1,-2);
            sec.topMargin=dp(2);
            details.addView(secondary,sec);
            panel.setOnFocusChangeListener((v,hasFocus)->{
                v.setBackground(shape(0xD1283348,dp(19),hasFocus?FOCUS:OUTLINE,
                    hasFocus?3:1));
                v.setScaleX(hasFocus?1.025f:1f);
                v.setScaleY(hasFocus?1.025f:1f);
            });
            panel.setOnClickListener(v -> launch(app));
        }
    }

    private void renderRecommendations() {
        if (recommendations == null) return;
        recommendations.removeAllViews();
        int shown = 0;
        for (AppEntry app : apps) {
            if (favorites.contains(app.key())) continue;
            if (shown >= 6) break;
            LinearLayout card = horizontal();
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            card.setFocusable(true);
            card.setClickable(true);
            card.setTag(app.key());
            card.setContentDescription("Recommended installed app " + app.label + ". Press OK to open");
            card.setBackground(shape(CARD, dp(17), OUTLINE, 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(224), dp(100));
            lp.rightMargin = dp(12);
            recommendations.addView(card, lp);
            card.addView(appIcon(app, 54), new LinearLayout.LayoutParams(dp(54), dp(54)));
            LinearLayout column = vertical();
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
            cp.leftMargin = dp(12);
            card.addView(column, cp);
            TextView label = text(app.label, 15, TEXT, Typeface.BOLD);
            label.setMaxLines(2);
            column.addView(label);
            TextView secondary = text("Installed on your TV", 11, SOFT, Typeface.NORMAL);
            column.addView(secondary);
            focusStyle(card, true, CARD, false);
            card.setOnClickListener(v -> launch(app));
            shown++;
        }
        if (shown == 0) recommendations.addView(text(
            "Your pinned apps are already in Quick Launch", 14, SOFT, Typeface.NORMAL));
    }

    private void renderAllApps() {
        if (appRows == null) return;
        appRows.removeAllViews();
        String query=search == null ? "" : search.getText().toString();
        List<AppEntry> visible=new ArrayList<>();
        for(AppEntry app:apps)if(AppFilter.matches(app.label,query))visible.add(app);
        DisplayMetrics dm=getResources().getDisplayMetrics();
        int widthDp=Math.round(dm.widthPixels/dm.density);
        int cols=Math.max(3,Math.min(6,(widthDp-76)/150));
        for(int i=0;i<visible.size();i+=cols){
            LinearLayout row=horizontal();
            row.setClipChildren(false);
            appRows.addView(row);
            for(int j=0;j<cols;j++){
                int pos=i+j;
                View v=pos<visible.size()?appTile(visible.get(pos),pos,false):new View(this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(111),1);
                lp.setMargins(0,dp(7),dp(10),dp(7));
                row.addView(v,lp);
            }
        }
        if(visible.isEmpty()){
            TextView empty=text("No matching apps found",16,SOFT,Typeface.NORMAL);
            empty.setPadding(dp(15),dp(20),0,dp(24));
            appRows.addView(empty);
        }
        if(appCount!=null)appCount.setText(getString(R.string.app_count,apps.size(),favorites.size()));
    }

    private View appTile(AppEntry app,int index,boolean prominent) {
        LinearLayout tile=vertical();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8),dp(7),dp(8),dp(6));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setLongClickable(true);
        tile.setTag(app.key());
        tile.setContentDescription(app.label+(favorites.contains(app.key())?", pinned. ":". ")
            +"Press OK to open; hold OK or Menu to toggle favorite");
        tile.setBackground(shape(CARD,dp(20),OUTLINE,1));
        tile.addView(appIcon(app,prominent?60:57),
            new LinearLayout.LayoutParams(dp(prominent?60:57),dp(prominent?60:57)));
        TextView label=text(app.label,prominent?14:13,TEXT,Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);
        tlp.topMargin=dp(6);
        tile.addView(label,tlp);
        tile.setOnFocusChangeListener((v,focused)->{
            v.setBackground(shape(focused?0xD254366C:CARD,dp(20),
                focused?FOCUS:OUTLINE,focused?4:1));
            v.setScaleX(focused?1.045f:1f);
            v.setScaleY(focused?1.045f:1f);
            v.setElevation(focused?dp(16):0);
            if(focused)focusedApp=app;
        });
        tile.setOnClickListener(v->launch(app));
        tile.setOnLongClickListener(v->{toggleFavorite(app);return true;});
        tile.setOnKeyListener((v,k,event)->{
            if(k!=KeyEvent.KEYCODE_MENU)return false;
            if(event.getAction()==KeyEvent.ACTION_UP)toggleFavorite(app);
            return true;
        });
        return tile;
    }

    private ImageView appIcon(AppEntry app,int size) {
        ImageView img=new ImageView(this);
        try { img.setImageDrawable(getPackageManager().getActivityIcon(app.component)); }
        catch(PackageManager.NameNotFoundException|RuntimeException e) {
            img.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return img;
    }

    private void toggleFavorite(AppEntry app) {
        if(!favorites.add(app.key()))favorites.remove(app.key());
        settings().edit().putStringSet(PREF_FAVORITES,new HashSet<>(favorites)).apply();
        renderDock();renderAllApps();
        restoreFocus(app.key());
        toast(app.label+(favorites.contains(app.key())?" pinned":" unpinned"));
    }

    private void restoreFocus(String key) {
        if (findAndFocus(dock,key))return;
        if (findAndFocus(continueRow,key))return;
        for(int i=0;i<appRows.getChildCount();i++){
            View row=appRows.getChildAt(i);
            if(row instanceof ViewGroup&&findAndFocus((ViewGroup)row,key))return;
        }
    }
    private boolean findAndFocus(ViewGroup group,String key) {
        if(group==null)return false;
        for(int i=0;i<group.getChildCount();i++){
            View v=group.getChildAt(i);
            if(key.equals(v.getTag()))return v.requestFocus();
        }
        return false;
    }

    private void launch(AppEntry app) {
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(app.category)
            .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                |Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
            List<String> keys=new ArrayList<>();
            keys.add(app.key());
            for(String old:settings().getString(PREF_RECENT,"").split("\n")) {
                if(!old.isEmpty()&&!keys.contains(old)&&keys.size()<5)keys.add(old);
            }
            settings().edit().putString(PREF_RECENT,TextUtils.join("\n",keys)).apply();
            renderRecent();
        } catch(ActivityNotFoundException|SecurityException e) {
            toast("Cannot open "+app.label);
            refreshApps();
        }
    }

    private void setScene(int index) {
        if(index<0||index>=SCENES.length)return;
        settings().edit().putInt(PREF_SCENE,index).apply();
        applyScene();
        toast(SCENE_TITLES[index]+" selected");
    }

    private void applyScene() {
        if(background==null)return;
        int selected=settings().getInt(PREF_SCENE,0);
        if(selected>=0&&selected<SCENES.length){
            background.setImageResource(SCENES[selected]);
            heroTitle.setText(SCENE_TITLES[selected]);
            heroDescription.setText(SCENE_DESCRIPTIONS[selected]);
        } else {
            boolean loaded=loadPrivateBackdrop();
            if(!loaded) {
                settings().edit().putInt(PREF_SCENE,0).apply();
                applyScene();
                return;
            }
            heroTitle.setText("YOUR OWN HORIZON");
            heroDescription.setText("Your personal artwork. Your private home screen.");
        }
    }

    private boolean loadPrivateBackdrop() {
        String saved=settings().getString(PREF_CUSTOM,null);
        try {
            Uri uri=saved==null?Uri.fromFile(new File(getFilesDir(),"backdrop.jpg")):Uri.parse(saved);
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            try(InputStream input=getContentResolver().openInputStream(uri)){
                if(input==null)return false;
                BitmapFactory.decodeStream(input,null,bounds);
            }
            if(bounds.outWidth<1||bounds.outHeight<1)return false;
            int sample=1;
            while(bounds.outWidth/sample>2560||bounds.outHeight/sample>1440)sample*=2;
            BitmapFactory.Options opt=new BitmapFactory.Options();
            opt.inSampleSize=sample;
            Bitmap bitmap;
            try(InputStream input=getContentResolver().openInputStream(uri)){
                if(input==null)return false;
                bitmap=BitmapFactory.decodeStream(input,null,opt);
            }
            if(bitmap==null)return false;
            background.setImageBitmap(bitmap);
            return true;
        } catch(Exception|OutOfMemoryError e){
            return false;
        }
    }

    private void chooseScene() {
        LinearLayout choices=vertical();
        choices.setPadding(dp(12),dp(8),dp(12),dp(8));
        for(int i=0;i<SCENES.length;i++){
            final int idx=i;
            LinearLayout row=horizontal();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8),dp(5),dp(12),dp(5));
            row.setBackground(shape(0xFF232A3C,dp(16),OUTLINE,1));
            row.setFocusable(true);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(78));
            lp.bottomMargin=dp(8);
            choices.addView(row,lp);
            ImageView preview=new ImageView(this);
            preview.setImageResource(SCENES[i]);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(preview,new LinearLayout.LayoutParams(dp(112),dp(65)));
            TextView label=text(SCENE_TITLES[i]
                +(settings().getInt(PREF_SCENE,0)==i?"  ✓":""),17,TEXT,Typeface.BOLD);
            LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(0,-2,1);
            nameLp.leftMargin=dp(16);
            row.addView(label,nameLp);
            focusStyle(row,true,0xFF232A3C,false);
            row.setContentDescription("Select "+SCENE_TITLES[i]+" wallpaper");
            row.setOnClickListener(v->{setScene(idx);});
        }
        Button personal=button("Use my own image",false);
        choices.addView(personal,new LinearLayout.LayoutParams(-1,dp(53)));
        personal.setOnClickListener(v->{
            if(settings().getString(PREF_CUSTOM,null)!=null
                ||new File(getFilesDir(),"backdrop.jpg").isFile()){
                settings().edit().putInt(PREF_SCENE,CUSTOM_SCENE).apply();
                applyScene();
                toast("Personal backdrop selected");
            }else choosePrivateImage();
        });
        new AlertDialog.Builder(this).setTitle("GoreeCloud TV · Wallpapers")
            .setView(choices).setNegativeButton("Done",(d,w)->d.dismiss()).show();
    }

    private void choosePrivateImage() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            |Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent,PICK_BACKGROUND); }
        catch(ActivityNotFoundException|SecurityException e){
            toast("No image picker on this TV. See private ADB import instructions.");
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_HOME){
            toast(resultCode==RESULT_OK?"Home role accepted":"Home unchanged");
        } else if(requestCode==PICK_BACKGROUND&&resultCode==RESULT_OK
            &&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try{
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                settings().edit().putString(PREF_CUSTOM,uri.toString())
                    .putInt(PREF_SCENE,CUSTOM_SCENE).apply();
                applyScene();
            }catch(SecurityException e){toast("Image permission unavailable");}
        }
    }

    private void requestHomeRole() {
        if(Build.VERSION.SDK_INT>=29){
            RoleManager manager=getSystemService(RoleManager.class);
            if(manager!=null&&manager.isRoleAvailable(RoleManager.ROLE_HOME)){
                if(manager.isRoleHeld(RoleManager.ROLE_HOME)){
                    toast("Already the selected Home app");return;
                }
                try{
                    startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_HOME);return;
                }catch(ActivityNotFoundException|SecurityException ignored){}
            }
        }
        try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
        catch(ActivityNotFoundException|SecurityException e){
            toast("This TV restricts Home selection to system settings.");
        }
    }

    private LinearLayout horizontal(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private TextView text(String value,int size,int color,int style){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
        t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",style));return t;
    }
    private Button button(String label,boolean primary){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(primary?0xFF1A1730:TEXT);
        b.setPadding(dp(10),0,dp(10),0);
        b.setFocusable(true);
        int color=primary?0xFFE8E9FA:0xB543445A;
        b.setBackground(shape(color,dp(18),primary?0xFFF4F5FF:OUTLINE,1));
        focusStyle(b,false,color,true);
        return b;
    }
    private void focusStyle(View v,boolean scale,int bg,boolean changeText){
        v.setOnFocusChangeListener((view,focused)->{
            view.setBackground(shape(focused?0xFF604B81:bg,dp(17),
                focused?FOCUS:OUTLINE,focused?3:1));
            if(scale){view.setScaleX(focused?1.025f:1f);view.setScaleY(focused?1.025f:1f);}
            if(changeText&&view instanceof TextView)((TextView)view).setTextColor(
                focused?TEXT:(bg==0xFFE8E9FA?0xFF1A1730:TEXT));
        });
    }
    private GradientDrawable shape(int color,int radius,int stroke,int strokeDp){
        GradientDrawable g=new GradientDrawable();g.setColor(color);
        g.setCornerRadius(radius);g.setStroke(dp(strokeDp),stroke);return g;
    }
    private int dp(int amount){return Math.round(amount*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private void updateClock(){
        if(clock!=null)clock.setText(DateFormat.getTimeInstance(
            DateFormat.SHORT,Locale.getDefault()).format(new Date()));
    }
    private static final class AppEntry{
        final ComponentName component;
        final String label,category;
        AppEntry(ComponentName c,String l,String g){component=c;label=l;category=g;}
        String key(){return component.flattenToString();}
    }
}
