LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := j1939-can
LOCAL_SRC_FILES := cansocket.cpp cansocketj1939.cpp
LOCAL_LDLIBS    := -llog

LOCAL_CFLAGS := \
	-O2 -g \
	-pipe -Wall -W \
	-fexceptions -fstack-protector -fPIC \
	--param=ssp-buffer-size=4 \
	-Wno-unused-parameter -Wno-long-long \
	-pedantic \
	-D_REENTRANT -D_GNU_SOURCE -D_FORTIFY_SOURCE=2 \

include $(BUILD_SHARED_LIBRARY)
