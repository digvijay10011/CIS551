
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import javax.json.*;

class AtmClient {

    String cardFileContent = "";
    
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
        String authFile = getOptionValue(cmd, 's', "bank.auth");
        String ipAddress = getOptionValue(cmd, 'i', "127.0.0.1");
        int port = Integer.valueOf(getOptionValue(cmd, 'p', "3000"));
        String cardFile = getOptionValue(cmd, 'c', account + ".card");
        String mode = "";
        double amount = 0;
        if (cmd.hasOption('n')) {
            mode = "n";
            amount = Double.valueOf(cmd.getOptionValue('n'));
        } else if (cmd.hasOption('d')) {
            mode = "d";
            amount = Double.valueOf(cmd.getOptionValue('d'));
        } else if (cmd.hasOption('w')) {
            mode = "w";
            amount = Double.valueOf(cmd.getOptionValue('w'));
        } else if (cmd.hasOption('g')) {
            mode = "g";
        } else {
            printInvalidArgs(options);
        }

        System.out.println(account);
        System.out.println(authFile);
        System.out.println(ipAddress);
        System.out.println(port);
        System.out.println(cardFile);
        System.out.println(mode);
        System.out.println(amount);
        

        JsonObject value = Json.createObjectBuilder().add("mode", mode).add("account", account).add("amount", amount).add("cardfile", cardFile).add("port", port).add("IPaddress", authFile).add("authFile", authFile).build();

        Socket serverSocket = null;
        PrintWriter pw = null;
        BufferedReader br; 
        
        try {
            serverSocket = new Socket(ipAddress, port);
            pw = new PrintWriter(serverSocket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            
            //passing the json object as string
            pw.println(value.toString());
            pw.flush();
            
            //getting response from the server. If in the response, "error" = true, then the transaction failed due to wrong inputs.
            String response = null;
            while((response=br.readLine()) == null); //waiting for server's respone
            
            JsonReader jsonReader = Json.createReader(new StringReader(response));
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
            
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }
}
