# Notifications Mapping

Types:
- follow_request: data {requesterUid}
- follow_request_accepted: data {targetUid}
- post_like: data {postId, likerUid}
- post_comment: data {postId, commenterUid, commentId}
- chat_message: data {conversationId, senderId}
- game_invite: data {gameId, lobbyId, hostUid}
- level_up: data {level}

Sources:
- Functions onFollowCreate -> follow_request notification to target (if private) OR auto-follow and notify
- Functions onMessageCreate -> chat_message to receiver
- onLikeCreate -> post_like to author
- onPostCreate -> notify followers (optional)
- onGameInviteCreate -> game_invite
- onXPLevelUp -> level_up

Storage:
- /notifications/{uid}/items/{nid}: {type, data, createdAt, read:false}

Client:
- Listen to items (limit 50), group by type, show counters on NOTIF tab
- Mark as read on open
