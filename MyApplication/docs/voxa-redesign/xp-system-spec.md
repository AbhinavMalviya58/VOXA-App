# XP / Level System Spec

Thresholds:
- Level 1: 0-1000
- Each next level increases threshold by +500 (L(n) = 1000 + (n-1)*500)

Sources (server-side increments only):
- Post created: +10 XP
- Post liked (author): +2 XP
- Like a post (liker): +1 XP
- Follow someone: +5 XP
- Gain a follower: +5 XP
- Daily login streak: +10 * streak multiplier
- Game win: +25 XP; loss: +5 XP; draw: +10 XP

Storage:
- users/{uid}: xp, level, rank, totalWins, currentStreak, highestStreak

Behavior:
- On XP update: if xp >= threshold -> level++ and carry over remainder
- Trigger levelUpConfetti animation on client via notification
- Rank: computed periodically server-side (top N leaderboard)

Implementation:
- Cloud Functions: awardXP(uid, amount, reason)
- Callable endpoints for daily login & game results
- Security: client cannot write xp/level directly (rules block)
