all: AtmClient.class BankServer.class Validator.class ArgumentParser.class atm bank

atm: AtmClient.class
	chmod +x atm

bank: BankServer.class
	chmod +x bank

test: ValidatorTest.class Validator.class
	java -cp .:junit-4.12.jar:hamcrest-core-1.3.jar org.junit.runner.JUnitCore ValidatorTest

AtmClient.class: AtmClient.java Validator.class ArgumentParser.class
	javac -cp .:javax.json-1.1.jar:commons-cli-1.4.jar AtmClient.java

BankServer.class: BankServer.java Validator.class ArgumentParser.class
	javac -cp .:javax.json-1.1.jar:commons-cli-1.4.jar BankServer.java

ArgumentParser.class: ArgumentParser.java
	javac -cp .:commons-cli-1.4.jar ArgumentParser.java

Validator.class: Validator.java
	javac Validator.java

ValidatorTest.class: ValidatorTest.java
	javac -cp .:junit-4.12.jar:hamcrest-core-1.3.jar ValidatorTest.java
	
.PHONY: clean
clean:
	rm -f *.class
