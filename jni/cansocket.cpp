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

static void throwOutOfMemoryError(JNIEnv *env, const std::string& message)
{
    	throwException(env, "java/lang/OutOfMemoryError", message);
}

static jint newCanSocket(JNIEnv *env, int socket_type, int protocol)
{
	const int fd = socket(PF_CAN, socket_type, protocol);
	if (fd != -1) {
		return fd;
	}
	throwIOExceptionErrno(env, errno);
	return -1;
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket__1openSocketRAW
(JNIEnv *env, jclass obj)
{
	return newCanSocket(env, SOCK_RAW, CAN_RAW);
}


JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket__1openSocketBCM
(JNIEnv *env, jclass obj)
{
	return newCanSocket(env, SOCK_DGRAM, CAN_BCM);
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket__1openSocketJ1939
(JNIEnv *env, jclass obj)
{
	return newCanSocket(env, SOCK_DGRAM, CAN_J1939);
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket__1close
(JNIEnv *env, jclass obj, jint fd)
{
	if (close(fd) == -1) {
		throwIOExceptionErrno(env, errno);
	}
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket__1discoverInterfaceIndex
(JNIEnv *env, jclass clazz, jint socketFd, jstring ifName)
{
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
	const int err = ioctl(socketFd, SIOCGIFINDEX, &ifreq);
	if (err == -1) {
		throwIOExceptionErrno(env, errno);
		return -1;
	} else {
		return ifreq.ifr_ifindex;
	}
}

JNIEXPORT jstring JNICALL Java_org_isoblue_can_CanSocket__1discoverInterfaceName
(JNIEnv *env, jclass obj, jint fd, jint ifIdx)
{
	struct ifreq ifreq;
	memset(&ifreq, 0x0, sizeof(ifreq));
	ifreq.ifr_ifindex = ifIdx;
	if (ioctl(fd, SIOCGIFNAME, &ifreq) == -1) {
		throwIOExceptionErrno(env, errno);
		return NULL;
	}
	const jstring ifname = env->NewStringUTF(ifreq.ifr_name);
	return ifname;
}


JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket__1bindToSocket
(JNIEnv *env, jclass obj, jint fd, jint ifIndex)
{
	struct sockaddr_can addr;
	addr.can_family = AF_CAN;
	addr.can_ifindex = ifIndex;
	addr.can_addr.j1939.name = J1939_NO_NAME;
	addr.can_addr.j1939.addr = J1939_NO_ADDR;
	addr.can_addr.j1939.pgn = J1939_NO_PGN;
	
	if (bind(fd, reinterpret_cast<struct sockaddr *>(&addr), sizeof(addr)) != 0) {
		throwIOExceptionErrno(env, errno);
	}
}

//JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket__1sendFrame
//(JNIEnv *env, jclass obj, jint fd, jint if_idx, jint canid, jbyteArray data)
//{
	//const int flags = 0;
	//ssize_t nbytes;
	//struct sockaddr_can addr;
	//struct can_frame frame;
	//memset(&addr, 0, sizeof(addr));
	//memset(&frame, 0, sizeof(frame));
	//addr.can_family = AF_CAN;
	//addr.can_ifindex = if_idx;
	//const jsize len = env->GetArrayLength(data);
	//if (env->ExceptionCheck() == JNI_TRUE) {
		//return;
	//}
	//frame.can_id = canid;
	//frame.can_dlc = static_cast<__u8>(len);
	//env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(&frame.data));
	//if (env->ExceptionCheck() == JNI_TRUE) {
		//return;
	//}
	//nbytes = sendto(fd, &frame, sizeof(frame), flags,
			//reinterpret_cast<struct sockaddr *>(&addr),
			//sizeof(addr));
	//if (nbytes == -1) {
		//throwIOExceptionErrno(env, errno);
	//} else if (nbytes != sizeof(frame)) {
		//throwIOExceptionMsg(env, "send partial frame");
	//}
//}

//JNIEXPORT jobject JNICALL Java_org_isoblue_can_CanSocket__1recvFrame
//(JNIEnv *env, jclass obj, jint fd)
//{
	//ssize_t nbytes;
	//struct msghdr msg;
	//struct iovec iov;
	//struct isobus_mesg mesg;

	//char ctrlmsg[CMSG_SPACE(sizeof(struct timeval))+CMSG_SPACE(sizeof(__u32))];

	//memset(&msg, 0, sizeof(msg));
	//memset(&mesg, 0, sizeof(mesg));
	//memset(&iov, 0, sizeof(iov));

	//msg.msg_iov = &iov;
	//msg.msg_control = &ctrlmsg;
	//msg.msg_controllen = sizeof(ctrlmsg);
	//msg.msg_iovlen = 1;
	//iov.iov_base = &mesg;
	//iov.iov_len = sizeof(mesg);

	//nbytes = recvmsg(fd, &msg, 0);
	//if (nbytes == -1) {
		//throwIOExceptionErrno(env, errno);
		//return NULL;
	//}

	//const jsize data_size = mesg.dlen;
	//const jint pgn = mesg.pgn;
	//const jbyteArray data = env->NewByteArray(data_size);

	//if (data == NULL) {
		//if (env->ExceptionCheck() != JNI_TRUE) {
			//throwOutOfMemoryError(env, "could not allocate ByteArray");
		//}
		//return NULL;
	//}

	//env->SetByteArrayRegion(data, 0, data_size, reinterpret_cast<jbyte *>(&mesg.data));
	//if (env->ExceptionCheck() == JNI_TRUE) {
		//return NULL;
	//}

	//const jclass can_frame_clazz = env->FindClass("org/isoblue/can/"
							//"CanSocket$CanFrame");
	//if (can_frame_clazz == NULL) {
		//return NULL;
	//}
	//const jmethodID can_frame_cstr = env->GetMethodID(can_frame_clazz,
							//"<init>", "(III[B)V");
							/* <init> is magic 
							* CanFrame class in CanSocket.java has 3 int, 1 byte array parameters,
							* returns void.
							* Hence the signature.
							*/
	//if (can_frame_cstr == NULL) {
		//return NULL;
	//}

	//const jobject ret = env->NewObject(can_frame_clazz, can_frame_cstr,
										   //0, 0, pgn, data);

	//return ret; 
//}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocket__1setsockopt
(JNIEnv *env, jclass obj, jint fd, jint op, jint stat)
{
 	const int _stat = stat;
 	if (setsockopt(fd, SOL_CAN_RAW, op, &_stat, sizeof(_stat)) == -1) {
		 throwIOExceptionErrno(env, errno);
 	}
}

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocket__1getsockopt
(JNIEnv *env, jclass obj, jint fd, jint op)
{
	int _stat = 0;
 	socklen_t len = sizeof(_stat);
	if (getsockopt(fd, SOL_CAN_RAW, op, &_stat, &len) == -1) {
		throwIOExceptionErrno(env, errno);
 	}
 	if (len != sizeof(_stat)) {
	 	throwIllegalArgumentException(env, "setsockopt return size is different");
		return -1;
 	}
 	return _stat;
}
