# VOXA Backend (Express) – Free Hosting on Vercel or Render

## Deploy to Vercel

1. Install Vercel CLI: `npm i -g vercel`
2. Clone this backend folder or ensure it’s inside your project root.
3. Set environment variables (Vercel dashboard or CLI):
   - `FIREBASE_SERVICE_ACCOUNT_JSON`: Full JSON service account (as a string).
   - `FIREBASE_STORAGE_BUCKET`: Your Firebase Storage bucket name (`your-project.appspot.com`).
4. Deploy: `vercel --prod`
5. Note the deployed URL and update `VoxApi.java` `BASE_URL`.

## Deploy to Render

1. Create a new Web Service on Render pointing to this repo.
2. Set the same environment variables in Render dashboard.
3. Render will auto-deploy on push. Use the provided `.onrender.com` URL in `VoxApi.java`.

## Android Integration

- Use `VoxApi.java` for all API calls. It automatically attaches the Firebase ID token.
- Ensure `FirebaseAuth.getInstance().getCurrentUser().getIdToken()` works.
- Replace any direct Firebase Functions calls with `VoxApi` methods.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/follow` | Bearer token | Follow a user (updates counters, sends notification) |
| POST | `/unfollow` | Bearer token | Unfollow a user (updates counters) |
| POST | `/message` | Bearer token | Update conversation metadata and send FCM push |
| POST | `/awardXP` | Bearer token | Award XP and handle level-up notifications |
| POST | `/gameResult` | Bearer token | Log game result, award XP, update leaderboards |
| POST | `/post` | Bearer token | Create a text post (media upload via separate endpoint) |
| POST | `/like` | Bearer token | Like/unlike a post (updates counters, notifies author) |
| POST | `/comment` | Bearer token | Add a comment to a post |
| GET | `/feed` | Bearer token | Paginated feed of posts |
| GET | `/notifications` | Bearer token | Paginated notifications |
| POST | `/profile` | Bearer token | Update profile (supports avatar/banner via multipart) |
| GET | `/health` | none | Health check |

## Notes

- All write operations verify the caller’s UID from the Firebase ID token.
- Media upload uses `multer` and writes to Firebase Storage (public URLs).
- XP thresholds: start 1000, +500 per level.
- Follow and like actions award small XP and create notifications.
- FCM push notifications are sent for messages, follows, likes, and level-ups.

## Local Development

1. `npm install`
2. Set env vars in `.env` (for Render) or Vercel CLI.
3. `vercel dev` runs the server locally with the same routes.
4. Test with `http://localhost:3000/health`.

## Security

- Never expose `FIREBASE_SERVICE_ACCOUNT_JSON` on the client.
- The backend validates the Firebase ID token on every protected route.
- Firestore writes are still protected by security rules; this backend only handles side-effects (counters, XP, notifications).
