import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
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
        System.out.println("Establish Connection?");
        String estab = scanner.nextLine();
        if (estab.equals("Yes")){
            System.out.println("Enter Client NAME of who you want to communicate with: ");
            String ClientName = scanner.nextLine();
            System.out.println("Enter Client PORT of who you want to communicate with: ");
            int portNumOfRec = scanner.nextInt();

            clientA.requestSharedKeyWith(ClientName, "localhost", 2400);
            Thread.sleep(5000);

            clientA.prepareConnectionTo("localhost",portNumOfRec);
            clientA.startedCommunication();
            clientA.start();

            Thread.sleep(4000);
            clientA.sendMessage("initiate");

            String message = "";
            while (message != "end"){
                System.out.println("Enter message: ");
                message = scanner.nextLine();
                clientA.sendEncryptedMessage(message);
            }

        }
        else{

            while (clientA.receivingComm != true){

            }

            String message = "";
            while (message != "end"){
                System.out.println("Enter message: ");
                message = scanner.nextLine();
                clientA.sendEncryptedMessage(message);
            }
        }

    }
}
