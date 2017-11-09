
import javax.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class BankServer {

    
    static HashMap<String, Account> allAccounts = null;
    static HashMap<String, String> cardFiles = null;

    public static void main(String[] args) {

        allAccounts = new HashMap<String, Account>();
        cardFiles = new HashMap<String, String>();   //we will need a separate HashMap for cardFiles !
                                                    // because we need to check if cardFileName is duplicated or not.

        char mode ='n';
        String requestedAccount = "";
        double requestedAmount = 0.0;
        String requestedCardFileName ="";
        int port = 3000;
        String authFile = "";
        String cardFileContent = "";

        ServerSocket server = null;
        Socket clientSocket = null;
        
        try {
            server = new ServerSocket(port);
        } catch (IOException ex) {
            //handle it !!!!!
        }
        
        while (true) {
            try {
                
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
                //cardFileContent isn't being sent yet

                Account account = null;

                // response that will be sent to the atm so that atm can print the required message or exit if theres error
                JsonObjectBuilder jsonBuilder = Json.createObjectBuilder().add("account", requestedAccount);
                JsonObject response = null;

                boolean error = false;
                
                switch (mode) {

                    case 'n':   //new account

                        if (allAccounts.get(requestedAccount) != null || requestedAmount < 10 || cardFiles.get(requestedCardFileName) != null) {
                            error = true;
                            break;
                        }

                        account = new Account();
                        account.balance = requestedAmount;
                        account.cardFileName = requestedCardFileName;

                        allAccounts.put(requestedAccount, account);
                        cardFiles.put(requestedCardFileName, cardFileContent);
                        
                        jsonBuilder.add("initial_balance", account.balance);

                        break;

                    case 'w':    //withdraw

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if (account.cardFileName.compareTo(requestedCardFileName) != 0 || !checkCardFiles(cardFiles.get(requestedCardFileName), cardFileContent) || requestedAmount <= 0) {
                            error = true;
                            break;
                        }

                        account.balance -= requestedAmount;

                        if (account.balance < 0) {
                            error = true;
                        }
                        
                        jsonBuilder.add("withdraw", account.balance);

                        break;

                    case 'd':  //deposit

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if (account.cardFileName.compareTo(requestedCardFileName) != 0 || !checkCardFiles(cardFiles.get(requestedCardFileName), cardFileContent) || requestedAmount <= 0) {
                            error = true;
                            break;
                        }

                        account.balance += requestedAmount;
                        //what about max balance ??
                        jsonBuilder.add("deposit", account.balance);

                        break;

                    case 'g':   //get the balance

                        account = allAccounts.get(requestedAccount);

                        if (account == null) {
                            error = true;
                            break;
                        }

                        if (account.cardFileName.compareTo(requestedCardFileName) != 0 || !checkCardFiles(cardFiles.get(requestedCardFileName), cardFileContent) || requestedAmount <= 0) {
                            error = true;
                        }
                        
                        jsonBuilder.add("balance", account.balance);

                        break;

                }
                
                
                if(error){
                    response = jsonBuilder.add("error", true).build();
                }
                else{
                    response = jsonBuilder.add("error", false).build();
                    
                }
                //sending response to atm
                pw.println(response.toString());
                pw.flush();
                System.out.println(response.toString());

                br.close();
                pw.close();
                clientSocket.close();

            } catch (Exception e) {
                System.out.println("error!");
                //everytime a client disconnects, exception will be thrown
            }
        }

    }

    static boolean checkCardFiles(String actual, String current) {

        //code to check the cardfiles
        // return true if cardfiles match
        return true;
    }

}

class Account {

    double balance;
    String cardFileName;

}
