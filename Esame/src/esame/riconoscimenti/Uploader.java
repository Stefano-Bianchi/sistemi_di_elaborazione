/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.riconoscimenti;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
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
        out.put("status", false);
        File jsonFile = null;
        try {
            /*
             *  generiamo un file temporaneo 
             * https://stackoverflow.com/questions/26860167/java-safe-way-to-create-a-temp-file
             */
            jsonFile = File.createTempFile("json-",".json"); // andrebbe generato un fileName casuale per evitare 
            OutputStream outStream = new FileOutputStream(jsonFile);
            outStream.write(json.toJSONString().getBytes());
            outStream.flush();
            
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("audio", new FileBody(new File(fileName)));
            entity.addPart("json",  new FileBody(jsonFile));
            
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
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (jsonFile!=null) {
            jsonFile.delete();
        }
        return out;
    }
}
