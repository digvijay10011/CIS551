import javax.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.*;
import java.security.*;

import org.apache.commons.cli.*;

class BankServer {

    
    static HashMap<String, Account> allAccounts = null;
    //static HashMap<String, String> cardFiles = null;

    public static void main(String[] args) {

        allAccounts = new HashMap<String, Account>();
        //cardFiles = new HashMap<String, String>();   //we will need a separate HashMap for cardFiles !
                                                    // because we need to check if cardFileName is duplicated or not.

        Options options = new Options();
        
        ArgumentParser.generateOption(options, "s", true, "auth-file", false);
        ArgumentParser.generateOption(options, "p", true, "account", false);
        // TODO: Remove this before submitting
        ArgumentParser.generateOption(options, "v", false, "verbose", false);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            ArgumentParser.printInvalidArgs(options);
        }

        String authFile = ArgumentParser.getOptionValue(cmd, 's', "bank.auth");
        if (!Validator.validateFileName(authFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        int port = Integer.valueOf(ArgumentParser.getOptionValue(cmd, 'p', "3000"));
        if (port < 1024 || port > 65535) {
            ArgumentParser.printInvalidArgs(options);
        }

        char mode ='n';
        String requestedAccount = "";
        double requestedAmount = 0.0;
        String requestedCardFileName ="";
        String cardFileContent = "";
        SSLContext sslContext;
        KeyStore clientKeyStore;
        KeyStore serverKeyStore;
        String passphrase = "correcthorsebatterystaple";
        SecureRandom secureRandom;
        String[] protocol = {"TLSv1.2"};
        String[] suites = {"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"};
        SSLServerSocket server = null;

        // setup random
        secureRandom = new SecureRandom();
        secureRandom.nextInt();

        // First Create the keystore with filename of input, default bank.auth
        // check if already exists and exit if it does
        
        File af = new File(authFile);
        if (af.exists()) {
          System.err.println("Error: authfile: " + authFile + "  already exists, exiting...");
          System.exit(255);
        }
        Process proc = null;
        String command = "sh key.sh " + authFile + " " + passphrase;
        try {
          proc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
          // TODO: handling
        }

        // wait for it to finish
        try {
          proc.waitFor();
        } catch (InterruptedException e) {
          //TODO: handling
        }
        
        System.out.println("created");
        System.out.flush();

        // setup keystores
        try {
          // setup Client Key Store
          clientKeyStore = KeyStore.getInstance("JKS");
          clientKeyStore.load(new FileInputStream(authFile), passphrase.toCharArray());
          // setup Server Key Store
          serverKeyStore = KeyStore.getInstance("JKS");
          serverKeyStore.load(new FileInputStream(authFile), passphrase.toCharArray());
          // setup SSL context
          TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
          tmf.init( clientKeyStore );
          KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
          kmf.init( serverKeyStore, passphrase.toCharArray() );

          sslContext = SSLContext.getInstance( "TLS" );
          sslContext.init( kmf.getKeyManagers(),
              tmf.getTrustManagers(),
              secureRandom );
          SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
          server = (SSLServerSocket)ssf.createServerSocket(port);
          // require client authorization
          server.setNeedClientAuth(true);
          // require TLSv1.2
          server.setEnabledProtocols(protocol);
          // and TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
          server.setEnabledCipherSuites(suites);
          
        
        } catch (GeneralSecurityException gse) {
          // TODO: handling
            System.err.println("GeneralSecurityException trying to create secure socket, exiting..");
            System.exit(255);
        } catch (IOException ex) {
            //handle it !!!!!
            System.err.println("IOException trying to create key file");
            System.exit(255);
        }
        
        while (true) {
            try {
                Socket clientSocket = null;
                
                clientSocket = server.accept();
                
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());
                
                String str = br.readLine();  //get the commands from client (basically, a JSON string)
                
                JsonReader jsonReader = Json.createReader(new StringReader(str));
                JsonObject object = jsonReader.readObject();
                jsonReader.close();

                mode = object.getString("mode").charAt(0);
                requestedAccount = object.getString("account");
                requestedAmount = object.getJsonNumber("amount").doubleValue();
                requestedCardFileName = object.getString("cardfile");
                authFile = object.getString("authFile");
                cardFileContent = object.getString("cardFileContent");

                Account account = null;

                // response that will be sent to the atm so that atm can print the required message or exit if theres error
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder().add("account", requestedAccount);
                JsonObject response = null;

                boolean error = false;
                
                BigDecimal bigRequestedAmount = null;
                
                switch (mode) {

                    case 'n':   //new account

                        if (allAccounts.get(requestedAccount) != null || requestedAmount < 10 ) {
                            error = true;
                            break;
                        }
                        
                        bigRequestedAmount = new BigDecimal(requestedAmount);
                        account = new Account();
                        //account.balance = requestedAmount;
                        account.bigbalance = bigRequestedAmount;
                        account.cardFileName = requestedCardFileName;
                        account.cardFileContent = cardFileContent;

                        allAccounts.put(requestedAccount, account);
                        //cardFiles.put(requestedCardFileName, cardFileContent);
                        
                        jsonBuilder.add("initial_balance", requestedAmount);

                        break;

                    case 'w':    //withdraw

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if ( !checkCardFiles(account.cardFileContent, cardFileContent) || requestedAmount <= 0) {
                            error = true;
                            break;
                        }
                        
                        BigDecimal temp = new BigDecimal(requestedAmount);
                        if(account.bigbalance.subtract(temp).compareTo(BigDecimal.ZERO) == -1){
                            error = true;
                            break;
                        }

                        
                        //if (account.balance-requestedAmount < 0) {
                        //    error = true;
                        //    break;
                        //}
                        //account.balance -= requestedAmount;
                        account.bigbalance = account.bigbalance.subtract(temp);
                        
                        jsonBuilder.add("withdraw", requestedAmount);

                        break;

                    case 'd':  //deposit

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if ( !checkCardFiles(account.cardFileContent, cardFileContent) || requestedAmount <= 0) {
                            error = true;
                            break;
                        }

                        //account.balance += requestedAmount;
                        account.bigbalance = account.bigbalance.add(new BigDecimal(requestedAmount));
                        //what about max balance ??
                        jsonBuilder.add("deposit", requestedAmount);

                        break;

                    case 'g':   //get the balance

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if ( !checkCardFiles(account.cardFileContent, cardFileContent) ) {
                            error = true;
                        }
                        
                        jsonBuilder.add("balance", account.bigbalance.setScale(2, BigDecimal.ROUND_HALF_UP));

                        break;

                }
                
                
                if(error){
                    response = jsonBuilder.add("error", true).build();
                }
                else{
                    response = jsonBuilder.add("error", false).build();
                    System.out.println(Json.createObjectBuilder(response).remove("error").build().toString());
                    
                }
                //sending response to atm
                
                pw.println(response.toString());
                pw.flush();
                
                

                br.close();
                pw.close();
                clientSocket.close();

            } catch (SSLHandshakeException e) {
              System.out.println("protocol_error");
            } catch (SSLException e) {
              System.out.println("protocol_error");
            } catch (Exception e) {
              //TODO: check for other exceptions?
              // get rid of printing stack trace
              e.printStackTrace();
                //everytime a client disconnects, exception will be thrown
            }
        }

    }

    static boolean checkCardFiles(String actual, String current) {
         return actual.equals(current);
    }

}

class Account {

    //double balance;
    BigDecimal bigbalance; 
    String cardFileName;
    String cardFileContent;

}
