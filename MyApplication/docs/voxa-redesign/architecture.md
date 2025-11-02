# VOXA Architecture (MVVM)

- presentation/
  - screens/ (activities + fragments per feature)
  - adapters/ (UI adapters)
  - components/ (reusable views)
  - navigation/ (FooterController 2.0, deep links)
- domain/
  - model/ (plain models)
  - usecase/ (XP award, Follow toggle, Search users, Send message)
- data/
  - repository/ (interfaces + impls)
  - remote/
    - firestore/ (queries, mappers)
    - rtdb/
    - storage/
  - local/ (prefs, cache)

Guidelines:
- Activities/Fragments are thin; ViewModels hold state and async work.
- Repositories orchestrate between Firestore/RTDB/Storage.
- Use LiveData or Flow for UI state; keep Java-friendly LiveData.
- Pagination via query cursors; avoid heavy listeners in Activities.
- Centralize Firebase references and paths in data layer.

Migration notes:
- Introduce ViewModels for Main, Profile, Chat, Notifications.
- Move follow/chat/search logic from Activities into repositories/usecases.
- Keep SocialRepository; split into FollowRepository and RequestsRepository if it grows.
