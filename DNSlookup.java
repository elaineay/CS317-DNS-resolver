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
        DatagramSocket socket = new DatagramSocket();
        DNSRequest request = new DNSRequest(fqdn);
        DatagramPacket reqPacket = request.createSendable(request, rootNameServer, port);
        socket.send(reqPacket);

        // Get response from DNS server
        byte[] responseBytes = new byte[1024];
        DatagramPacket respPacket = new DatagramPacket(responseBytes, responseBytes.length);

        socket.receive(respPacket);

        //TODO: checking if received
        System.out.println("\n\nReceived: " + respPacket.getLength() + " bytes");

        for (int i = 0; i < respPacket.getLength(); i++) {
            System.out.print(" 0x" + String.format("%x", responseBytes[i]) + " " );
        }
        System.out.println("\n");

        //TODO make the respPacket into a DNSResponse

        DNSResponse response = new DNSResponse(responseBytes, respPacket.getLength());


        // Format packet into byte array input stream
        DataInputStream dInput = new DataInputStream(new ByteArrayInputStream(responseBytes));

//        // if the response is an A response return, else iterate
//        //should take a response that gives an NS and then queries until finds an A record
//        // lookup the first of the additional information servers
//
//        //ITERATIVE PART
//        // if there is an answer print it
//
//        if (response.getAnswerCount() > 0) {
//            if(tracingOn) {
//
//            } else if (response.answer.type == 'A'){ // if response answer type A or AAAA done
//                // TODO: response.TTL, response.type, response.IP
//                // TODO: IPV6 version
//                System.out.println(fqdn + " " + response.TTL + "   " + response.type + " " + response.IP);
//            }
//            //no answer
//        } else {
//            // TODO: authNameServer.IP, authNameServer.type
//            if (authNameServer.[0].type == 'A') { // IPV4 address
//                lookup(authNameServer[0].IP, fqdn, tracingOn, IPV6Query);
//            } else if (authNameServer[0].type == 'NS') {
//                // look through the additional section of the response to see if
//                // it contains the IP for the domain name given
//                lookup(the found IP, fqdn, tracingOn, IPV6Query);
//            }
//        }
    }

	
    
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

