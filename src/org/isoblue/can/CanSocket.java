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
	private static native void mClose(final int fd) throws IOException;
	private static native int mOpenSocket(final int socktype,
		final int protocol) throws IOException;
	private static native int mGetIfIndex(final int fd, final String ifName)
		throws IOException;
	private static native void mbind(final int fd, final int ifIndex)
		throws IOException;
	private static native void mSetsockopt(final int fd, final int level,
		final int optname, final int optval) throws IOException;

	static {
		final String LIB_CAN_INTERFACE = "can";
        	try {
            		System.loadLibrary(LIB_CAN_INTERFACE);
        	} catch (final UnsatisfiedLinkError e) {
            		try {
                		loadLibFromJar(LIB_CAN_INTERFACE);
            		} catch (final IOException _e) {
                	throw new UnsatisfiedLinkError(LIB_CAN_INTERFACE);
            		}
        	}
    	}

    	private static void copyStream(final InputStream in,
            	final OutputStream out) throws IOException {
        		final int BYTE_BUFFER_SIZE = 0x1000;
        		final byte[] buffer = new byte[BYTE_BUFFER_SIZE];
        		for (int len; (len = in.read(buffer)) != -1;) {
            			out.write(buffer, 0, len);
        		}
    	}	

    	private static void loadLibFromJar(final String libName)
            	throws IOException {
        		Objects.requireNonNull(libName);
        		final String fileName = "/lib/lib" + libName + ".so";
        		final FileAttribute<Set<PosixFilePermission>> permissions =
                	PosixFilePermissions.asFileAttribute(
                        	PosixFilePermissions.fromString("rw-------"));
        		final Path tempSo = Files.createTempFile(CanSocket.class.getName(),
                		".so", permissions);
        	try {
            		try (final InputStream libstream =
                    		CanSocket.class.getResourceAsStream(fileName)) {
                		if (libstream == null) {
                    			throw new FileNotFoundException("jar:*!" + fileName);
                		}
                	try (final OutputStream fout = Files.newOutputStream(tempSo,
                        	StandardOpenOption.WRITE,
                        	StandardOpenOption.TRUNCATE_EXISTING)) {
                    			copyStream(libstream, fout);
                		}
            		}
            		System.load(tempSo.toString());
        	} finally {
            		Files.delete(tempSo);
        	}
    	}

	protected int getmFd() {
		return this.mFd;
	}

	public CanSocket(final int socktype, final int protocol)
		throws IOException {
		this.mFd = mOpenSocket(socktype, protocol);
	}

	public CanSocket(final int socktype, final int protocol,
		final String ifName) throws IOException {
		this.mFd = mOpenSocket(socktype, protocol);
		this.mIfIndex = mGetIfIndex(mFd, ifName);
		mbind(mFd, mIfIndex);
	}

        public void setsockopt(final int level, final int optname,
		final int optval) throws IOException {
		mSetsockopt(mFd, level, optname, optval);
	}
	
	public abstract static class CanFilter {
	}

	@Override
	public void close() throws IOException {
                mClose(mFd);
        }	
}
