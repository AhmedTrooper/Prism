#define UTIL_EXTERN
#include "jni_utils.h"

#include <jni.h>
#include <stdlib.h>

bool acquire_jni_env(JavaVM *vm, JNIEnv **env)
{
    int ret = vm->GetEnv((void**) env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED)
        return vm->AttachCurrentThread(env, NULL) == 0;
    else
        return ret == JNI_OK;
}

// Apparently it's considered slow to FindClass and GetMethodID every time we need them,
// so let's have a nice cache here.

void init_methods_cache(JNIEnv *env)
{
    static bool methods_initialized = false;
    if (methods_initialized)
        return;

    #define FIND_CLASS(name) reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(name)))
    java_Integer = FIND_CLASS("java/lang/Integer");
    java_Integer_init = env->GetMethodID(java_Integer, "<init>", "(I)V");
    java_Double = FIND_CLASS("java/lang/Double");
    java_Double_init = env->GetMethodID(java_Double, "<init>", "(D)V");
    java_Boolean = FIND_CLASS("java/lang/Boolean");
    java_Boolean_init = env->GetMethodID(java_Boolean, "<init>", "(Z)V");

    android_graphics_Bitmap = FIND_CLASS("android/graphics/Bitmap");
    // createBitmap(int[], int, int, android.graphics.Bitmap$Config)
    android_graphics_Bitmap_createBitmap = env->GetStaticMethodID(android_graphics_Bitmap, "createBitmap", "([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    android_graphics_Bitmap_Config = FIND_CLASS("android/graphics/Bitmap$Config");
    // static final android.graphics.Bitmap$Config ARGB_8888
    android_graphics_Bitmap_Config_ARGB_8888 = env->GetStaticFieldID(android_graphics_Bitmap_Config, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");

    prism_PrismLib = FIND_CLASS("com/ahmedtrooper/prism/PrismLib");
    prism_PrismLib_eventProperty_S  = env->GetStaticMethodID(prism_PrismLib, "eventProperty", "(Ljava/lang/String;)V"); // eventProperty(String)
    prism_PrismLib_eventProperty_Sb = env->GetStaticMethodID(prism_PrismLib, "eventProperty", "(Ljava/lang/String;Z)V"); // eventProperty(String, boolean)
    prism_PrismLib_eventProperty_Sl = env->GetStaticMethodID(prism_PrismLib, "eventProperty", "(Ljava/lang/String;J)V"); // eventProperty(String, long)
    prism_PrismLib_eventProperty_Sd = env->GetStaticMethodID(prism_PrismLib, "eventProperty", "(Ljava/lang/String;D)V"); // eventProperty(String, double)
    prism_PrismLib_eventProperty_SS = env->GetStaticMethodID(prism_PrismLib, "eventProperty", "(Ljava/lang/String;Ljava/lang/String;)V"); // eventProperty(String, String)
    prism_PrismLib_event = env->GetStaticMethodID(prism_PrismLib, "event", "(I)V"); // event(int)
    prism_PrismLib_logMessage_SiS = env->GetStaticMethodID(prism_PrismLib, "logMessage", "(Ljava/lang/String;ILjava/lang/String;)V"); // logMessage(String, int, String)
    #undef FIND_CLASS

    methods_initialized = true;
}
