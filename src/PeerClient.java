import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Shaaheen on 03-Jun-17.
 * THIS IS TO ABSTRACT AWAY SERVER/CLIENT SOCKET PROGRAMMING
 */
public class PeerClient {
    private String name;
    private Server serverForPeering;
    private int port;
    protected boolean connectionEstablished;
    private String connectedWithName;
    private int connectedWithPort;

    PeerClient(String clientName, int port){
        this.serverForPeering = new Server(port, clientName);
        this.port = port;
        connectionEstablished = false;
        this.name = clientName;
        serverForPeering.start();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //serverForPeering.run();
    }

    protected void stopServer() throws IOException {
        connectionEstablished = false;
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
                connectionEstablished = true;
                System.out.println("Paired client> Name is: " + message);
                connectedWithName = message;
                connectedWithPort = portNumberToConnect;
                message = "I am " + name;
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

    public String getConnectedWithName() {
        return connectedWithName;
    }

    public int getConnectedWithPort() {
        return connectedWithPort;
    }

    public String getName(){
        return name;
    }

    public int getPort() {
        return port;
    }
}

class Server extends Thread {
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private int port;
    private ArrayList<ClientThread> clients;
    private boolean notStopped;
    private String serverName;

    /**
     * Server constructor
     */
    public Server(int port, String name) {
        this.port = port;
        clients = new ArrayList<ClientThread>();
        this.serverName = name;
    }

    public void run() {
        notStopped = true;
        //create server socket and wait for connections
        try {
            serverSocket = new ServerSocket(port);
            while (notStopped) {
                System.out.println("Server waiting for clients on port " + port + "...");

                clientSocket = serverSocket.accept();
                System.out.println("Connection received from " + clientSocket.getInetAddress().getHostName());
                if (!notStopped)
                    break;
                ClientThread t = new ClientThread(clientSocket,serverName);
                clients.add(t);
                t.start();
            }//end of while

            try {
                serverSocket.close();
                System.out.println("Server ended");
                for (int i = 0; i < clients.size(); i++) {
                    ClientThread tc = clients.get(i);
                    try {
                        tc.is.close();
                        tc.os.close();
                        tc.clientSocket.close();
                    } catch (Exception e) {

                    }
                }//end of for loop
            } catch (IOException e) {
                System.out.println("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            System.out.println(" Exception on new ServerSocket: " + e);
        }
    }


    public void stopServer() throws IOException {
        notStopped = false;
        serverSocket.close();
    }

}
/*
 * The chat client thread.
 *
 */
class ClientThread extends Thread {
    String username = null;
    ObjectInputStream  is = null;
    ObjectOutputStream os = null;
    Socket clientSocket = null;

    ClientThread(Socket clientSocket, String serverName){
        this.clientSocket = clientSocket;
        try{
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            os.flush();
            is = new ObjectInputStream(clientSocket.getInputStream());
            sendMessage(serverName);
            String message ="";
            do{
                try{
                    message = (String)is.readObject();
                    System.out.println("Me> " + message);
                    if (message.equals("bye")) {
                        sendMessage("bye");
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }while(!message.equals("bye"));
        } catch (IOException e){
            System.out.println("Exception creating new Input/output Streams: " + e);
            return;
        }
    }

    private void sendMessage(String mesage) throws IOException {
        os.writeObject(mesage);
        os.flush();
    }
}