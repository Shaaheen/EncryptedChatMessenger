import com.sun.corba.se.spi.activation.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
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

}
