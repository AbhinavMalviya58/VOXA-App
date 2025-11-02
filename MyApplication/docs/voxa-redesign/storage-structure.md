# Storage Structure

- profile_images/{uid}.jpg
- banners/{uid}.jpg
- posts/{postId}/{fileName}
- chat_images/{conversationId}/{messageId}.jpg
- game_assets/{gameId}/{file}

Rules (outline):
- Users can write their own profile/banner images.
- Post media: only post author can write; public read.
- Chat images: participants only read/write.
