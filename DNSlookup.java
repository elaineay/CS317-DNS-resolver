import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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
    static int numLookUps = 1;
    static String authServer1Name;
    static boolean tracingOn = false;
    static boolean IPV6Query = false;
    static String queryType = "A";
    static InetAddress rootNameServer;
    static String fqdn;
    static String lookForIPofCN;
    static boolean nsSwitch = false;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        DNSResponse response; // Just to force compilation
        int argCount = args.length;

        if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
            usage();
            return;
        }

        // IP address of DNS server to start search at, may/may not be root DNS server
        rootNameServer = InetAddress.getByName(args[0]);
        fqdn = args[1]; // fully qualified domain name to look up

        if (argCount == 3) {  // option provided
            if (args[2].equals("-t")) {// print trace of queries, responses, result
                tracingOn = true;
        } else if (args[2].equals("-6")) { // retrieve IPV6 address
                IPV6Query = true;
                queryType = "AAAA";
        } else if (args[2].equals("-t6")) { // trace and IPV6
                tracingOn = true;
                IPV6Query = true;
                queryType = "AAAA";
            } else  { // option present but wasn't valid option
                usage();
                return;
            }
        }
        lookup();
    }

	// Start adding code here to initiate the lookup

    public static void lookup() throws IOException {
        DNSResponse response = sendAndReceivePacket(rootNameServer, fqdn, IPV6Query);

        validFlagsCheck(response.getFlags(), fqdn, response);
        if (tracingOn) {
            printResponseInfo(response);
        }
        ArrayList<DNSServer> answerServers = response.getAnswerServers();

//        System.out.println("answer server class " + answerServers.get(0).serverClass);
//        System.out.println("answer server type " + answerServers.get(0).serverType);

        // if this is the answer return, else iterate until answer found
        if (response.getAnswerCount() > 0) {
            // if response answer is the same as original search and type A or AAAA done
            //TODO: for loop returning all the answers if more than 1
//            if (answerServers.get(0).serverName.equals(fqdn) && answerServers.get(0).serverType.equals(queryType)) {
//                // TODO: IPV6 version
//                System.out.println(fqdn + " " + answerServers.get(0).timeTL + "   " + answerServers.get(0).serverType + " " + answerServers.get(0).serverNameServer);
//                // you're done if you reach here
//            }
            for(int i = 0; i < response.getAnswerCount(); i++){
                if (answerServers.get(i).serverType == queryType){
                    DNSServer ans = answerServers.get(i);
                    System.out.println(fqdn + " " + ans.timeTL + "   " + ans.serverType + " " + ans.serverNameServer);
                }
            }
        } else {
            // if not answer look it up iteratively
            iterateLookup(response);
        }
    }

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
        //          keep track of CN and keep looking for that until you get an answer
        //          --> lookup(rootNameServer, answer.domain name, tracingOn, IPV6Query)
        //      3. if (ns1 name = answer name)
        //          --> lookup(answer.ip, fqdn, tracingOn, IPV6Query)
        //
        // 0 answers:
        //      1. look in nameservers, pick first= ns1. Check additional for IP. (A or AAAA)
        //         if (IP) --> lookup(ns1.IP, fqdn, tracingOn, IPV6Query)
        //         if (additionalInfo = 0) --> lookup(rootNameServer, ns1, tracingOn, IPV6Query)
        //              once found an IP, keep using ns1 domain to search until answer.name = ns1.name
        //                  then switch back to using fqdn

    //TODO: put the trace thing in for each iteration
    public static void iterateLookup(DNSResponse currResponse){
        ArrayList<DNSServer> answerServers = currResponse.getAnswerServers();
        numLookUps++;
        if (numLookUps == 30) {
            System.err.println(fqdn + " -3 A 0.0.0.0");
            System.exit(-1);
        }

        try{
            // keep iterating when you don't have an answer
            DNSResponse nextResponse;
            String currRespDomainName = currResponse.getQueryName();
            if (currResponse.getAnswerCount() > 0) {
                DNSServer ansCompatible = getCompatible(currResponse.getAnswerCount(), currResponse.getAnswerServers());
                System.out.println(ansCompatible);
                InetAddress ansIP = InetAddress.getByName(ansCompatible.serverNameServer);
                // if this is an authority record
                if (ansCompatible.serverType.equals(queryType)) {
                    // if this is what we're looking for then done!
                    if (!ansCompatible.serverName.equals(lookForIPofCN)) {

                        for (int i = 0; i < currResponse.getAnswerCount(); i++) {
                            System.out.println(fqdn + " " + answerServers.get(i).timeTL + "   " + answerServers.get(i).serverType + " " + answerServers.get(i).serverNameServer);
                            i++;
                        }
                        // you're done if you reach here
                    } else {
                        // if we find the IP address of the CN we were looking for iterate again
                        // done looking for NS
                        System.out.println("HELLO");
                        if (IPV6Query) { nsSwitch = true;}

                        nextResponse = sendAndReceivePacket(ansIP, fqdn, IPV6Query);
                        validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                        if (tracingOn) {
                            printResponseInfo(nextResponse);
                        }
                        iterateLookup(nextResponse);
                    }
                } else if (ansCompatible.serverName.equals(currRespDomainName) && ansCompatible.serverType.equals("CN")){
                    // if get a CN answer search for the CN domain with root IP

                    nextResponse = sendAndReceivePacket(rootNameServer, ansCompatible.serverNameServer, IPV6Query);
                    validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);

                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                }
            } else {
                // if answerCount = 0
                if (currResponse.getAdditionalCount() > 0) {
                    DNSServer addCompatible = getCompatible(currResponse.getAdditionalCount(), currResponse.getAdditionalRecords());
                    System.out.println("addCompatible: " + addCompatible);
                    InetAddress additionalIP;
                    additionalIP = InetAddress.getByName(addCompatible.serverNameServer);
                    System.out.println("additionalIP: "+additionalIP);

                    if (IPV6Query) {
                        nextResponse = sendAndReceivePacket(additionalIP, currRespDomainName, nsSwitch);
                    } else {
                        System.out.println("got here");
                        nextResponse = sendAndReceivePacket(additionalIP, currRespDomainName, IPV6Query);
                    }

                    validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                } else {
                    // when answer = 0 and additional = 0
                    // need to keep looking for this IP until reach A record
                    DNSServer authCompatible = getCompatible(currResponse.getAuthCount(), currResponse.getAuthoritativeServers());
                    System.out.println(authCompatible);
                    String authIPName = authCompatible.serverNameServer;
                    lookForIPofCN = authIPName;

                    if (IPV6Query) {
                        nsSwitch = false;
                        nextResponse = sendAndReceivePacket(rootNameServer, lookForIPofCN, nsSwitch);
                    } else {
                        nextResponse = sendAndReceivePacket(rootNameServer, lookForIPofCN, IPV6Query);
                    }

                    validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                }
            }
        } catch (Exception e){}

    }

    private static DNSResponse sendAndReceivePacket(InetAddress server, String domainName, boolean isIPV6Query) throws IOException, SocketTimeoutException {
        DatagramSocket socket = new DatagramSocket();

        DNSRequest request = new DNSRequest(domainName, isIPV6Query);

        DatagramPacket reqPacket = request.createSendable(request, server, port);
        int sendAttempt = 0;
        socket.send(reqPacket);
        

        // Get response from DNS server
        byte[] responseBytes = new byte[1024];
        DatagramPacket respPacket = new DatagramPacket(responseBytes, responseBytes.length);
        socket.setSoTimeout(5000);
        while (true) {
            try {
                socket.receive(respPacket);
                break;
            } catch (SocketTimeoutException e) {
                sendAttempt++;
                if (tracingOn) {
                    System.out.println("\n");
                    System.out.println("Query ID     " + (int)request.getTransactionID() + " " + domainName + "  " + request.getIPVType() + " --> " + server.getHostAddress());
                }
                if (sendAttempt < 2) {
                    socket.send(reqPacket);
                    continue;
                } else {
                    System.err.println(domainName + " -2 A 0.0.0.0");
                    System.exit(-1);
                }
            }
        }
        // socket.receive(respPacket);

        //TODO: checking if received
        // System.out.println("\n\nReceived: " + respPacket.getLength() + " bytes");

        // for (int i = 0; i < respPacket.getLength(); i++) {
        //     System.out.print(" 0x" + String.format("%x", responseBytes[i]) + " ");
        // }
        // System.out.println("\n");
        // System.out.println("We are using responsebyte: "  + responseBytes + " with length: " + respPacket.getLength());
        DNSResponse response = new DNSResponse(responseBytes, respPacket.getLength());
        if (tracingOn) {
            System.out.println("\n");
            System.out.println("Query ID     " + (int)response.getQueryID() + " " + fqdn + "  " + response.getRecordType() + " --> " + server.getHostAddress()); 
        }
        return response;
    }

    private static void printResponseInfo(DNSResponse response) {
        System.out.println("Response ID: " + (int)response.getQueryID() + " Authoritative = " + (response.getAnswerCount() > 0));

        System.out.println("  Answers (" + response.getAnswerCount() + ")");
        ArrayList<DNSServer> answerServers = response.getAnswerServers();
        for (int i = 0; i < response.getAnswerCount(); i++) {
            DNSServer currentServer = answerServers.get(i);
            printTraceServerInfo(currentServer);
        }

        System.out.println("  Nameservers (" + response.getAuthCount() + ")");
        ArrayList<DNSServer> authoritativeServers = response.getAuthoritativeServers();
        for (int i = 0; i < response.getAuthCount(); i++) {
            DNSServer currentServer = authoritativeServers.get(i);
            printTraceServerInfo(currentServer);
        }

        System.out.println("  Additional Information (" + response.getAdditionalCount() + ")");
        ArrayList<DNSServer> additionalRecords = response.getAdditionalRecords();
        for (int i = 0; i < response.getAdditionalCount(); i++) {
            DNSServer currentServer = additionalRecords.get(i);
            printTraceServerInfo(currentServer);
        }
    }

    private static void validFlagsCheck(Short flags, String fqdn, DNSResponse response) {
        switch (flags & 0x0F) {
            case 0:
                break;
            case 3:
                System.err.println(fqdn + " -1 A 0.0.0.0");
                System.exit(-1);
            default:
                System.err.println(fqdn + " -4 A 0.0.0.0");
                System.exit(-1);
        }
    }

    private static DNSServer getCompatible(int maxCount, ArrayList<DNSServer> loopServers) {
        DNSServer best = loopServers.get(0);
        for (int i=0;i < maxCount;i++){
            if (loopServers.get(i).serverType == queryType){
                best = loopServers.get(i);
            }
        }
        return best;
    }

    private static void printTraceServerInfo(DNSServer currentServer) {
        String toPrint = String.format("%7s%-31s%-11s%-5s%-25s", " ", currentServer.serverName, currentServer.timeTL, currentServer.serverType, currentServer.serverNameServer);
        System.out.println(toPrint);
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

