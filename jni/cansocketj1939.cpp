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
#include <sys/time.h>
#include <unistd.h>
#include <inttypes.h>
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

static const int ERRNO_BUFFER_LEN = 1024;
static const int RECV_BUFFER_LEN = 1024;
static char ctrlmsg[CMSG_SPACE(sizeof(struct timeval))
	+ CMSG_SPACE(sizeof(uint8_t)) /* dest addr */
	+ CMSG_SPACE(sizeof(uint64_t)) /* dest name */
	+ CMSG_SPACE(sizeof(uint8_t)) /* priority */
	];

jfieldID sockID;
jfieldID ifIndID;

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
		throwIOExceptionMsg(env, std::string(message));
	} else if (((long)msg) == -1) {
		snprintf(message, ERRNO_BUFFER_LEN, "errno %d", exc_errno);
		throwIOExceptionMsg(env, std::string(message));
	} else {
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

JNIEXPORT jint JNICALL Java_org_isoblue_can_CanSocketJ1939_fetch
(JNIEnv *env, jclass obj, jstring param)
{
    	const char *str = env->GetStringUTFChars(param, NULL);
	if (strcmp(str, "CAN_J1939") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return CAN_J1939;
	}
	else if (strcmp(str, "SOCK_DGRAM") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SOCK_DGRAM;
	}
	else if (strcmp(str, "SOL_J1939") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SOL_CAN_J1939;
	}
	else if (strcmp(str, "SOL_SOCKET") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SOL_SOCKET;
	}
	else if (strcmp(str, "FILTER") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SO_J1939_FILTER;
	}
	else if (strcmp(str, "PROMISC") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SO_J1939_PROMISC;
	}
	else if (strcmp(str, "RECVOWN") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SO_J1939_RECV_OWN;
	}
	else if (strcmp(str, "PRIORITY") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SO_PRIORITY;
	}
	else if (strcmp(str, "TIMESTAMP") == 0) {
		env->ReleaseStringUTFChars(param, str);
		return SO_TIMESTAMP;
	}
	else {
		throwIllegalArgumentException(env,
			"fetch argument is invalid");		
	}

	env->ReleaseStringUTFChars(param, str);
	return -1;
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocketJ1939_initIds
(JNIEnv *env, jclass cls)
{
	sockID = env->GetFieldID(cls, "mFd", "I");	
	ifIndID = env->GetFieldID(cls, "mIfIndex", "I");
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocketJ1939_setJ1939Filter
(JNIEnv *env, jobject obj, jlongArray names, jintArray addrs, jintArray pgns)
{
	int i;
	/* get the array length */
	jsize len = env->GetArrayLength(names);	

	struct j1939_filter* filt = (struct j1939_filter*) malloc(
		len * sizeof(struct j1939_filter));
	if (filt == NULL) {
		throwOutOfMemoryError(env, "could not allocate filters");		
	}
	/*get the sock fd */
	jint sockfd = env->GetIntField(obj, sockID);

	/* fill up the filter(s) */
	jlong *name = env->GetLongArrayElements(names, NULL);
	jint *addr = env->GetIntArrayElements(addrs, NULL);
	jint *pgn = env->GetIntArrayElements(pgns, NULL);
	for (i = 0; i < len; i++) {
		filt[i].name = name[i];
		filt[i].name_mask = ~0ULL;
		filt[i].addr = addr[i];
		filt[i].addr_mask = ~0;
		filt[i].pgn = pgn[i];
		filt[i].pgn_mask = ~0;
	}
	env->ReleaseLongArrayElements(names, name, 0);	
	env->ReleaseIntArrayElements(addrs, addr, 0);	
	env->ReleaseIntArrayElements(pgns, pgn, 0);

	/* apply filters to socket */
	if (setsockopt(sockfd, SOL_CAN_J1939, SO_J1939_FILTER, filt,
		 len * sizeof(struct j1939_filter)) == -1) {
		throwIOExceptionErrno(env, errno);
	}

	free(filt);
}

JNIEXPORT jobject JNICALL Java_org_isoblue_can_CanSocketJ1939_recvMsg
(JNIEnv *env, jobject obj)
{
	/* get sock fd and ifindex */
	jint sockfd = env->GetIntField(obj, sockID);
	jint ifindex = env->GetIntField(obj, ifIndID);

	/* setup the msg and iov struct */
	int ret;
	unsigned int len;
	uint8_t priority, dst_addr;
	uint64_t dst_name;	
	static uint8_t *buf;
	struct sockaddr_can src;
	struct ifreq ifr;
	struct iovec iov;
	struct msghdr msg;
	struct cmsghdr *cmsg;
	struct timeval tv = { 0 };

	buf = static_cast<uint8_t *>(malloc(RECV_BUFFER_LEN));
	if (!buf) {
		throwOutOfMemoryError(env, "could not allocate recv buf");
	}
	memset(&src, 0, sizeof(src));

	src.can_ifindex = ifindex;
	src.can_family = AF_CAN;
	src.can_addr.j1939.name = J1939_NO_NAME;
	src.can_addr.j1939.addr = J1939_NO_ADDR;
	src.can_addr.j1939.pgn = J1939_NO_PGN;
	
	iov.iov_base = &buf[0];
	msg.msg_name = &src;
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;
	msg.msg_control = &ctrlmsg;
	
	iov.iov_len = RECV_BUFFER_LEN;
	msg.msg_namelen = sizeof(src);
	msg.msg_controllen = sizeof(ctrlmsg);
	msg.msg_flags = 0;
	
	ret = recvmsg(sockfd, &msg, 0);
	if (ret < 0) {
		throwIOExceptionErrno(env, errno);
		return NULL;
	}
	len = ret;
	for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
		switch (cmsg->cmsg_level) {
			case SOL_SOCKET:
				if(cmsg->cmsg_type == SO_TIMESTAMP)
					memcpy(&tv, CMSG_DATA(cmsg),
						sizeof(tv));
				break;
			case SOL_CAN_J1939:
				if (cmsg->cmsg_type == SCM_J1939_DEST_ADDR)
					dst_addr = *CMSG_DATA(cmsg);
				else if (cmsg->cmsg_type == SCM_J1939_DEST_NAME)
					memcpy(&dst_name, CMSG_DATA(cmsg),
						cmsg->cmsg_len - CMSG_LEN(0));
				else if (cmsg->cmsg_type == SCM_J1939_PRIO)
					priority = *CMSG_DATA(cmsg);
				break;
		}
	}
	
	/* find epoch timestamp */
	jint timestamp = tv.tv_sec;

	/* find name of receive interface */
	ifr.ifr_ifindex = src.can_ifindex;
	ioctl(sockfd, SIOCGIFNAME, &ifr);
	jstring jifname = env->NewStringUTF(ifr.ifr_name);

	const jsize dsize = len;
	const jclass j1939frame_clazz = env->FindClass("org/isoblue/can/"
						"CanSocketJ1939$Frame");
	if (j1939frame_clazz == NULL) {
		return NULL;
	}
	const jmethodID j1939frame_cstr = env->GetMethodID(j1939frame_clazz,
							"<init>",
							"(Ljava/lang/String;"
							"JIJIIII[BI)V");
	if (j1939frame_cstr == NULL) {
		return NULL;
	}
	const jbyteArray data = env->NewByteArray(dsize);	
	if (data == NULL) {
		if (env->ExceptionCheck() != JNI_TRUE) {
			throwOutOfMemoryError(env,
				"could not allocate data array");
		}
		return NULL;
	}
	env->SetByteArrayRegion(data, 0, dsize,
		reinterpret_cast<jbyte *>(buf));
	if (env->ExceptionCheck() == JNI_TRUE) {
		return NULL;
	}
	const jobject retobj = env->NewObject(j1939frame_clazz, j1939frame_cstr,
					jifname,
					src.can_addr.j1939.name,
					src.can_addr.j1939.addr,
					dst_name, dst_addr,
					src.can_addr.j1939.pgn,
					len, priority, data, timestamp);
	
	free(buf);
	return retobj;
						
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocketJ1939_bindToSocket
(JNIEnv *env, jobject obj)
{
	jint sockfd = env->GetIntField(obj, sockID);
	jint ifindex = env->GetIntField(obj, ifIndID);
	struct sockaddr_can addr;
	/* bind with no filtering */
	addr.can_family = AF_CAN;
	addr.can_ifindex = ifindex;
	addr.can_addr.j1939.name = J1939_NO_NAME;
	addr.can_addr.j1939.addr = J1939_NO_ADDR;
	addr.can_addr.j1939.pgn = J1939_NO_PGN;
	
	if (bind(sockfd, reinterpret_cast<struct sockaddr *>(&addr),
		sizeof(addr)) != 0) {
		throwIOExceptionErrno(env, errno);
	}
}

JNIEXPORT void JNICALL Java_org_isoblue_can_CanSocketJ1939_sendMsg
(JNIEnv *env, jobject obj, jobject frameobj)
{
	struct sockaddr_can addr;
	static uint8_t *buf;
	buf = static_cast<uint8_t *>(calloc(8, sizeof(uint8_t)));
	memset(&addr, 0, sizeof(addr));
	
	jint sockfd = env->GetIntField(obj, sockID);
	//jclass frame_clazz = env->GetObjectClass(frameobj);
	//jfieldID dstnameID = env->GetFieldID(env, frame_clazz, "dstName", "J");
	//jfieldID dstaddrID = env->GetFieldID(frame_clazz, "dstAddr", "I");
	//jfieldID pgnID = env->GetFieldID(frame_clazz, "pgn", "I");
	//jlong dstname = env->GetLongField(frameobj, dstnameID);
	//jint dstaddr = env->GetIntField(frameobj, dstaddrID);
	//jint pgn = env->GetIntField(frameobj, pgnID);

	addr.can_addr.j1939.name = J1939_NO_NAME;
	addr.can_addr.j1939.addr = 0x30;
	addr.can_addr.j1939.pgn = 0x12300;

	if (sendto(sockfd, buf, sizeof(buf), 0,
		reinterpret_cast<struct sockaddr *>(&addr),
		sizeof(addr)) < 0) {
		free(buf);
		throwIOExceptionErrno(env, errno);
	}
}
