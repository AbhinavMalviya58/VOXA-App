# Deep Links & FooterController 2.0 Integration Spec

## Goals
- Enable `voxa://` and `https://voxa.app/` deep links to open the correct screen and tab.
- Implement per-tab backstacks with reselect behavior (scroll-to-top or pop-to-root).
- Keep footer tab selection in sync with navigation.

---

## 1. AndroidManifest Intent Filters

Add to `MainActivity` (or a dedicated DeepLinkActivity if you prefer):

```xml
<activity android:name=".ui.activities.MainActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="voxa" />
        <data android:scheme="https" android:host="voxa.app" />
    </intent-filter>
</activity>
```

---

## 2. Routing Logic in MainActivity

```java
public class MainActivity extends AppCompatActivity {
    private FooterController footer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        footer = new FooterController(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            // Default: show Home
            footer.selectTab(FooterController.Tab.HOME);
            return;
        }
        String path = uri.getPath();
        if (path == null) return;
        // Simple routing based on deep-links.json
        switch (path) {
            case "/home":
                footer.selectTab(FooterController.Tab.HOME);
                break;
            case "/vox":
                footer.selectTab(FooterController.Tab.VOX);
                break;
            case "/vibe":
                footer.selectTab(FooterController.Tab.VIBE);
                break;
            case "/games":
                footer.selectTab(FooterController.Tab.GAMES);
                break;
            case "/notifications":
                footer.selectTab(FooterController.Tab.NOTIF);
                break;
            default:
                // Profile or chat with parameters
                if (path.startsWith("/profile/")) {
                    String uid = uri.getLastPathSegment();
                    Intent i = new Intent(this, ProfileActivity.class);
                    i.putExtra("uid", uid);
                    startActivity(i);
                    footer.selectTab(FooterController.Tab.HOME);
                } else if (path.startsWith("/chat/")) {
                    String uid = uri.getLastPathSegment();
                    // Find or create conversation, then open ChatDetailActivity
                    // For now, open ChatListActivity
                    footer.selectTab(FooterController.Tab.HOME);
                    startActivity(new Intent(this, ChatListActivity.class));
                }
                break;
        }
    }
}
```

---

## 3. FooterController 2.0 State Machine

### Data structures
```java
public class FooterController {
    public enum Tab { HOME, VOX, VIBE, GAMES, NOTIF }

    private final Activity activity;
    private Tab currentTab;
    private final Map<Tab, Stack<Class<?>>> backstacks = new EnumMap<>(Tab.class);
    private final Map<Tab, Runnable> reselectActions = new EnumMap<>(Tab.class);

    public FooterController(Activity activity) {
        this.activity = activity;
        initTabs();
        bindClickListeners();
    }

    private void initTabs() {
        for (Tab t : Tab.values()) {
            backstacks.put(t, new Stack<>());
        }
        // Default reselect: scroll-to-top for list screens
        reselectActions.put(Tab.HOME, this::scrollHomeToTop);
        reselectActions.put(Tab.VOX, this::scrollVoxToTop);
        reselectActions.put(Tab.VIBE, this::scrollVibeToTop);
        reselectActions.put(Tab.GAMES, this::scrollGamesToTop);
        reselectActions.put(Tab.NOTIF, this::scrollNotifToTop);
    }
```

### Core navigation methods
```java
    // Public entry used by deep links and tab clicks
    public void selectTab(Tab tab) {
        if (currentTab == tab) {
            // Reselect behavior
            reselectActions.get(tab).run();
            return;
        }
        // Save current tab's backstack
        if (currentTab != null) {
            saveCurrentTabToBackstack();
        }
        // Switch tab
        currentTab = tab;
        restoreTabStack(tab);
        updateFooterIcons(tab);
        // Optionally animate transition
        animateTabSwitch(tab);
    }

    private void saveCurrentTabToBackstack() {
        Class<?> top = activity.getClass(); // In a real implementation, track the current screen class per tab
        backstacks.get(currentTab).push(top);
    }

    private void restoreTabStack(Tab tab) {
        Stack<Class<?>> stack = backstacks.get(tab);
        if (stack.isEmpty()) {
            // Navigate to the root of this tab
            navigateToRoot(tab);
        } else {
            // Reopen the top of the stack (singleTop)
            Class<?> top = stack.peek();
            Intent i = new Intent(activity, top);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(i);
        }
    }

    private void navigateToRoot(Tab tab) {
        Class<?> root = getRootActivityForTab(tab);
        Intent i = new Intent(activity, root);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(i);
    }

    private Class<?> getRootActivityForTab(Tab tab) {
        switch (tab) {
            case HOME: return MainActivity.class;
            case VOX: return VoxActivity.class;
            case VIBE: return VibeActivity.class;
            case GAMES: return GameHubActivity.class;
            case NOTIF: return NotificationsActivity.class;
            default: return MainActivity.class;
        }
    }
```

