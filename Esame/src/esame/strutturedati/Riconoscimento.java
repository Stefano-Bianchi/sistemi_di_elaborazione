/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.strutturedati;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Stefano
 */
public class Riconoscimento {
    long id;
    String warning;
    String fileName;
    Double lat;
    Double lng;
    ArrayList<String> keywords;
    String time;
    String deviceId;
    String deviceName;
    
    public Riconoscimento(JSONObject json){
        this.keywords=new ArrayList();
        long id=(long) json.get("id"); // per quanto noi memorizziamo interi come id, lui nel cast lo gestisce come long
        JSONObject data = (JSONObject) json.get("data");
        JSONObject riconoscimento=(JSONObject) data.get("riconoscimento");
        JSONObject devicePosition=(JSONObject) data.get("devicePosition");
        this.deviceName=(String) data.get("deviceName");
        this.deviceId=(String) data.get("deviceId");
        this.time=(String) riconoscimento.get("time");
        String[] coordinates=((String) devicePosition.get("coordinates")).split(",");
        this.lat=Double.valueOf(coordinates[1].substring(0, coordinates[1].length()-1));
        this.lng=Double.valueOf(coordinates[0].substring(1, coordinates[1].length()-1));
        this.fileName=(String) riconoscimento.get("fileName");
        this.warning=(String) riconoscimento.get("warning-level");
        JSONArray tmpKeywords= (JSONArray) riconoscimento.get("keywords");
        Iterator iter=tmpKeywords.iterator();
        while (iter.hasNext()){
            String tmp= (String) iter.next();
            if (tmp!=null||!tmp.contentEquals("")){
                keywords.add(tmp);
            }
        }
    }
    
    public String toString(){
        return "("+this.warning+") "+this.deviceName+" "+this.time;
    }
    
    public String printKeyWords(){ // restituisce una stringa formattata di parole chiave
        String out="";
        for (String tmp : this.keywords){
            out+="["+tmp+"]";
        }
        return out;
    }
    
}
