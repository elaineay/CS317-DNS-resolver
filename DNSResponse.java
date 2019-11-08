import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNS response helpful. The class below has a bunch of instance data that typically needs to be
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion.  Feel free to add or delete methods or instance variables to best suit your implementation.



public class DNSResponse {
    // DNS section
    private int queryID;                  // this is for the response it must match the one in the request 
    private int flags;                    // type of response this is
    private int questionCount;            // number of questions
    private int answerCount = 0;          // number of answers
    private int authCount;                // number of authoritative records

    // Queries


    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record

    public class DNSServer {
        String serverName;
        String serverType;
        String serverClass;
        int timeTL;
        int dataLength;
        String serverNameServer;

        public DNSServer(String serverName, String serverType, String serverClass, int timeTL, int dataLength, String serverNameServer){
            this.serverName = serverName;
            this.serverType = serverType;
            this.serverClass = serverClass;
            this.timeTL = timeTL;
            this.dataLength = dataLength;
            this.serverNameServer = serverNameServer;
        }
    }

    private ArrayList<DNSServer> answerServers;
    private ArrayList<DNSServer> authoritativeServers;
    private ArrayList<DNSServer> additionalRecords;

    // Note you will almost certainly need some additional instance variables.

    // When in trace mode you probably want to dump out all the relevant information in a response

	void dumpResponse() {
		


	}

    // The constructor: you may want to add additional parameters, but the two shown are 
    // probably the minimum that you need.

	public DNSResponse (byte[] data, int len) throws IOException {
	    // receive 12 bytes
	    
	    // The following are probably some of the things 
	    // you will need to do.
	    // Extract the query ID

        // TODO: Use a random class to get a random number generator to use between 0 to 65535
        queryID = data[0];
        System.out.println("Hey we made it QueryID:" + queryID);

        System.out.println("Domain Name System (response) \n");

        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(data));
        System.out.println("Transaction ID: " + String.format("%x", dataInput.readShort()));


        // flags = data[1];
        flags = shortToInt(dataInput.readShort());
        System.out.println("Flags: " + flags);

        // QuestionCount;
        questionCount = shortToInt(dataInput.readShort());
        System.out.println("Questions: " + questionCount);

        answerCount = shortToInt(dataInput.readShort());
        System.out.println("Answers RRs: " + answerCount);

        authCount = shortToInt(dataInput.readShort());
        System.out.println("Authority RRs: " + authCount);

        additionalCount = shortToInt(dataInput.readShort());
        System.out.println("Additional RRs: " + additionalCount);

        String queryName = "";

        // References can contain references
        int recLen = 0;
        while ((recLen = dataInput.readByte()) > 0) {
            byte[] record = new byte[recLen];

            for (int i = 0; i < recLen; i++) {
                record[i] = dataInput.readByte();
            }

            queryName += new String(record, "UTF-8") + ".";
        }
        System.out.println("Query Name: " + queryName);


        System.out.println("Record Type: 0x" + String.format("%x", dataInput.readShort()));
        System.out.println("Class: 0x" + String.format("%x", dataInput.readShort()));

        // May have to do the same for Answer servers and additional servers, reuse this logic
        System.out.println("Start authoritative name server section");


        for (int i = 0; i < authCount; i++) {
            // authoritativeServers.add(buildServerResult(data, dataInput));

            Short authNameShort = dataInput.readShort();
            String authName = String.format("%x", authNameShort); 
            // Make this cleaner later, bitwise operator to get c0, use the pointer to get authName
            if ((authNameShort >> 6) == -256) {
                System.out.println("This true");
                int pointer = Integer.decode("0x0" + authName.substring(1));
                int authNameLen = data[pointer];
                authName = "";
                recLen = 0;
                
            }
            System.out.println("authName: " + authName);
            String authType = String.format("%x", dataInput.readShort());
            String authClass = String.format("%x", dataInput.readShort());
            int authTTL = dataInput.readInt();
            int authRDLen = dataInput.readShort();
            System.out.println("authRDLen : " + authRDLen);

            String nameServer = "";
            recLen = 0;
            while ((recLen = dataInput.readByte()) > 0) {
                byte[] record = new byte[recLen];

                for (int j = 0; j < recLen; j++) {
                    record[j] = dataInput.readByte();
                }
                nameServer += (new String(record, "UTF-8")) + ".";
            }
            if ((recLen >> 6) == -1) {
                // We gotta handle the full address like above
                int pointer = dataInput.readByte();
                int ipNameLen = data[pointer];
                for (int j = 1; j <= ipNameLen; j++) {
                    byte tmp = data[pointer + j];
                    nameServer += (char)tmp;
                }

            }
            System.out.println("nameServer is: " + nameServer);
        }



