# Release Config

- In app/build.gradle(.kts):
  - buildTypes.release { minifyEnabled true; shrinkResources true }
  - proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
- R8 rules additions:
  - Keep Firebase model classes where reflection is used
  - Keep Glide generated classes
- Enable Crashlytics mapping file upload (plugin already applied)
- Optimize images (webp where possible)
