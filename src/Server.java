/**
 * Created by Shaaheen on 25-May-17.
 */
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {
  	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private int port;
	private ArrayList<ClientThread> clients;

	/**
	* Server constructor
	*/
	public Server(int port){
		this.port = port;
		clients = new ArrayList<ClientThread>();
	}

	public void start(){
		//create server socket and wait for connections
		try{
			serverSocket = new ServerSocket(port);
			while (true){
				System.out.println("Server waiting for clients on port " + port + "...");

				clientSocket = serverSocket.accept();
				ClientThread t = new ClientThread(clientSocket);
				clients.add(t);
				t.start();
				try{
				serverSocket.close();
				for (int i = 0; i < clients.size(); i++){
					ClientThread tc = clients.get(i);
					try{
						tc.is.close();
						tc.os.close();
						tc.clientSocket.close();
					} catch(Exception e){}
				}
			} catch(IOException e){}
			}//end of while
			/**
			try{
				serverSocket.close();
				for (int i = 0; i < clients.size(); i++){
					ClientThread tc = clients.get(i);
					try{
						tc.is.close();
						tc.os.close();
						tc.clientSocket.close();
					} catch(Exception e){}
				}
			} catch(IOException e){} */
		}catch(IOException e){}
	}

	public static void main(String args[]) {
		int portNumber = 2222;
		if (args.length == 1){
			try{
				portNumber = Integer.parseInt(args[0]);
			} catch(Exception e){
				System.out.println("Invalid port number.");
				System.out.println("Usage: java Server <portNumber>");
				return;
			}	
		} else if (args.length == 0){
			;
		} else{
			System.out.println("Usage: java Server <portNumber>");
			return;
		}
		Server server = new Server(portNumber);
		server.start();
	}//end of main method
}//end of server class

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
			is = new ObjectInputStream(clientSocket.getInputStream());
			try{			
				username = (String) is.readObject();
			} catch(ClassNotFoundException e){}
			System.out.println("Welcome   " + username);
		} catch (IOException e){
			System.out.println("Exception creating new Input/output Streams: " + e);
			return;
		}
	}
}
