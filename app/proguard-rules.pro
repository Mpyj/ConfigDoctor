# نگه داشتن کلاس‌های libv2ray و کدهای Go
-keep class libv2ray.** { *; }
-keep class go.** { *; }
-keep interface libv2ray.** { *; }

# نگه داشتن مدل‌های داده و دیتابیس (برای Room)
-keep class com.configdoctor.model.** { *; }
-keep class com.configdoctor.data.** { *; }

# نگه داشتن کلاس‌های Compose که ممکنه با reflection کار کنن
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# جلوگیری از حذف JSON (چون توی XrayRealDelayTester استفاده می‌شه)
-keep class org.json.** { *; }