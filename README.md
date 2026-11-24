# অফলাইন অনুবাদ (Offline Anubad)

ইন্টারনেট ছাড়া ৫৯টা ভাষায় অনুবাদের Android অ্যাপ। কোনো বিজ্ঞাপন নেই। অনুবাদ করে Google ML Kit, পুরোপুরি ফোনের ভেতরেই।

## ফিচার

- লিখে, বলে (ভয়েস) বা ক্যামেরায় স্ক্যান করে অনুবাদ
- গ্যালারির ছবি থেকে লেখা পড়া
- অনুবাদ শোনা (TTS), কপি, শেয়ার
- ভাষা প্যাক আগে থেকে নামানো ও মুছে ফেলা (উপরের ডান কোণে ⬇ বোতাম)
- হিস্টরি: লেখা দিয়ে খোঁজা, তারকা দেওয়া, তারিখ দিয়ে ছাঁকা
- হিস্টরি CSV ফাইলে এক্সপোর্ট (Excel-এ বাংলা ঠিকঠাক দেখায়)
- অন্য অ্যাপ থেকে লেখা শেয়ার করলে, বা লেখা সিলেক্ট করে "অনুবাদ করুন" চাপলে সরাসরি অনুবাদ
- ফোন বদলালে হিস্টরি Google ব্যাকআপ থেকে ফিরে আসে

## APK বানানোর ধাপ

1. কম্পিউটারে **Android Studio** ইন্সটল করুন: https://developer.android.com/studio
2. এই zip ফাইলটা unzip করুন।
3. Android Studio খুলে **File > Open** দিয়ে `OfflineAnubad` ফোল্ডারটা বেছে নিন।
4. নিচে **Gradle sync** শেষ হওয়া পর্যন্ত অপেক্ষা করুন (প্রথমবার ইন্টারনেট লাগবে, কয়েক মিনিট সময় নেবে)।
   - Android Studio যদি "upgrade AGP / Gradle" করতে বলে, **Upgrade** চাপতে পারেন।
5. মেনু থেকে **Build > Build App Bundle(s) / APK(s) > Build APK(s)** চাপুন।
6. APK ফাইলটা পাবেন এখানে: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub দিয়ে APK বানানো (কম্পিউটারে কিছু ইন্সটল না করে)

1. github.com-এ একটা ফ্রি অ্যাকাউন্ট খুলুন।
2. উপরের **+ > New repository** চাপুন। নাম দিন `OfflineAnubad`, তারপর **Create repository**।
3. **uploading an existing file** লিংকে চাপুন। unzip করা `OfflineAnubad` ফোল্ডারের **ভেতরের সব কিছু** টেনে এনে ছেড়ে দিন, তারপর **Commit changes** চাপুন।
4. `.github` ফোল্ডারটা লুকানো থাকে, তাই অনেক সময় আপলোড হয় না। আপলোড না হলে **Add file > Create new file** চাপুন, নামের ঘরে লিখুন `.github/workflows/build-apk.yml`, আর এই ফাইলের লেখাটা পেস্ট করে Commit করুন।
5. **Actions** ট্যাবে যান। "Build APK" চলবে (৫-১০ মিনিট)। সবুজ ✓ হলে সেটায় ঢুকে নিচে **Artifacts** থেকে `OfflineAnubad-apk` নামান।
6. নামানো zip খুললে `app-debug.apk` পাবেন। ফোনে পাঠিয়ে ইন্সটল করুন।

লাল ✗ এলে ওই রানে ঢুকে "Build the APK" ধাপের error লেখাটা কপি করে পাঠান।

## ফোনে ইন্সটল

**উপায় ১: ফাইল পাঠিয়ে**
`app-debug.apk` ফাইলটা USB, Google Drive বা Telegram দিয়ে ফোনে পাঠান, তারপর ট্যাপ করুন। "Install unknown apps" অনুমতি চাইলে Allow দিন।

**উপায় ২: কেবল দিয়ে সরাসরি**
ফোনে Settings > About phone > Build number-এ ৭ বার ট্যাপ করুন। তারপর Developer options থেকে USB debugging চালু করুন। ফোন কম্পিউটারে লাগিয়ে Android Studio-র সবুজ ▶ (Run) বোতাম চাপুন।

## প্রথমবার চালানো

- প্রথম অনুবাদের সময় অ্যাপ ভাষা প্যাক নামাতে বলবে। একবার ইন্টারনেট দিয়ে নামিয়ে নিন। প্রতিটা প্রায় ৩০ MB।
- অফলাইনে শুনতে চাইলে: সেটিংস > "অফলাইন ভয়েস ডেটা ইন্সটল করুন"।

## জেনে রাখুন

- **ভয়েস ইনপুটে ইন্টারনেট লাগে।** এটা Google-এর স্পিচ সার্ভিস ব্যবহার করে, মূল অ্যাপের মতোই।
- **ক্যামেরা বাংলা লেখা পড়তে পারে না।** Google ML Kit এখনো শুধু ইংরেজি/ল্যাটিন, চাইনিজ, হিন্দি (দেবনাগরী), জাপানি ও কোরিয়ান লিপি পড়ে। ইংরেজি সাইনবোর্ড বা মেনু স্ক্যান করে বাংলায় অনুবাদ ঠিকঠাক কাজ করবে।
- ইংরেজি ছাড়া দুটো ভাষার মধ্যে অনুবাদ (যেমন বাংলা → আরবি) ভেতরে ভেতরে ইংরেজির মধ্য দিয়ে যায়, তাই মান একটু কম হতে পারে।
- Google-এর নিয়ম অনুযায়ী অ্যাপে "Translated by Google" লেখাটা রাখতে হবে।

## Play Store-এ দিতে চাইলে

- `app/build.gradle.kts`-এ `applicationId` বদলে নিজের একটা নাম দিন (যেমন `com.yourname.anubad`)।
- **Build > Generate Signed App Bundle** দিয়ে release বিল্ড বানান।
- অ্যাপের নাম বদলাতে: `app/src/main/res/values/strings.xml`-এ `app_name`।

## কোডের গঠন

```
app/src/main/java/com/anubad/offline/
  MainActivity.kt        অ্যাপ শুরু, অন্য অ্যাপ থেকে শেয়ার করা লেখা নেওয়া
  MainViewModel.kt       সব লজিক: অনুবাদ, ডাউনলোড, হিস্টরি, সেটিংস
  data/
    TranslatorEngine.kt  Google ML Kit অনুবাদ ও ভাষা প্যাক
    Ocr.kt               ছবি থেকে লেখা পড়া
    Speaker.kt           লেখা পড়ে শোনানো (TTS)
    HistoryDb.kt         হিস্টরির ডাটাবেস (SQLite)
    Languages.kt         ভাষার তালিকা ও নাম
    Prefs.kt, Dates.kt
  ui/
    AppRoot.kt           ট্যাব, উপরের বার, স্ন্যাকবার
    TranslateScreen.kt   মূল অনুবাদ স্ক্রিন
    LanguageScreens.kt   ভাষা বাছাই ও ভাষা প্যাক ম্যানেজার
    HistoryScreen.kt     হিস্টরি
    SettingsScreen.kt    সেটিংস
    Dialogs.kt, Theme.kt, Intents.kt
```
