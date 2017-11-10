all: AtmClient.class BankServer.class

atm: AtmClient.class

bank: BankServer.class

AtmClient.class: AtmClient.java
	javac -cp .:javax.json-1.1.jar:commons-cli-1.4.jar AtmClient.java

BankServer.class: BankServer.java
	javac -cp javax.json-1.1.jar:commons-cli-1.4.jar BankServer.java
	
.PHONY: clean
clean:
	rm -f *.class
