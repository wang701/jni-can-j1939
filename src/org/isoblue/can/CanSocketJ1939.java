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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class CanSocketJ1939 extends CanSocket {

	private static native int mFetch(final String param);
	private static native void mSetfilter(final long name, final int addr);
	private static final int CAN_J1939 = mFetch("CAN_J1939");
	private static final int SOCK_DGRAM = mFetch("SOCK_DGRAM");
	private static final int SOL_CAN_J1939 = mFetch("SOL");
	private static final int SO_J1939_FILTER = mFetch("FILTER");
	private static final int SO_J1939_PROMISC = mFetch("PROMISC");
	private static final int SO_J1939_RECV_OWN = mFetch("RECVOWN");
	private static final int SO_PRIORITY = mFetch("PRIORITY");

	public CanSocketJ1939() throws IOException {
		super(SOCK_DGRAM, CAN_J1939);
	}
	
	public CanSocketJ1939(final String ifName) throws IOException {
		super(SOCK_DGRAM, CAN_J1939, ifName);
	}
	
	public void setPromisc() throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_J1939_PROMISC, 1);	
	}	

	public void setRecvown() throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_J1939_RECV_OWN, 1);	
	}	

	public void setPriority(final int priority) throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_PRIORITY, priority);	
	}	
	
	@Override
	public void setJ1939filter(final long name, final int addr)
		throws IOException {
		mSetJ1939filter(name, addr);	
	} 
}

