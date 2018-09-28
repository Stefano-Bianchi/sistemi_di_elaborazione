/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.server;

import com.sun.net.httpserver.Headers;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.commons.fileupload.FileItem;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/upload", new MyHandlerDevices()); //     Endpoint utilizzato dai dispositivi remoti. RaspBerry Pi nella prima veriosne
        server.createContext("/user", new MyHandlerUsers()); //     Endpoint utilizzato dai dispositivi remoti. RaspBerry Pi nella prima veriosne
        server.setExecutor(null); // creates a default executor
        server.start();
    }
    
    public static Connection connect() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:data.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            
            //System.out.println("Connection to SQLite has been established.");
            String query="CREATE TABLE IF NOT EXISTS `ricezioni` (\n" +
                    "	`id`	INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "	`warning`	TEXT,\n" +
                    "	`file`	TEXT,\n" +
                    "	`time`	TEXT,\n" +
                    "	`new`	NUMERIC,\n" +
                    "	`json`	TEXT\n" +
                    ");"; // Se non esiste il database, lo creo e genero la struttra
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.executeUpdate();
            
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } 
        return conn;
    }
    
    /*
    * Classe che implementa l'handler per gestire le operazioni delle board remote
    * Porzioni di codice prese da :
    * https://stackoverflow.com/questions/33732110/file-upload-using-httphandler
    */
    static class MyHandlerDevices implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
//            for (Iterator<Entry<String, List<String>>> it = t.getRequestHeaders().entrySet().iterator(); it.hasNext();) {
//                Entry<String, List<String>> header = it.next();
//                System.out.println(header.getKey() + ": " + header.getValue().get(0)); // solo per debug
//            }
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
                    JSONObject json=null;
                    String fileName="";
                    for(FileItem fi : result) {
                        switch(fi.getFieldName()){
                            case ("json"):
                                BufferedReader bri=new BufferedReader(new InputStreamReader(fi.getInputStream()));
                                JSONParser parser = new JSONParser();
                                json = (JSONObject) parser.parse(bri.readLine());         
                                break;
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
                                System.out.println("Formato imprevisto");
                                break;
                        }
                        if (!(fileName.compareTo("")==0||json==null)){ // se sono stati valorizzati, quindi ho ricevuto correttamente i dati
                            inserisciDb(json); // inserisco nel db locale
                        }
                    }
                    out.put("status",true);
                    t.sendResponseHeaders(200, 0);
                    os.write(out.toJSONString().getBytes());
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(404, 0);
                    os.write(out.toJSONString().getBytes());
                    os.close();
                } 


        }

        private void inserisciDb(JSONObject json) throws SQLException {
            String fileName="";
            String warningLevel="";
            String time="";
            if (json.containsKey("riconoscimento")) {
                
                JSONObject tmp= (JSONObject) json.get("riconoscimento");
                if (tmp.containsKey("filename")) {
                    fileName=(String) tmp.get("filename");
                }
                if (tmp.containsKey("warning-level")) {
                    warningLevel=(String) tmp.get("warning-level");
                }
                if (tmp.containsKey("time")) {
                    time=(String) tmp.get("time");
                }
            }
            
            
            Connection conn=connect();
            if (conn!=null){
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO ricezioni(\n"+
                        "`warning`,`file`,`time`,`new`,`json`) \n"+
                        "VALUES (?,?,?,?,?)");
                stmt.setString(1,warningLevel);
                stmt.setString(2,fileName);
                stmt.setString(3,time);
                stmt.setInt(4, 1);
                stmt.setString(5, json.toString());
                stmt.executeUpdate();
                conn.close();
            }
            else {
                throw new SQLException("Impossibile inserire");
            }

        }
    }
    
    
    /*
    * Classe che implementa l'handler per gestire le operazioni utente
    * Porzioni di codice prese da :
    * https://stackoverflow.com/questions/33732110/file-upload-using-httphandler
    */
    static class MyHandlerUsers implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            HashMap<String, Object> params = new HashMap();
            parseQuery(t.getRequestURI().getQuery(),params);

            OutputStream os = t.getResponseBody();
            JSONObject out = new JSONObject();
            out.put("status", false);
            try {
                t.getResponseHeaders().add("Content-type", "text/plain");
                String command=(String) params.get("command");
                int id=-1; // un valore non valido, ma ci fa comodo così. Se nell'URL id fosse impostato -1 verrebbe sovrascritto
                try {
                    id=Integer.parseInt((String) params.get("id"));
                } catch(Exception ex){
                    ; // non mi interessa fare nulla
                }

                /*
                * dopo aver acquisito il comando da eseguire
                * e l'eventuale paramentro
                * si esegue il metodo opportuno
                * i metodi restituiranno sempre un JSON nel formato
                * {"
                    id":id,
                    "results":[
                        ... array con i dati della decodifica
                    ]
                }
                */

                if (command!=null){

                    switch(command){ 
                        case "update": // imposta lo stato di id=... a letto
                            out=updateDB(id);
                            break;
                        case "getFile": // chiede il file associato a id=...
                            out=recuperaFile(id);
                            break;
                        case "listNew": // chiede al db i soli contenuti con flag new a 1
                        
                        case "listAll": // chiede al db tutti i contenuti
                        
                        case "read": // chiede al db il contentuo con id=...
                            out=interrogaDB(command,id);
                            break;
                        
                        default: // restituiamo un errore
                            break;
                    }
                } else {
                    throw new Exception("Comando non valido"); // per inviare all'altro capo delal connessione lo stato d'errore,
                                                               // sollevo un eccezione che verrà gestita nel catch
                                                               // così riutilizziamo la generazione dello stato 404 e del KO come messaggio di stato
                }
                t.sendResponseHeaders(200, 0);
                os.write(out.toJSONString().getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                t.sendResponseHeaders(404, 0);
                os.write(out.toJSONString().getBytes());
                os.close();
            } 


        }
        
        //  https://www.codeproject.com/Tips/1040097/Create-simple-http-server-in-Java
        public static void parseQuery(String query, Map<String, 
	Object> parameters) throws UnsupportedEncodingException {

        if (query != null) {
                String pairs[] = query.split("[&]");
                for (String pair : pairs) {
                        String param[] = pair.split("[=]");
                        String key = null;
                        String value = null;
                        if (param.length > 0) {
                        key = URLDecoder.decode(param[0], 
                              System.getProperty("file.encoding"));
                        }

                        if (param.length > 1) {
                                 value = URLDecoder.decode(param[1], 
                                 System.getProperty("file.encoding"));
                        }

                        if (parameters.containsKey(key)) {
                                 Object obj = parameters.get(key);
                                 if (obj instanceof List<?>) {
                                          List<String> values = (List<String>) obj;
                                          values.add(value);

                                 } else if (obj instanceof String) {
                                          List<String> values = new ArrayList<String>();
                                          values.add((String) obj);
                                          values.add(value);
                                          parameters.put(key, values);

                                 }
                        } else {
                                 parameters.put(key, value);
                        }
               }
         }
}
        
