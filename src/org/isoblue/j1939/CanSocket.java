package org.isoblue.j1939;

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

public final class CanSocket implements Closeable {
    static {
        final String LIB_CAN_INTERFACE = "j1939";
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

    public static final CanInterface CAN_ALL_INTERFACES = new CanInterface(0);
    
    private static native int _openSocketRAW() throws IOException;
    private static native int _openSocketBCM() throws IOException;
    private static native int _openSocketJ1939() throws IOException;
    
    private static native int _discoverInterfaceIndex(final int fd, final String ifName)
	throws IOException;
    private static native String _discoverInterfaceName(final int fd, final int ifIndex)
	throws IOException;
    private static native void _bindToSocket(final int fd, final int ifId)
	throws IOException;
    //private static native CanFrame _recvFrame(final int fd)
	//throws IOException;
    //private static native void _sendFrame(final int fd, final int canif,
	//final int canid, final byte[] data) throws IOException;
    private static native void _close(final int fd) throws IOException;
    
    public final static class CanId implements Cloneable {
        private int _canId = 0;
        
        public static enum StatusBits {
            ERR, EFFSFF, RTR
        }
        
        public CanId(final int address) {
            _canId = address;
        }
        
        @Override
        protected Object clone() {
            return new CanId(_canId);
        }
    }
    
    public final static class CanInterface implements Cloneable {
        private final int _ifIndex;
        private String _ifName;
        
        public CanInterface(final CanSocket socket, final String ifName)
                throws IOException {
            this._ifIndex = _discoverInterfaceIndex(socket._fd, ifName);
            this._ifName = ifName;
        }
        
        private CanInterface(int ifIndex, String ifName) {
            this._ifIndex = ifIndex;
            this._ifName = ifName;
        }
        
        private CanInterface(int ifIndex) {
            this(ifIndex, null);
        }
        
        public int getInterfaceIndex() {
            return _ifIndex;
        }

        @Override
        public String toString() {
            return "CanInterface [_ifIndex=" + _ifIndex + ", _ifName="
                    + _ifName + "]";
        }

        public String getIfName() {
            return _ifName;
        }
        
        public String resolveIfName(final CanSocket socket) {
            if (_ifName == null) {
                try {
                    _ifName = _discoverInterfaceName(socket._fd, _ifIndex);
                } catch (IOException e) { /* EMPTY */ }
            }
            return _ifName;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + _ifIndex;
            result = prime * result
                    + ((_ifName == null) ? 0 : _ifName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CanInterface other = (CanInterface) obj;
            if (_ifIndex != other._ifIndex)
                return false;
            if (_ifName == null) {
                if (other._ifName != null)
                    return false;
            } else if (!_ifName.equals(other._ifName))
                return false;
            return true;
        }
        
        @Override
        protected Object clone() {
            return new CanInterface(_ifIndex, _ifName);
        }
    }

    //public final static class CanFrame implements Cloneable {
        //private final CanInterface canIf;
        //private final CanId canId;
        //private final byte[] data;
        //private final int pgn;
        
        //public CanFrame(final CanInterface canIf, final CanId canId,
                //int pgn, byte[] data) {
            //this.canIf = canIf;
            //this.canId = canId;
            //this.data = data;
            //this.pgn = pgn;
        //}
        
        //[> this constructor is used in native code <]
        //@SuppressWarnings("unused")
        //private CanFrame(int canIf, int canid, int pgn, byte[] data) {
            //if (data.length > 8) {
                //throw new IllegalArgumentException();
            //}
            //this.canIf = new CanInterface(canIf);
            //this.canId = new CanId(canid);
            //this.pgn = pgn;
            //this.data = data;
        //}
        
        //public CanId getCanId() {
            //return canId;
        //}
        
        //public byte[] getData() {
            //return data;
        //}

        //public int getPGN() {
            //return pgn;
        //}
        
        //public CanInterface getCanInterfacae() {
            //return canIf;
        //}

	//@Override
	//public String toString() {
	    //return "CanFrame [canIf=" + canIf + ", canId=" + canId + ", data="
		    //+ Arrays.toString(data) + "]";
	//}
	
	//@Override
	//protected Object clone() {
	    //return new CanFrame(canIf, (CanId)canId.clone(),
		    //pgn, Arrays.copyOf(data, data.length));
	//}
    //}
    
    public static enum Mode {
        RAW, BCM, J1939 
    }
    
    private final int _fd;
    private final Mode _mode;
    private CanInterface _boundTo;
    
    public CanSocket(Mode mode) throws IOException {
        switch (mode) {
        case BCM:
            _fd = _openSocketBCM();
            break;
        case RAW:
            _fd = _openSocketRAW();
            break;
        case J1939:
            _fd = _openSocketJ1939();
            break;
        default:
            throw new IllegalStateException("unkown mode " + mode);
        }
        this._mode = mode;
    }
    
    public void bind(CanInterface canInterface) throws IOException {
        _bindToSocket(_fd, canInterface._ifIndex);
        this._boundTo = canInterface;
    }

    //public void send(CanFrame frame) throws IOException {
        //_sendFrame(_fd, frame.canIf._ifIndex, frame.canId._canId, frame.data);
    //}
    
    //public CanFrame recv() throws IOException {
	   //return _recvFrame(_fd);
    //}
    
    @Override
    public void close() throws IOException {
        _close(_fd);
    }
}
