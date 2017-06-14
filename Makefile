JAVAC = javac
JFLAGS = -g
SOURCES = src/PeerClient.java \
	src/SecureClient.java \
	src/TrustedCryptoServer.java \
	src/SecureChatInterface.java

.SUFFIXES: .java .class

CLS= $(SOURCES:.java=.class)

all:	$(CLS)

.java.class:
	$(JAVAC) $(JFLAGS) $< -cp bcprov-ext-jdk15on-157.jar


clean:
	@rm -f Main*.class
	@rm -f $(SOURCES:.java=.class)