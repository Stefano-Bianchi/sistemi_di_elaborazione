/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.riconoscimenti;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;

/**
 *
 * @author Stefano
 */
public class RemoteDevice {
    String id;
    String name;
    JSONObject position;
    static LinkedList<JSONObject> outQueue;
    private Uploader upl;
    
    public RemoteDevice(){
        id=""; // un numero univoco del dispositivo
        name=""; // giusto per identificare più agevolmente il dispositivo
        position=new JSONObject();
        outQueue=new LinkedList();
        upl=new Uploader();
        upl.setToSend(outQueue);
        upl.setUrl("http://localhost:8000/upload"); // questo valore andrebbe inserito in un file di configurazione, st
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
        pos[1]=lat; 
        position.put("coordinates", Arrays.toString(pos));
    }
    
    public LinkedList<JSONObject> getOutQueue(){
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
    
    public static void main(String[] args){
        RemoteDevice remd=new RemoteDevice();
        remd.setId("1");
        remd.setName("Limina 1");
        remd.setPosition(16.188085, 38.391084);

        try {
            remd.start();
        } catch (IOException ex) {
            Logger.getLogger(RemoteDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void start() throws IOException{
        HttpServer server = HttpServer.create(new InetSocketAddress(8100), 0);
        server.createContext("/avaible", new MyHandlerDevices()); //     Endpoint utilizzato dal monitor per inviare un file appena presente
        server.setExecutor(null); // creates a default executor
        server.start();
        upl.start();
    }
    
    class MyHandlerDevices implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
                DiskFileItemFactory d = new DiskFileItemFactory();      
                OutputStream os = t.getResponseBody();               
                JSONObject out = new JSONObject();
                out.put("status", false);    
                try {
                    ServletFileUpload up = new ServletFileUpload(d);
                    List<FileItem> result = up.parseRequest(new RequestContext() {

                        @Override
                        public String getCharacterEncoding() {
                            return "UTF-8";
                        }

                        @Override
                        public int getContentLength() {
                            return 0; //tested to work with 0 as return
                        }

                        @Override
                        public String getContentType() {
                            return t.getRequestHeaders().getFirst("Content-type");
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return t.getRequestBody();
                        }

                    });
                    t.getResponseHeaders().add("Content-type", "text/plain");
                    String fileName="";
                    for(FileItem fi : result) {
                        switch(fi.getFieldName()){
                            case ("audio"):
                                byte[] buffer = new byte[4096];
                                InputStream is=fi.getInputStream();
                                fileName=fi.getName();
                                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
                                while(-1 != is.read(buffer)) {
                                    outputStream.write(buffer);
                                }
                                break;
                            default:
                                System.out.println("Comando imprevisto");
                                break;
                        }
                        JSONObject elaborato=elabora(fileName);
                        synchronized (this){ // acquisisco il lock sulla risorsa condivisa tra RemoteDevice e Uploader
                            outQueue.add(elaborato); // per come consigliato su https://winterbe.com/posts/2015/04/30/java8-concurrency-tutorial-synchronized-locks-examples/
                        }
                    }
                    JSONObject response=new JSONObject();
                    response.put("status",true);
                    t.sendResponseHeaders(200, 0);
                    os.write(response.toJSONString().getBytes());
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(404, 0);
                    os.write(out.toJSONString().getBytes());
                    os.close();
                } 
        }

        private JSONObject elabora(String fileName) throws Exception {
            JSONObject obj = new JSONObject();
            RiconoscimentoAudio ra;
            ra=new RiconoscimentoAudio(fileName);
            obj.put("deviceId", getId());
            obj.put("deviceName", getName());
            obj.put("devicePosition", getPosition());
            obj.put("riconoscimento",ra.decode());
            return obj;
        }   
    }
}
