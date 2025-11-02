# MVVM Setup Guidance – VOXA 2.0

## Package Structure (per package-refactor.md)

```
com.WANGDULabs.VOXA
├─ presentation
│  ├─ home
│  │  ├─ HomeActivity.java
│  │  └─ HomeViewModel.java
│  ├─ profile
│  │  ├─ ProfileActivity.java
│  │  └─ ProfileViewModel.java
│  ├─ chat
│  │  ├─ ChatListActivity.java
│  │  ├─ ChatDetailActivity.java
│  │  └─ ChatViewModel.java
│  ├─ components
│  │  └─ reusable views (e.g., UserRowView)
│  └─ navigation
│     └─ FooterController.java (enhanced)
├─ domain
│  ├─ model/
│  │  └─ plain POJOs (User, Post, ChatMessage, GameResult)
│  └─ usecase/
│     ├─ AwardXpUseCase.java
│     ├─ ToggleFollowUseCase.java
│     └─ SendMessageUseCase.java
└─ data
   ├─ repository/
   │  ├─ FirestoreRepository.java
   │  ├─ FollowRepository.java
   │  ├─ ChatRepository.java
   │  ├─ StorageRepository.java
   │  └─ AuthRepository.java
   └─ local/
      └─ SharedPreferencesHelper.java
```

---

## Dependency Flow

```
UI (Activity/Fragment) → ViewModel → UseCase/Repository → Firebase/Local
```

- **UI**: Observes `LiveData`/`Flow`; forwards user events.
- **ViewModel**: Holds UI state, orchestrates use cases, survives config changes.
- **Repository**: Single source of truth; abstracts Firestore/RTDB/Storage.
- **UseCase**: Encapsulates business logic (XP award, follow toggle) that may span multiple repositories.

---

## ViewModels (Java + LiveData)

### HomeViewModel
```java
public class HomeViewModel extends ViewModel {
    private final UserRepository userRepo;
    private final FollowRepository followRepo;
    private final MutableLiveData<List<SearchUser>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeViewModel(UserRepository userRepo, FollowRepository followRepo) {
        this.userRepo = userRepo;
        this.followRepo = followRepo;
    }

    public LiveData<List<SearchUser>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> error() { return error; }

    public void searchUsers(String query) {
        isLoading.setValue(true);
        userRepo.searchByNameOrHandle(query, 20)
            .addOnSuccessListener(list -> {
                searchResults.setValue(list);
                isLoading.setValue(false);
            })
            .addOnFailureListener(e -> {
                error.setValue(e.getMessage());
                isLoading.setValue(false);
            });
    }

    public void toggleFollow(String targetUid) {
        followRepo.toggleFollow(targetUid);
    }
}
```

### ProfileViewModel
```java
public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepo;
    private final FollowRepository followRepo;
    private final MutableLiveData<User> profile = new MutableLiveData<>();
    private final MutableLiveData<String> followState = new MutableLiveData<>();
    private final MutableLiveData<List<User>> followers = new MutableLiveData<>();
    private final MutableLiveData<List<User>> following = new MutableLiveData<>();

    public ProfileViewModel(UserRepository userRepo, FollowRepository followRepo) {
        this.userRepo = userRepo;
        this.followRepo = followRepo;
    }

    public void loadProfile(String uid) {
        userRepo.getUser(uid).addOnSuccessListener(doc -> {
            profile.setValue(doc.toObject(User.class));
        });
    }

    public void refreshFollowState(String myUid, String otherUid) {
        followRepo.getFollowState(myUid, otherUid, state -> followState.setValue(state));
    }

    public void toggleFollow(String myUid, String otherUid) {
        followRepo.toggleFollow(myUid, otherIdx, success -> refreshFollowState(myUid, otherUid));
    }

    public LiveData<User> getProfile() { return profile; }
    public LiveData<String> getFollowState() { return followState; }
    public LiveData<List<User>> getFollowers() { return followers; }
    public LiveData<List<User>> getFollowing() { return following; }
}
```

### ChatViewModel
```java
public class ChatViewModel extends ViewModel {
    private final ChatRepository chatRepo;
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ChatViewModel(ChatRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    public void openConversation(String conversationId) {
        chatRepo.listenToMessages(conversationId, list -> messages.setValue(list));
    }

    public void sendMessage(String conversationId, String text) {
        chatRepo.sendMessage(conversationId, text);
    }

    public void sendImage(String conversationId, Uri imageUri) {
        chatRepo.sendImage(conversationId, imageUri);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<String> error() { return error; }
}
```

---

## Repositories (Firebase + Pagination)

### FirestoreRepository (generic)
```java
public class FirestoreRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<List<DocumentSnapshot>> queryPaginated(CollectionReference ref, DocumentSnapshot startAfter, int limit) {
        Query q = ref.limit(limit);
        if (startAfter != null) q = q.startAfter(startAfter);
        return q.get().continueWith(task -> {
            if (task.isSuccessful()) return task.getResult().getDocuments();
            throw task.getException();
        });
    }

    public Task<DocumentSnapshot> getDocument(String collection, String docId) {
        return db.collection(collection).document(docId).get();
    }

    public Task<Void> setDocument(String collection, String docId, Object data) {
        return db.collection(collection).document(docId).set(data);
    }
}
```

