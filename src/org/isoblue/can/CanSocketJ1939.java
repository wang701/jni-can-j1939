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

	private static native int mFetchJ1939();
	private static native int mFetchDGRAM();	
	private static final int CAN_J1939 = mFetchJ1939();
	private static final int SOCK_DGRAM = mFetchDGRAM();

	public CanSocketJ1939() throws IOException {
		super(SOCK_DGRAM, CAN_J1939);
	}	
}

