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
import java.util.Set;
import java.util.HashSet;
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
import java.security.cert.CertificateException;
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

    public static void main(String[] args) {
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

        if (!Validator.validateEquals(args)) {
            ArgumentParser.printInvalidArgs(options);
        }

        Set<String> replacedArgs = new HashSet<String>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-a") || args[i].equals("-c") ||
                args[i].equals("-s") || args[i].equals("-gs") ||
                args[i].equals("-ga") || args[i].equals("-gc")) {
                if (args[i+1].length() >=2) {
                    if (args[i+1].substring(0, 2).equals("-a") || args[i+1].substring(0, 2).equals("-ga") ||
                        args[i+1].substring(0, 2).equals("-s") || args[i+1].substring(0, 2).equals("-gs") ||
                        args[i+1].substring(0, 2).equals("-i") || args[i+1].substring(0, 2).equals("-gi") ||
                        args[i+1].substring(0, 2).equals("-p") || args[i+1].substring(0, 2).equals("-gp") ||
                        args[i+1].substring(0, 2).equals("-c") || args[i+1].substring(0, 2).equals("-gc") ||
                        args[i+1].substring(0, 2).equals("-n") || args[i+1].substring(0, 2).equals("-d") ||
                        args[i+1].substring(0, 2).equals("-w") || args[i+1].substring(0, 2).equals("-g") ||
                        args[i+1].substring(0, 2).equals("--")) {
                        args[i+1] = args[i+1].substring(1);
                        replacedArgs.add(args[i]);
                    }
                }
            }
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            ArgumentParser.printInvalidArgs(options);
        } 

        if (args.length >= 2) {
            if (!(args[args.length-2].equals("-a") || args[args.length-2].equals("-ga") ||
            args[args.length-2].equals("-c") || args[args.length-2].equals("-gc") ||
            args[args.length-2].equals("-s") || args[args.length-2].equals("-gs") 
            ) && args[args.length-1].equals("--")) {
                ArgumentParser.printInvalidArgs(options);
            }
        }

        if (cmd.getArgs().length > 0) {
            ArgumentParser.printInvalidArgs(options);
        }

        if (ArgumentParser.hasDuplicateFlags(cmd.getOptions())) {
            ArgumentParser.printInvalidArgs(options);
        }

        String account = ArgumentParser.getOptionValue(cmd, 'a', "");
        if (replacedArgs.contains("-a") || replacedArgs.contains("-ga")) {
            account = "-" + account;
        }
        if (!Validator.validateAccountName(account)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String authFile = ArgumentParser.getOptionValue(cmd, 's', "bank.auth");
        if (replacedArgs.contains("-s") || replacedArgs.contains("-gs")) {
            authFile = "-" + authFile;
        }
        if (!Validator.validateFileName(authFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String ipAddress = ArgumentParser.getOptionValue(cmd, 'i', "127.0.0.1");
        if (!Validator.validateIPAddress(ipAddress)) {
            ArgumentParser.printInvalidArgs(options);
        }
        if (!Validator.validateNumber(ArgumentParser.getOptionValue(cmd, 'p', "3000"))) {
            ArgumentParser.printInvalidArgs(options);
        }
        int port = 3000;
        try{
            port = Integer.valueOf(ArgumentParser.getOptionValue(cmd, 'p', "3000"));
        }catch(Exception e){
            System.exit(255);
        }
        if (port < 1024 || port > 65535) {
            ArgumentParser.printInvalidArgs(options);
        }
        String cardFile = ArgumentParser.getOptionValue(cmd, 'c', account + ".card");
        if (replacedArgs.contains("-c") || replacedArgs.contains("-gc")) {
            cardFile = "-" + cardFile;
        }
        if (!Validator.validateFileName(cardFile)) {
            ArgumentParser.printInvalidArgs(options);
        }
        String mode = "";
        double amount = 0;
        if (cmd.hasOption('n')) {
            mode = "n";
            String num = ArgumentParser.getOptionValue(cmd, 'n', "");
            if (!Validator.validateBalance(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            try{
            amount = Double.valueOf(num);
            }catch(Exception e){
                System.exit(255);
            }
        } else if (cmd.hasOption('d')) {
            mode = "d";
            String num = ArgumentParser.getOptionValue(cmd, 'd', "");
            if (!Validator.validateBalance(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            try{
            amount = Double.valueOf(num);
            }catch(Exception e){
                System.exit(255);
            }
        } else if (cmd.hasOption('w')) {
            mode = "w";
            String num = ArgumentParser.getOptionValue(cmd, 'w', "");
            if (!Validator.validateBalance(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            try{
            amount = Double.valueOf(num);
            }catch(Exception e){
                System.exit(255);
            }
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
        }

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

        secureRandom = new SecureRandom();
        secureRandom.nextInt();
        try {
          serverKeyStore = KeyStore.getInstance("JKS");
          serverKeyStore.load( new FileInputStream(authFile), passphrase.toCharArray() );
          clientKeyStore = KeyStore.getInstance("JKS");
          clientKeyStore.load( new FileInputStream(authFile), passphrase.toCharArray() );
          TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

          tmf.init( serverKeyStore);

          KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
          kmf.init( clientKeyStore, passphrase.toCharArray() );

          sslContext = SSLContext.getInstance("TLS");
          sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), secureRandom );
        } catch (GeneralSecurityException gse) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
            }
            System.exit(255);
        } catch (FileNotFoundException ex) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
            }
           System.exit(255);
        } catch (IOException ex) {
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

          serverSocket.setEnabledProtocols(protocol);
          serverSocket.setEnabledCipherSuites(suites);
          serverSocket.setSoTimeout(10000);
            
         
        
        JsonObject value = Json.createObjectBuilder().add("mode", mode)
                            .add("account", account).add("amount", amount)
                            .add("cardfile", cardFile).add("port", port)
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
                    pw.println(value.toString());
                    pw.flush();
                    
                    String response = null;
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
          System.exit(63);
        } catch (Exception e) {
            if(cardFileCreated){
                    File f = new File(cardFile);
                    if(f.exists())
                        System.err.println(f.delete());
            }
            System.exit(63);
        }

        executor.shutdownNow();
        System.exit(0);
    }
}
