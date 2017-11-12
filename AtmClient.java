import java.io.BufferedReader;
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

    private static void generateOption(Options options, String opt,
                                    boolean hasArg, String description,
                                    boolean isRequired) {
        Option input = new Option(opt, hasArg, description);
        input.setRequired(isRequired);
        options.addOption(input);
    }

    private static String getOptionValue(CommandLine cmd, char opt, String def){
        if (cmd.hasOption(opt)) {
            return cmd.getOptionValue(opt);
        }
        return def;
    }

    private static void printInvalidArgs(Options options) {
        (new HelpFormatter()).printHelp("atm", options);
        System.exit(255);
        return;
    }
    
    private static boolean createCardFile(String cardFile){
            Random random = new SecureRandom();
            char buf[] = new char[150];
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
        
        generateOption(options, "a", true, "account", true);
        generateOption(options, "s", true, "auth-file", false);
        generateOption(options, "i", true, "ip-address", false);
        generateOption(options, "p", true, "port", false);
        generateOption(options, "c", true, "card-file", false);
        // TODO: Remove this before submitting
        generateOption(options, "v", false, "verbose", false);

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
            System.out.println(e.getMessage());
            printInvalidArgs(options);
        } 

        String account = cmd.getOptionValue('a');
        if (!Validator.validateAccountName(account)) {
            printInvalidArgs(options);
        }
        String authFile = getOptionValue(cmd, 's', "bank.auth");
        if (!Validator.validateFileName(authFile)) {
            printInvalidArgs(options);
        }
        String ipAddress = getOptionValue(cmd, 'i', "127.0.0.1");
        if (!Validator.validateIPAddress(ipAddress)) {
            printInvalidArgs(options);
        }
        int port = Integer.valueOf(getOptionValue(cmd, 'p', "3000"));
        if (port < 1024 || port > 65535) {
            printInvalidArgs(options);
        }
        String cardFile = getOptionValue(cmd, 'c', account + ".card");
        if (!Validator.validateFileName(cardFile)) {
            printInvalidArgs(options);
        }
        String mode = "";
        double amount = 0;
        boolean logging = cmd.hasOption('v');
        if (cmd.hasOption('n')) {
            mode = "n";
            String num = cmd.getOptionValue('n');
            if (!Validator.validateNumber(num)) {
                printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('d')) {
            mode = "d";
            String num = cmd.getOptionValue('d');
            if (!Validator.validateNumber(num)) {
                printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('w')) {
            mode = "w";
            String num = cmd.getOptionValue('w');
            if (!Validator.validateNumber(num)) {
                printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('g')) {
            mode = "g";
        } else {
            printInvalidArgs(options);
        }

        if (logging) {
            System.out.println(account);
            System.out.println(authFile);
            System.out.println(ipAddress);
            System.out.println(port);
            System.out.println(cardFile);
            System.out.println(mode);
            System.out.println(amount);
        }
        
        if(mode.equals("n"))
            createCardFile(cardFile); //shouldn't return if cardfile already exists
        //System.out.print("\n>--"+readCardFile(cardFile)+"--\n");
        String cardFileContent = readCardFile(cardFile);
        
        
        JsonObject value = Json.createObjectBuilder().add("mode", mode)
                            .add("account", account).add("amount", amount)
                            .add("cardfile", cardFile).add("port", port) //no need to send cardfile name
                            .add("IPaddress", authFile)
                            .add("authFile", authFile)
                            .add("cardFileContent", cardFileContent)
                            .build();

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
          //TODO
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
                System.exit(255);
            } else {
                //responseObject;
                System.out.println(responseObject.toString());
            }

            pw.close();
            br.close();
            serverSocket.close();

    
        } catch (TimeoutException e) {
            future.cancel(true);
            System.exit(63);
        } catch (ConnectException e) {
            System.exit(63);
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
        System.exit(0);
    }
}
