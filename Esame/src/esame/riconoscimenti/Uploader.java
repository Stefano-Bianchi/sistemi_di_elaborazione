/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.riconoscimenti;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

/**
 *
 * @author Stefano
 */
public class Uploader extends Thread{
    
    String url; // L'URL del server
    LinkedList<JSONObject> toSend; // La coda di messaggi da inviare, struttra condivisa
    private JSONObject json;
    private String fileName;
    
    public void setUrl(String url){
        this.url=url;
    }
    
    public void setToSend(LinkedList toSend){
        this.toSend=toSend;
    }
    
    @Override
    public void run(){
        while (true){
            synchronized (this){
                
                if (this.toSend.size()>0){
                    System.out.println("Uploader: contenuti nella coda, eseguo l'invio");
                    json=toSend.peek(); // prende il primo elemento della coda
                    JSONObject riconoscimento=(JSONObject) json.get("riconoscimento");
                    fileName=(String) riconoscimento.get("filename");
                    System.out.println(json.toJSONString());
                    JSONObject ret=sendPost();
                    if ((boolean)ret.get("status")){
                        System.out.println("Uploader: contenuto inviato");
                        toSend.pop(); // rimuovo il primo elemento perché il trasferimento è andato a buon file
                    } else {
                        System.out.println("Uploader: contenuto non inviato, nuovo tentativo in corso");
                        try {
                            TimeUnit.SECONDS.sleep(5); // attendo 5 secondi per non sovracaricare il server
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    System.out.println("Uploader: non ci sono dati da inviare");
                }
            }
            //System.out.println("Attendo 5 secondi");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    /*
    * questa parte è presa da https://stackoverflow.com/questions/2469451/upload-files-from-java-client-to-a-http-server
    */
    private JSONObject sendPost() {
        JSONObject out=new JSONObject();
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("audio", new FileBody(new File(fileName)));
        //new File("json.json").new ByteArrayInputStream(json.toJSONString().getBytes()))
        entity.addPart("json",  new FileBody(null));
        
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);

        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        try {
            response = client.execute(request);
            System.out.println(response);
            out.put("status", true);
        } catch (IOException ex) {
            Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
            out.put("status", false);
        }
        return out;
    }
    
//    private JSONObject sendPOST() throws IOException {
//        URL obj = new URL(url);
//        JSONObject json=new JSONObject();
//        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//        con.setRequestMethod("POST");
//        con.setRequestProperty("User-Agent", "");
//
//        // For POST only - START
//        con.setDoOutput(true);
//        OutputStream os = con.getOutputStream();
//        os.write("".getBytes());
//        os.flush();
//        os.close();
//        // For POST only - END
//
//        int responseCode = con.getResponseCode();
//        System.out.println("POST Response Code :: " + responseCode);
//
//        if (responseCode == HttpURLConnection.HTTP_OK) { //success
//            BufferedReader in = new BufferedReader(new InputStreamReader(
//                            con.getInputStream()));
//            String inputLine;
//            StringBuffer response = new StringBuffer();
//
//            while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//            }
//            in.close();
//
//            // print result
//            System.out.println(response.toString());
//             json.put("status", true);
//        } else {
//            json.put("status", false);
//        }
//        return json;
//    }
}
