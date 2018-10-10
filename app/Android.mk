LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
LOCAL_MODULE    := vtm-jni
LOCAL_C_INCLUDES := src/main/jni src/main/jni/libtess2/Include
 
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -O2 -ffast-math -DNDEBUG
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -O2 -ffast-math -DNDEBUG
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm
 
LOCAL_SRC_FILES := src/main/jni/org.oscim.utils.TessJNI.cpp\
	src/main/jni/libtess2/Source/sweep.c\
	src/main/jni/libtess2/Source/priorityq.c\
	src/main/jni/libtess2/Source/bucketalloc.c\
	src/main/jni/libtess2/Source/geom.c\
	src/main/jni/libtess2/Source/tess.c\
	src/main/jni/libtess2/Source/dict.c\
	src/main/jni/libtess2/Source/mesh.c\
	src/main/jni/gl/utils.c
 
include $(BUILD_SHARED_LIBRARY)
