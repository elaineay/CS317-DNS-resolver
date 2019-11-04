import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
/**
 *
 */
/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {
    
    
    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
    static final int port = 53;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String fqdn;
        DNSResponse response; // Just to force compilation
        int argCount = args.length;
        boolean tracingOn = false;
        boolean IPV6Query = false;
        InetAddress rootNameServer;


        if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
            usage();
            return;
        }

        // IP address of DNS server to start search at, may/may not be root DNS server
        rootNameServer = InetAddress.getByName(args[0]);
        fqdn = args[1]; // fully qualified domain name to look up

        if (argCount == 3) {  // option provided
            if (args[2].equals("-t")) // print trace of queries, responses, result
                tracingOn = true;
            else if (args[2].equals("-6")) // retrieve IPV6 address
                IPV6Query = true;
            else if (args[2].equals("-t6")) { // trace and IPV6
                tracingOn = true;
                IPV6Query = true;
            } else  { // option present but wasn't valid option
                usage();
                return;
            }
        }

        lookup(rootNameServer, fqdn, tracingOn, IPV6Query);
    }

	// Start adding code here to initiate the lookup

    public static void lookup(InetAddress rootNameServer, String fqdn, boolean tracingOn, boolean IPV6Query) throws IOException {
// MOVED to DNSRequest class this works though
        //        ByteArrayOutputStream bAOutput = new ByteArrayOutputStream();
//        DataOutputStream dOutput = new DataOutputStream(bAOutput);
//
//        // REFERENCE https://stackoverflow.com/questions/36743226/java-send-udp-packet-to-dns-server
//        // building DNS request
//        dOutput.writeShort(0x1234); // Transaction ID of query (16 bits)
//        dOutput.writeShort(0x0100); // flag, standard query
//        dOutput.writeShort(0x0001); // # questions
//        dOutput.writeShort(0x0000); // # answer records
//        dOutput.writeShort(0x0000); // # authority records
//        dOutput.writeShort(0x0000); // # additional records
//
//        // change into bytes
//        // REFERENCE https://beginnersbook.com/2013/12/java-string-getbytes-method-example/
//        String[] dnSections = fqdn.split("\\.");
//        dOutput.writeByte(dnSections.length);
//
//        // write the domain into the DNS request
//        for (String s : dnSections) {
//            byte[] byteArray = s.getBytes("UTF-8");
//            dOutput.write(byteArray);
//        }
//
//        dOutput.writeByte(0x00); //signify end of DNS request
//        dOutput.writeShort(0x0001); // record type A (host request)
//        dOutput.writeShort(0x0001); // class IN
//
//        byte[] outputFrame = bAOutput.toByteArray();
//
//        // TODO: just checking if sending
//        System.out.println("Sending: " + outputFrame.length + " bytes");
//        for (int i =0; i< outputFrame.length; i++) {
//            System.out.print("0x" + String.format("%x", outputFrame[i]) + " " );
//        }
//
//        // Send DNS Request
        DatagramSocket socket = new DatagramSocket();
//        DatagramPacket reqPacket = new DatagramPacket(outputFrame,
//                                                   outputFrame.length,
//                                                   rootNameServer,
//                                                   port);
        DNSRequest request = new DNSRequest();
        request.DNSRequest(fqdn);
        DatagramPacket reqPacket = request.createSendable(request, rootNameServer, port);
        socket.send(reqPacket);

        // Get response from DNS server
        byte[] responseBytes = new byte[1234];
        DatagramPacket respPacket = new DatagramPacket(responseBytes, responseBytes.length);
        socket.receive(respPacket);

        //TODO: checking if received
        System.out.println("\n\nReceived: " + respPacket.getLength() + " bytes");

        for (int i = 0; i < respPacket.getLength(); i++) {
            System.out.print(" 0x" + String.format("%x", responseBytes[i]) + " " );
        }
        System.out.println("\n");

        //TODO make the respPacket into a DNSResponse
//        for (byte b : responseBytes) {
//            // do something where I split up the bytes and place them into a response
//        }

        DNSResponse response = new DNSResponse(responseBytes, respPacket.getLength());


        // Format packet into byte array input stream
        DataInputStream dInput = new DataInputStream(new ByteArrayInputStream(responseBytes));



        // Print with trace
        if (tracingOn) {
            //

        } else if (!tracingOn) {
            // Print without trace -t: name_being_looked_up TTL ADDRESS_TYPE IP_address
            System.out.println(fqdn + " ");
        }



    }

    // lookup should print name, space, TTL, 3 spaces, type, space, resolved IP address
    // IPv4 address: type "A"
    // IPV6 address: type "AAAA"
    //    System.out.println();
	
    
    private static void usage() {
        System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
        System.out.println("   where");
        System.out.println("       rootDNS - the IP address (in dotted form) of the root");
        System.out.println("                 DNS server you are to start your search at");
        System.out.println("       name    - fully qualified domain name to lookup");
        System.out.println("       -6      - return an IPV6 address");
        System.out.println("       -t      - trace the queries made and responses received");
        System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
    }
}

