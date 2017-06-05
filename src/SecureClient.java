import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * Created by Shaaheen on 25-May-17.
 */
public class SecureClient extends PeerClient{
    protected byte[] sharedKey;
    public static String ENCRYPTED_MESSAGE_KEYPHRASE = "encrypted_message_incoming";
    private boolean startedCommunication;
    protected SecureClientThread currentSecureClientThread;

    SecureClient(String clientName, int port) {
        super(clientName, port);
        sharedKey = new byte[1]; //placeholder for sharedkey
        this.startedCommunication = false;
        this.currentSecureClientThread = null;
    }

    SecureClient(String clientName, int port, byte[] sharedKey) {
        super(clientName, port);
        this.sharedKey = sharedKey;
        this.startedCommunication = false;
        this.currentSecureClientThread = null;
    }

    protected void startedCommunication(){
        this.startedCommunication = true;
    }

//    protected void requestEncryptedConnectionWith( String peerClientName ) throws IOException {
//        requestSharedKeyWith( peerClientName, );
//
//    }

    protected SecureConnection requestEncryptedConnectionWith( SecureClient clientToConnectTo , TrustedCryptoServer trustedCryptoServer ) throws IOException {
        requestSharedKeyWith( clientToConnectTo.getClientName() , trustedCryptoServer);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clientToConnectTo.stopServer();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SecureClient receivingClient = new SecureClient( clientToConnectTo.getClientName() , clientToConnectTo.getPort() , sharedKey);
        SecureClient clientWithConnection = new SecureClient( getClientName() , getPort() , sharedKey);
        clientWithConnection.prepareConnectionTo(receivingClient.getHostName(), receivingClient.getPort());
        clientWithConnection.startedCommunication();
        clientWithConnection.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new SecureConnection(clientWithConnection, receivingClient,sharedKey);

//        prepareConnectionTo(clientToConnectTo.getHostName(), clientToConnectTo.getPort());
//        connectToPeer();

        //return clientWithConnection;

    }

    protected void sendEncryptedMessage(String message) throws IOException, InvalidCipherTextException {
        if ( startedCommunication ){
            sendMessage( ENCRYPTED_MESSAGE_KEYPHRASE );
            byte[] encryptedMessage = encryptWithAesKey( message.getBytes() , sharedKey );
            System.out.println( getClientName() + "> Sending Encrypted message = "
                            + byteArrayToHex(encryptedMessage) + " with key " + byteArrayToHex( sharedKey )
            );
            out.writeInt( encryptedMessage.length );
            out.write(encryptedMessage);
            out.flush();
        }
        else{
            if (currentSecureClientThread !=  null){
                currentSecureClientThread.sendMessage(ENCRYPTED_MESSAGE_KEYPHRASE);
                byte[] encryptedMessage = encryptWithAesKey( message.getBytes() , sharedKey );
                System.out.println( getClientName() + "> Sending Encrypted message = "
                                + byteArrayToHex(encryptedMessage) + " with key " + byteArrayToHex( sharedKey )
                );
                currentSecureClientThread.os.writeInt(encryptedMessage.length);
                currentSecureClientThread.os.write(encryptedMessage);
                currentSecureClientThread.os.flush();
            }
//            if ( !getConnectedWithName().equals("Unknown") ){
//                clientThread = serverForPeering.getClientThread(getConnectedWithName())
//            }

        }

    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    protected static byte[] encryptWithAesKey(byte[] input, byte[] sharedKey) throws InvalidCipherTextException {
        return processEncryptedInput( true , input , sharedKey );
    }

    protected static byte[] decryptWithAesKey(byte[] input, byte[] sharedKey) throws InvalidCipherTextException {
        return processEncryptedInput( false , input , sharedKey );
    }

    private static byte[] processEncryptedInput(boolean encrypt, byte[] input, byte[] sharedKey) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new AESEngine(), new PKCS7Padding());
        pbbc.init(encrypt, new KeyParameter(sharedKey));

        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(
                input, 0, input.length, output, 0);

        pbbc.doFinal(output, bytesWrittenOut);

