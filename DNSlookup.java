import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
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

        if (tracingOn) {
            System.out.println("Query ID     " + response.getQueryID() + " " + fqdn + "  " + response.getRecordType() + " --> " + rootNameServer);
            System.out.println("Response ID: " + response.getQueryID() + " Authoritative = " + (response.getAnswerCount() > 0));
            
            System.out.println("  Answers (" + response.getAnswerCount() + ")");
            ArrayList<DNSServer> answerServers = response.getAnswerServers();
            for (int i = 0; i < response.getAnswerCount(); i++) {
                DNSServer currentServer = answerServers.get(i);
                System.out.println("       " + currentServer.serverName + "                    " + currentServer.timeTL
                 + "        " + currentServer.serverType + " " + currentServer.serverNameServer);
            }

            System.out.println("  Nameservers (" + response.getAuthCount() + ")");
            ArrayList<DNSServer> authoritativeServers = response.getAuthoritativeServers();
            for (int i = 0; i < response.getAuthCount(); i++) {
                DNSServer currentServer = authoritativeServers.get(i);
                System.out.println("       " + currentServer.serverName + "                    " + currentServer.timeTL
                 + "        " + currentServer.serverType + " " + currentServer.serverNameServer);
            }

            System.out.println("  Additional Information (" + response.getAdditionalCount() + ")");
            ArrayList<DNSServer> additionalRecords = response.getAdditionalRecords();
            for (int i = 0; i < response.getAdditionalCount(); i++) {
                DNSServer currentServer = additionalRecords.get(i);
                System.out.println("       " + currentServer.serverName + "             " + currentServer.timeTL
                 + "        " + currentServer.serverType + " " + currentServer.serverNameServer);
            }
            System.out.println("\n\n");

        }


        // Format packet into byte array input stream
        DataInputStream dInput = new DataInputStream(new ByteArrayInputStream(responseBytes));

        // if the response is an A response return, else iterate
        //should take a response that gives an NS and then queries until finds an A record
        // lookup the first of the additional information servers

        //ITERATIVE PART
        // if there is an answer print it

        // doing just IPV4 rn
        // Response cases:
        // There is >0 answer
        //      1. if (fqdn = answer name && answer.type = A/AAAA)
        //          --> DONE
        //      2. if (fqdn = answer name && answer.type = CN)
        //          --> lookup(rootNameServer, answer.domain name, tracingOn, IPV6Query)
        //      3. if (ns1 name = answer name)
        //          --> lookup(answer.ip, fqdn, tracingOn, IPV6Query)
        //
        // 0 answers:
        //      1. look in nameservers, pick first= ns1. Check additional for IP. (A or AAAA)
        //         if (IP) --> lookup(ns1.IP, fqdn, tracingOn, IPV6Query)
        //         if (additionalInfo = 0) --> lookup(rootNameServer, ns1, tracingOn, IPV6Query)


//        if (response.getAnswerCount() > 0) {
//            if (response.answer.name == fqdn) {
//                if (response.answer.type == 'A'){ // if response answer type A or AAAA done
//                    // TODO: response.TTL, response.type, response.IP
//                    // TODO: IPV6 version
//                    System.out.println(fqdn + " " + response.TTL + "   " + response.type + " " + response.IP);
//                    // you're done if you reach here
//                } else if (response.answer.type == 'CN'){
//                    lookup(rootNameServer, answer.domainName, tracingOn, IPV6Query);
//                }
//            } else if (response.answer.name = ns1.name){
//                lookup(answer.IP, fqdn, tracingOn, IPV6Query);
//            }
//        } else {
//            if (response.getAdditionalCount() > 0) {
//                // need to get the IP of the first name server from additional section
//                // i think also a case where lookup the ns1 name with the res ns1 ip
//                lookup(ns1.IP, fqdn, tracingOn, IPV6Query);
//            } else {
//                lookup(rootNameServer, ns1.name, tracingOn, IPV6Query);
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

