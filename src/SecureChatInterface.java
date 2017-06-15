import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Scanner;

/**
 * Created by Shaaheen on 14-Jun-17.
 */
public class SecureChatInterface {
    public static void main(String[] args) throws InterruptedException, IOException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidCipherTextException {
        //java SecureChatInterface Client_C 5310
        //java SecureChatInterface Client_D 5410

        int port = Integer.parseInt(args[1]);
        String name = String.valueOf(args[0]);
        String password = "";
        SecureClient clientA = new SecureClient( args[0], Integer.parseInt(args[1] ));
        SecureClient clientACopy = new SecureClient(name, port);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Client started...");
        System.out.println("Establish connection with server?");
        String estab = scanner.nextLine();
        Key[] pair = SecureClient.getPublicPrivateKeyPair(1024, clientACopy.getClientName());
        clientACopy.setPublicPrivateKeyPair( pair );

        if (estab.equals("Yes") || estab.equals("yes") || estab.equals("y"))
        {
            System.out.println("Enter Client NAME of who you want to communicate with: ");
            String ClientName = scanner.nextLine();

            System.out.println("Enter Client PORT of who you want to communicate with: ");
            int portNumOfRec = scanner.nextInt();
//            int portNumOfRec = 0;
//            ClientName = "Client_D";
//            portNumOfRec = 5411;
//            password = "connect123";

            System.out.println("Enter password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            password = br.readLine();

            clientA.requestSharedKeyWith(ClientName, "localhost", 2400, password);
            Thread.sleep(10000);

            System.out.println("SHared " + SecureClient.byteArrayToHex(clientA.sharedKey));
            System.out.println("Connecting to "+ClientName+"...");

            clientA.stopServer();
            //clientA.start(); // Causes an IllegalStateExceptionError

			clientACopy.sharedKey = clientA.sharedKey;
			clientACopy.peerName = ClientName;
			clientACopy.prepareConnectionTo("localhost", portNumOfRec);
			clientACopy.start();
            clientACopy.startedCommunication();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            String message = "";
            while(true)
            {
                System.out.println("Enter message: ");
                BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
                message = br2.readLine();

                if(message.equals("end"))
                {
                    break;
                }
                else if(!message.equals("") && !message.equals(" "))
                {
                    clientACopy.sendEncryptedMessage(message);
                }
            }

        }
        else
        {

            System.out.println("Wating for connection..");
            if(String.valueOf(args[0]).equals("Client_D"))
            {
                clientA.peerName = "Client_C";
            }
            else if(String.valueOf(args[0]).equals("Client_C"))
            {
                clientA.peerName = "Client_D";
            }

            clientA.setPublicPrivateKeyPair(pair);
            clientACopy.peerName = "Client_C";

            Thread.sleep(30000);

//            System.out.println("SHared " + SecureClient.byteArrayToHex(clientA.sharedKey));

            String message = "";
            while (!message.equals("end")){
                System.out.println("Enter message: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                message = br.readLine();
                clientA.sendEncryptedMessage(message);

            }
        }

    }
}
