import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnitTestApplication {
    SecureClient clientA;

    @Before
    public void setUp() throws Exception {
         clientA = new SecureClient( "Client_A", 2222 );
    }

    @Test
    public void testA() throws Exception{
        //clientA.stopServer();
        SecureClient clientB = new SecureClient("Client_B", 3222);
        clientA.connectTo("localhost",3222);
        System.out.println("Running test");

        //clientA.stopServer();
    }

    @After
    public void tearDown() throws Exception {

    }
}