        System.out.println("Start building Additional Records");

        // for (int i = 0; i < additionalCount; i++) {
        //     Short addNameShort = dataInput.readShort();
        //     String authName = String.format("%x", addNameShort); 
        //     System.out.println("WE GO AGAIN: " + authName);
        //     // Make this cleaner later, bitwise operator to get c0, use the pointer to get authName
        //     if ((addNameShort >> 6) == -256) {
        //         int pointer = Integer.decode("0x0" + authName.substring(1));
        //         int additionalNameLen = data[pointer];
        //         System.out.println("Additional pointer: " + additionalNameLen);
        //         authName = "";
        //         for (int n = 1; n <= additionalNameLen; n++) {
        //             byte tmp = data[pointer + n];
        //             authName += (char)tmp;
        //         }
        //     }
        //     System.out.println("authName: " + authName);
        //     String authType = String.format("%x", dataInput.readShort());
        //     String authClass = String.format("%x", dataInput.readShort());
        //     int authTTL = dataInput.readInt();
        //     int authRDLen = dataInput.readShort();
        //     System.out.println("authRDLen : " + authRDLen);

        //     String nameServer = "";
        //     recLen = 0;
        //     while ((recLen = dataInput.readByte()) > 0) {
        //         byte[] record = new byte[recLen];

        //         for (int j = 0; j < recLen; j++) {
        //             record[j] = dataInput.readByte();
        //         }
        //         nameServer += (new String(record, "UTF-8")) + ".";
        //     }
        //     if ((recLen >> 6) == -1) {
        //         int pointer = dataInput.readByte();
        //         int ipNameLen = data[pointer];
        //         for (int j = 1; j <= ipNameLen; j++) {
        //             byte tmp = data[pointer + j];
        //             nameServer += (char)tmp;
        //         }

        //     }
        //     System.out.println("nameServer is: " + nameServer);
        // }


	    // Make sure the message is a query response and determine
	    // if it is an authoritative response or not

	    // determine answer count

	    // determine NS Count

	    // determine additional record count

	    // Extract list of answers, name server, and additional information response 
	    // records

	}

    // Takes in a short and returns it as an integer
    private Integer shortToInt(short s) {
        String str = String.format("%x", s);
        return Integer.parseInt(str);
    }

    private DNSServer buildServerResult(byte[] data, DataInputStream dataInput) throws IOException {
        Short authNameShort = dataInput.readShort();
        String authName = String.format("%x", authNameShort); 
        int recLen = 0;
        // Make this cleaner later, bitwise operator to get c0, use the pointer to get authName
        if ((authNameShort >> 6) == -256) {
            System.out.println("This true");
            int pointer = Integer.decode("0x0" + authName.substring(1));
            int authNameLen = data[pointer];
            authName = "";
        }
        System.out.println("authName: " + authName);
        String authType = String.format("%x", dataInput.readShort());
        String authClass = String.format("%x", dataInput.readShort());
        int authTTL = dataInput.readInt();
        int authRDLen = dataInput.readShort();
        System.out.println("authRDLen : " + authRDLen);

        String nameServer = "";
        recLen = 0;
        while ((recLen = dataInput.readByte()) > 0) {
            byte[] record = new byte[recLen];

            for (int j = 0; j < recLen; j++) {
                record[j] = dataInput.readByte();
            }
            nameServer += (new String(record, "UTF-8")) + ".";
        }
        if ((recLen >> 6) == -1) {
            // We gotta handle the full address like above
            int pointer = dataInput.readByte();
            int ipNameLen = data[pointer];
            for (int j = 1; j <= ipNameLen; j++) {
                byte tmp = data[pointer + j];
                nameServer += (char)tmp;
            }

        }
        System.out.println("nameServer is: " + nameServer);

        DNSServer oServer = new DNSServer("s", "s", "s", 0, 0, "s");
        return oServer;
    }

    // You will probably want a method to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records. 
}
