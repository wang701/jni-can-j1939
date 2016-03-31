package org.isoblue.can;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class CanSocket implements Closeable {

	private int mFd;
	private int mIfIndex;

	private static native void initIds();
	private native void closesocket() throws IOException;
	private native int openSocket(final int socktype, final int protocol) throws IOException;
	private native int getIfIndex(final String ifName) throws IOException;
	private native void setsockopt(final int level, final int optname, final int optval)
		throws IOException;
	private native int getsockopt(final int level, final int optname) throws IOException;
	private native int selectFd(final int timeout) throws IOException;

	static {

		final String LIB_CAN_INTERFACE = "j1939-can";
		try {
			System.loadLibrary(LIB_CAN_INTERFACE);
		} catch (final UnsatisfiedLinkError e) {
			System.out.println("libj1939-can.so not loaded successfully");
		}
		System.out.println("libj1939-can.so loaded");
	}

	protected int getmFd() {

		return this.mFd;
	}

	public CanSocket(final int socktype, final int protocol) throws IOException {

		initIds();
		this.mFd = openSocket(socktype, protocol);
	}

	public CanSocket(final int socktype, final int protocol, final String ifName)
		throws IOException {

		this(socktype, protocol);
		if (ifName == "") {
			this.mIfIndex = 0;
		}
		else {
			this.mIfIndex = getIfIndex(ifName);
		}
	}

    public void setSockOpt(final int level, final int optname, final int optval)
		throws IOException {

		setsockopt(level, optname, optval);
	}

	public int getSockOpt(final int level, final int optname) throws IOException {

		return getsockopt(level, optname);
	}

	public int select(final int timeout) throws IOException {

		return selectFd(timeout);
	}

	public abstract static class CanFrame {
	}

	public abstract static class CanFilter {
	}

	@Override
	public void close() throws IOException {

		closesocket();
    }
}
