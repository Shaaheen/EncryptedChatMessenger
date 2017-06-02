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
public class SecureClient {
    private String name;
    private Server serverForPeering;

    SecureClient(String clientName, int port){
        this.serverForPeering = new Server(port);
        this.name = clientName;
        serverForPeering.start();
        //serverForPeering.run();
    }

    protected void stopServer(){
        this.serverForPeering.stopServer();
    }

    protected void connectTo( String hostName, int portNumberToConnect) throws IOException {

        Socket communicationSocket = new Socket(hostName, portNumberToConnect);

        ObjectOutputStream out = new ObjectOutputStream(communicationSocket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(communicationSocket.getInputStream());
        String message = "";

        do{
            try{
                message = (String)in.readObject();
                System.out.println("Paired client> " + message);
                message = "Messagy back";
                sendMessage(out, message);
                message = "bye";
                sendMessage(out, message);
            }
            catch(ClassNotFoundException classNot){
                System.err.println("data received in unknown format");
            }
        }while(!message.equals("bye"));


    }

    private void sendMessage(ObjectOutputStream out, String message) throws IOException {
        out.writeObject(message);
        out.flush();
    }


}
