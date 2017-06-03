import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class UnitTestApplication {
    SecureClient clientA;
    SecureClient clientB;
    TrustedCryptoServer trustedCryptoServer;

    @Before
    public void setUp() throws Exception {
        clientA = new SecureClient( "Client_A", 4422 );
        clientB = new SecureClient("Client_B", 5422);
        trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty",5120);
    }

    @Test
    public void testClientToClientConnection() throws Exception{
        System.out.println("Connect test start");

        clientA.connectTo("localhost",clientB.getPort());
        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), clientB.getName());
        Assert.assertEquals(clientA.getConnectedWithPort(), clientB.getPort());

        System.out.println("Connect end test");

        tearDown();
    }

    @Test
    public void testClientToTrustedServerConnection() throws Exception{
        System.out.println("Connect test start");

        clientA.connectTo("localhost",trustedCryptoServer.getPort());
        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), trustedCryptoServer.getName());
        Assert.assertEquals(clientA.getConnectedWithPort(), trustedCryptoServer.getPort());

        System.out.println("Connect end test");

        tearDown();
    }

    @Test
    public void testTrusterCryptoServer() throws Exception{
        System.out.println("Trusted Crypto test start");

        trustedCryptoServer.certifyNewClient("ClientG",856);
        Assert.assertEquals(trustedCryptoServer.findPortNumOfClient("ClientG"),856);

        Assert.assertEquals(trustedCryptoServer.findPortNumOfClient("ClientA"),-1);

        System.out.println("Trusted Crypto test complete");

        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Closing sockets");
        clientA.stopServer();
        clientB.stopServer();
        trustedCryptoServer.stopServer();
        trustedCryptoServer.clearCertifiedClients();
        System.out.println();
    }
}