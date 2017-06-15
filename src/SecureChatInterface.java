import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
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

        SecureClient clientA = new SecureClient( args[0], Integer.parseInt(args[1] ));
        Scanner scanner = new Scanner(System.in);
        System.out.println("Client started...");
        System.out.println("Establish connection with server?");
        String estab = scanner.nextLine();
        clientA.setPublicPrivateKeyPair( SecureClient.getPublicPrivateKeyPair(1024, clientA.getClientName()) );

        if (estab.equals("Yes") || estab.equals("yes") || estab.equals("y"))
        {
            System.out.println("Enter Client NAME of who you want to communicate with: ");
            String ClientName = scanner.nextLine();
            System.out.println("Enter Client PORT of who you want to communicate with: ");
            int portNumOfRec = scanner.nextInt();


            clientA.requestSharedKeyWith(ClientName, "localhost", 2400);
            Thread.sleep(5000);

            System.out.println("Connecting to "+ClientName+"...");

            //clientA.start(); // Causes an IllegalStateExceptionError
            clientA.startedCommunication();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            String message = "";
            while(true)
            {
                System.out.println("Enter message: ");
                message = scanner.next();
                if(message.equals("end"))
                {
                    break;
                }
                else if(!message.equals("") && !message.equals(" "))
                {
                    clientA.sendEncryptedMessage(message);
                }
            }

        }
        else
        {


            String incomingMessage = "";
            while(true)
            {
                //Wait
                if(!clientA.receivingComm)
                {
                    break;
                }
            }

            String message = "";
            while (message.equals("end")){
                System.out.println("Enter message: ");
                message = scanner.nextLine();
                clientA.sendEncryptedMessage(message);

            }
        }

    }
}
