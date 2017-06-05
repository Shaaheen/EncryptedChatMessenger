import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;

/**
 * Created by Shaaheen on 25-May-17.
 */
public class SecureClient extends PeerClient{
    protected byte[] sharedKey;
    public static String ENCRYPTED_MESSAGE_KEYPHRASE = "encrypted_message_incoming";
    public static String ENCRYPTED_FILE_KEYPHRASE = "encrypted_file_incoming-";
    public static String SIGN_AUTHENTICATION_KEYPHRASE = "signature_incoming";

    private boolean startedCommunication;
    protected SecureClientThread currentSecureClientThread;
    private Key[] publicPrivateKeyPair;
    private SecureConnection secureConnectionWithPeer;

    SecureClient(String clientName, int port) {
        super(clientName, port);
        sharedKey = new byte[1]; //placeholder for sharedkey
        this.startedCommunication = false;
        this.currentSecureClientThread = null;
        this.secureConnectionWithPeer = null;
    }

    SecureClient(String clientName, int port, byte[] sharedKey) {
        super(clientName, port);
        this.sharedKey = sharedKey;
        this.startedCommunication = false;
        this.currentSecureClientThread = null;
        this.secureConnectionWithPeer = null;
    }

    protected void startedCommunication(){
        this.startedCommunication = true;
    }

    protected void establishSecureConnection(SecureConnection secureConnection){
        this.secureConnectionWithPeer = secureConnection;
    }

    public Key getPublicKey(){
        return publicPrivateKeyPair[0];
    }

//    protected void requestEncryptedConnectionWith( String peerClientName ) throws IOException {
//        requestSharedKeyWith( peerClientName, );
//
//    }

    protected SecureConnection requestEncryptedConnectionWith( SecureClient clientToConnectTo , TrustedCryptoServer trustedCryptoServer ) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        System.out.println();
        System.out.println("GENERATING MASTER KEY AND SHARING IT...");
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
        System.out.println();
        System.out.println("--- SECURE MESSAGING CHAT ---");
        SecureClient receivingClient = new SecureClient( clientToConnectTo.getClientName() , clientToConnectTo.getPort() , sharedKey);
        SecureClient clientWithConnection = new SecureClient( getClientName() , getPort() , sharedKey);

        receivingClient.setPublicPrivateKeyPair( SecureClient.getPublicPrivateKeyPair(1024) );
        clientWithConnection.setPublicPrivateKeyPair( SecureClient.getPublicPrivateKeyPair(1024) );

