# Developer Handoff – VOXA 2.0

## Routes & Navigation
See navigation-map.json for full list. Tabs: home, vox, vibe, games, notif. Per-tab backstacks; deep links via voxa://.

## Firebase Schema
- Firestore: firestore-schema.json
- RTDB: rtdb-schema.json
- Storage: storage-structure.md
- Rules: firestore-rules-draft.rules (client-safe)

## XP System
- Thresholds: 1000 + level*500
- Triggers: posts, likes, follows, wins, daily login
- Server-only writes; see cloud-functions-plan.md

## Build & Release
- Enable minify and shrink; see release-config.md
- Glide & Firebase keep rules in proguard file

## Design
- Tokens: design-tokens.json
- Micro-animations: micro-animations.json
- Mockups: JSON layers TBD; export PNGs from design tool

## Refactor
- Package plan: package-refactor.md
- ViewModel adoption per feature, repositories for data

## Immediate Tasks
- Implement deep links (see deep-links.json)
- Wire FooterController 2.0 (state + reselect behavior)
- Port Activities to ViewModels incrementally
- Adopt rules and update client-side checks accordingly
