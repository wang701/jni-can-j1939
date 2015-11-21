#include<string>
#include<algorithm>
#include<utility>

#include<cstring>
#include<cstddef>
#include<cerrno>

extern "C" {
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <net/if.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <linux/can/j1939.h>
}

#if defined(ANDROID) || defined(__ANDROID__)
#include "jni.h"
#else
#include "org_isoblue_can_CanSocketJ1939.h"
#endif

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocketJ1939_mFetchJ1939
(JNIEnv *env, jclass obj)
{
	return CAN_J1939;
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocketJ1939_mFetchDGRAM
(JNIEnv *, jclass)
{
	return SOCK_DGRAM;
}


