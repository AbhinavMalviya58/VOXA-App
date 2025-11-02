# Package Refactor Plan

From:
- com.WANGDULabs.VOXA.ui.activities.* (heavy logic)
- com.WANGDULabs.VOXA.data.* (mixed concerns)

To:
- com.WANGDULabs.VOXA.presentation.home|vox|vibe|games|chat|profile|notifications
- com.WANGDULabs.VOXA.presentation.components
- com.WANGDULabs.VOXA.presentation.navigation
- com.WANGDULabs.VOXA.domain.model
- com.WANGDULabs.VOXA.domain.usecase
- com.WANGDULabs.VOXA.data.repository
- com.WANGDULabs.VOXA.data.remote.firestore|rtdb|storage
- com.WANGDULabs.VOXA.data.local

Steps:
1) Create new packages and migrate one feature at a time.
2) Introduce ViewModel per screen; move logic from Activity.
3) Extract Firebase access into data.remote with interfaces in repository.
4) Keep SocialRepository but decouple UI callbacks to repository callbacks or LiveData.
