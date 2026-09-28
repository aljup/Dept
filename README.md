# تطبيق ديوني (Duyuni) 📱💰
### النظام المالي الذكي لإدارة الديون والالتزامات الشخصية والتجارية

![Version](https://img.shields.io/badge/Version-2.5.0-emerald.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)
![Database](https://img.shields.io/badge/Database-Room%20(Local%20Encrypted)-orange.svg)

---

## 📖 نبذة عن التطبيق
**تطبيق ديوني** هو تطبيق أندرويد متكامل وعصري صُمم خصيصاً لمساعدة الأفراد وأصحاب الأعمال والأنشطة التجارية الصغيرة على متابعة وإدارة ديونهم والتزاماتهم المالية بكل دقة وسهولة وأمان. يعتمد التطبيق على مبدأ **الخصوصية والتخزين المحلي 100%** حيث تُحفظ كافة السجلات والبيانات محلياً على جهازك في قاعدة بيانات مشفرة دون الحاجة للاتصال بالإنترنت.

---

## ✨ أبرز الميزات الرئيسية

### 1. 👑 التهيئة الأولى وإدارة المشرف
* **إعداد حساب المشرف عند الفتح لأول مرة**: شاشة مخصصة لتعيين اسم المشرف، البريد الإلكتروني، كلمة المرور مع مؤشر القوة، رمز القفل السريع (PIN)، والعملة الافتراضية.
* **لوحة تحكم المشرف (Admin Panel)**: إدارة جميع المستخدمين، إعادة تعيين رموز PIN، التحكم بكافة المعاملات والأشخاص.

### 2. 🔐 الأمان والمصادقة المتقدمة
* **تسجيل دخول آمن**: مصادقة بكلمة المرور المشفرة بتقنية SHA-256.
* **قفل التطبيق السريع (App Lock)**: قفل التطبيق برمز PIN مكوّن من 4 أرقام بنقرة واحدة، مع شاشة فتح فورية.
* **زر خصوصية الأرصدة (👁️)**: إخفاء وإظهار الأرقام والمبالغ المالية في لوحة التحكم لحماية الخصوصية أمام الآخرين.

### 3. 📊 لوحة تحكم ذكية ومؤشرات مالية عصرية
* **بطاقة المركز المالي الصافي (Net Balance)**: عرض فوري لصافي الوضع المالي (فائض مستحق لك / التزامات عليك) بتدرج لوني ديناميكي.
* **مؤشر نسبة التحصيل والسداد**: شريط تقدم تفاعلي يوضح نسبة المعاملات المنجزة.
* **بطاقات إحصائيات مفصلة**: إجمالي ما لك (دائن)، إجمالي ما عليك (مدين)، وعدد العمليات النشطة والمسددة.
* **رسم بياني تفاعلي (Canvas Chart)**: توزيع مرئي للديون والمستحقات حسب النوع والتصنيف.

### 4. 👥 دليل الأشخاص وكشوفات الحساب
* **سجل كامل لجهات التعامل**: إضافة وتعديل الأشخاص مع أرقام الهواتف والملاحظات.
* **كشف حساب تفصيلي لكل شخص**:
  * ملخص مالي فوري (ما لك، ما عليك، الصافي).
  * قائمة بالديون المرتبطة مع المبلغ الأصلي، المسدد، والمتبقي بدقة.
  * سجل زمني لكافة الدفعات المسددة.
  * مشاركة كشف الحساب نصياً أو تصديره.

### 5. 💳 إدارة الديون والدفعات
* **تسجيل ديون مرنة**: دعم الديون المستحقة لك (دائن) أو عليك (مدين)، مع تحديد الفئات (شخصي، عمل، عائلي، سلفة...).
* **تسجيل دفعات جزئية وكلية**: تحديث فوري للمبلغ المتبقي فور تسجيل الدفعة مع تحديد طريقة الدفع (نقدي، تحويل بنكي، شيك...).
* **تحديد تواريخ الاستحقاق والتنبيهات**: إشعارات ذكية تلقائية قبل موعد الاستحقاق وعند التأخر.

### 6. 💾 النسخ الاحتياطي والاستعادة المتقدمة
* **تصدير كامل للبيانات (JSON)**: حفظ نسخة احتياطية شاملة لكافة المستخدمين، الأشخاص، الديون، والدفعات مع بصمة تحقق وتاريخ الإنشاء.
* **استعادة ذكية مع معاينة مسبقة**:
  * **وضع الدمج (Merge Mode)**: إضافة البيانات غير الموجودة وتحديث التغييرات دون حذف البيانات الحالية.
  * **وضع الاستبدال الكامل (Replace Mode)**: استعادة النسخة الاحتياطية بدقة مطابقة.

---

## 🛠️ البنية التقنية (Tech Stack)

* **لغة البرمجة**: Kotlin 2.0+
* **واجهة المستخدم**: Jetpack Compose مع نظام التصميم Material Design 3 (M3).
* **معمارية التطبيق**: Clean Architecture + MVVM (Model-View-ViewModel).
* **إدارة الحالة**: Kotlin Coroutines & StateFlow / SharedFlow.
* **قاعدة البيانات**: Room Database (SQLite) مع KSP.
* **التخزين الآمن**: Encrypted Preferences & SharedPreferences للجلسات.
* **التنبيهات**: AlarmManager و WorkManager لجدولة إشعارات الاستحقاق اليومية.
* **التوافق**: Android 8.0 (API 26) فما فوق مع دعم كامل لنظام Android 14+ و Edge-to-Edge.

---

## 📁 هيكلية المشروع (Project Structure)

```
app/src/main/java/com/example/
├── data/
│   ├── dao/             # Room DAOs (UserDao, DebtDao, PersonDao, PaymentDao)
│   ├── local/           # Room Database definition & migrations
│   ├── model/           # Data Entities (User, Debt, Person, Payment, Category)
│   ├── preferences/     # SessionManager & user preferences
│   └── repository/      # Repository implementations & BackupManager
├── notification/        # NotificationHelper & DebtReminderScheduler
├── ui/
│   ├── components/      # Reusable UI components & Dialogs
│   ├── screens/
│   │   ├── admin/       # AdminPanelScreen
│   │   ├── auth/        # AdminSetup, Welcome, Login, Register, PinUnlock
│   │   ├── dashboard/   # Modern DashboardScreen
│   │   ├── debt/        # AddEditDebtScreen, DebtDetailsScreen
│   │   ├── ledger/      # TransactionsLedgerTab
│   │   ├── payments/    # PaymentsLogTab
│   │   └── persons/     # PersonsManagementTab, PersonDetailsScreen
│   └── theme/           # Color palette, Typography, Theme definition
└── viewmodel/           # ViewModels (Auth, Debt, Person, Admin)
```

---

## 🚀 التشغيل والبناء (Build & Run)

1. استنسخ المستودع:
   ```bash
   git clone https://github.com/username/duyuni-android.git
   ```
2. افتح المشروع في **Android Studio** (Ladybug / Iguana أو أحدث).
3. دع Gradle يقوم بمزامنة التبعيات (Gradle Sync).
4. اضغط **Run** على جهاز أو محاكي يعمل بنظام أندرويد 8.0 فما فوق.

---

## 📄 الترخيص (License)
هذا المشروع مفتوح المصدر ومخصص للاستخدام الشخصي والتطوير المالي تحت رخصة [MIT License](LICENSE).
