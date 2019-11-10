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
    static String authServer1Name;
    static boolean tracingOn = false;
    static boolean IPV6Query = false;
    static InetAddress rootNameServer;
    static String fqdn;
    static String lookForIPofCN;

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
        lookup();
    }

	// Start adding code here to initiate the lookup

    public static void lookup() throws IOException {
        DNSResponse response = sendAndReceivePacket(rootNameServer, fqdn);

        validFlagsCheck(response.getFlags(), fqdn, rootNameServer);
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
            if (answerServers.get(0).serverName == fqdn && answerServers.get(0).serverType == "0x0001") {
                // TODO: IPV6 version
                System.out.println("YA DONE");
                System.out.println(fqdn + " " + answerServers.get(0).timeTL + "   " + answerServers.get(0).serverType + " " + answerServers.get(0).serverNameServer);
                // you're done if you reach here
            }
        } else {
            // if not answer look it up iteratively
            System.out.println("Iterator is called");
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
        ArrayList<DNSServer> authoritativeServers = currResponse.getAuthoritativeServers();
        ArrayList<DNSServer> additionalRecords = currResponse.getAdditionalRecords();

        try{
            // keep iterating when you don't have an answer
            DNSResponse nextResponse;
            String currRespDomainName = currResponse.getQueryName();
            System.out.println("NAME WE ARE QUERYING: " +currRespDomainName);
            if (currResponse.getAnswerCount() > 0) {
                InetAddress ansIP = InetAddress.getByName(answerServers.get(0).serverNameServer);
                // if this is an authority record
                if (answerServers.get(0).serverType == "A") {
                    // if this is what we're looking for then done!
                    if (answerServers.get(0).serverName == currRespDomainName && answerServers.get(0).serverName != lookForIPofCN) {
                        // TODO: IPV6 version
                        System.out.println("YA DONE");
                        System.out.println(fqdn + " " + answerServers.get(0).timeTL + "   " + answerServers.get(0).serverType + " " + answerServers.get(0).serverNameServer);
                        // you're done if you reach here
                    } else if (answerServers.get(0).serverName == lookForIPofCN){
                        // if we find the IP address of the CN we were looking for iterate again
                        nextResponse = sendAndReceivePacket(ansIP, fqdn);
                        validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                        if (tracingOn) {
                            printResponseInfo(nextResponse);
                        }
                        iterateLookup(nextResponse);
                    }
                } else if (answerServers.get(0).serverName == currRespDomainName && answerServers.get(0).serverType == "CN"){
                    // if get a CN answer search for the CN domain with root IP
                    nextResponse = sendAndReceivePacket(rootNameServer, answerServers.get(0).serverNameServer);
                    validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                }
            } else {
                // if answerCount = 0
                if (currResponse.getAdditionalCount() > 0) {
                    InetAddress additionalIP = InetAddress.getByName(additionalRecords.get(0).serverNameServer);
                    nextResponse = sendAndReceivePacket(additionalIP, currRespDomainName);
                    System.out.println("NextReponse flags:" + String.format("%x", nextResponse.getFlags()));
                    // validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                } else {
                    String authIP = authoritativeServers.get(0).serverNameServer;
                    // when answer = 0 and additional = 0
                    // need to keep looking for this IP until reach A record
                    lookForIPofCN = authIP;
                    nextResponse = sendAndReceivePacket(rootNameServer, lookForIPofCN);
                    // validFlagsCheck(nextResponse.getFlags(), fqdn, rootNameServer);
                    if (tracingOn) {
                        printResponseInfo(nextResponse);
                    }
                    iterateLookup(nextResponse);
                }
            }
        } catch (Exception e){}

    }

    private static DNSResponse sendAndReceivePacket(InetAddress server, String domainName) throws IOException{
        DatagramSocket socket = new DatagramSocket();
        DNSRequest request = new DNSRequest(domainName);
        DatagramPacket reqPacket = request.createSendable(request, server, port);
        socket.send(reqPacket);

        // Get response from DNS server
        byte[] responseBytes = new byte[1024];
        DatagramPacket respPacket = new DatagramPacket(responseBytes, responseBytes.length);

        socket.receive(respPacket);

        //TODO: checking if received
        // System.out.println("\n\nReceived: " + respPacket.getLength() + " bytes");

        // for (int i = 0; i < respPacket.getLength(); i++) {
        //     System.out.print(" 0x" + String.format("%x", responseBytes[i]) + " ");
        // }
        // System.out.println("\n");
        // System.out.println("We are using responsebyte: "  + responseBytes + " with length: " + respPacket.getLength());
        DNSResponse response = new DNSResponse(responseBytes, respPacket.getLength());
        return response;
    }

    private static void printResponseInfo(DNSResponse response) {
        System.out.println("Query ID     " + response.getQueryID() + " " + fqdn + "  " + response.getRecordType() + " --> " + rootNameServer.getHostAddress());
        System.out.println("Response ID: " + response.getQueryID() + " Authoritative = " + (response.getAnswerCount() > 0));

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
        System.out.println("\n");
    }

    private static void validFlagsCheck(Short flags, String fqdn, InetAddress rootNameServer) {
        switch (flags & 0xFF) {
            case 0:
                break;
            case 3:
                System.err.println(fqdn + " -3 A " + rootNameServer.getHostAddress());
                System.exit(-1);
            default:
                System.err.println(fqdn + " -2 A " + rootNameServer.getHostAddress());
                System.exit(-1);
        }
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

