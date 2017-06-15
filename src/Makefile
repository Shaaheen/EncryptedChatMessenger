JAVAC = javac
JFLAGS = -g
SOURCES = PeerClient.java \
	SecureClient.java \
	TrustedCryptoServer.java \
	SecureChatInterface.java \
	ExampleServerInterface.java

.SUFFIXES: .java .class

CLS= $(SOURCES:.java=.class)

all:	$(CLS)

.java.class:
	$(JAVAC) $(JFLAGS) $< -cp bcprov-ext-jdk15on-157.jar;C:\Users\Omondi\GitHub\EncryptedChatMessenger\src


clean:
	@rm -f *.class
	
start_C:
	java -cp bcprov-ext-jdk15on-157.jar;C:\Users\Omondi\GitHub\EncryptedChatMessenger\src SecureChatInterface Client_C 5310
	
start_D:
	java -cp bcprov-ext-jdk15on-157.jar;C:\Users\Omondi\GitHub\EncryptedChatMessenger\src SecureChatInterface Client_D 5410

start_server:
	java -cp bcprov-ext-jdk15on-157.jar;C:\Users\Omondi\GitHub\EncryptedChatMessenger\src ExampleServerInterface