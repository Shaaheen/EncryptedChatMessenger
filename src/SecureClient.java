import com.sun.corba.se.spi.activation.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Shaaheen on 25-May-17.
 */
public class SecureClient extends PeerClient{
    protected byte[] sharedKey;

    SecureClient(String clientName, int port) {
        super(clientName, port);
        sharedKey = new byte[1]; //placeholder for sharedkey
    }

    protected void requestSharedKeyWith(String peerClientName) throws IOException {
        System.out.println("Requesting shared key from server...");
        sendMessage("sharedkey_request:" + getClientName() + ":" + peerClientName);
    }

    //Method to process messages appropriately with sharedkey processing
    protected void processMessage(String message) throws IOException {
        if (message.contains(":")){
            if (message.split(":")[0].equals("name")){
                connectedWithName = message.split(":")[1];
                connectedWithPort = portNumToConnectTo;
                System.out.println(connectedWithName+"> My Name is: " + connectedWithName);
                message = "I am " + name;
                sendMessage(message);
            }
        }
        else if ( message.contains(TrustedCryptoServer.INCOMING_KEY_KEYWORD) ){
            int length = in.readInt();                    // read length of incoming message
            if(length>0) {
                sharedKey = new byte[length];
                in.readFully(sharedKey, 0, sharedKey.length); // read the message
            }
            System.out.println(name + ">Received SharedKey: " + Arrays.toString( sharedKey ) );
        }
        else{
            System.out.println(name + "> " + message);
        }
    }

    protected int getSharedKeyLength(){
        return sharedKey.length;
    }

    //Returns a trusted server thread - Communication thread for client
    protected ClientThread getNewClientThread(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        System.out.println("OVERIDED METHOD 2" );
        return new SecureClientThread( clientSocket, serverName, this );
    }
}

class SecureClientThread extends ClientThread{

    private SecureClient secureClient;

    SecureClientThread(Socket clientSocket, String serverName, SecureClient secureClient) throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        super(clientSocket, serverName);
        this.secureClient = secureClient;
        setKeywordsInMessages(); //Set new shared key request keyword
        communicateWithClient(clientSocket,serverName);
    }

    //Add shared key to key word reaction list
    protected void setKeywordsInMessages(){
        this.keywordsInMessages = Arrays.asList( "end_connection" , TrustedCryptoServer.INCOMING_KEY_KEYWORD );
    }

    //Method to react to specific keywords
    // Will react to a shared key request by a client towards another client
    protected void reactToKeyword(String keyword) throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        //System.out.println("DIFF KEYWORDs: " + this.keywordsInMessages + " and keyword : " + keyword);
        if (keyword.equals("end_connection")){
            sendMessage("end_connection");
        }
        else if (keyword.contains(":")){
            if (keyword.split(":")[0].equals("name")){
                username = username + keyword.split(":")[1];
                System.out.println(username+"> my name " +  keyword.split(":")[1]);
            }
        }
        else if (keyword.equals(TrustedCryptoServer.INCOMING_KEY_KEYWORD)){
            System.out.println(secureClient.getClientName() + "> Receiving sharedkey...");

            int length = is.readInt();//is.readInt();                    // read length of incoming message
            System.out.println("LENGHT IS " + length);
            if(length>0) {
                secureClient.sharedKey = new byte[length];
                is.readFully(secureClient.sharedKey, 0, secureClient.sharedKey.length); // read the message
            }

            System.out.println(secureClient.name + ">Received SharedKey: " + Arrays.toString( secureClient.sharedKey ) );
        }
        else{
            System.out.println(username + "> " + keyword);
        }
    }
}

