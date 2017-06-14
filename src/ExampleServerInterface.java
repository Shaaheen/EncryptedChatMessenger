/**
 * Created by Shaaheen on 14-Jun-17.
 */
public class ExampleServerInterface {
    public static void main(String[] args) {
        TrustedCryptoServer trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty", 2400);
        trustedCryptoServer.certifyNewClient("Client_C", 5310 , "localhost" );
        trustedCryptoServer.certifyNewClient( "Client_D", 5410 , "localhost" );


    }
}