### FollowRepository
```java
public class FollowRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public void toggleFollow(String targetUid, ToggleCallback callback) {
        String myUid = auth.getUid();
        // Use SocialRepository logic or move here
        new SocialRepository().toggleFollowOrRequest(myUid, targetUid, (success, state, e) -> {
            callback.onResult(state, e);
        });
    }

    public void getFollowState(String myUid, String otherUid, FollowStateCallback callback) {
        // Combine follows and follow_requests reads
        // Return "following", "requested", "none"
    }

    public Task<List<DocumentSnapshot>> getFollowers(String uid, DocumentSnapshot startAfter, int limit) {
        return new FirestoreRepository().queryPaginated(
            db.collection("follows").document(uid).collection("followers"),
            startAfter, limit
        );
    }

    public Task<List<DocumentSnapshot>> getFollowing(String uid, DocumentSnapshot startAfter, int limit) {
        return new FirestoreRepository().queryPaginated(
            db.collection("follows").document(uid).collection("following"),
            startAfter, limit
        );
    }

    public interface ToggleCallback { void onResult(String state, Exception e); }
    public interface FollowStateCallback { void onState(String state); }
}
```

### ChatRepository
```java
public class ChatRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private ListenerRegistration messagesReg;

    public void listenToMessages(String conversationId, MessagesCallback callback) {
        Query q = db.collection("conversations").document(conversationId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING).limit(500);
        messagesReg = q.addSnapshotListener((snapshots, e) -> {
            if (e != null) return;
            List<ChatMessage> list = new ArrayList<>();
            for (DocumentSnapshot d : snapshots.getDocuments()) {
                list.add(d.toObject(ChatMessage.class));
            }
            callback.onMessages(list);
        });
    }

    public void sendMessage(String conversationId, String text) {
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", FirebaseAuth.getInstance().getUid());
        data.put("text", text);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("conversations").document(conversationId).collection("messages").add(data);
    }

    public void sendImage(String conversationId, Uri imageUri) {
        StorageReference ref = storage.getReference().child("chat_images")
                .child(conversationId).child(UUID.randomUUID().toString() + ".jpg");
        ref.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            Map<String, Object> data = new HashMap<>();
            data.put("senderId", FirebaseAuth.getInstance().getUid());
            data.put("imageUrl", uri.toString());
            data.put("createdAt", FieldValue.serverTimestamp());
            db.collection("conversations").document(conversationId).collection("messages").add(data);
        });
    }

    public void cleanup() { if (messagesReg != null) messagesReg.remove(); }

    public interface MessagesCallback { void onMessages(List<ChatMessage> messages); }
}
```

---

## UseCase Examples

### AwardXpUseCase
```java
public class AwardXpUseCase {
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    public void award(int amount, String reason, Callback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("reason", reason);
        functions.getHttpsCallable("awardXP").call(data)
            .addOnSuccessListener(r -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e));
    }

    public interface Callback {
        void onSuccess();
        void onError(Exception e);
    }
}
```

---

## Wiring in Activities (Example: HomeActivity)

```java
public class HomeActivity extends AppCompatActivity {
    private HomeViewModel viewModel;
    private SearchResultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate, bind views...
        UserRepository userRepo = new UserRepository();
        FollowRepository followRepo = new FollowRepository();
        viewModel = new ViewModelProvider(this, new HomeViewModelFactory(userRepo, followRepo)).get(HomeViewModel.class);

        EditText searchInput = findViewById(R.id.Search);
        searchInput.addTextChangedListener(new TextWatcher() {
            // Debounce and call viewModel.searchUsers(query)
        });

        RecyclerView results = findViewById(R.id.searchResults);
        adapter = new SearchResultsAdapter(this, user -> {
            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra("uid", user.getUid());
            startActivity(i);
        }, targetUid -> viewModel.toggleFollow(targetUid));
        results.setAdapter(adapter);

        viewModel.getSearchResults().observe(this, items -> adapter.setItems(items));
        viewModel.isLoading().observe(this, loading -> {/* show/hide loader */});
        viewModel.error().observe(this, err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }
}
```

---

## Migration Tips
- **One screen at a time**: Keep existing Activity logic until ViewModel/Repository is verified.
- **Keep SocialRepository** for now; wrap it with FollowRepository.
- **Use LiveData** (Java-friendly) instead of Flow to avoid library changes.
- **Unit tests**: Write tests for ViewModels using `androidx.arch.core:core-testing`.
- **Emulator suite**: Run local Firestore/RTDB emulators for repository integration tests.

---

## Next Steps after MVVM
- Add Vox feed (PostsRepository, PostViewModel).
- Add Vibe curated reader (VibeRepository, VibeViewModel).
- Implement FooterController 2.0 state machine with per-tab backstacks.
