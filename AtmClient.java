
import java.io.BufferedReader;
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

class AtmClient {
    public static void main(String[] args) throws IOException {
        Options options = new Options();
        
        ArgumentParser.generateOption(options, "a", true, "account", true);
        ArgumentParser.generateOption(options, "s", true, "auth-file", false);
        ArgumentParser.generateOption(options, "i", true, "ip-address", false);
        ArgumentParser.generateOption(options, "p", true, "port", false);
        ArgumentParser.generateOption(options, "c", true, "card-file", false);
        // TODO: Remove this before submitting
        ArgumentParser.generateOption(options, "v", false, "verbose", false);

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
            // System.out.println(e.getMessage());
            ArgumentParser.printInvalidArgs(options);
        } 

        String account = cmd.getOptionValue('a');
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
        boolean logging = cmd.hasOption('v');
        if (cmd.hasOption('n')) {
            mode = "n";
            String num = cmd.getOptionValue('n');
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('d')) {
            mode = "d";
            String num = cmd.getOptionValue('d');
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('w')) {
            mode = "w";
            String num = cmd.getOptionValue('w');
            if (!Validator.validateNumber(num)) {
                ArgumentParser.printInvalidArgs(options);
            }
            amount = Double.valueOf(num);
        } else if (cmd.hasOption('g')) {
            mode = "g";
        } else {
            ArgumentParser.printInvalidArgs(options);
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

        JsonObject value = Json.createObjectBuilder().add("mode", mode)
                            .add("account", account).add("amount", amount)
                            .add("cardfile", cardFile).add("port", port)
                            .add("IPaddress", authFile)
                            .add("authFile", authFile).build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = null;
        try {
            final Socket serverSocket = new Socket(ipAddress, port);
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
