import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.*;
import javax.json.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

class AtmClient {

    private static boolean createCardFile(String cardFile){
            File f = new File(cardFile);
            if(f.exists()){
                System.exit(255);
            }
            Random random = new SecureRandom();
            char buf[] = new char[200];
            char s[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789".toCharArray();
            for(int i=0; i<buf.length; i++){
                buf[i] = s[random.nextInt(s.length)];
            }
        try {
            FileWriter fw = new FileWriter(cardFile);
            PrintWriter printWriter = new PrintWriter(fw);
            printWriter.print(buf);
            printWriter.close(); //check for nullPointerException
        } catch (IOException ex) {
            Logger.getLogger(AtmClient.class.getName()).log(Level.SEVERE, null, ex);
        }
           
        return true;
    }
    
    private static String readCardFile(String cardFile){
        String line="";
        try{
        FileReader fileReader = 
                new FileReader(cardFile);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
            line = bufferedReader.readLine();
            bufferedReader.close(); 
        }catch(FileNotFoundException e1){
            System.exit(255); //is this correct ??
        }
        catch(IOException e2){
            System.exit(255); //is this correct ??
        }
        return line;
    }

    
    public static void main(String[] args) throws IOException {
        Options options = new Options();
        
        ArgumentParser.generateOption(options, "a", true, "account", true);
        ArgumentParser.generateOption(options, "s", true, "auth-file", false);
        ArgumentParser.generateOption(options, "i", true, "ip-address", false);
        ArgumentParser.generateOption(options, "p", true, "port", false);
        ArgumentParser.generateOption(options, "c", true, "card-file", false);
        
        OptionGroup optionGroup = new OptionGroup();
        optionGroup.addOption(new Option("n", true, "new-account"));
        optionGroup.addOption(new Option("d", true, "deposit"));
        optionGroup.addOption(new Option("w", true, "withdraw"));
        optionGroup.addOption(new Option("g", false, "balance"));
        optionGroup.setRequired(true);
        options.addOptionGroup(optionGroup);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            ArgumentParser.printInvalidArgs(options);
        } 

        // If there are any unrecognized arguments left, reject it
        if (cmd.getArgs().length > 0) {
            ArgumentParser.printInvalidArgs(options);
        }

        String account = ArgumentParser.getOptionValue(cmd, 'a', "");
        if (!Validator.validateAccountName(account)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String authFile = ArgumentParser.getOptionValue(cmd, 's', "bank.auth");
        if (!Validator.validateFileName(authFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String ipAddress = ArgumentParser.getOptionValue(cmd, 'i', "127.0.0.1");
        if (!Validator.validateIPAddress(ipAddress)) {
            ArgumentParser.printInvalidArgs(options);
        }
        int port = Integer.valueOf(ArgumentParser.getOptionValue(cmd, 'p', "3000"));
        if (port < 1024 || port > 65535) {
            ArgumentParser.printInvalidArgs(options);
        }
        String cardFile = ArgumentParser.getOptionValue(cmd, 'c', account + ".card");
        if (!Validator.validateFileName(cardFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String mode = "";
        double amount = 0;
        if (cmd.hasOption('n')) {
            mode = "n";
            String num = ArgumentParser.getOptionValue(cmd, 'n', "");
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('d')) {
            mode = "d";
            String num = ArgumentParser.getOptionValue(cmd, 'd', "");
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('w')) {
            mode = "w";
            String num = ArgumentParser.getOptionValue(cmd, 'w', "");
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('g')) {
            mode = "g";
        } else {
            ArgumentParser.printInvalidArgs(options);
        }

        String cardFileContent = "";
        boolean cardFileCreated = false;
           
            if(mode.equals("n")){
            if(amount < 10)
                System.exit(255);
            createCardFile(cardFile);
            cardFileCreated = true;
        }//shouldn't return if cardfile already exists
        //System.out.print("\n>--"+readCardFile(cardFile)+"--\n");
        
        
        cardFileContent = readCardFile(cardFile);
        
        
        SSLSocketFactory ssf = null;
        SSLSocket serverSocket = null;

        SSLContext sslContext = null;
        KeyStore clientKeyStore;
        KeyStore serverKeyStore;
        String passphrase = "correcthorsebatterystaple";
        SecureRandom secureRandom;
        String[] protocol = { "TLSv1.2" };
        String[] suites = {"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"};

        // setup random
        secureRandom = new SecureRandom();
        secureRandom.nextInt();
        try {
          // setup server key store 
          serverKeyStore = KeyStore.getInstance("JKS");
          serverKeyStore.load( new FileInputStream(authFile),
              passphrase.toCharArray() );
          // and client key store
          clientKeyStore = KeyStore.getInstance("JKS");
          clientKeyStore.load( new FileInputStream(authFile),
              passphrase.toCharArray() );
          // and ssl context
          TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

          tmf.init( serverKeyStore);

          KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
          kmf.init( clientKeyStore, passphrase.toCharArray() );

          sslContext = SSLContext.getInstance("TLS");
          sslContext.init(kmf.getKeyManagers(),
              tmf.getTrustManagers(),
              secureRandom );
        } catch (GeneralSecurityException gse) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
            System.exit(255);
        }
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = null;
        try {
          ssf = sslContext.getSocketFactory();
          serverSocket = (SSLSocket)ssf.createSocket(ipAddress, port);

          // require TLS 1.2
          serverSocket.setEnabledProtocols(protocol);
          // and require TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
          serverSocket.setEnabledCipherSuites(suites);
            
         
        
        JsonObject value = Json.createObjectBuilder().add("mode", mode)
                            .add("account", account).add("amount", amount)
                            .add("cardfile", cardFile).add("port", port) //no need to send cardfile name
                            .add("IPaddress", authFile)
                            .add("authFile", authFile)
                            .add("cardFileContent", cardFileContent)
                            .build();

          
            final PrintWriter pw = new PrintWriter(
                                    serverSocket.getOutputStream());
            final BufferedReader br = new BufferedReader(
                                        new InputStreamReader(
                                            serverSocket.getInputStream()));
            
            future = executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    //passing the json object as string
                    pw.println(value.toString());
                    pw.flush();
                    
                    //getting response from the server.
                    //If in the response, "error" = true, then
                    //the transaction failed due to wrong inputs.
                    String response = null;
                    //waiting for server's respone
                    while((response=br.readLine()) == null);
                    return response;
                }
            });

            String response = future.get(10, TimeUnit.SECONDS);
            
            JsonReader jsonReader = Json.createReader(
                                                    new StringReader(response));
            JsonObject responseObject = jsonReader.readObject();
            jsonReader.close();

            boolean error = responseObject.getBoolean("error");

            if (error) {
                if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
                System.exit(255);
            } else {
                //responseObject;
                
                //Json.createObjectBuilder(responseObject).remove("error").build().toString();
                System.out.println(Json.createObjectBuilder(responseObject).remove("error").build().toString());
            }

            pw.close();
            br.close();
            serverSocket.close();

    
        } catch (TimeoutException e) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
            future.cancel(true);
            System.exit(63);
        } catch (ConnectException e) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
            System.exit(63);
        } catch (ExecutionException e) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
          System.out.println("protocol_error");
          System.exit(63);
        } catch (Exception e) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
                }
          //TODO: check for other exceptions? refine ExecutionException?
          // get rid of printing stacktrace
          e.printStackTrace();
        }

        executor.shutdownNow();
        System.exit(0);
    }
}
