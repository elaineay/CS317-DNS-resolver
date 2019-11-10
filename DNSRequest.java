import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class DNSRequest {
    private int transactionID;
    private int flag;
    private int questionCount;
    private int answerCount;
    private int authCount;
    private int addCount;

    private int ending = 0x00;
    private int type = 0x0001;
    private int classIN = 0x0001;

    private byte[] outputFrame;


    public DNSRequest(String fqdn, boolean isIPV6) throws IOException {
        ByteArrayOutputStream bAOutput = new ByteArrayOutputStream();
        DataOutputStream dOutput = new DataOutputStream(bAOutput);

        Random randomGenerator = new Random();

        transactionID = randomGenerator.nextInt(0xFFFF)+1;
        flag = 0x0100;
        questionCount = 0x0001;
        answerCount = 0x0000;
        authCount = 0x0000;
        addCount = 0x0000;

        dOutput.writeShort(transactionID); // Transaction ID of query (16 bits)
        dOutput.writeShort(flag); // flag, standard query
        dOutput.writeShort(questionCount); // # questions
        dOutput.writeShort(answerCount); // # answer records
        dOutput.writeShort(authCount); // # authority records
        dOutput.writeShort(addCount); // # additional records

        String[] dnSections = fqdn.split("\\.");
        // write the domain into the DNS request
        for (String s : dnSections) {
            byte[] byteArray = s.getBytes("UTF-8");
            dOutput.write(byteArray.length);
            dOutput.write(byteArray);
        }

        dOutput.writeByte(ending); //signify end of DNS request
        if (isIPV6) {
            type = 0x001c;
        }
        dOutput.writeShort(type);
         // record type A (host request)
        dOutput.writeShort(classIN); // class IN

        outputFrame = bAOutput.toByteArray();

        // TODO: just checking if sending
        // System.out.println("Lookup being called");
        // System.out.println("Sending: " + outputFrame.length + " bytes");
        // for (int i =0; i < outputFrame.length; i++) {
        //     System.out.print("0x" + String.format("%x", outputFrame[i]) + " " );
        // }


    }

    public int getTransactionID() {
        return transactionID;
    }

    public String getIPVType() {
        switch(type) {
            case 0x001c:
                return "AAAA";
            default:
                return "A";
        }
    }

    public DatagramPacket createSendable(DNSRequest dnsRequest,InetAddress rootNameServer, int port) {
        DatagramPacket reqPacket = new DatagramPacket(dnsRequest.outputFrame,
                dnsRequest.outputFrame.length,
                rootNameServer,
                port);

        return reqPacket;
    }
}
