# 保留OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

# 保留Gson
-keep class com.google.gson.** { *; }
-keep class edu.hitsz.aircraftwar.dao.Score { *; }
