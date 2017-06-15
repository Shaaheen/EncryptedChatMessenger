import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by Shaaheen on 02-Jun-17.
 */
public class ExampleApplication {

    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, InterruptedException, InvalidCipherTextException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        SecureClient clientA = new SecureClient( "Client_C", 5712 );
        SecureClient clientB = new SecureClient("Client_D", 3400);
        TrustedCryptoServer trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty", 2400);
        trustedCryptoServer.setClientPassword("connect123");
        trustedCryptoServer.certifyNewClient(clientA.getClientName(), clientA.getPort(), clientA.getHostName());
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );

        //KEY REQUEST
        SecureConnection clientAtoBConnection = clientA.requestEncryptedConnectionWith(clientB, trustedCryptoServer);
        SecureClient secureClientA = clientAtoBConnection.getStartingClient();
        SecureClient secureClientB = clientAtoBConnection.getReceivingClient();

        Thread.sleep(1000);
        String fileName3 = "testfile.txt";
        //String fileName3 = "techback.jpg";
        //Test messages
        secureClientA.sendEncryptedMessage( "Hi there class!" );
        secureClientB.sendEncryptedMessage( "Hello friend");
        secureClientA.sendEncryptedMessage( "What are you doing?" );
        secureClientB.sendEncryptedMessage( "Nothing much man" );
        secureClientA.sendEncryptedMessage( "Thats lovely, thats cool" );
        secureClientA.sendEncryptedMessage( "Heres a file for ya" );
        secureClientB.sendEncryptedFile(fileName3,fileName3);
        secureClientB.sendEncryptedMessage("Thanks!");

        Thread.sleep(3000);
        secureClientA.closeConnection();
        secureClientB.stopServer();
        clientA.stopServer();
        clientB.stopServer();
        trustedCryptoServer.stopServer();
    }

}