        clientWithConnection.prepareConnectionTo(receivingClient.getHostName(), receivingClient.getPort());
        clientWithConnection.startedCommunication();
        clientWithConnection.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SecureConnection secureConnection = new SecureConnection(clientWithConnection, receivingClient,sharedKey);
        receivingClient.establishSecureConnection(secureConnection);
        clientWithConnection.establishSecureConnection(secureConnection);
        return secureConnection;

    }

    protected void sendEncryptedMessage(String message) throws IOException, InvalidCipherTextException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidKeyException {
        if ( startedCommunication ) {
            sendMessage(ENCRYPTED_MESSAGE_KEYPHRASE);
        } else{
            currentSecureClientThread.sendMessage(ENCRYPTED_MESSAGE_KEYPHRASE);
        }
        sendEncryptedByteArray(message.getBytes());
    }

    private void sendEncryptedByteArray(byte[] input) throws IOException, InvalidCipherTextException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchProviderException, InvalidKeyException {
        if ( startedCommunication ){
            //sendMessage( ENCRYPTED_MESSAGE_KEYPHRASE );
            byte[] encryptedMessage = encryptWithAesKey( input, sharedKey );
            System.out.println(getClientName() + " is Sending Encrypted message = "
                            + byteArrayToHex(encryptedMessage) + " with key " + byteArrayToHex(sharedKey)
            );
            out.writeInt(encryptedMessage.length);
            out.write(encryptedMessage);
            out.flush();
            sendMessage(SIGN_AUTHENTICATION_KEYPHRASE);
            byte[] signature = getSignForMessage( encryptedMessage );
            out.writeInt(signature.length);
            out.write(signature);
            out.flush();
            System.out.println(getClientName() + " sent the file with signature : " + byteArrayToHex( signature ));

        }
        else{
            if (currentSecureClientThread !=  null){
                //currentSecureClientThread.sendMessage(ENCRYPTED_MESSAGE_KEYPHRASE);
                byte[] encryptedMessage = encryptWithAesKey(input, sharedKey);
                System.out.println(getClientName() + " is Sending Encrypted message = "
                                + byteArrayToHex(encryptedMessage) + " with key " + byteArrayToHex(sharedKey)
                );
                currentSecureClientThread.os.writeInt(encryptedMessage.length);
                currentSecureClientThread.os.write(encryptedMessage);
                currentSecureClientThread.os.flush();

                currentSecureClientThread.sendMessage(SIGN_AUTHENTICATION_KEYPHRASE);
                byte[] signature = getSignForMessage( encryptedMessage );
                currentSecureClientThread.os.writeInt(signature.length);
                currentSecureClientThread.os.write(signature);
                currentSecureClientThread.os.flush();
                System.out.println(getClientName() + " sent the file with signature : " + byteArrayToHex( signature ));
            }

        }
    }

    protected void sendEncryptedFile(String fileName, String filePath) throws IOException, InvalidCipherTextException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidKeyException {
        Path fileLocation = Paths.get(filePath);
        byte[] data = Files.readAllBytes(fileLocation);
        System.out.println(getClientName() + " read in file.");

        if ( startedCommunication ) {
            sendMessage(ENCRYPTED_FILE_KEYPHRASE + fileName);
        } else{
            currentSecureClientThread.sendMessage(ENCRYPTED_FILE_KEYPHRASE + fileName);
        }

        sendEncryptedByteArray( data );

    }

    protected static boolean authenticateMessage(byte[] encryptedMessage, ObjectInputStream is, SecureClient secureClient) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchProviderException, NoSuchPaddingException, InvalidCipherTextException {
        String authenticationReq = (String)is.readObject();
        if (authenticationReq.equals(SecureClient.SIGN_AUTHENTICATION_KEYPHRASE)){
            System.out.println(secureClient.getClientName() + " is authenticating message...");
            int lengthAuth = is.readInt();//is.readInt();                    // read length of incoming message
            byte[] signature = new byte[1];
            if(lengthAuth>0) {
                signature = new byte[lengthAuth];
                is.readFully(signature, 0, signature.length); // read the message
            }
            byte[] decryptedSign = SecureClient.decryptWithRSAKey( signature , secureClient.getSecureConnectionWithPeer().getOtherPublicKey(secureClient.getClientName()) );
            System.out.println("Decrypted sign : "+  SecureClient.byteArrayToHex(decryptedSign) + " with pubkey :" + SecureClient.byteArrayToHex(secureClient.getSecureConnectionWithPeer().getOtherPublicKey(secureClient.getClientName()).getEncoded()) );
            if ( Arrays.equals(decryptedSign, SecureClient.hashByteArray(encryptedMessage)) ){
                System.out.println(secureClient.getClientName() + " has successfully authenticated the message.");
                return true;
            }
            else{
                System.out.println( secureClient.getClientName() + " has failed to authenticate the message." );
                return false;
            }
        }
        else{
            System.out.println("No Authentication found");
            return false;
        }
    }

    protected byte[] getSignForMessage(byte[] messageData) throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchProviderException, NoSuchPaddingException {
        //Encrypt the hash of the message with the private key
        System.out.println("Orig hash message : " + byteArrayToHex(hashByteArray(messageData)) + " need pubkey : " + byteArrayToHex(publicPrivateKeyPair[0].getEncoded()));
        return ( SecureClient.encryptWithRSAKey( hashByteArray(messageData), publicPrivateKeyPair[1] ) );
    }

    protected static byte[] hashByteArray( byte[] input ) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest( input );
    }

    protected static Key[] getPublicPrivateKeyPair(int lengthOfKeys) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        Key[] keyPair = new Key[2];
        Security.addProvider(new BouncyCastleProvider());
        SecureRandom random = new SecureRandom();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

        generator.initialize(lengthOfKeys, random);
        KeyPair pair = generator.generateKeyPair();
        Key pubKey = pair.getPublic();
        Key privKey = pair.getPrivate();

        keyPair[0] = pubKey;
        keyPair[1] = privKey;

        return keyPair;
    }

    protected static byte[] encryptWithRSAKey( byte[] input, Key rsaKey ) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
        SecureRandom random = new SecureRandom();
        cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
        return cipher.doFinal( input );
    }

    protected static byte[] decryptWithRSAKey( byte[] input, Key rsaKey ) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, rsaKey);
        return cipher.doFinal( input );
    }

    protected static void receiveAndStoreEncryptedFile( String fileName, ObjectInputStream objectInputStream, SecureClient secureClient ) throws IOException, InvalidCipherTextException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchProviderException, InvalidKeyException, ClassNotFoundException {
        System.out.println(secureClient.getClientName() + " is receiving encrypted file...");

        int length = objectInputStream.readInt();//is.readInt();                    // read length of incoming message
        byte[] encryptedMessage = new byte[1];
        if(length>0) {
            encryptedMessage = new byte[length];
            objectInputStream.readFully(encryptedMessage, 0, encryptedMessage.length); // read the message
        }
        System.out.println(secureClient.getClientName() + " has received Encrypted msg= " + SecureClient.byteArrayToHex(encryptedMessage));

        if (SecureClient.authenticateMessage(encryptedMessage, objectInputStream, secureClient)){
            System.out.println( secureClient.getClientName() + " received Encrypted msg= " + SecureClient.byteArrayToHex( encryptedMessage ) );

            FileOutputStream fos = new FileOutputStream(secureClient.getClientName() + fileName);
            fos.write( SecureClient.decryptWithAesKey(encryptedMessage, secureClient.sharedKey) );
            fos.close();

            System.out.println("----------------");
            System.out.println(secureClient.getClientName() + ">saved file : " + fileName);
            System.out.println("----------------");

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
    protected void processMessage(String message) throws IOException, InvalidCipherTextException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchProviderException, InvalidKeyException, ClassNotFoundException {
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
            System.out.println(name + " received SharedKey: " + SecureClient.byteArrayToHex( sharedKey ) );
        }
        else if (message.equals( SecureClient.ENCRYPTED_MESSAGE_KEYPHRASE )){
            System.out.println(getClientName() + " receiving encrypted message...");

            int length = in.readInt();//is.readInt();                    // read length of incoming message
            byte[] encryptedMessage = new byte[1];
            if(length>0) {
                encryptedMessage = new byte[length];
                in.readFully(encryptedMessage, 0, encryptedMessage.length); // read the message
            }

            if (SecureClient.authenticateMessage(encryptedMessage, in, this)){
                System.out.println( getClientName() + " received Encrypted msg= " + SecureClient.byteArrayToHex( encryptedMessage ) );
                System.out.println("----------------");
                System.out.println( getClientName() + "> Received decrypted msg= "
                        + new String( SecureClient.decryptWithAesKey( encryptedMessage, sharedKey ), "UTF-8" ) );
                System.out.println("-----------------=");
            }
        }
        else if ( message.contains(SecureClient.ENCRYPTED_FILE_KEYPHRASE) ){
            System.out.println(getClientName() + " is going to receive encrypted file...");

            String fileName = message.split("-")[1];
            SecureClient.receiveAndStoreEncryptedFile(fileName, in, this);
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
    protected ClientThread getNewClientThread(Socket clientSocket, String serverName) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {
        return new SecureClientThread( clientSocket, serverName, this );
    }

    public void setPublicPrivateKeyPair(Key[] publicPrivateKeyPair) {
        this.publicPrivateKeyPair = publicPrivateKeyPair;
    }

    public SecureConnection getSecureConnectionWithPeer() {
        return secureConnectionWithPeer;
    }
}


class SecureClientThread extends ClientThread{

    private SecureClient secureClient;

    SecureClientThread(Socket clientSocket, String serverName, SecureClient secureClient) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
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
    protected void reactToKeyword(String keyword) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidCipherTextException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, ClassNotFoundException {
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
            System.out.println(secureClient.getClientName() + " is receiving sharedkey...");

            int length = is.readInt();//is.readInt();                    // read length of incoming message
            if(length>0) {
                secureClient.sharedKey = new byte[length];
                is.readFully(secureClient.sharedKey, 0, secureClient.sharedKey.length); // read the message
            }

            System.out.println(secureClient.name + " received SharedKey: " + SecureClient.byteArrayToHex( secureClient.sharedKey ) );
        }
        else if (keyword.equals( SecureClient.ENCRYPTED_MESSAGE_KEYPHRASE )){
            System.out.println(secureClient.getClientName() + " is receiving an encrypted message...");

            int length = is.readInt();//is.readInt();                    // read length of incoming message
            byte[] encryptedMessage = new byte[1];
            if(length>0) {
                encryptedMessage = new byte[length];
                is.readFully(encryptedMessage, 0, encryptedMessage.length); // read the message
            }

            if (SecureClient.authenticateMessage(encryptedMessage, is, secureClient)){
                System.out.println( secureClient.name + " received Encrypted msg= " + SecureClient.byteArrayToHex( encryptedMessage ) );
                System.out.println("----------------");
                System.out.println( secureClient.name + "> Received decrypted msg= "
                        + new String( SecureClient.decryptWithAesKey( encryptedMessage, secureClient.sharedKey ), "UTF-8" ) );
                System.out.println("----------------");
            }

        }
        else if ( keyword.contains( SecureClient.ENCRYPTED_FILE_KEYPHRASE ) ){
            System.out.println( secureClient.getClientName() + " is going to receive encrypted file..." );

            String fileName = keyword.split("-")[1];
            SecureClient.receiveAndStoreEncryptedFile(fileName, is, secureClient);
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

    protected Key getOtherPublicKey(String clientName){
        if (startingClient.getClientName().equals(clientName)){
            return receivingClient.getPublicKey();
        }
        else{
            return startingClient.getPublicKey();
        }
    }
}

