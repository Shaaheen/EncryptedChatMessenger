/**
 * Created by Shaaheen on 25-May-17.
 */
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;

public class Server extends Thread {
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private int port;
	private ArrayList<ClientThread> clients;
	private boolean notStopped;

	/**
	 * Server constructor
	 */
	public Server(int port) {
		this.port = port;
		clients = new ArrayList<ClientThread>();
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
				ClientThread t = new ClientThread(clientSocket);
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


	public void stopServer() {
		notStopped = false;
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
	
	ClientThread(Socket clientSocket){
		this.clientSocket = clientSocket;
		try{
			os = new ObjectOutputStream(clientSocket.getOutputStream());
			os.flush();
			is = new ObjectInputStream(clientSocket.getInputStream());
			os.writeObject("Connection Successful");
			os.flush();
			String message ="";
			do{
				try{
					message = (String)is.readObject();
					System.out.println("client>" + message);
					if (message.equals("bye")) {
						//sendMessage("bye");
						os.writeObject(message);
						os.flush();
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
}
