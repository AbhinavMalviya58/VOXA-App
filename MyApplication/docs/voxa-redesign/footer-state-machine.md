# FooterController 2.0 – State Machine

States: HOME, VOX, VIBE, GAMES, NOTIF

- Entry: select(tab)
  - Set selected icon
  - Navigate if tab != current (singleTop)
  - Animate pulse
- Re-select on same tab
  - Scroll-to-top broadcast for current list
  - If at root, no-op; else pop to root of tab stack
- Cross-tab navigation
  - Maintain per-tab backstacks
  - On switch, save current tab's stack
  - Restore stack when returning

Events:
- onTabClick(tab)
- onDeepLink(route)
- onBackPressed()

Transitions:
- onTabClick: current->target (slideLeft)
- onDeepLink: push route to appropriate tab
- onBackPressed: pop; if stack empty, moveTaskToBack()

Badges:
- Provide setBadge(tab, count) API
- NOTIF shows unread notifications count
- VOX shows new posts count
