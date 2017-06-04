import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Shaaheen on 03-Jun-17.
 * THIS IS TO ABSTRACT AWAY SERVER/CLIENT SOCKET PROGRAMMING
 */
public class PeerClient extends Thread{
    protected String name;
    private Server serverForPeering;
    private int port;
    protected boolean connectionEstablished;
    protected String connectedWithName;
    protected int connectedWithPort;

    protected ObjectOutputStream out;
    protected ObjectInputStream in;

    protected String hostName;
    protected int portNumToConnectTo;

    PeerClient(String clientName, int port){
        this.serverForPeering = new Server(port, clientName,this);
        this.port = port;
        connectionEstablished = false;
        this.name = clientName;
        serverForPeering.start();
        this.connectedWithName = "Unknown";
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

    protected void prepareConnectionTo(String hostName, int portNumToConnectTo){
        this.hostName = hostName;
        this.portNumToConnectTo = portNumToConnectTo;
    }

    protected void connectToPeer() throws IOException {

        Socket communicationSocket = new Socket(hostName, portNumToConnectTo);

        out = new ObjectOutputStream(communicationSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream( communicationSocket.getInputStream() );
        String message = "" ;

        do{
            try{
                message = (String)in.readObject();
                connectionEstablished = true;
                processMessage(message);
            }
            catch(ClassNotFoundException classNot){
                System.err.println("data received in unknown format");
            }
        }while(!message.equals("end_connection"));

    }

    //Method to process messages appropriately
    protected void processMessage(String message) throws IOException {
        if (message.contains(":")){
            if (message.split(":")[0].equals("name")){
                connectedWithName = message.split(":")[1];
                connectedWithPort = portNumToConnectTo;
                System.out.println(connectedWithName+"> My Name is: " + connectedWithName);
                message = "name:" + name;
                sendMessage(message);
            }
        }
        else{
            System.out.println(name + "> " + message);
        }
    }

    public void run(){
        try {
            connectToPeer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void closeConnection() throws IOException {
        sendMessage("end_connection");
    }

    protected void sendMessage(String message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    public String getConnectedWithName() {
        return connectedWithName;
    }

    public int getConnectedWithPort() {
        return connectedWithPort;
    }

    public String getClientName(){
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getHostName(){
        return hostName;
    }

    protected ClientThread getNewClientThread(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        return new ClientThread(clientSocket,serverName,true );
    }
}

class Server extends Thread {
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private int port;
    private ArrayList<ClientThread> clients;
    private boolean notStopped;
    private String serverName;

    private PeerClient peerClient;

    private String[] keywordsInMessages;

    /**
     * Server constructor
     */
    public Server(int port, String name, PeerClient peerClient) {
        this.port = port;
        clients = new ArrayList<ClientThread>();
        this.serverName = name;
        this.peerClient = peerClient;
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
                ClientThread t = null;
                try {
                    t = peerClient.getNewClientThread(clientSocket,serverName);
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
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
    String username;
    ObjectInputStream  is = null;
    ObjectOutputStream os = null;
    Socket clientSocket = null;

    protected List keywordsInMessages;

    ClientThread(Socket clientSocket, String serverName, boolean run) throws NoSuchProviderException, NoSuchAlgorithmException {
        this.clientSocket = clientSocket;
        username = serverName + "<-|";
        setKeywordsInMessages();
        if (run) communicateWithClient(clientSocket, serverName);
    }

    ClientThread(Socket clientSocket, String serverName){
        this.clientSocket = clientSocket;
        username = serverName + "<-|";
    }

    protected void communicateWithClient(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException {
        try{
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            os.flush();
            is = new ObjectInputStream(clientSocket.getInputStream());
            sendMessage("name:"+serverName);
            String message ="";
            do{
                try{
                    message = (String)is.readObject();
                    reactToKeyword(message); //If keyword, will react
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }while(!message.equals("end_connection"));
        } catch (IOException e){
            System.out.println("Exception creating new Input/output Streams: " + e);
            return;
        }
    }

    //Default keywords
    protected void setKeywordsInMessages(){
        this.keywordsInMessages = Arrays.asList("end_connection");
    }

    protected void sendMessage(String mesage) throws IOException {
        os.writeObject(mesage);
        os.flush();
    }

    protected void reactToKeyword(String keyword) throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        if (keyword.equals("end_connection")){
            sendMessage("end_connection");
            System.out.println("Me> " + keyword);
        }
        else if (keyword.contains(":")){
            if (keyword.split(":")[0].equals("name")){
                username = username + keyword.split(":")[1];
                System.out.println(username+"> My name is " +  keyword.split(":")[1]);
            }
        }
        else{
            System.out.println("Me> " + keyword);
        }
    }
}