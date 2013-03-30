#include <jni.h>
#include <pthread.h>
#include <string.h>

#include "mediaplayer.h"
#include "logging.h"

namespace {

jfieldID sHandleFieldID = NULL;
jmethodID sStateMethodID = NULL;
JavaVM* sVm = NULL;
const char* kClassName = "org/clementine_player/gstmediaplayer/MediaPlayer";


MediaPlayer* This(JNIEnv* env, jobject object) {
  return reinterpret_cast<MediaPlayer*>(
      env->GetLongField(object, sHandleFieldID));
}

jlong CreateNativeInstance(JNIEnv* env, jobject object, jstring url) {
  const char* c_url = env->GetStringUTFChars(url, NULL);
  MediaPlayer* instance =
      new MediaPlayer(sVm, env, object, sStateMethodID, c_url);
  env->ReleaseStringUTFChars(url, c_url);

  return reinterpret_cast<jlong>(instance);
}

void DestroyNativeInstance(JNIEnv* env, jobject object) {
  MediaPlayer* player = This(env, object);
  player->Release(env);
  delete player;
}

void Start(JNIEnv* env, jobject object) {
  This(env, object)->Start();
}

void Pause(JNIEnv* env, jobject object) {
  This(env, object)->Pause();
}

void SetVolume(JNIEnv* env, jobject object, jfloat volume) {
  This(env, object)->SetVolume(volume);
}

const JNINativeMethod kNativeMethods[] = {
  {"CreateNativeInstance", "(Ljava/lang/String;)J", reinterpret_cast<void*>(CreateNativeInstance)},
  {"DestroyNativeInstance", "()V", reinterpret_cast<void*>(DestroyNativeInstance)},
  {"Start", "()V", reinterpret_cast<void*>(Start)},
  {"Pause", "()V", reinterpret_cast<void*>(Pause)},
  {"SetVolume", "(F)V", reinterpret_cast<void*>(SetVolume)},
};

}  // namespace


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }

  jclass clazz = env->FindClass(kClassName);
  env->RegisterNatives(clazz, kNativeMethods, G_N_ELEMENTS(kNativeMethods));

  sVm = vm;
  sHandleFieldID = env->GetFieldID(clazz, "handle_", "J");
  sStateMethodID =
      env->GetMethodID(clazz, "NativeStateChanged", "(ILjava/lang/String;)V");

  return JNI_VERSION_1_6;
}

