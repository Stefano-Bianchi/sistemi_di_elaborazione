/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.riconoscimenti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONObject;

/**
 *
 * @author Stefano
 */
public class Uploader extends Thread{
    
    String url; // L'URL del server
    ArrayList<JSONObject> toSend; // La coda di messaggi da inviare, struttra condivisa
    
    public void setUrl(String url){
        this.url=url;
    }
    
    public void setToSend(ArrayList toSend){
        this.toSend=toSend;
    }
    
    @Override
    public void run(){
        while (true){
            // facciamo il lock della risorsa JSON
            // se la risorsa non è vuota si chiama il metodo sendPost
            // se il codice di ritorno ci conferma che è stato ricevuto
            // rimuoviamo l'elemento dalla coda, altrimenti lo si lascia così
            // verrà ritrasmesso
        }
    }
    
    private int sendPost() throws Exception { // metodo da sistemare

		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		//print result
		System.out.println(response.toString());
                return responseCode;

	}
}