        return output;
    }


    protected void sendSharedKeyRequestMessage(String peerClientName) throws IOException {
        System.out.println("Requesting shared key from server...");
        sendMessage("sharedkey_request:" + getClientName() + ":" + peerClientName);
    }

    protected void requestSharedKeyWith(String peerClientName, TrustedCryptoServer trustedCryptoServer) throws IOException {
        prepareConnectionTo(trustedCryptoServer.getHostName(), trustedCryptoServer.getPort());
        start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendSharedKeyRequestMessage( peerClientName );
    }

    //Method to process messages appropriately with sharedkey processing
    protected void processMessage(String message) throws IOException, InvalidCipherTextException {
        if (message.contains(":")){
            if (message.split(":")[0].equals("name")){
                connectedWithName = message.split(":")[1];
                connectedWithPort = portNumToConnectTo;
                System.out.println(connectedWithName+"> My Name is: " + connectedWithName);
//                message = "I am " + name;
//                sendMessage(message);
                sendMessage("name:" + name);
            }
        }
        else if ( message.contains(TrustedCryptoServer.INCOMING_KEY_KEYWORD) ){
            int length = in.readInt();                    // read length of incoming message
            if(length>0) {
                sharedKey = new byte[length];
                in.readFully(sharedKey, 0, sharedKey.length); // read the message
            }
            System.out.println(name + ">Received SharedKey: " + Arrays.toString( sharedKey ) );
        }
        else if (message.equals( SecureClient.ENCRYPTED_MESSAGE_KEYPHRASE )){
            System.out.println(getClientName() + "> Receiving encrypted message...");

            int length = in.readInt();//is.readInt();                    // read length of incoming message
            byte[] encryptedMessage = new byte[1];
            if(length>0) {
                encryptedMessage = new byte[length];
                in.readFully(encryptedMessage, 0, encryptedMessage.length); // read the message
            }

            System.out.println(getClientName() + "> Received Encrypted msg= " + SecureClient.byteArrayToHex(encryptedMessage));
            System.out.println( getClientName() + "> Decrypted msg= "
                    + new String( SecureClient.decryptWithAesKey( encryptedMessage, sharedKey ), "UTF-8" ) );
        }
        else if (message.equals("end_connection")){
            sendMessage("end_connection");
            System.out.println(name + "> " + message);
        }
        else{
            System.out.println(name + "> " + message);
        }
    }

    protected int getSharedKeyLength(){
        return sharedKey.length;
    }

    //Returns a trusted server thread - Communication thread for client
    protected ClientThread getNewClientThread(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException {
        System.out.println("OVERIDED METHOD 2" );
        return new SecureClientThread( clientSocket, serverName, this );
    }
}


class SecureClientThread extends ClientThread{

    private SecureClient secureClient;

    SecureClientThread(Socket clientSocket, String serverName, SecureClient secureClient) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException {
        super(clientSocket, serverName);
        this.secureClient = secureClient;
        setKeywordsInMessages(); //Set new shared key request keyword
        secureClient.currentSecureClientThread = this;
        communicateWithClient(clientSocket,serverName);

    }

    //Add shared key to key word reaction list
    protected void setKeywordsInMessages(){
        this.keywordsInMessages = Arrays.asList( "end_connection" , TrustedCryptoServer.INCOMING_KEY_KEYWORD );
    }

    //Method to react to specific keywords
    // Will react to a shared key request by a client towards another client
    protected void reactToKeyword(String keyword) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidCipherTextException {
        //System.out.println("DIFF KEYWORDs: " + this.keywordsInMessages + " and keyword : " + keyword);
        if (keyword.equals("end_connection")){
            sendMessage("end_connection");
        }
        else if (keyword.contains(":")){
            if (keyword.split(":")[0].equals("name")){
                username = keyword.split(":")[1] + username ;
                System.out.println(username+"> my name is " +  keyword.split(":")[1]);
            }
        }
        else if (keyword.equals(TrustedCryptoServer.INCOMING_KEY_KEYWORD)){
            System.out.println(secureClient.getClientName() + "> Receiving sharedkey...");

            int length = is.readInt();//is.readInt();                    // read length of incoming message
            System.out.println("LENGTH IS " + length);
            if(length>0) {
                secureClient.sharedKey = new byte[length];
                is.readFully(secureClient.sharedKey, 0, secureClient.sharedKey.length); // read the message
            }

            System.out.println(secureClient.name + ">Received SharedKey: " + Arrays.toString( secureClient.sharedKey ) );
        }
        else if (keyword.equals( SecureClient.ENCRYPTED_MESSAGE_KEYPHRASE )){
            System.out.println(secureClient.getClientName() + "> Receiving encrypted message...");

            int length = is.readInt();//is.readInt();                    // read length of incoming message
            byte[] encryptedMessage = new byte[1];
            if(length>0) {
                encryptedMessage = new byte[length];
                is.readFully(encryptedMessage, 0, encryptedMessage.length); // read the message
            }

            System.out.println( secureClient.name + "> Received Encrypted msg= " + SecureClient.byteArrayToHex( encryptedMessage ) );
            System.out.println( secureClient.name + "> Decrypted msg= "
                    + new String( SecureClient.decryptWithAesKey( encryptedMessage, secureClient.sharedKey ), "UTF-8" ) );
        }
        else{
            System.out.println(username + "> " + keyword);
        }
    }
}


class SecureConnection {
    private SecureClient startingClient;
    private SecureClient receivingClient;
    private byte[] sharedKeyBetweenClients;

    SecureConnection(SecureClient startingClient, SecureClient receivingClient) {
        this.startingClient = startingClient;
        this.receivingClient = receivingClient;
    }
    SecureConnection(SecureClient startingClient, SecureClient receivingClient, byte[] sharedKey) {
        this.startingClient = startingClient;
        this.receivingClient = receivingClient;
        this.sharedKeyBetweenClients = sharedKey;
    }

    public SecureClient getStartingClient() {
        return startingClient;
    }

    public SecureClient getReceivingClient() {
        return receivingClient;
    }
}

