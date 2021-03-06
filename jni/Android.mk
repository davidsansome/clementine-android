LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := gstmediaplayer
LOCAL_SRC_FILES := gstmediaplayer.cc mediaplayer.cc fht.cc
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)

ifndef GSTREAMER_SDK_ROOT
ifndef GSTREAMER_SDK_ROOT_ANDROID
$(error GSTREAMER_SDK_ROOT_ANDROID is not defined!)
endif
GSTREAMER_SDK_ROOT        := $(GSTREAMER_SDK_ROOT_ANDROID)
endif
GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_SDK_ROOT)/share/gst-android/ndk-build/
include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS         :=  $(GSTREAMER_PLUGINS_CORE) $(GSTREAMER_PLUGINS_PLAYBACK) $(GSTREAMER_PLUGINS_CODECS) $(GSTREAMER_PLUGINS_NET) $(GSTREAMER_PLUGINS_SYS)
G_IO_MODULES              := gnutls
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer.mk
