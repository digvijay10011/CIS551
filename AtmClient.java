
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import javax.json.*;

class AtmClient {

    public static void main(String[] args) throws IOException {

        //somehow values from the command line arguments should br loaded
        //into the following variables which will then be passed to the bank.
        String mode = "n";
        String account = "bobby";
        double amount = 10;
        String cardfile = account + ".card";
        int port = 3000;
        String IPaddress = "127.0.0.1";
        String authFile = "bank.auth";

        JsonObject value = Json.createObjectBuilder().add("mode", mode).add("account", account).add("amount", amount).add("cardfile", cardfile).add("port", port).add("IPaddress", IPaddress).add("authFile", authFile).build();

        Socket serverSocket = null;
        PrintWriter pw = null;
        BufferedReader br; 
        
        try {
            serverSocket = new Socket(IPaddress, port);
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
