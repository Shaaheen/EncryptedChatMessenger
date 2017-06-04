import com.sun.corba.se.spi.activation.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Shaaheen on 25-May-17.
 */
public class SecureClient extends PeerClient{

    SecureClient(String clientName, int port) {
        super(clientName, port);
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
        else if ( message.contains("sharedkey_incoming") ){
            int length = in.readInt();                    // read length of incoming message
            byte[] sharedKey = new byte[1];
            if(length>0) {
                sharedKey = new byte[length];
                in.readFully(sharedKey, 0, sharedKey.length); // read the message
            }
            System.out.println("Received SharedKey: " + Arrays.toString( sharedKey ) );
        }
        else{
            System.out.println(name + "> " + message);
        }
    }

}
