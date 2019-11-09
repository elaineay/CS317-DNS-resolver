public class DNSServer {
    public String serverName;
    public String serverType;
    public String serverClass;
    public int timeTL;
    public int dataLength;
    public String serverNameServer;

    public DNSServer(String serverName, String serverType, String serverClass, int timeTL, int dataLength, String serverNameServer){
        this.serverName = serverName;
        this.serverType = serverType;
        this.serverClass = serverClass;
        this.timeTL = timeTL;
        this.dataLength = dataLength;
        this.serverNameServer = serverNameServer;
    }
}