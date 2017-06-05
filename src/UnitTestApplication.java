import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;

public class UnitTestApplication {
    SecureClient clientA;
    SecureClient clientB;
    TrustedCryptoServer trustedCryptoServer;

    @Before
    public void setUp() throws Exception {
        System.out.println("Setting up clients and server" + "\r\n");
        clientA = new SecureClient( "Client_A", 4421 );
        clientB = new SecureClient("Client_B", 5421);
        trustedCryptoServer = new TrustedCryptoServer("Trusted3rdParty", 5221);
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
        clientA.sendSharedKeyRequestMessage(clientB.getClientName());

        clientA.closeConnection();
        Assert.assertEquals( trustedCryptoServer.generateNewSharedKey().length, 32);

        //clientA.closeConnection();
        Thread.sleep(2000);
        Assert.assertEquals( clientA.getSharedKeyLength() , 32 );

        // 32 bytes = 256 bits, checking that sharedkeys are the correct size
        Assert.assertEquals( trustedCryptoServer.connectionEstablished , true );
        Assert.assertEquals( trustedCryptoServer.connectedWithName , clientB.getClientName() );

        //clientA.closeConnection();
        Thread.sleep(3000);
        Assert.assertEquals( clientB.getSharedKeyLength() , 32 );

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testEncryptionAndDecryptionConnection() throws Exception{
        trustedCryptoServer.certifyNewClient( clientA.getClientName(), clientA.getPort(), clientA.getHostName() );
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );
        clientA.prepareConnectionTo( "localhost" , trustedCryptoServer.getPort() );
        clientA.start();
        Thread.sleep(1000);

        //KEY REQUEST
        clientA.sendSharedKeyRequestMessage(clientB.getClientName());

        clientA.closeConnection();

        //clientA.closeConnection();
        Thread.sleep(3000);

        String test = "Test message one";

        Assert.assertEquals( test.substring(0,test.length() - 2) ,  (new String ( SecureClient.decryptWithAesKey( SecureClient.encryptWithAesKey( test.getBytes("UTF-8") , clientA.sharedKey) , clientA.sharedKey ), "UTF-8" )).substring(0, test.length() - 2) );
        //clientA.closeConnection();
        Thread.sleep(2000);

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testEncryptedMessagingConnection() throws Exception{
        trustedCryptoServer.certifyNewClient(clientA.getClientName(), clientA.getPort(), clientA.getHostName());
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );

        //KEY REQUEST
        SecureConnection clientAtoBConnection = clientA.requestEncryptedConnectionWith(clientB, trustedCryptoServer);
        SecureClient secureClientA = clientAtoBConnection.getStartingClient();
        SecureClient secureClientB = clientAtoBConnection.getReceivingClient();

        Thread.sleep(1000);
        secureClientA.sendMessage("HI HI");
        secureClientA.sendEncryptedMessage( "encrypto bismo" );
        secureClientB.sendEncryptedMessage("pls work");

        Thread.sleep(3000);
        secureClientA.closeConnection();
        secureClientB.stopServer();

        tearDown();
    }

    //Used when client requests shared key from server
    @Test
    public void testEncryptedFileTransferConnection() throws Exception{
        trustedCryptoServer.certifyNewClient( clientA.getClientName(), clientA.getPort(), clientA.getHostName() );
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );

        //KEY REQUEST
        SecureConnection clientAtoBConnection = clientA.requestEncryptedConnectionWith(clientB, trustedCryptoServer);
        SecureClient secureClientA = clientAtoBConnection.getStartingClient();
        SecureClient secureClientB = clientAtoBConnection.getReceivingClient();
        //trustedCryptoServer.closeConnection();

        Thread.sleep(1000);
        String fileName = "testfile.txt";
        secureClientA.sendEncryptedFile( fileName, fileName );

//        String fileName2 = "techback.jpg";
//        secureClientA.sendEncryptedFile( fileName2, fileName2 );

