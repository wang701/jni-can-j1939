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

	private static native int fetch(final String param);
	private static native void initIds();
	private native void setJ1939Filter(long[] names,
		int[] addrs, int[] pgns);
	private native Frame recvMsg();
	private native void bindToSocket() throws IOException;
	private native void sendMsg(Frame f) throws IOException;
	private native void bindToAddr(final int addr) throws IOException;
	private native void bindToName(final long name) throws IOException;
	private static final int CAN_J1939 = fetch("CAN_J1939");
	//private static final int J1939_NO_NAME = fetch("NO_NAME");
	private static final int SOCK_DGRAM = fetch("SOCK_DGRAM");
	private static final int SOL_SOCKET = fetch("SOL_SOCKET");
	private static final int SOL_CAN_J1939 = fetch("SOL_J1939");
	private static final int SO_J1939_FILTER = fetch("FILTER");
	private static final int SO_J1939_PROMISC = fetch("PROMISC");
	private static final int SO_J1939_RECV_OWN = fetch("RECVOWN");
	private static final int SO_TIMESTAMP = fetch("TIMESTAMP");
	private static final int SO_PRIORITY = fetch("PRIORITY");

	public CanSocketJ1939() throws IOException {
		this("all");
	}
	
	public CanSocketJ1939(final String ifName) throws IOException {
		super(SOCK_DGRAM, CAN_J1939, ifName);
		initIds();
		bind();
	}
	
	public CanSocketJ1939(final String ifName, final int addr)
		throws IOException {
		super(SOCK_DGRAM, CAN_J1939, ifName);
		initIds();
		bindaddr(addr);	
	}

	public CanSocketJ1939(final String ifName, final long name)
		throws IOException {
		super(SOCK_DGRAM, CAN_J1939, ifName);
		initIds();
		bindname(name);	
	}

	@Override
	public void bind() throws IOException {
		bindToSocket();
	}
	
	public void bindaddr(final int addr) throws IOException {
		//TODO: make addr, name class members?
		bindToAddr(addr);
	}

	public void bindname(final long name) throws IOException {
		bindToName(name);
	}
	
	public void setPromisc() throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_J1939_PROMISC, 1);	
	}	

	public int getPromisc() throws IOException {
		return super.getsockopt(SOL_CAN_J1939, SO_J1939_PROMISC);
	}	

	public void setRecvown() throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_J1939_RECV_OWN, 1);	
	}	

	public int getRecvown() throws IOException {
		return super.getsockopt(SOL_CAN_J1939, SO_J1939_RECV_OWN);	
	}	

	public void setPriority(final int priority) throws IOException {
		super.setsockopt(SOL_CAN_J1939, SO_PRIORITY, priority);	
	}

	public void setTimestamp() throws IOException {
		super.setsockopt(SOL_SOCKET, SO_TIMESTAMP, 1);	
	}	

	public int getTimestamp() throws IOException {
		return super.getsockopt(SOL_SOCKET, SO_TIMESTAMP);
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
	
	public static class Frame extends CanSocket.CanFrame {
		protected String ifName;
		protected long name;
		protected int addr;
		protected long dstName;
		protected int dstAddr;
		protected int pgn;
		protected int len;
		protected int priority;
		protected byte[] data;
		protected int timestamp;
    		
		private static String byteArrayToHex(byte[] a) {
       			StringBuilder sb = new StringBuilder(a.length * 2);
       			for(byte b: a)
          			sb.append(String.format("%02x", b & 0xff));
       			return sb.toString();
    		}
		
		/* recv frame constructor */
		public Frame(String ifName, long name, int addr,
			long dstName, int dstAddr, int pgn, int len,
			int priority, byte[] data, int timestamp) {
			this.ifName = ifName;
			this.name = name;
			this.addr = addr;
			this.dstName = dstName;
			this.dstAddr = dstAddr;
			this.pgn = pgn;
			this.len = len;
			this.priority = priority;
			this.data = data;
			this.timestamp = timestamp;
		}
		
		/* send frame constructor */
		public Frame(int dstAddr, int pgn) {
			this.dstName = dstName;
			this.dstAddr = dstAddr;
			this.pgn = pgn;
		}
		
		public void print(final int verbose) {
			if (verbose == 1) {
				System.out.printf("\n%d,%s,%d,%d,%d,%d,%d,%d,"
					+ "%d,%s", timestamp, ifName, name,
					addr, dstName, dstAddr, pgn, len,
					priority, byteArrayToHex(data));
			}
			else {
				System.out.printf("\n%s,d,%d,%d,%s",
					ifName, addr, pgn, len,
					byteArrayToHex(data));
			}
		}
	}
	
	public Frame recvmsg() throws IOException {
		return recvMsg();
	}

	public void sendmsg(Frame f) throws IOException {
		sendMsg(f);	
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
		setJ1939Filter(names, addrs, pgns);	
	} 
}

