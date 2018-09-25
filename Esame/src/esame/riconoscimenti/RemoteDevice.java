/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.riconoscimenti;

import java.util.ArrayList;
import java.util.Arrays;
import org.json.simple.JSONObject;

/**
 *
 * @author elsoft
 */
public class RemoteDevice {
    String id;
    String name;
    JSONObject position;
    ArrayList<JSONObject> outQueue;
    
    public RemoteDevice(){
        id="";
        name="";
        position=new JSONObject();
        outQueue=new ArrayList();
    }
    
    public void setId(String id){
        this.id=id;
    }
    
    public void setName(String name){
        this.name=name;
    }
    
    public void setPosition(double lat, double lng){
        position.put("type", "Point"); // http://geojson.org/geojson-spec.html
        double[] pos= new double[2];
        pos[0]=lng; // nel formato GeoJSON sono memorizzate invertite queste informazioni
        pos[1]=lat; // ma meglio verificare, potrei ricordare male
        position.put("coordinates", Arrays.toString(pos));
    }
    
    public ArrayList<JSONObject> getOutQueue(){
        return this.outQueue;
    }
    
    public String getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public JSONObject getPosition(){
        return this.position;
    }
    
    public static void main(String[] args) throws Exception{
        RemoteDevice rm= new RemoteDevice();
        rm.setId("DEV1");
        rm.setName("Limina 1");
        rm.setPosition(38.391084, 16.188085);
        
        Uploader up=new Uploader();
        up.setUrl("localhost:8000");
        up.setToSend(rm.getOutQueue());

        // all'interno di while true va fatto un check sulla disponibilità di un nuovo file da processare
        // se il file è presente se ne lancia la decodifica (questo in multithreading)
        // alla ricezione dell'output si potrebbe verificare se esistono nella decodifica delle parole chiave
        // che riconosciamo come una emergenza
        // se sì allora si imposta un flag
        // obj.put("emergency":"red");
        // altrimenti lo si imposta come
        // obj.put("emergency":"green");
        // questo permette di verifcare la bontà delle decodifiche e di non scartare messaggi che potrebbero
        // contenere informazioni utili che il sistema non ha riconosciuto
        // a questo punto si fa un push dell'oggetto JSON creato nella coda outQueue
        // in parallelo si mantiene sempre
        //
//        while (true){
         
//        }

// per testare sul momento
// questo andrebbe messo nel while true ed eseguito per ciascun file trovato
// di fatto in produzione andrebbe creato un servizio di monitoraggio su una cartella
// cartella nella quale il dispositivo appena è disponibile una comunicazione in enrtata la registra e salva
// a questo punto identificando la presenza di un nuovo file se ne lancia la decodifica
        JSONObject obj = new JSONObject();
        RiconoscimentoAudio ra=new RiconoscimentoAudio("C:\\Users\\Stefano\\Documents\\Test_Tesi\\LOST.WAV"); // inserire il percorso assoluto al file di test
        obj.put("deviceId", rm.getId());
        obj.put("deviceName", rm.getName());
        obj.put("devicePosition", rm.getPosition());
        obj.put("riconoscimento",ra.decode());
        System.out.println(obj);
    }
}
