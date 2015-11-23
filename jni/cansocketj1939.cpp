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

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocketJ1939_mFetch
(JNIEnv *env, jclass obj, jstring param)
{
    	const char *str = env->GetStringUTFChars(param, NULL);
	if (strcmp(str, "CAN_J1939") == 0) {
		return CAN_J1939;
	}
	else if (strcmp(str, "SOCK_DGRAM") == 0) {
		return SOCK_DGRAM;
	}
	else if (strcmp(str, "SOL") == 0) {
		return SOL_CAN_J1939;
	}
	else if (strcmp(str, "FILTER") == 0) {
		return SO_J1939_FILTER;
	}
	else if (strcmp(str, "PROMISC") == 0) {
		return SO_J1939_PROMISC;
	}
	else if (strcmp(str, "RECVOWN") == 0) {
		return SO_J1939_RECV_OWN;
	}
	else if (strcmp(str, "PRIORITY") == 0) {
		return SO_PRIORITY;
	}

	return -1;
}

