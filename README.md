# 🩺 Config Doctor | دکتر کانفیگ

<div align="center">

### Your Configurations Deserve Better.

**Your Smart Companion for Diagnosing, Testing, and Healing Proxy Configurations.**

Tired of unstable configurations, confusing connection tests, and manually searching for better IPs?

Meet **Config Doctor** — a smart Android application designed to diagnose, analyze, and improve your VLESS, VMess, and Trojan configurations.

**Diagnose. Analyze. Heal.**

[🇮🇷 فارسی](#-دکتر-کانفیگ--config-doctor) • [📥 Download](https://github.com/Mpyj/ConfigDoctor/releases) • [🐛 Report a Bug](https://github.com/Mpyj/ConfigDoctor/issues)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-Mpyj%20Attribution-orange)](LICENSE)

</div>

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Why Config Doctor?](#-why-config-doctor)
- [Features](#-features)
- [How It Works](#-how-it-works)
- [Auto-Healing](#-auto-healing)
- [Supported Protocols](#-supported-protocols)
- [Companion Tools](#-companion-tools)
- [Privacy & Security](#-privacy--security)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Installation](#-installation)
- [Build from Source](#-build-from-source)
- [Usage](#-usage)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)

---

## 🌟 Overview

**Config Doctor** is an Android application built to make proxy configuration diagnostics and optimization easier.

Supporting **VLESS, VMess, and Trojan**, it provides a collection of diagnostic tools to help users understand the condition of their configurations, identify connectivity problems, and find potentially better-performing alternatives.

But Config Doctor is more than just a ping tester.

Its multi-stage diagnostic engine evaluates different aspects of a connection, while its automatic healing system generates alternative configurations using Cloudflare IP addresses, tests them in parallel, and selects the best-performing candidate.

Whether you're dealing with unstable connections, high delays, or configurations that don't perform as expected, Config Doctor gives you better insight into what's happening.

### 🎯 Our Mission

To make configuration testing, troubleshooting, and optimization simpler, smarter, and more accessible.

---

## 💡 Why Config Doctor?

Have you ever wondered:

- Why does your configuration connect but perform poorly?
- Is the problem related to TCP connectivity or TLS?
- How stable is your connection?
- Are you experiencing packet loss or high jitter?
- Could another Cloudflare IP provide a better result?

Config Doctor brings several diagnostic capabilities together in one application.

Instead of relying on a single connectivity result, you can examine multiple performance indicators and use automatic healing to search for better alternatives.

**Less guessing. More information. Smarter diagnostics.**

---

## ✨ Features

### 🔍 Smart Diagnosis

A multi-stage diagnostic engine designed to examine different aspects of proxy connectivity.

- **TCP Test:** Checks TCP connectivity and measures response time.
- **TLS Test:** Evaluates TLS connectivity and related parameters, including SNI.
- **Real Delay Test:** Measures actual connection delay using the integrated Xray-based core.
- **Packet Loss Test:** Evaluates packet delivery and connection stability.
- **Jitter Analysis:** Measures fluctuations in connection response times.

### ⚡ Automatic Configuration Healing

Why stop at identifying a problem when you can try to improve the configuration?

Config Doctor's Auto-Healing system:

- Generates alternative configurations using different Cloudflare IP addresses.
- Creates multiple modified configuration candidates.
- Tests generated candidates in parallel.
- Compares diagnostic results.
- Selects and returns the best-performing candidate.

**Current limitation:** The healing system currently focuses on Cloudflare IP replacement. Other configuration parameters are not automatically modified. Additional healing methods are planned for future development.

### 📦 Subscription Support

- Import configurations through subscription URLs.
- Retrieve multiple configurations.
- Parse supported configuration formats.
- Manage imported configurations.

### 📊 Connection Quality Analysis

Understand more than just whether a connection works.

- TCP response time.
- TLS connectivity results.
- Real connection delay.
- Packet loss measurements.
- Jitter information.

### 📷 QR Code Scanner

- Scan configuration QR codes directly using your phone camera.
- Extract supported configuration links.
- Import configurations without manually copying links.

### 📜 History Management

- Save previous diagnostic results.
- Access previously tested configurations.
- Keep track of healed configurations.
- Review previous testing information.

### 🎨 Modern User Interface

- Jetpack Compose-based interface.
- Material Design 3.
- Dark and Light themes.
- Automatic theme selection based on time.
- Manual theme selection.

### 🌐 Bilingual Experience

- Full Persian and English support.
- RTL support for Persian.
- LTR support for English.
- Language-aware layouts.

---

## ⚙️ How It Works

Config Doctor follows a straightforward diagnostic workflow:

```text
       Import Configuration
                |
                v
          Parse Config
                |
                v
         Run Diagnostics
                |
                v
        Analyze Test Results
                |
                v
        Is Healing Needed?
                |
          +-----+-----+
          |           |
         No          Yes
          |           |
          v           v
       Results    Auto-Healing
                      |
                      v
               Generate Candidates
                      |
                      v
                 Parallel Tests
                      |
                      v
                Compare Results
                      |
                      v
                Select Best One
                      |
                      v
              Return Configuration
```

---

## ⚡ Auto-Healing

One of the key features that makes Config Doctor different is its automatic configuration healing system.

The goal is simple: help users find a better-performing alternative instead of merely reporting that something is wrong.

### Current Healing Process

1. Receive the original configuration.
2. Generate alternative Cloudflare IP addresses.
3. Create multiple modified configuration candidates.
4. Test candidates in parallel.
5. Compare their diagnostic performance.
6. Select the best-performing candidate.
7. Return the improved configuration.

### Future Development

Cloudflare IP replacement is the current healing method.

More diagnostic and healing strategies may be introduced in future releases.

---

## 🔌 Supported Protocols

| Protocol | Support |
|---|---|
| VLESS | ✅ |
| VMess | ✅ |
| Trojan | ✅ |

Config Doctor supports individual configuration links and subscription sources.

---

## 🤝 A Companion to Your Favorite Proxy Clients

**Config Doctor is designed to complement proxy clients, not replace them.**

It works alongside tools such as:

- **v2rayNG**
- **PattNG**

Proxy clients handle connections and traffic management. Config Doctor focuses on configuration diagnostics, performance analysis, and automatic optimization.

### Recommended Workflow

1. Import your configuration into Config Doctor.
2. Run diagnostic tests.
3. Examine connection quality and performance.
4. Use Auto-Healing when needed.
5. Use the resulting configuration in your preferred compatible proxy client.

**Important:** Config Doctor does not provide its own VPN servers and is not a replacement for proxy clients.

---

## 🔐 Privacy & Security

Your configurations should remain under your control.

Config Doctor is designed with local processing in mind.

- **Local Processing:** Configuration parsing, diagnostics, and healing operations are performed on the user's device.
- **No Configuration Upload:** Configuration data is not uploaded to an external server by the application.
- **Local History:** Diagnostic history and application preferences are stored locally.
- **Network Communication:** Connectivity tests necessarily communicate with destination servers and testing endpoints.

---

## 🛠 Technology Stack

Config Doctor is built with Kotlin and modern Android development technologies.

| Technology | Purpose |
|---|---|
| Kotlin 2.0.21 | Main Programming Language |
| Jetpack Compose | UI Framework |
| Material 3 | UI Components |
| MVVM | Application Architecture |
| Room Database | Local History |
| DataStore | Preferences |
| CameraX | Camera Integration |
| Google ML Kit | QR / Barcode Scanning |
| Java Networking APIs | HTTP and TCP Networking |
| libv2ray | Real Delay Testing |
| GitHub Actions | Build Automation |

### Core Library

[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)

---

## 📂 Project Structure

Main source code organization:

```text
app/src/main/java/com/configdoctor/
│
├── data/
│   ├── database/
│   ├── preferences/
│   └── models/
│
├── diagnose/
│   ├── Diagnostician.kt
│   ├── TcpTester.kt
│   ├── TlsTester.kt
│   ├── PacketLossTester.kt
│   ├── XrayRealDelayTester.kt
│   └── CloudflareCleanIps.kt
│
├── model/
│
├── parser/
│
├── ui/
│   ├── components/
│   └── screens/
│
└── MainActivity.kt
```

*This is a simplified representation of the main application packages.*

---

## 📥 Installation

### Option 1: Download APK

Download the latest available version from GitHub Releases.

**[➡️ Download Config Doctor](https://github.com/Mpyj/ConfigDoctor/releases)**

Available APK variants:

| APK | Description |
|---|---|
| `app-arm64-v8a-release.apk` | Most modern 64-bit Android devices |
| `app-armeabi-v7a-release.apk` | Compatible 32-bit ARM devices |
| `app-universal-release.apk` | Universal package supporting the included architectures |

**Recommended:** If you're unsure about your device architecture, use the Universal APK.

### Option 2: Build from Source

Clone the repository:

```bash
git clone https://github.com/Mpyj/ConfigDoctor.git
cd ConfigDoctor
```

Build the Debug APK:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK will be available at:

```text
app/build/outputs/apk/debug/
```

---

## 🚀 Usage

1. Open Config Doctor.
2. Import a VLESS, VMess, or Trojan configuration.
3. Start the diagnostic process.
4. Review TCP, TLS, Real Delay, Packet Loss, and Jitter results.
5. Use Auto-Healing if you want to explore alternative configurations.
6. Allow the application to generate and test Cloudflare IP alternatives.
7. Select the best-performing candidate.
8. Use the resulting configuration in your preferred compatible proxy client.

---

## 🗺 Roadmap

Config Doctor is continuously evolving.

- [x] VLESS support
- [x] VMess support
- [x] Trojan support
- [x] TCP diagnostics
- [x] TLS diagnostics
- [x] Real Delay testing
- [x] Packet Loss analysis
- [x] Cloudflare IP-based Auto-Healing
- [x] Subscription importing
- [x] QR Code scanning
- [x] Diagnostic history
- [x] Dark and Light themes
- [x] Persian and English support
- [ ] Additional automatic healing methods
- [ ] More advanced diagnostic capabilities
- [ ] Further performance improvements

*The roadmap may change as development progresses.*

---

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are welcome!

To contribute:

1. Fork the repository.
2. Create a new branch.
3. Make your changes.
4. Test your implementation.
5. Submit a Pull Request.

For bug reports and feature requests:

[**GitHub Issues**](https://github.com/Mpyj/ConfigDoctor/issues)

Please preserve the original project attribution and license notices in all redistributed or modified versions.

---

## 📜 License

**Config Doctor is distributed under the Mpyj Attribution License v1.0.**

This is a custom source-available license requiring mandatory attribution to the original developer.

You are allowed to:

- Use the project for personal and commercial purposes.
- Study and modify the source code.
- Create derivative works.
- Redistribute original and modified versions.

However, attribution is mandatory.

Any redistribution or derivative work must:

1. Credit the original developer: **Mpyj**.
2. Identify the original project: **Config Doctor**.
3. Link to the original repository: https://github.com/Mpyj/ConfigDoctor
4. Preserve the original copyright and license notices.
5. Clearly identify modifications.

Removing, hiding, or misrepresenting the original attribution is prohibited.

**Note:** This is a custom attribution-based license and is not an OSI-approved Open Source license.

See the [`LICENSE`](LICENSE) file for the complete terms.

---

## 📬 Contact

Developed and maintained by **Mpyj**.

| Platform | Link |
|---|---|
| GitHub | [github.com/Mpyj](https://github.com/Mpyj) |
| Telegram | [@MpyjTelegram](https://t.me/MpyjTelegram) |
| X (Twitter) | [@Mpyj_X](https://x.com/Mpyj_X) |
| Repository | [Mpyj/ConfigDoctor](https://github.com/Mpyj/ConfigDoctor) |

---

<div align="center">

### Config Doctor

**Diagnose. Analyze. Heal.**

Made with ❤️ by Mpyj.

*Your Configurations Deserve Better.*

</div>

---

# 🇮🇷 دکتر کانفیگ | Config Doctor

<div align="center">

### کانفیگ‌های شما لایق عملکرد بهتری هستند!

**همراه هوشمند شما برای تست، عیب‌یابی و ترمیم کانفیگ‌های پروکسی**

تا حالا شده با کانفیگ‌های ناپایدار، تست‌های گیج‌کننده اتصال و جست‌وجوی دستی IPهای بهتر سروکله بزنی؟

تا حالا شده ندونی مشکل کانفیگت از اتصال TCP هست، TLS یا کیفیت مسیر ارتباطی؟

**Config Doctor اینجاست تا این کارها رو ساده‌تر کنه!**

یک ابزار هوشمند اندرویدی برای تست، تحلیل، عیب‌یابی و بهبود کانفیگ‌های VLESS، VMess و Trojan.

دیگه لازم نیست فقط به یک نتیجه پینگ ساده برای ارزیابی وضعیت اتصالت اکتفا کنی.

**تشخیص بده، تحلیل کن و برای ترمیم اقدام کن!**

[🇬🇧 English](#-config-doctor--دکتر-کانفیگ) • [📥 دانلود](https://github.com/Mpyj/ConfigDoctor/releases) • [🐛 گزارش مشکل](https://github.com/Mpyj/ConfigDoctor/issues)

</div>

---

## 📖 فهرست مطالب

- [معرفی پروژه](#-معرفی-پروژه)
- [چرا دکتر کانفیگ؟](#-چرا-دکتر-کانفیگ)
- [ویژگی‌ها](#-ویژگیها)
- [نحوه عملکرد](#-نحوه-عملکرد)
- [سیستم ترمیم خودکار](#-سیستم-ترمیم-خودکار)
- [پروتکل‌های پشتیبانی‌شده](#-پروتکلهای-پشتیبانیشده)
- [ابزارهای مکمل](#-ابزارهای-مکمل)
- [حریم خصوصی و امنیت](#-حریم-خصوصی-و-امنیت)
- [تکنولوژی‌ها](#-تکنولوژیهای-استفادهشده)
- [ساختار پروژه](#-ساختار-پروژه)
- [نصب و راه‌اندازی](#-نصب-و-راهاندازی)
- [نحوه استفاده](#-نحوه-استفاده)
- [نقشه راه](#-نقشه-راه)
- [مشارکت در توسعه](#-مشارکت-در-توسعه)
- [مجوز انتشار](#-مجوز-انتشار)
- [ارتباط با ما](#-ارتباط-با-ما)

---

## 🌟 معرفی پروژه

**Config Doctor** یک اپلیکیشن اندرویدی برای ساده‌تر کردن فرآیند تست، عیب‌یابی، تحلیل و بهبود کانفیگ‌های پروکسی است.

این برنامه با پشتیبانی از پروتکل‌های **VLESS، VMess و Trojan** مجموعه‌ای از ابزارهای تخصصی را برای بررسی وضعیت اتصال در اختیار کاربران قرار می‌دهد.

اما دکتر کانفیگ فقط یک ابزار تست پینگ نیست!

این برنامه با استفاده از چندین مرحله آزمایش، بخش‌های مختلف اتصال را بررسی می‌کند و با سیستم ترمیم خودکار خود، نسخه‌های جایگزین کانفیگ را با IPهای مختلف Cloudflare تولید و به‌صورت موازی آزمایش می‌کند تا بهترین نتیجه را پیدا کند.

چه با اتصال ناپایدار مواجه باشی، چه تأخیر بالا داشته باشی یا بخواهی عملکرد کانفیگت رو بهتر بشناسی، Config Doctor به کمکت میاد.

### 🎯 هدف ما

ساده‌تر، هوشمندتر و در دسترس‌تر کردن فرآیند تست، عیب‌یابی و بهینه‌سازی کانفیگ‌ها.

---

## 💡 چرا دکتر کانفیگ؟

تا حالا از خودت پرسیدی:

- چرا کانفیگم متصل می‌شه ولی عملکرد خوبی نداره؟
- مشکل از اتصال TCP هست یا TLS؟
- کیفیت و پایداری اتصالم چطوره؟
- آیا پکت‌لاس یا جیتر بالایی دارم؟
- آیا IP دیگری از Cloudflare می‌تونه نتیجه بهتری ارائه بده؟

Config Doctor این قابلیت‌ها رو در یک برنامه جمع کرده.

به‌جای تکیه بر یک تست ساده، می‌تونی چندین شاخص عملکردی رو بررسی کنی و با استفاده از قابلیت ترمیم خودکار، به دنبال جایگزین‌های بهتر باشی.

**حدس کمتر، اطلاعات بیشتر، عیب‌یابی هوشمندتر!**

---

## ✨ ویژگی‌ها

### 🔍 سیستم عیب‌یابی هوشمند

مجموعه‌ای از تست‌های تخصصی برای بررسی بخش‌های مختلف اتصال:

- **TCP Test:** بررسی اتصال TCP و اندازه‌گیری زمان پاسخ‌گویی.
- **TLS Test:** بررسی اتصال TLS و پارامترهای مرتبط از جمله SNI.
- **Real Delay Test:** اندازه‌گیری تأخیر واقعی اتصال با استفاده از هسته مبتنی بر Xray.
- **Packet Loss Test:** بررسی پایداری ارتباط و میزان ازدست‌رفتن بسته‌ها.
- **Jitter Analysis:** اندازه‌گیری نوسانات زمان پاسخ‌گویی.

### ⚡ سیستم ترمیم خودکار (Auto-Healing)

چرا فقط مشکل رو شناسایی کنیم، وقتی می‌تونیم برای بهبودش هم تلاش کنیم؟

سیستم Auto-Healing دکتر کانفیگ:

- IPهای جایگزین Cloudflare رو تولید می‌کنه.
- چندین نسخه اصلاح‌شده از کانفیگ می‌سازه.
- نسخه‌ها رو به‌صورت موازی آزمایش می‌کنه.
- نتایج تست‌ها رو مقایسه می‌کنه.
- بهترین نسخه از نظر عملکرد رو انتخاب و برمی‌گردونه.

**محدودیت فعلی:** سیستم ترمیم در حال حاضر فقط روی جایگزینی IPهای Cloudflare تمرکز داره و سایر پارامترهای کانفیگ رو به‌صورت خودکار تغییر نمی‌ده. روش‌های ترمیم بیشتر برای نسخه‌های آینده در نظر گرفته شده‌اند.

### 📦 پشتیبانی از ساب (Subscription)

- دریافت کانفیگ از طریق لینک ساب.
- دریافت چندین کانفیگ به‌صورت هم‌زمان.
- پردازش فرمت‌های پشتیبانی‌شده.
- مدیریت کانفیگ‌های واردشده.

### 📊 تحلیل کیفیت اتصال

فقط به متصل‌شدن یا نشدن کانفیگ اکتفا نکن!

- زمان پاسخ TCP.
- وضعیت اتصال TLS.
- تأخیر واقعی اتصال.
- میزان Packet Loss.
- اطلاعات Jitter.

### 📷 اسکنر QR Code

- اسکن مستقیم QR کانفیگ با دوربین گوشی.
- استخراج لینک‌های پشتیبانی‌شده.
- واردکردن سریع کانفیگ بدون کپی دستی.

### 📜 تاریخچه

- ذخیره نتایج تست‌های قبلی.
- دسترسی به کانفیگ‌های آزمایش‌شده.
- نگهداری کانفیگ‌های ترمیم‌شده.
- مشاهده اطلاعات تست‌های گذشته.

### 🎨 رابط کاربری مدرن

- طراحی با Jetpack Compose.
- استفاده از Material Design 3.
- پشتیبانی از تم تاریک و روشن.
- انتخاب خودکار تم بر اساس ساعت.
- امکان انتخاب دستی تم.

### 🌐 پشتیبانی از دو زبان

- زبان فارسی و انگلیسی.
- پشتیبانی از RTL و LTR.
- رابط کاربری سازگار با جهت نوشتار هر زبان.

---

## ⚙️ نحوه عملکرد

Config Doctor از یک فرآیند چندمرحله‌ای برای بررسی کانفیگ‌ها استفاده می‌کنه:

```text
       واردکردن کانفیگ
               |
               v
         پردازش کانفیگ
               |
               v
         اجرای تست‌ها
               |
               v
        تحلیل نتایج تست
               |
               v
       آیا نیاز به ترمیم است؟
               |
          +----+----+
          |         |
         خیر        بله
          |         |
          v         v
       نمایش     ترمیم خودکار
       نتایج         |
                     v
               ساخت نسخه‌ها
                     |
                     v
                تست موازی
                     |
                     v
               مقایسه نتایج
                     |
                     v
                انتخاب بهترین
                     |
                     v
             بازگرداندن کانفیگ
```

---

## ⚡ سیستم ترمیم خودکار

یکی از قابلیت‌های اصلی و متمایزکننده Config Doctor، سیستم ترمیم خودکار آن است.

هدف ساده است: به‌جای اینکه فقط اعلام کنیم یک کانفیگ عملکرد مناسبی ندارد، تلاش کنیم جایگزین بهتری برای آن پیدا کنیم.

### فرآیند فعلی ترمیم

1. دریافت کانفیگ اولیه.
2. تولید IPهای جایگزین Cloudflare.
3. ساخت چندین نسخه اصلاح‌شده.
4. آزمایش موازی نسخه‌ها.
5. مقایسه نتایج عملکردی.
6. انتخاب بهترین نسخه.
7. بازگرداندن کانفیگ بهبودیافته.

### توسعه آینده

در نسخه فعلی، ترمیم خودکار بر پایه جایگزینی IPهای Cloudflare انجام می‌شه.

در نسخه‌های آینده، روش‌های بیشتری برای عیب‌یابی و ترمیم خودکار در نظر گرفته شده.

---

## 🔌 پروتکل‌های پشتیبانی‌شده

| پروتکل | وضعیت |
|---|---|
| VLESS | ✅ |
| VMess | ✅ |
| Trojan | ✅ |

امکان واردکردن کانفیگ به‌صورت تکی و از طریق لینک‌های Subscription وجود داره.

---

## 🤝 مکمل ابزارهای پروکسی شما

**قرار نیست Config Doctor جایگزین کلاینت‌های پروکسی بشه؛ بلکه قراره مکمل خوبی برای اون‌ها باشه!**

از جمله ابزارهایی که می‌تونن در کنار Config Doctor استفاده بشن:

- **v2rayNG**
- **PattNG**

کلاینت‌های پروکسی وظیفه برقراری و مدیریت اتصال رو برعهده دارن؛ اما Config Doctor روی بررسی کیفیت کانفیگ، عیب‌یابی، تحلیل عملکرد و پیدا کردن نسخه‌های بهتر تمرکز می‌کنه.

### روند پیشنهادی استفاده

1. کانفیگت رو وارد Config Doctor کن.
2. تست‌های عیب‌یابی رو اجرا کن.
3. کیفیت و عملکرد اتصال رو بررسی کن.
4. در صورت نیاز از Auto-Healing استفاده کن.
5. کانفیگ بهبودیافته رو در کلاینت پروکسی دلخواهت استفاده کن.

**نکته:** Config Doctor سرور VPN اختصاصی ارائه نمی‌ده و جایگزین کلاینت‌های اتصال پروکسی نیست.

---

## 🔐 حریم خصوصی و امنیت

کانفیگ‌های شما باید تحت کنترل خودتون باقی بمونن.

Config Doctor با تمرکز بر پردازش محلی طراحی شده:

- **پردازش محلی:** پردازش کانفیگ‌ها، عیب‌یابی و ترمیم روی دستگاه کاربر انجام می‌شه.
- **عدم آپلود کانفیگ:** اطلاعات کانفیگ‌ها توسط برنامه به سرور خارجی ارسال نمی‌شن.
- **تاریخچه محلی:** تاریخچه تست‌ها و تنظیمات برنامه روی دستگاه ذخیره می‌شن.
- **ارتباط شبکه:** تست‌های اتصال برای بررسی ارتباط، طبیعتاً با سرورهای مقصد و نقاط آزمایش ارتباط برقرار می‌کنن.

---

## 🛠 تکنولوژی‌های استفاده‌شده

Config Doctor با زبان Kotlin و فناوری‌های مدرن توسعه اندروید ساخته شده است.

| تکنولوژی | کاربرد |
|---|---|
| Kotlin 2.0.21 | زبان برنامه‌نویسی |
| Jetpack Compose | فریم‌ورک رابط کاربری |
| Material 3 | کامپوننت‌های رابط کاربری |
| MVVM | معماری برنامه |
| Room Database | ذخیره تاریخچه |
| DataStore | تنظیمات برنامه |
| CameraX | دسترسی به دوربین |
| Google ML Kit | اسکن QR و بارکد |
| Java Networking APIs | ارتباطات HTTP و TCP |
| libv2ray | تست Real Delay |
| GitHub Actions | خودکارسازی Build |

### کتابخانه اصلی

[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)

---

## 📂 ساختار پروژه

```text
app/src/main/java/com/configdoctor/
│
├── data/
│   ├── database/
│   ├── preferences/
│   └── models/
│
├── diagnose/
│   ├── Diagnostician.kt
│   ├── TcpTester.kt
│   ├── TlsTester.kt
│   ├── PacketLossTester.kt
│   ├── XrayRealDelayTester.kt
│   └── CloudflareCleanIps.kt
│
├── model/
│
├── parser/
│
├── ui/
│   ├── components/
│   └── screens/
│
└── MainActivity.kt
```

*این ساختار نمایی ساده‌شده از پکیج‌های اصلی پروژه است.*

---

## 📥 نصب و راه‌اندازی

### روش اول: دانلود APK

برای نصب آسان، آخرین نسخه برنامه رو از بخش Releases گیت‌هاب دریافت کن.

**[➡️ دانلود Config Doctor](https://github.com/Mpyj/ConfigDoctor/releases)**

نسخه‌های مختلف:

| فایل | توضیحات |
|---|---|
| `app-arm64-v8a-release.apk` | مناسب اکثر گوشی‌های مدرن ۶۴ بیتی |
| `app-armeabi-v7a-release.apk` | مناسب دستگاه‌های ARM سازگار ۳۲ بیتی |
| `app-universal-release.apk` | نسخه جامع با پشتیبانی از معماری‌های قرارگرفته در بسته |

**پیشنهاد:** اگر معماری گوشی خودت رو نمی‌دونی، نسخه Universal رو دانلود کن.

### روش دوم: ساخت از سورس

ابتدا مخزن پروژه رو دریافت کن:

```bash
git clone https://github.com/Mpyj/ConfigDoctor.git
cd ConfigDoctor
```

برای ساخت نسخه Debug:

```bash
./gradlew assembleDebug
```

در ویندوز:

```powershell
.\gradlew.bat assembleDebug
```

فایل APK ساخته‌شده در مسیر زیر قرار می‌گیره:

```text
app/build/outputs/apk/debug/
```

---

## 🚀 نحوه استفاده

1. برنامه Config Doctor رو باز کن.
2. یک کانفیگ VLESS، VMess یا Trojan وارد کن.
3. فرآیند عیب‌یابی رو شروع کن.
4. نتایج TCP، TLS، Real Delay، Packet Loss و Jitter رو بررسی کن.
5. در صورت نیاز از Auto-Healing استفاده کن.
6. اجازه بده برنامه نسخه‌های جایگزین با IPهای مختلف Cloudflare رو تولید و آزمایش کنه.
7. بهترین نتیجه رو انتخاب کن.
8. کانفیگ بهبودیافته رو در کلاینت پروکسی دلخواهت استفاده کن.

---

## 🗺 نقشه راه

Config Doctor همچنان در حال توسعه است و قابلیت‌های بیشتری برای آن در نظر گرفته شده.

- [x] پشتیبانی از VLESS
- [x] پشتیبانی از VMess
- [x] پشتیبانی از Trojan
- [x] تست TCP
- [x] تست TLS
- [x] تست Real Delay
- [x] تحلیل Packet Loss
- [x] ترمیم خودکار با IPهای Cloudflare
- [x] واردکردن Subscription
- [x] اسکن QR Code
- [x] تاریخچه تست‌ها
- [x] تم تاریک و روشن
- [x] پشتیبانی فارسی و انگلیسی
- [ ] روش‌های بیشتر ترمیم خودکار
- [ ] قابلیت‌های پیشرفته‌تر عیب‌یابی
- [ ] بهبودهای بیشتر عملکردی

*نقشه راه ممکن است در طول توسعه تغییر کند.*

---

## 🤝 مشارکت در توسعه

از مشارکت توسعه‌دهندگان، گزارش باگ‌ها و پیشنهاد قابلیت‌های جدید استقبال می‌کنیم!

برای مشارکت:

1. مخزن پروژه رو Fork کن.
2. یک Branch جدید بساز.
3. تغییراتت رو اعمال کن.
4. تغییرات رو آزمایش کن.
5. یک Pull Request ارسال کن.

برای گزارش مشکلات و پیشنهاد قابلیت‌ها:

[**GitHub Issues**](https://github.com/Mpyj/ConfigDoctor/issues)

لطفاً هنگام بازنشر یا توسعه نسخه‌های مشتق‌شده، انتساب سازنده اصلی و اطلاعیه‌های مجوز رو حفظ کن.

---

## 📜 مجوز انتشار (License)

پروژه Config Doctor تحت مجوز اختصاصی **Mpyj Attribution License v1.0** منتشر می‌شه.

این مجوز یک مجوز سفارشی مبتنی بر الزام ذکر نام سازنده اصلی است.

### چه کارهایی مجاز هستند؟

- استفاده شخصی و تجاری از پروژه.
- مشاهده و بررسی سورس کد.
- تغییر و توسعه سورس کد.
- ساخت نسخه‌های مشتق‌شده.
- بازنشر نسخه اصلی یا اصلاح‌شده.

### چه شرایطی باید رعایت شوند؟

**ذکر نام سازنده اصلی الزامی است.**

در هرگونه بازنشر، تغییر یا توسعه نسخه مشتق‌شده باید:

1. نام توسعه‌دهنده اصلی یعنی **Mpyj** به‌وضوح ذکر شود.
2. نام پروژه اصلی یعنی **Config Doctor** حفظ شود.
3. لینک مخزن اصلی درج شود: https://github.com/Mpyj/ConfigDoctor
4. اطلاعیه‌های حق نشر و متن مجوز حفظ شوند.
5. تغییرات انجام‌شده به‌وضوح مشخص شوند.

حذف، مخفی‌کردن یا تحریف انتساب سازنده اصلی مجاز نیست.

**توجه:** این یک مجوز اختصاصی مبتنی بر انتساب است و مجوز Open Source تأییدشده توسط OSI محسوب نمی‌شود.

برای مشاهده شرایط کامل، فایل [`LICENSE`](LICENSE) رو مطالعه کن.

---

## 📬 ارتباط با ما

توسعه‌دهنده و نگهدارنده پروژه: **Mpyj**

| پلتفرم | لینک |
|---|---|
| GitHub | [github.com/Mpyj](https://github.com/Mpyj) |
| Telegram | [@MpyjTelegram](https://t.me/MpyjTelegram) |
| X (Twitter) | [@Mpyj_X](https://x.com/Mpyj_X) |
| Repository | [Mpyj/ConfigDoctor](https://github.com/Mpyj/ConfigDoctor) |

---

<div align="center">

### Config Doctor

**تشخیص، تحلیل، ترمیم**

ساخته‌شده با ❤️ توسط Mpyj

*کانفیگ‌های شما لایق عملکرد بهتری هستند!*

</div>