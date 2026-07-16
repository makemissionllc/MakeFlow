-keepattributes SourceFile,LineNumberFile

# --- Room (reflection: _Impl classes loaded by RoomDatabase.Builder.build()) ---
-keep class **.*_Impl { *; }

# --- TFLite / LiteRT (JNI: native libtensorflowlite_jni.so calls into Java) ---
-keep class org.tensorflow.lite.** { *; }

# --- Glance (reflection: widget subclasses and ActionCallbacks loaded by framework) ---
-keep class com.makemission.makeflow.widget.** extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.makemission.makeflow.widget.** extends androidx.glance.appwidget.action.ActionCallback { *; }

# --- Gson (reflection: backup/restore JSON serialization) ---
-keepclassmembers class com.makemission.makeflow.data.backup.BackupData { *; }
-keepclassmembers class com.makemission.makeflow.data.local.entity.** { *; }
