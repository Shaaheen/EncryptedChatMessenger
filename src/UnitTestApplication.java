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
        trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty", 5220);
    }

    //Used for client messaging
    @Test
    public void testClientToClientConnection() throws Exception{
        clientA.prepareConnectionTo( "localhost" , clientB.getPort() );
        clientA.start();
        Thread.sleep(1000);
        //clientA.connectTo("localhost",clientB.getPort());
        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), clientB.getClientName());
        Assert.assertEquals(clientA.getConnectedWithPort(), clientB.getPort());
        clientA.closeConnection();

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testClientToTrustedServerConnection() throws Exception{
        clientA.prepareConnectionTo( "localhost" , trustedCryptoServer.getPort() );
        clientA.start();
        Thread.sleep(1000);

        //clientA.connectTo("localhost",trustedCryptoServer.getPort());

        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), trustedCryptoServer.getClientName());
        Assert.assertEquals(clientA.getConnectedWithPort(), trustedCryptoServer.getPort());
        clientA.closeConnection();
        tearDown();
    }

    //Used when server needs to share key with other client
    @Test
    public void testTrustedServerToClientConnection() throws Exception{
        trustedCryptoServer.prepareConnectionTo( "localhost" , clientB.getPort() );
        trustedCryptoServer.start();
        Thread.sleep(1000);

        //trustedCryptoServer.connectTo("localhost",clientB.getPort());

        Assert.assertEquals(trustedCryptoServer.connectionEstablished, true);
        Assert.assertEquals(trustedCryptoServer.getConnectedWithName(), clientB.getClientName());
        Assert.assertEquals(trustedCryptoServer.getConnectedWithPort(), clientB.getPort());
        trustedCryptoServer.closeConnection();
        tearDown();
    }

    @Test
    public void testTrustedCryptoServer() throws Exception{

        //testing certified client search function
        trustedCryptoServer.certifyNewClient("ClientG", 856, "localhost");
        Assert.assertNotNull( trustedCryptoServer.getCertifiedClient("ClientG") );

        Assert.assertNull(trustedCryptoServer.getCertifiedClient("ClientA"));

        trustedCryptoServer.certifyNewClient( "ClientK", 555 , "localhost" );
        Assert.assertEquals( trustedCryptoServer.verifyThatClientsAreTrusted( "ClientK", "ClientG" ), true );

        Assert.assertEquals( trustedCryptoServer.verifyThatClientsAreTrusted( "ClientA", "ClientG" ), false );

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testSharedKeyRequestAndGenerationConnection() throws Exception{
        trustedCryptoServer.certifyNewClient( clientA.getClientName(), clientA.getPort(), clientA.getHostName() );
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );
        clientA.prepareConnectionTo( "localhost" , trustedCryptoServer.getPort() );
        clientA.start();
        Thread.sleep(1000);

        Assert.assertEquals(clientA.connectionEstablished, true);
        Assert.assertEquals(clientA.getConnectedWithName(), trustedCryptoServer.getClientName());
        Assert.assertEquals(clientA.getConnectedWithPort(), trustedCryptoServer.getPort());

        //KEY REQUEST
        clientA.requestSharedKeyWith( clientB.getClientName() );

        clientA.closeConnection();
        Assert.assertEquals( trustedCryptoServer.generateNewSharedKey().length, 32);

        //clientA.closeConnection();
        Thread.sleep(5000);
        Assert.assertEquals( clientA.getSharedKeyLength() , 32 );

        // 32 bytes = 256 bits, checking that sharedkeys are the correct size
        Assert.assertEquals( trustedCryptoServer.connectionEstablished , true );
        Assert.assertEquals( trustedCryptoServer.connectedWithName , clientB.getClientName() );

        //clientA.closeConnection();
        Thread.sleep(3000);
        Assert.assertEquals( clientB.getSharedKeyLength() , 32 );

        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Closing sockets");
        Thread.sleep(1000);
        clientA.stopServer();
        clientB.stopServer();
        trustedCryptoServer.stopServer();
        trustedCryptoServer.clearCertifiedClients();
        System.out.println();
    }
}