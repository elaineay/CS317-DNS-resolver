import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class DNSRequest {
    private int transactionID;
    private int flag;
    private int questionCount;
    private int answerCount;
    private int authCount;
    private int addCount;

    private int ending = 0x00;
    private int typeA = 0x0001;
    private int classIN = 0x0001;

    private byte[] outputFrame;


    public DNSRequest(String fqdn) throws IOException {
        ByteArrayOutputStream bAOutput = new ByteArrayOutputStream();
        DataOutputStream dOutput = new DataOutputStream(bAOutput);

        transactionID = 0x1234;
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
        dOutput.writeByte(dnSections.length);

        for (String s : dnSections) {
            byte[] byteArray = s.getBytes("UTF-8");
            dOutput.write(byteArray);
        }

        dOutput.writeByte(ending); //signify end of DNS request
        dOutput.writeShort(typeA); // record type A (host request)
        dOutput.writeShort(classIN); // class IN

        outputFrame = bAOutput.toByteArray();

        // TODO: just checking if sending
        System.out.println("Sending: " + outputFrame.length + " bytes");
        for (int i =0; i< outputFrame.length; i++) {
            System.out.print("0x" + String.format("%x", outputFrame[i]) + " " );
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