### Footer UI updates
```java
    private void bindClickListeners() {
        findViewById(R.id.footerHome).setOnClickListener(v -> selectTab(Tab.HOME));
        findViewById(R.id.footerVox).setOnClickListener(v -> selectTab(Tab.VOX));
        findViewById(R.id.footerVibe).setOnClickListener(v -> selectTab(Tab.VIBE));
        findViewById(R.id.footerGames).setOnClickListener(v -> selectTab(Tab.GAMES));
        findViewById(R.id.footerNotif).setOnClickListener(v -> selectTab(Tab.NOTIF));
    }

    private void updateFooterIcons(Tab active) {
        // Reset all to inactive, then set active
        int activeColor = ContextCompat.getColor(activity, R.color.footerActive);
        int inactiveColor = ContextCompat.getColor(activity, R.color.footerInactive);
        // Example: update TextView drawables or ImageView tint
        ((TextView)findViewById(R.id.footerHome)).setTextColor(active == Tab.HOME ? activeColor : inactiveColor);
        // ... repeat for other tabs
    }

    private void animateTabSwitch(Tab tab) {
        // Example: subtle scale pulse on the active icon
        View activeIcon = getIconView(tab);
        activeIcon.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> {
            activeIcon.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
        }).start();
    }
```

### Reselect helpers (scroll-to-top)
```java
    private void scrollHomeToTop() {
        // If MainActivity hosts a RecyclerView, scroll it to position 0
        Intent i = new Intent("com.WANGDULabs.VOXA.SCROLL_TO_TOP");
        i.putExtra("tab", "HOME");
        LocalBroadcastManager.getInstance(activity).sendBroadcast(i);
    }
    // Implement similar for other tabs
```

---

## 4. Per-Tab Backstack Persistence (Optional)

If you want backstacks to survive process death, persist to SharedPreferences:

```java
private void saveBackstacks() {
    SharedPreferences prefs = activity.getSharedPreferences("footer_backstacks", MODE_PRIVATE);
    SharedPreferences.Editor ed = prefs.edit();
    for (Tab t : Tab.values()) {
        Stack<Class<?>> stack = backstacks.get(t);
        StringBuilder sb = new StringBuilder();
        for (Class<?> c : stack) sb.append(c.getName()).append(',');
        ed.putString(t.name(), sb.toString());
    }
    ed.putString("currentTab", currentTab.name());
    ed.apply();
}

private void restoreBackstacks() {
    SharedPreferences prefs = activity.getSharedPreferences("footer_backstacks", MODE_PRIVATE);
    for (Tab t : Tab.values()) {
        String serialized = prefs.getString(t.name(), "");
        Stack<Class<?>> stack = backstacks.get(t);
        stack.clear();
        if (!serialized.isEmpty()) {
            for (String cls : serialized.split(",")) {
                try {
                    stack.push(Class.forName(cls));
                } catch (ClassNotFoundException ignored) {}
            }
        }
    }
    String cur = prefs.getString("currentTab", Tab.HOME.name());
    currentTab = Tab.valueOf(cur);
}
```

Call `saveBackstacks()` in `onStop` and `restoreBackstacks()` in `onCreate`.

---

## 5. Deep Link Edge Cases

- **App already running**: `onNewIntent` will be called; handle it to switch tabs.
- **Multiple deep links in sequence**: Use `Intent.FLAG_ACTIVITY_CLEAR_TOP` to avoid stacking multiple MainActivity instances.
- **Invalid paths**: Default to Home tab.

---

## 6. Testing Checklist

- [ ] Clicking each footer icon updates the active icon and navigates to the correct root.
- [ ] Re-selecting a tab scrolls its list to top or pops to root if already scrolled.
- [ ] Deep link `voxa://vox` opens Vox tab.
- [ ] Deep link `voxa://profile/abc123` opens ProfileActivity for uid abc123 and keeps Home tab selected.
- [ ] Backstack per tab works: navigate Home→Profile→Chat, switch to Games, then back to Home returns to Chat (or root depending on design).
- [ ] Badge counters on NOTIF and VOX update via `FooterController.setBadge(tab, count)`.

---

## 7. Integration with MVVM

- Activities should expose a static method `scrollToTop()` that FooterController can invoke via LocalBroadcast or an interface.
- ViewModels should not know about Footer; UI layer forwards tab changes.
- For deep links that require data (e.g., profile uid), pass extras via Intent; ViewModel reads extras from Activity.

---

## 8. Optional: Jetpack Navigation + BottomNavigationView

If you prefer to adopt Jetpack Navigation later, the same routing map (`navigation-map.json`) can be expressed in a NavGraph, and BottomNavigationView can replace FooterController. The current FooterController approach keeps the project Java-only and lightweight.
