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
#include "org_isoblue_can_CanSocket.h"
#endif

static const int ERRNO_BUFFER_LEN = 1024;
jfieldID socketID;
jfieldID ifIndexID;

static void throwException(JNIEnv *env, const std::string& exception_name,
			   const std::string& msg)
{
	const jclass exception = env->FindClass(exception_name.c_str());
	if (exception == NULL) {
		return;
	}
	env->ThrowNew(exception, msg.c_str());
}

static void throwIOExceptionMsg(JNIEnv *env, const std::string& msg)
{
	throwException(env, "java/io/IOException", msg);
}

static void throwIOExceptionErrno(JNIEnv *env, const int exc_errno)
{
	char message[ERRNO_BUFFER_LEN];
	const char *const msg = (char *) strerror_r(exc_errno, message, ERRNO_BUFFER_LEN);
	if (((long)msg) == 0) {
		// POSIX strerror_r, success
		throwIOExceptionMsg(env, std::string(message));
	} else if (((long)msg) == -1) {
		// POSIX strerror_r, failure
		// (Strictly, POSIX only guarantees a value other than 0. The safest
		// way to implement this function is to use C++ and overload on the
		// type of strerror_r to accurately distinguish GNU from POSIX. But
		// realistic implementations will always return -1.)
		snprintf(message, ERRNO_BUFFER_LEN, "errno %d", exc_errno);
		throwIOExceptionMsg(env, std::string(message));
	} else {
		// glibc strerror_r returning a string
		throwIOExceptionMsg(env, std::string(msg));
	}
}

static void throwIllegalArgumentException(JNIEnv *env, const std::string& message)
{
	throwException(env, "java/lang/IllegalArgumentException", message);
}

/*
 * This function initializes two global variables and provide
 * access and linkage to private members in corresponding
 * CanSocket class *
 */
JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket_initIds
(JNIEnv *env, jclass cls)
{
	socketID = env->GetFieldID(cls, "mFd", "I");
	ifIndexID = env->GetFieldID(cls, "mIfIndex", "I");
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket_closeSocket
(JNIEnv *env, jobject obj)
{
	jint sockfd = env->GetIntField(obj, socketID);
	if (close(sockfd) == -1) {
		throwIOExceptionErrno(env, errno);
	}
}


JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket_openSocket
(JNIEnv *env, jobject obj, jint socktype, jint protocol)
{
	const int fd = socket(PF_CAN, socktype, protocol);
        if (fd != -1) {
                return fd;
        }
        throwIOExceptionErrno(env, errno);
        return -1;
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket_getIfIndex
(JNIEnv *env, jobject obj, jstring ifName)
{
	jint sockfd = env->GetIntField(obj, socketID);
	struct ifreq ifreq;
	const jsize ifNameSize = env->GetStringUTFLength(ifName);
	if (ifNameSize > IFNAMSIZ-1) {
		throwIllegalArgumentException(env, "illegal interface name");
		return -1;
	}

	/* fetch interface name */
	memset(&ifreq, 0x0, sizeof(ifreq));
	env->GetStringUTFRegion(ifName, 0, ifNameSize,
				ifreq.ifr_name);
	if (env->ExceptionCheck() == JNI_TRUE) {
		return -1;
	}
	/* discover interface id */
	const int err = ioctl(sockfd, SIOCGIFINDEX, &ifreq);
	if (err == -1) {
		throwIOExceptionErrno(env, errno);
		return -1;
	} else {
		return ifreq.ifr_ifindex;
	}
	
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket_setsockopt
(JNIEnv *env, jobject obj, jint level, jint optname, jint optval)
{
	jint sockfd = env->GetIntField(obj, socketID);
	const int _optval = optval;
	if (setsockopt(sockfd, level, optname, &_optval,
		sizeof(_optval)) == -1) {
		throwIOExceptionErrno(env, errno);
	}	
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket_getsockopt
(JNIEnv *env, jobject obj, jint level, jint optname)
{
	int _stat = 0;
	jint sockfd = env->GetIntField(obj, socketID);
	socklen_t len = sizeof(_stat);
	if (getsockopt(sockfd, level, optname, &_stat, &len) == -1) {
		throwIOExceptionErrno(env, errno);
	}
	if (len != sizeof(_stat)) {
		throwIllegalArgumentException(env,
			"setsockopt return size is different");
		return -1;
	}
	return _stat;	

}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket_selectFd
(JNIEnv *env, jobject obj, jint timeout)
{
	const int _timeout = timeout;
	int rv;
	struct timeval to = { 0 };
	fd_set readfd;
	to.tv_sec = _timeout;
	jint sockfd = env->GetIntField(obj, socketID);

	FD_ZERO(&readfd);
	FD_SET(sockfd, &readfd);
	rv = select(sockfd + 1, &readfd, NULL, NULL, &to);	
	
	if (rv == -1) {
		throwIOExceptionErrno(env, errno);
		return -2;
	} else if (rv == 0) {
		return -1;
	}

	return 0;
}
