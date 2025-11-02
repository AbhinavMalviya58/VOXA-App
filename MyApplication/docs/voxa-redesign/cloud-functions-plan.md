# Cloud Functions Plan

- onFollowCreate: award XP to followed user and follower; create notification to target.
- onFollowDelete: optional adjustments.
- onMessageCreate: already implemented push; also bump conversation updatedAt/lastMessage server-side.
- onPostCreate: write denormalized author fields; notify followers; award XP.
- onLikeCreate: notify post author; award small XP to liker and author.
- onDailyLogin: callable function to award streak XP.
- onGameMatchResult: HTTPS callable to write match results, update leaderboards, and XP.

Data integrity:
- All XP/level writes from Functions only (admin SDK bypasses rules).
- Maintain counters via transactions/batches.
