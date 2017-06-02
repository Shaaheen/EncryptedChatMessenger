import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnitTestApplication {
    Client clientA;

    @Before
    public void setUp() throws Exception {
         clientA = new Client( "Client_A", 2222 );
    }

    @Test
    public void testA() throws Exception{
        clientA.stopServer();
        Client clientB = new Client("Client_B", 3222);
        clientA.connectTo("localhost",3222);
        System.out.println("Running test");

        //clientA.stopServer();
    }

    @After
    public void tearDown() throws Exception {

    }
}