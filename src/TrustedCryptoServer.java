import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Shaaheen on 02-Jun-17.
 *
 * Trusted 3rd Party server used to generate and distribute secret shared keys
 *  - Keeps track of certified clients (known trusted clients) and thus acts as
 *  a secure gateway for messaging
 */
public class TrustedCryptoServer extends PeerClient{
    private ArrayList<CertifiedClient> certifiedClients;
    protected String keyPhraseForSharedKey;

    public static String INCOMING_KEY_KEYWORD = "sharedkey_incoming";

    TrustedCryptoServer(String trustedServerName, int port)  {
        super(trustedServerName, port);
        this.certifiedClients = new ArrayList<CertifiedClient>();

        //Default format -> "sharedkey_request":OwnClientName:ClientNameRequestingCommWith
        //Will search for the keyphrase "sharedkey_request" by default
        this.keyPhraseForSharedKey = "sharedkey_request";
    }

    TrustedCryptoServer(String trustedServerName, int port, String keyPhraseForSharedKey) {
        super(trustedServerName, port);
        this.certifiedClients = new ArrayList<CertifiedClient>();
        this.keyPhraseForSharedKey = keyPhraseForSharedKey;
    }

    //Method to generate a secure random 256 key bit for AES encryption
    protected byte[] generateNewSharedKey() throws NoSuchAlgorithmException, NoSuchProviderException {

        Security.addProvider(new BouncyCastleProvider());
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom rand = new SecureRandom();
        keyGen.init(rand);
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();

        return secretKey.getEncoded();
    }

    //New trusted client certified by the trusted 3rd party
    protected void certifyNewClient(String name, int portNumber, String hostName){
        certifiedClients.add( new CertifiedClient(name, portNumber, hostName) );
    }

    //Checks if given client is certified, returns port number if client is certified, -1 if not
    protected CertifiedClient getCertifiedClient(String clientName){
        for (CertifiedClient certifiedClient: certifiedClients){
            if ( certifiedClient.getName().equals( clientName ) ){
                return certifiedClient;
            }
        }
        return null;
    }

    //If no port number exists, then client is not trusted
    protected boolean verifyThatClientsAreTrusted(String clientA, String clientB){
        return  ( ( getCertifiedClient(clientA) != null ) && ( getCertifiedClient(clientB) != null) ) ;
    }

    protected void sendByteArray(int length, byte[] byteArray) throws IOException {
        out.writeInt(length);
        out.write( byteArray );
    }

    protected void clearCertifiedClients(){
        certifiedClients.clear();
    }

    //Returns a trusted server thread - Communication thread for client
    protected ClientThread getNewClientThread(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {
        return new CertifiedClientThread( clientSocket, serverName, this );
    }

    protected void sendSharedKeyToClient(CertifiedClient certifiedClient, byte[] sharedKey) throws IOException {
        prepareConnectionTo( certifiedClient.getHostname(), certifiedClient.getPortNum() );
        start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendMessage(INCOMING_KEY_KEYWORD);
        out.writeInt( sharedKey.length );
        out.write( sharedKey );
        out.flush();
//        out.writeObject("end_connection");
//        out.flush();

    }

}

class CertifiedClientThread extends ClientThread{
    private TrustedCryptoServer trustedCryptoServer;

    CertifiedClientThread(Socket clientSocket, String serverName, TrustedCryptoServer trustedCryptoServer) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        super(clientSocket, serverName);
        this.trustedCryptoServer = trustedCryptoServer;
        setKeywordsInMessages(); //Set new shared key request keyword
        communicateWithClient(clientSocket,serverName);
    }

    //Add shared key to key word reaction list
    protected void setKeywordsInMessages(){
        this.keywordsInMessages = Arrays.asList( "end_connection" , trustedCryptoServer.keyPhraseForSharedKey );
    }

    //Method to react to specific keywords
    // Will react to a shared key request by a client towards another client
    protected void reactToKeyword(String keyword) throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        //System.out.println("DIFF KEYWORDs: " + this.keywordsInMessages + " and keyword : " + keyword);
        if (keyword.equals("end_connection")){
            sendMessage("end_connection");
        }
        // If message contains the shared key request keywords
        else if ( keyword.contains( trustedCryptoServer.keyPhraseForSharedKey ) ) {
            String senderClient  = keyword.split(":")[1];
            String requestedClient  = keyword.split(":")[2];

            //If both clients are trusted by the server, then gen and pass on shared key
            if ( trustedCryptoServer.verifyThatClientsAreTrusted( senderClient , requestedClient ) ){
                byte[] keyBytes = trustedCryptoServer.generateNewSharedKey();
                System.out.println("Generated new key : "  + SecureClient.byteArrayToHex(keyBytes) ); /*+ new String( keyBytes, Charset.forName("UTF-8"))*/
                //System.out.println(Arrays.toString(new String( keyBytes,"UTF8").getBytes()));
                sendMessage(TrustedCryptoServer.INCOMING_KEY_KEYWORD);
                os.writeInt(keyBytes.length);
                os.write( keyBytes );
                os.flush();
                closeConnection();

                CertifiedClient requestedClientInfo = trustedCryptoServer.getCertifiedClient( requestedClient );
                trustedCryptoServer.sendSharedKeyToClient( requestedClientInfo , keyBytes );
                trustedCryptoServer.closeConnection();
            }
            else{
                System.out.println("One or more clients are not trusted by Server");
            }

        }
    }
}

class CertifiedClient {
    private String name;
    private int portNum;
    private String hostname;

    CertifiedClient(String name, int portNum, String hostname) {
        this.name = name;
        this.portNum = portNum;
        this.hostname = hostname;
    }

    public String getName() {
        return name;
    }

    public int getPortNum() {
        return portNum;
    }

    public String getHostname() {
        return hostname;
    }
}
