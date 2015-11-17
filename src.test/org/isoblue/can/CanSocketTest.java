package org.isoblue.can;

import java.io.IOException;
import java.util.Arrays;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.isoblue.can.CanSocket.CanFrame;
import org.isoblue.can.CanSocket.CanId;
import org.isoblue.can.CanSocket.CanInterface;
import org.isoblue.can.CanSocket.Mode;

public class CanSocketTest {
	private static final String CAN_INTERFACE = "vcan0";

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
                    System.out.println("OK");
                } catch (final Exception e) {
                    System.out.println("FAILED");
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
    public void testSocketCanIsobusCreate() throws IOException {
         new CanSocket(Mode.ISOBUS).close();
    }

    @Test
    public void testInterface() throws IOException {
         try (final CanSocket socket = new CanSocket(Mode.ISOBUS)) {
             new CanInterface(socket, CAN_INTERFACE);
         }
    }
    
    @Test
    public void testBindInterface() throws IOException {
         try (final CanSocket socket = new CanSocket(Mode.ISOBUS)) {
            final CanInterface canIf = new CanInterface(socket, CAN_INTERFACE);
            socket.bind(canIf);
         }
    }

    @Test
    public void testRecv() throws IOException {
        try (final CanSocket socket = new CanSocket(Mode.ISOBUS)) {
            final CanInterface canIf = new CanInterface(socket, CAN_INTERFACE);
            socket.bind(canIf);
            int pgn = 0;
            byte[] data = { 0 };
            final CanFrame canf = socket.recv();
            data = canf.getData();
            pgn = canf.getPGN();

            System.out.print("\nPGN:");
            System.out.format("%x", pgn);
            System.out.print("\nDATA:");
            // for (byte c : data) {
            //     System.out.format("%x", c);
            // }
            System.out.println(byteArrayToHex(data));
        }
    }

}
