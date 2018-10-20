/* Comment block
   CS656-003 Group M5
   Dhavalkumar (dpp56), Frank (fjr7), John (jp828), Kamran (kr255)
*/
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Web {
    private static ServerSocket serverSocket;
    private static InetAddress inetAddress;
    private static InetSocketAddress inetSocketAddress;
    private static Socket clientSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static CharsetDecoder charsetDecoder;
    private static CharsetEncoder charsetEncoder;

    private static final byte[] notFound = "NO IP ADDRESSES FOUND\n".getBytes();

    public static void runServer(int port) {
        try {
            serverSocket = new ServerSocket();
            inetAddress = InetAddress.getLocalHost();
            inetSocketAddress = new InetSocketAddress(inetAddress.getHostAddress(), port);
            serverSocket.bind(inetSocketAddress, port);
            System.out.println("DNS Server Listening on socket " + port);
            int requestCount = 0;
            while(true) {
                requestCount++;
                clientSocket = serverSocket.accept();
                inputStream = clientSocket.getInputStream();
                outputStream = clientSocket.getOutputStream();
                System.out.println("(" + requestCount + ") Incoming client connection from ["
                        + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + "] to me ["
                        + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + "]");
                byte[] buffer = new byte[1024];
                inputStream.read(buffer, 0, 1024);
                byte[] request = trimByteArray(buffer);
                if(byteCompare(request, "exit".getBytes())) break;
                if(!dns(request))
                    outputStream.write(notFound, 0, notFound.length);
                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error: Could not initialize server.");
        }
    }

    public static byte[] trimByteArray(byte[] array){ // Trims the byte array by creating a new array with all content before CR
        int arrayLength = 0;
        while(array[arrayLength] != 13) {
            arrayLength++;
            if (arrayLength == 1024) break;
        }
        byte[] r = new byte[arrayLength];
        for(int i = 0; i < r.length; i++)
            r[i] = array[i];
        return r;
    }

    public static boolean byteCompare(byte[] a, byte[] b){ // Compares two byte arrays
        if(a.length != b.length) return false;
        for(int i = 0; i < a.length; i++)
            if(a[i] != b[i]) return false;
        return true;
    }

    public static byte[] encode(char[] input){
        try {
            return charsetEncoder.encode(CharBuffer.wrap(input)).array();
        } catch (CharacterCodingException e) {
            System.err.println("Error: Could not encode data");
        }
        return new byte[0];
    }

    public static char[] decode(byte[] input){
        try {
            return charsetDecoder.decode(ByteBuffer.wrap(input)).array();
        } catch (CharacterCodingException e) {
            System.err.println("Error: Could not decode data");
        }
        return new char[0];
    }

    public static boolean dns(byte[] request) {
        if(request.length == 0) return false;
        InetAddress[] resultList;
        char[] requestDecoded;
        try {
            requestDecoded = decode(request);
            System.out.print("REQ ");
            System.out.println(requestDecoded);
            System.out.println();
            resultList = InetAddress.getAllByName(String.valueOf(requestDecoded));
            for(int i = 0; i < resultList.length; i++){
                outputStream.write("IP = ".getBytes());
                outputStream.write(encode(resultList[i].getHostAddress().toCharArray()));
                outputStream.write("\n".getBytes());
            }
            outputStream.write("PREFERRED IP = ".getBytes());
            outputStream.write(encode(resultList[0].getHostAddress().toCharArray()));
            outputStream.write("\n".getBytes());
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args){
        if(args.length == 0){
            System.err.println("Error: Please enter a port number.");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);

        charsetDecoder = Charset.forName("US-ASCII").newDecoder(); // Initializes our encoder and decoder to parse 7 bit ascii 
        charsetEncoder = Charset.forName("US-ASCII").newEncoder();

        runServer(port);
    }
}