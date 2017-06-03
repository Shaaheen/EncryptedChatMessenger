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
        System.out.println("Setting up clients and server" + "\r\n");
        clientA = new SecureClient( "Client_A", 4422 );
        clientB = new SecureClient("Client_B", 5422);
        trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty",5120);
    }

    //Used for client messaging
    @Test
    public void testClientToClientConnection() throws Exception{
        clientA.connectTo("localhost",clientB.getPort());
        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), clientB.getName());
        Assert.assertEquals(clientA.getConnectedWithPort(), clientB.getPort());

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testClientToTrustedServerConnection() throws Exception{
        clientA.connectTo("localhost",trustedCryptoServer.getPort());
        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), trustedCryptoServer.getName());
        Assert.assertEquals(clientA.getConnectedWithPort(), trustedCryptoServer.getPort());

        tearDown();
    }

    //Used when server needs to share key with other client
    @Test
    public void testTrustedServerToClientConnection() throws Exception{
        trustedCryptoServer.connectTo("localhost",clientB.getPort());
        Assert.assertEquals(trustedCryptoServer.connectionEstablished, true);
        Assert.assertEquals(trustedCryptoServer.getConnectedWithName(), clientB.getName());
        Assert.assertEquals(trustedCryptoServer.getConnectedWithPort(), clientB.getPort());

        tearDown();
    }

    @Test
    public void testTrustedCryptoServer() throws Exception{

        //testing certified client search function
        trustedCryptoServer.certifyNewClient("ClientG",856);
        Assert.assertEquals(trustedCryptoServer.findPortNumOfClient("ClientG"),856);

        Assert.assertEquals(trustedCryptoServer.findPortNumOfClient("ClientA"),-1);

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