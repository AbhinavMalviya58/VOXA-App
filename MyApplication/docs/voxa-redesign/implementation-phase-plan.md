# VOXA 2.0 – Implementation Phase Plan

## Overview
This plan translates the VOXA 2.0 design deliverables into three focused sprints. Each sprint targets the core issues reported (DMs not working, follower/following not showing, XP/game status not updating, posts/news missing) and builds toward a stable MVVM + Firebase backend.

---

## Sprint 1: Security Rules Adoption & Testing

**Goal:** Ensure all Firestore/RTDB operations succeed and data is secure. Fix DMs, follow/follower visibility, and basic CRUD.

### Tasks
1. **Adopt finalized Firestore rules** (see finalized rules section below)
   - Replace `firebase/firestore.rules` with the final version.
   - Deploy via Firebase CLI: `firebase deploy --only firestore:rules`.
2. **Validate follow/following reads**
   - Test that `FollowersListActivity` and `FollowingListActivity` load data.
   - Verify `SocialRepository` can write/read `follows/{uid}/following|followers`.
3. **Fix DM gating**
   - Confirm `ChatDetailFirestoreActivity` respects mutual-follow rule.
   - Adjust UI to show “Follow to message” when blocked.
4. **Add minimal Storage rules** (profile/banner images)
   - Draft and apply `storage.rules` to allow users to write their own media.
5. **Add unit tests for rule paths**
   - Use Firebase emulators; write tests for follow, message, and profile paths.

### Rationale
- Rules are the root cause of “nothing works”: blocked reads/writes manifest as missing data and failed sends.
- Emulator tests give confidence before client changes.

---

## Sprint 2: Cloud Functions (XP, Notifications, Chat Metadata)

**Goal:** Server-side XP, push notifications, and conversation metadata so XP/level updates and notifications appear.

### Tasks
1. **Set up Functions project**
   - Initialize if needed: `firebase init functions`.
   - Use Node.js (existing) and add dependencies: `firebase-admin`, `firebase-functions`.
2. **Implement XP service**
   - `awardXP(uid, amount, reason)` callable function.
   - `onUserUpdate` trigger to recompute level when XP crosses thresholds.
   - Write `xp`/`level` via admin SDK (bypass rules).
3. **Add notification triggers**
   - `onFollowCreate` → write to `/notifications/{targetUid}/items`.
   - `onLikeCreate` → notify post author.
   - `onMessageCreate` → push via FCM to receiver (already exists).
   - `onLevelUp` → push “Level up!” and write client event for confetti.
4. **Conversation metadata on message**
   - Extend `onMessageCreate` to update `conversations/{cid}` with `lastMessage` and `updatedAt`.
   - Ensure participants array is set on first message.
5. **Game result logging**
   - Callable `logGameResult(uid, gameId, won, score)` to update leaderboards and award XP.
6. **Deploy and test**
   - `firebase deploy --only functions`.
   - Verify XP increments appear in Profile and level-up push is received.

### Rationale
- XP and notifications must be server-side to prevent cheating and to trigger push.
- Centralizing conversation metadata fixes “chat list not updating.”

---

## Sprint 3: MVVM Refactor (Home, Profile, Chat)

**Goal:** Move logic out of Activities, improve pagination, and prepare for future features (Vox feed, Vibe).

### Tasks
1. **Create new packages** (per package-refactor.md)
   - `presentation/home|profile|chat`
   - `domain/model`, `domain/usecase`
   - `data/repository` (Firestore, RTDB, Storage, Follow)
2. **Introduce ViewModels**
   - `HomeViewModel`: search users, feed placeholder, pagination.
   - `ProfileViewModel`: load profile data, follow/unfollow, edit profile.
   - `ChatViewModel`: conversation list, message send/receive, image upload.
3. **Implement Repositories**
   - `FirestoreRepository`: generic CRUD, typed queries.
   - `FollowRepository`: follow, unfollow, request, approve/decline.
   - `ChatRepository`: conversation metadata, messages, image upload.
   - `StorageRepository`: profile/banner, post media, chat images.
4. **Replace Activity logic**
   - Move Firebase listeners from Activities into Repositories.
   - Expose LiveData/Flow to ViewModels; Activities observe.
5. **Add pagination helpers**
   - Use startAfter/limit with cursors in Repositories.
   - Expose `loadMore()` in ViewModels.
6. **Wire FooterController 2.0**
   - Implement per-tab backstack and reselect behavior (see spec below).
   - Add deep link handling in MainActivity.
7. **Add placeholder screens**
   - VoxActivity (empty list), VibeActivity (curated reader), EditProfileActivity.
8. **Testing**
   - Unit test ViewModels (use LiveData test utilities).
   - Integration test Repository with emulators.

### Rationale
- MVVM eliminates “buggy” Activity-scoped listeners and enables testable state.
- Pagination prevents memory blow and prepares for real feeds.
- Footer 2.0 and deep links improve navigation UX.

---

## Success Criteria per Sprint
- **Sprint 1:** All data reads/writes succeed; DMs work only with mutual follow; followers lists load.
- **Sprint 2:** XP updates after wins/likes; push notifications arrive for follows/likes/level-ups; chat list shows latest message.
- **Sprint 3:** Activities are thin; ViewModels hold state; pagination works; footer navigation and deep links behave as designed.

---

## Risks & Mitigations
- **Rules lockout:** Deploy rules to a test project first; use emulator suite.
- **Functions cold starts:** Keep functions small; use region close to users.
- **MVVM thrash:** Migrate one screen at a time; keep old paths until new ones are verified.

---

## Next Steps after Sprint 3
- Implement Vox feed (posts, likes, comments) using new MVVM scaffolding.
- Add Vibe curated reader from Firestore collection.
- Expand multiplayer invites via RTDB lobbies and Functions matchmaking.