        String fileName3 = "techback.jpg";
        secureClientB.sendEncryptedFile( fileName3, fileName3 );

        Path fileLocation = Paths.get(fileName);
        byte[] data = Files.readAllBytes(fileLocation);
        String fileString1 = new String(data);

        Path fileLocation2 = Paths.get(secureClientB.getClientName() + fileName);
        byte[] data2 = Files.readAllBytes(fileLocation2);
        String fileString2 = new String(data2);

        Assert.assertEquals(fileString2.substring(0, fileString2.length() - 4), fileString1);

        Thread.sleep(3000);
        secureClientA.closeConnection();
        secureClientB.stopServer();
        //trustedCryptoServer.closeConnection();
        tearDown();
    }

    //Testing RSA encryption/decryption
    @Test
    public void testRSAEncryption() throws Exception {
        Key[] keys = SecureClient.getPublicPrivateKeyPair(1024);
        String testString = "Testing stringy thingy wingy";
        System.out.println("Orignal : " + testString);
        String decryptedString = new String(SecureClient.decryptWithRSAKey( SecureClient.encryptWithRSAKey( testString.getBytes(), keys[1] ) , keys[0]) );
        System.out.println("Private key : " + SecureClient.byteArrayToHex( keys[1].getEncoded() ));
        System.out.println("Public key : " + SecureClient.byteArrayToHex( keys[0].getEncoded() ));
        System.out.println("Encrypted : " + SecureClient.byteArrayToHex( SecureClient.encryptWithRSAKey( testString.getBytes(), keys[1] ) ));
        System.out.println("Decrypted : " + decryptedString);
        Assert.assertEquals( decryptedString , testString ) ;

        //Test hash RSA encryption
        System.out.println("Orig has: " + SecureClient.byteArrayToHex(SecureClient.hashByteArray(testString.getBytes())) );
        System.out.println("Decrypted hash: " + SecureClient.byteArrayToHex(SecureClient.decryptWithRSAKey(SecureClient.encryptWithRSAKey(SecureClient.hashByteArray(testString.getBytes()),keys[1]),keys[0])));
        Assert.assertArrayEquals(SecureClient.hashByteArray(testString.getBytes()), SecureClient.decryptWithRSAKey(SecureClient.encryptWithRSAKey(SecureClient.hashByteArray(testString.getBytes()),keys[1]),keys[0]));
    }

    //Used when client requests shared key from server
    @Test
    public void testAuthenticatedEncryptedMessaging() throws Exception{
        trustedCryptoServer.certifyNewClient(clientA.getClientName(), clientA.getPort(), clientA.getHostName());
        trustedCryptoServer.certifyNewClient( clientB.getClientName(), clientB.getPort(), clientB.getHostName() );

        //KEY REQUEST
        SecureConnection clientAtoBConnection = clientA.requestEncryptedConnectionWith(clientB, trustedCryptoServer);
        SecureClient secureClientA = clientAtoBConnection.getStartingClient();
        SecureClient secureClientB = clientAtoBConnection.getReceivingClient();

        Thread.sleep(1000);
        String fileName3 = "testfile.txt";
        secureClientA.sendEncryptedMessage( "encrypto bismo" );
        secureClientB.sendEncryptedMessage( "Testing msg");
        secureClientB.sendEncryptedFile(fileName3,fileName3);

        Path fileLocation = Paths.get(fileName3);
        byte[] data = Files.readAllBytes(fileLocation);
        String fileString1 = new String(data);

        Path fileLocation2 = Paths.get(secureClientA.getClientName() + fileName3);
        byte[] data2 = Files.readAllBytes(fileLocation2);
        String fileString2 = new String(data2);

        Assert.assertEquals( fileString2.substring(0,fileString2.length() -4), fileString1 );

        Thread.sleep(3000);
        secureClientA.closeConnection();
        secureClientB.stopServer();

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
        Thread.sleep(2000);
        System.out.println();
    }
}