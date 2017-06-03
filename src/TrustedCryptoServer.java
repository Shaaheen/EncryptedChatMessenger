import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by Shaaheen on 02-Jun-17.
 */
public class TrustedCryptoServer extends PeerClient{
    private ArrayList<CertifiedClient> certifiedClients;

    TrustedCryptoServer(String trustedServerName, int port) {
        super(trustedServerName, port);
        this.certifiedClients = new ArrayList<CertifiedClient>();
    }

    protected void clearCertifiedClients(){
        certifiedClients.clear();
    }

    protected void certifyNewClient(String name, int portNumber){
        certifiedClients.add(new CertifiedClient(name, portNumber));
    }

    protected int findPortNumOfClient(String clientName){
        for (CertifiedClient certifiedClient: certifiedClients){
            if ( certifiedClient.getName().equals( clientName ) ){
                return certifiedClient.getPortNum();
            }
        }
        return -1;
    }

    private void generateNewSharedKey(){
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[128];
        random.nextBytes(keyBytes);
    }
}

class CertifiedClient {
    private String name;
    private int portNum;

    CertifiedClient(String name, int portNum) {
        this.name = name;
        this.portNum = portNum;
    }


    public String getName() {
        return name;
    }

    public int getPortNum() {
        return portNum;
    }
}
