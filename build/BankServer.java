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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.*;
import java.security.*;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import org.apache.commons.cli.*;

class BankServer {
    static HashMap<String, Account> allAccounts = null;

    public static void main(String[] args)  {

      Signal.handle(new Signal("TERM"), new SignalHandler() {
        public void handle(Signal sig) {
          System.exit(0);
        }
      });
      Signal.handle(new Signal("INT"), new SignalHandler() {
        public void handle(Signal sig) {
          System.exit(0);
        }
      });
        allAccounts = new HashMap<String, Account>();
        Options options = new Options();
        
        ArgumentParser.generateOption(options, "s", true, "auth-file", false);
        ArgumentParser.generateOption(options, "p", true, "account", false);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            ArgumentParser.printInvalidArgs(options);
        }

        if (ArgumentParser.hasDuplicateFlags(cmd.getOptions())) {
            ArgumentParser.printInvalidArgs(options);
        }

        String authFile = ArgumentParser.getOptionValue(cmd, 's', "bank.auth");
        if (!Validator.validateFileName(authFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        if (!Validator.validateNumber(ArgumentParser.getOptionValue(cmd, 'p', "3000"))) {
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

        secureRandom = new SecureRandom();
        secureRandom.nextInt();

        File af = new File(authFile);
        if (af.exists()) {
          System.exit(255);
        }
        Process proc = null;
        String dir_path = System.getenv("my_dir");
        String command = "sh "+dir_path+"/key.sh " + authFile + " " + passphrase;
        try {
          proc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
          System.exit(255);
        }

        try {
          proc.waitFor();
        } catch (InterruptedException e) {
          System.exit(255);
        }
        
        System.out.println("created");
        System.out.flush();

        try {
          clientKeyStore = KeyStore.getInstance("JKS");
          clientKeyStore.load(new FileInputStream(authFile), passphrase.toCharArray());
          serverKeyStore = KeyStore.getInstance("JKS");
          serverKeyStore.load(new FileInputStream(authFile), passphrase.toCharArray());
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
          server.setNeedClientAuth(true);
          server.setEnabledProtocols(protocol);
          server.setEnabledCipherSuites(suites);
        } catch (GeneralSecurityException gse) {
            System.err.println("GeneralSecurityException trying to create secure socket, exiting..");
            System.exit(255);
        } catch (IOException ex) {
            System.err.println("IOException trying to create key file");
            System.exit(255);
        }
        
        while (true) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = null;
            try(SSLSocket clientSocket = (SSLSocket) server.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());)
            {

                future = executor.submit(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        String response = null;
                        while((response=br.readLine()) == null);
                        return response;
                    }
                });
                
                String str = future.get(10, TimeUnit.SECONDS);
                
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

                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder().add("account", requestedAccount);
                JsonObject response = null;

                boolean error = false;
                
                BigDecimal bigRequestedAmount = null;
                
                switch (mode) {
                    case 'n':
                        if (allAccounts.get(requestedAccount) != null || requestedAmount < 10 ) {
                            error = true;
                            break;
                        }
                        bigRequestedAmount = new BigDecimal(requestedAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
                        account = new Account();
                        account.bigbalance = bigRequestedAmount;
                        account.cardFileName = requestedCardFileName;
                        account.cardFileContent = cardFileContent;
                        allAccounts.put(requestedAccount, account);
                        jsonBuilder.add("initial_balance", requestedAmount);
                        break;
                    case 'w':
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
                        temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                        if(account.bigbalance.subtract(temp).compareTo(BigDecimal.ZERO) == -1){
                            error = true;
                            break;
                        }
                        account.bigbalance = account.bigbalance.subtract(temp);
                        jsonBuilder.add("withdraw", requestedAmount);
                        break;
                    case 'd':
                        account = allAccounts.get(requestedAccount);
                        if (account == null) {
                            error = true;
                            break;
                        }
                        if ( !checkCardFiles(account.cardFileContent, cardFileContent) || requestedAmount <= 0) {
                            error = true;
                            break;
                        }
                        account.bigbalance = account.bigbalance.add(new BigDecimal(requestedAmount).setScale(2, BigDecimal.ROUND_HALF_UP));
                        jsonBuilder.add("deposit", requestedAmount);
                        break;
                    case 'g':
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
                    System.out.flush();
                }
                
                pw.println(response.toString());
                pw.flush();
                
                executor.shutdownNow();

            } catch(TimeoutException e) {
                future.cancel(true);
                executor.shutdownNow();
                System.out.println("protocol_error");
                System.out.flush();
            } catch (SSLHandshakeException e) {
                executor.shutdownNow();
                System.out.println("protocol_error");
                System.out.flush();
            } catch (SSLException e) {
                executor.shutdownNow();
                System.out.println("protocol_error");
                System.out.flush();
            } catch (Exception e) {
                executor.shutdownNow();
                System.out.println("protocol_error");
                System.out.flush();
            }
        }
    }

    static boolean checkCardFiles(String actual, String current) {
         return actual.equals(current);
    }

}

class Account {
    BigDecimal bigbalance; 
    String cardFileName;
    String cardFileContent;
}
