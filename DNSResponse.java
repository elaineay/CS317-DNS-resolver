import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
    private int answerCount;              // number of answers
    private int authCount;                // number of authoritative records

    // Queries
    private String queryName = "";
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount;                  // number of nscount response records
    private int additionalCount;          // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record
    private String recordType;

    // public class DNSServer {
    //     String serverName;
    //     String serverType;
    //     String serverClass;
    //     int timeTL;
    //     int dataLength;
    //     String serverNameServer;

    //     public DNSServer(String serverName, String serverType, String serverClass, int timeTL, int dataLength, String serverNameServer){
    //         this.serverName = serverName;
    //         this.serverType = serverType;
    //         this.serverClass = serverClass;
    //         this.timeTL = timeTL;
    //         this.dataLength = dataLength;
    //         this.serverNameServer = serverNameServer;
    //     }
    // }
    private ArrayList<ArrayList<DNSServer>> allRecords = new ArrayList<ArrayList<DNSServer>>();

    private ArrayList<DNSServer> answerServers = new ArrayList<DNSServer>();
    private ArrayList<DNSServer> authoritativeServers = new ArrayList<DNSServer>();
    private ArrayList<DNSServer> additionalRecords = new ArrayList<DNSServer>();

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

        recordType = getTypeValue(dataInput.readShort());
        System.out.println("Record Type: " + recordType);
        System.out.println("Class: 0x" + String.format("%x", dataInput.readShort()));

        System.out.println("Start answer name server section");
        for (int i = 0; i < answerCount; i++) {
            DNSServer currentServer = buildServerResult(data, dataInput, true);
            answerServers.add(currentServer);
            System.out.println("Answer Server: " + answerServers.get(i).serverNameServer);
        }


        System.out.println("Start authoritative name server section");
        for (int i = 0; i < authCount; i++) {
            System.out.println("This ran: " + i);
            DNSServer currentServer = buildServerResult(data, dataInput, false);
            authoritativeServers.add(currentServer);
            System.out.println("Authority Server: " + authoritativeServers.get(i).serverNameServer);
        }

        System.out.println("Start building Additional Records");
        for (int i = 0; i < additionalCount; i++) {
            DNSServer currentServer = buildServerResult(data, dataInput, true);
            additionalRecords.add(currentServer);
            System.out.println("Additional Records: " + additionalRecords.get(i).serverNameServer);
        }

        allRecords.add(answerServers);
        allRecords.add(authoritativeServers);
        allRecords.add(additionalRecords);
	}

    // Takes in a short and returns it as an integer
    private Integer shortToInt(short s) {
        String str = String.format("%x", s);
        return Integer.parseInt(str);
    }

    private DNSServer buildServerResult(byte[] data, DataInputStream dataInput, boolean usesIP) throws IOException {
        Short authNameShort = dataInput.readShort();
        String authName = String.format("%x", authNameShort); 
        // Make this cleaner later, bitwise operator to get c0, use the pointer to get authName
        if ((authNameShort >> 14) == -1) {
            int pointer = Integer.decode("0x0" + authName.substring(1));
            int authNameLen = data[pointer];
            authName = handleCompression(data, pointer);
            // Remove the extra "." at the end
            authName = authName.substring(0, authName.length() - 1);
        }
        System.out.println("authName: " + authName);
        int authTypeVal = dataInput.readShort();
        String authType = getTypeValue(authTypeVal);
        System.out.println("authType: "  + authType);
        String authClass = String.format("%x", dataInput.readShort());

        System.out.println("authClass: " + authClass);
        int authTTL = dataInput.readInt();
        System.out.println("authTTL: " + authTTL);
        int authRDLen = dataInput.readShort();
        System.out.println("authRDLen : " + authRDLen);
        String nameServer = "";

        if (usesIP & authType == "AAAA") {
            for (int i = 0; i < authRDLen/2 - 1; i++) {
                short ipv6Short = dataInput.readShort();
                String ipv6address = String.format("%02X ", ipv6Short);
                if (ipv6Short != 0) {
                    nameServer += ipv6address + ":";
                }
            }
            nameServer += ":" + String.format("%02X ", dataInput.readShort());
            // remove the whitespace
            nameServer = nameServer.replaceAll("\\s+","");
        } else if (usesIP) {
            for (int i = 0; i < authRDLen; i++) {
                int ipVal = dataInput.readByte() & 0xFF;
                nameServer += ipVal + ".";
            }
            // remove extra "." at the end
            nameServer = nameServer.substring(0, nameServer.length() - 1);
        } else {
            int recLen = 0;
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
                nameServer += handleCompression(data, pointer);
            }
            // remove extra "." at the end
            nameServer = nameServer.substring(0, nameServer.length() - 1);
        }
        DNSServer oServer = new DNSServer(authName, authType, authClass, authTTL, authRDLen, nameServer);
        return oServer;
    }

    private String handleCompression(byte[] data, int pointer) throws UnsupportedEncodingException{
        int n = 0;
        int dataLen = 0;
        String retString = "";
        while ((dataLen = data[pointer + n]) > 0) {
            byte[] retByte = new byte[dataLen];

            for (int j = 0; j < dataLen; j++) {
                n++;
                retByte[j] = data[pointer + n];
            }
            retString += (new String(retByte, "UTF-8")) + ".";
            n++;
        }
        if ((data[pointer + n] >> 14) == -1) {
            // Use the next byte as the pointer value
            n++;
            retString += handleCompression(data, data[pointer + n]);
        }
        return retString;
    }

    private String getTypeValue(int typeNum) {
        String authType = "";
        switch(typeNum) {
            case 1:
                authType = "A";
                break;
            case 2:
                authType = "NS";
                break;
            case 3:
                authType = "MD";
                break;
            case 4:
                authType = "MF";
                break;
            case 5:
                authType = "CNAME";
                break;
            case 6:
                authType = "SOA";
                break;
            case 7:
                authType = "MB";
                break;
            case 8:
                authType = "MG";
                break;
            case 9:
                authType = "MR";
                break;
            case 10:
                authType = "NULL";
                break;
            case 11:
                authType = "WKS";
                break;
            case 12:
                authType = "PTR";
                break;
            case 13:
                authType = "HINFO";
                break;
            case 14:
                authType = "MINFO";
                break;
            case 15:
                authType = "MX";
                break;
            case 16:
                authType = "TXT";
                break;
            case 28:
                authType = "AAAA";
                break;
            default:
                System.out.println("Something is wrong.");
                authType = "NULL";
        }
        return authType;
    }

    public int getAnswerCount() {
	    return answerCount;
    }

    public int getAdditionalCount() {
	    return additionalCount;
	}

    public int getAuthCount() {
        return authCount;
    }

    public int getQueryID() {
        return queryID;
    }

    public String getQueryName() {
	    return queryName;
    }

    public String getRecordType() {
        return recordType;
    }

    public ArrayList<DNSServer> getAnswerServers() {
        return answerServers;
    }

    public ArrayList<DNSServer> getAuthoritativeServers() {
        return authoritativeServers;
    }

    public ArrayList<DNSServer> getAdditionalRecords() {
        return additionalRecords;
    }

    // You will probably want a method to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records. 
}