//        static Map<String, String> getParameters(HttpExchange httpExchange) throws IOException {
//            Map<String, String> parameters = new HashMap<>();
//            InputStream inputStream = httpExchange.getRequestBody();
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            byte[] buffer = new byte[2048];
//            int read = 0;
//            while ((read = inputStream.read(buffer)) != -1) {
//              byteArrayOutputStream.write(buffer, 0, read);
//            }
//            String[] keyValuePairs = byteArrayOutputStream.toString().split("&");
//            for (String keyValuePair : keyValuePairs) {
//              String[] keyValue = keyValuePair.split("=");
//              if (keyValue.length != 2) {
//                continue;
//              }
//              parameters.put(keyValue[0], keyValue[1]);
//            }
//            return parameters;
//          }

        
        /*
        * usiamo https://github.com/fangyidong/json-simple
        * come consigliato da http://www.java67.com/2016/10/3-ways-to-convert-string-to-json-object-in-java.html
        */

        private JSONObject interrogaDB(String command, int id) throws SQLException, ParseException {
            JSONObject out=new JSONObject();
            String query="SELECT * FROM ricezioni"; // seleziona tutti i contenuti
            switch(command){ 
                case "listNew": // chiede al db i soli contenuti con flag new a 1
                    query+=" WHERE `new`= 1"; // aggiunge la clausola WHERE che imposta un filtro sul valore della colonna new. 1 vuol dire che non è stato ancora marcato come letto
                    break;
                case "read":
                    query+=" WHERE `id`= ?"; // aggiunge la clausola WHERE che imposta un filtro sul valore della colonna id
                    break;
                case "listAll": // chiede al db tutti i contenuti
                    break;
            }
            query+=" ORDER BY `time` ASC, `warning` DESC"; // ordina i risultati dal più vecchio ed in base al walore di WARNING
            Connection conn=connect();
            PreparedStatement stmt=conn.prepareStatement(query);
            if (id!=-1){
                stmt.setInt(1, id);
            }
            
            ResultSet rs=stmt.executeQuery(); // esegue la query e riceve i risultati
            ArrayList risultati= new ArrayList();
            while (rs.next()){ // scorre i risultati
                JSONObject tmp=new JSONObject(); // oggetto temporaneao dove memorizzare i risultati
                tmp.put("id", rs.getInt("id"));
                tmp.put("warning", rs.getString("warning"));
                tmp.put("file", rs.getString("file"));
                tmp.put("time", rs.getString("time"));
                tmp.put("new", rs.getInt("new"));
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(rs.getString("json"));
                tmp.put("data", json);
                risultati.add(tmp); // accoda il risultato
            }
            out.put("status", true); // imposto true per confermare che tutto è andato bebe
            out.put("results", risultati);
            return out;
        }
        
        /*
         *  Quando l'utente marca come letta una notivica
         *  Si imposta a a 0 il flag new
         */ 

        private JSONObject updateDB(int id) throws SQLException {
            Connection conn=connect();
            JSONObject out=new JSONObject();
            out.put("status", false);
            if (conn!=null){
                PreparedStatement stmt = conn.prepareStatement("UPDATE  ricezioni SET `new`= 0 WHERE id=?");
                stmt.setInt(1, id);
                stmt.executeUpdate();
                conn.close();
                out.put("status", true);
            }
            else {
                throw new SQLException("Impossibile aggiornare");
            }
            return out;
        }

        private JSONObject recuperaFile(int id) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
    
}