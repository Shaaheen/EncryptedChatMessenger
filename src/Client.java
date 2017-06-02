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
public class Client {
    private String name;
    private Server serverForPeering;

    Client(String clientName, int port){
        this.serverForPeering = new Server(port);
        this.name = clientName;
        serverForPeering.start();
        //serverForPeering.run();
        Timer timer = new Timer();

//        timer.schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//                try {
//                    System.out.println("Sleep");
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 700);

//        try {
//            Thread.sleep(15000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //System.out.println("Thread sleeping done");
    }

    protected void stopServer(){
        this.serverForPeering.stopServer();
    }

    protected void connectTo( String hostName, int portNumberToConnect) throws IOException {

        Socket communicationSocket = new Socket(hostName, portNumberToConnect);
//        PrintWriter out = new PrintWriter(communicationSocket.getOutputStream(), true);
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(communicationSocket.getInputStream()));

        ObjectOutputStream out = new ObjectOutputStream(communicationSocket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(communicationSocket.getInputStream());
        String message = "";

        do{
            try{
                message = (String)in.readObject();
                System.out.println("server>" + message);
//                sendMessage("Hi my server");
                message = "Messagy back";
                out.writeObject(message);
                out.flush();
                message = "bye";
//                sendMessage(message);
                out.writeObject(message);
                out.flush();
            }
            catch(ClassNotFoundException classNot){
                System.err.println("data received in unknown format");
            }
        }while(!message.equals("bye"));


    }





}
