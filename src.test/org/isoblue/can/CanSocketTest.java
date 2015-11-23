package org.isoblue.can;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.isoblue.can.CanSocket;
import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocket.CanFilter;
import org.isoblue.can.CanSocketJ1939.J1939Filter;

public class CanSocketTest {
	private static final String CAN_INTERFACE_0 = "can0";

    	@Retention(RetentionPolicy.RUNTIME)
    	@Target({ElementType.METHOD})
    	@interface Test { /* EMPTY */ }

    	public static void main(String[] args) throws IOException {
        	// PressKeytoStart();
			startTests();
    	}

    	private static void PressKeytoStart() {
        	System.out.println("Press any key to continue...");
        	try {
            		System.in.read();
        	}  
        	catch (Exception e) {}
    	}

    	private static String byteArrayToHex(byte[] a) {
       		StringBuilder sb = new StringBuilder(a.length * 2);
       		for(byte b: a)
          		sb.append(String.format("%02x", b & 0xff));
       		return sb.toString();
    	}

    	private static void startTests() {
        	final CanSocketTest dummy = new CanSocketTest();
        	boolean succeeded = true;
        	for (Method testMethod : CanSocketTest.class.getMethods()) {
            		if (testMethod.getAnnotation(Test.class) != null) {
                		System.out.print("Test: " + testMethod.getName());
                		try {
                    			testMethod.invoke(dummy);
                    			System.out.println("...OK");
                		} catch (final Exception e) {
                    			System.out.println("...FAILED");
                    			e.printStackTrace();
                    			succeeded = false;
                		}
            		}
        	}
        	if (!succeeded) {
            		System.out.println("unit tests went wrong".toUpperCase());
            		System.exit(-1);
        	}
   	}	

	@Test
	public void testJ1939Create() throws IOException {
		final CanSocketJ1939 socket = new CanSocketJ1939();
		socket.close();
	}

	@Test
	public void testJ1939BindNoFilter() throws IOException {
		final CanSocketJ1939 socket = new CanSocketJ1939("can0");
		socket.close();
	}	

	@Test
	public void testJ1939SetPromisc() throws IOException {
		final CanSocketJ1939 socket = new CanSocketJ1939("can0");
		socket.setPromisc();
		socket.close();
	}

	@Test
	public void testJ1939SetRecvown() throws IOException {
		final CanSocketJ1939 socket = new CanSocketJ1939("can0");
		socket.setRecvown();
		socket.close();
	}

	@Test
	public void testJ1939SetFilters() throws IOException {
		final CanSocketJ1939 socket = new CanSocketJ1939("can0");
		J1939Filter f1 = new J1939Filter(0x0010, 0x34, 0xFFA4);
		J1939Filter f2 = new J1939Filter(0x0210, 0x44, 0xFFD4);
		J1939Filter f3 = new J1939Filter(0x0310, 0x00, 0xFF04);
		ArrayList<J1939Filter> filters = new ArrayList<J1939Filter>();
		filters.add(f1);
		filters.add(f2);
		filters.add(f3);
		socket.close();	
	}


}
