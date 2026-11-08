# CipherVault R8 rules
#
# Order matters less than coverage: the blocks below are all "keep" rules for code
# reached through JNI, reflection or the system service entry points, which R8
# cannot see from a normal call graph.

# --- Diagnostics: keep stack traces readable, mapping stays out of the APK ---------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# --- SQLCipher: the .so resolves Java classes/methods by name over JNI ------------
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# --- argon2kt: same JNI name lookup for Argon2id ---------------------------------
-keep class com.lambdapioneer.argon2kt.** { *; }
-dontwarn com.lambdapioneer.argon2kt.**

# Keep every class that declares a native method (covers future crypto providers).
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# --- ZXing: reader/writer sets are assembled from BarcodeFormat at runtime --------
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# --- Room: generated Database_Impl is instantiated reflectively by Room -----------
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-dontwarn androidx.room.paging.**

# --- Components the framework instantiates by class name --------------------------
# AutofillService + its auth Activity are entered from the system, not from our own
# call graph, so a rename here silently disables credential filling.
-keep class com.ciphervault.app.CipherVaultApp
-keep class com.ciphervault.app.ui.MainActivity
-keep class com.ciphervault.app.autofill.VaultAutofillService
-keep class com.ciphervault.app.autofill.AutofillAuthActivity

# --- AndroidX consumer rules that still need a nudge under full-mode R8 -----------
-keep class androidx.biometric.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
