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
import java.util.Iterator;

public class CanSocketJ1939 extends CanSocket {

	private static native int mFetch(final String param);
	private static native void initIds();
	private native void mSetJ1939filter(long[] names,
		int[] addrs, int[] pgns);
	private static final int CAN_J1939 = mFetch("CAN_J1939");
	private static final int SOCK_DGRAM = mFetch("SOCK_DGRAM");
	private static final int SOL_CAN_J1939 = mFetch("SOL");
	private static final int SO_J1939_FILTER = mFetch("FILTER");
	private static final int SO_J1939_PROMISC = mFetch("PROMISC");
	private static final int SO_J1939_RECV_OWN = mFetch("RECVOWN");
	private static final int SO_PRIORITY = mFetch("PRIORITY");

	public CanSocketJ1939() throws IOException {
		super(SOCK_DGRAM, CAN_J1939);
		//this("ALL");
	}
	
	public CanSocketJ1939(final String ifName) throws IOException {
		super(SOCK_DGRAM, CAN_J1939, ifName);
		initIds();
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
	
	public static class Filter extends CanSocket.CanFilter {
		protected final long name;
		protected final int addr;
		protected final int pgn;
		
		public Filter(final long name, final int addr,
			final int pgn) {
			this.name = name;
			this.addr = addr;
			this.pgn = pgn;
		}
	}
	
	public void setfilter(Collection<Filter> filter) 
		throws IOException, IllegalArgumentException {
		long[] names = new long[filter.size()];
		int[] addrs = new int[filter.size()];
		int[] pgns = new int[filter.size()];
		int i = 0;
		Iterator<Filter> it = filter.iterator();
		while (it.hasNext()) {
			Filter filt = it.next();
			if (filt.addr >= 0xFF) {
				throw new IllegalArgumentException("addr: "
					+ filt.addr + " out of range");
			}
			if (filt.pgn > 0x3FFFF) {
				throw new IllegalArgumentException("pgn: "
					+ filt.pgn + " out of range");
			}
			names[i] = filt.name;
			addrs[i] = filt.addr;
			pgns[i] = filt.pgn;
			i++;
		}
		mSetJ1939filter(names, addrs, pgns);	
	} 
}

