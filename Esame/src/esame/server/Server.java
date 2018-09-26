/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.server;

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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
    
    // https://stackoverflow.com/questions/33732110/file-upload-using-httphandler
    
    static class MyHandlerDevices implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
//            for (Iterator<Entry<String, List<String>>> it = t.getRequestHeaders().entrySet().iterator(); it.hasNext();) {
//                Entry<String, List<String>> header = it.next();
//                System.out.println(header.getKey() + ": " + header.getValue().get(0)); // solo per debug
//            }
                DiskFileItemFactory d = new DiskFileItemFactory();      
                OutputStream os = t.getResponseBody();               
                    
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
                        //System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName()+" size="+fi.getSize());
                    }
                    t.sendResponseHeaders(200, 0);
                    os.write("OK\r\n".getBytes());
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(404, 0);
                    os.write("KO\r\n".getBytes());
                    os.close();
                } 


        }

        private void inserisciDb(JSONObject json) throws SQLException {
            String fileName="";
            String warningLevel="";
            String time="";
            //System.out.println(json.keySet().toString());
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
            
            
            //System.out.println("Inserisco:"+fileName+" "+warningLevel+" "+time);
        }
    }
    
    
    
    static class MyHandlerUsers implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
//            for (Iterator<Entry<String, List<String>>> it = t.getRequestHeaders().entrySet().iterator(); it.hasNext();) {
//                Entry<String, List<String>> header = it.next();
//                System.out.println(header.getKey() + ": " + header.getValue().get(0)); // solo per debug
//            }
                DiskFileItemFactory d = new DiskFileItemFactory();      
                OutputStream os = t.getResponseBody();               
                    
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
                    String command="";
                    int id=-1;
                    for(FileItem fi : result) {
                        switch(fi.getFieldName()){
                            case ("command"):
                                command=fi.getName();
                                break;
                            case ("id"):
                                String tmp=fi.getName();
                                if (tmp!=null){
                                    id= Integer.valueOf(tmp);
                                }
                                break;
                            default:
                                System.out.println("Opzione imprevista");
                                break;
                        }
                    }
                    System.out.println("comando: " + command +" id: "+id);
                    t.sendResponseHeaders(200, 0);
                    os.write("OK\r\n".getBytes());
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    t.sendResponseHeaders(404, 0);
                    os.write("KO\r\n".getBytes());
                    os.close();
                } 


        }

        private void inserisciDb(JSONObject json) throws SQLException {
            String fileName="";
            String warningLevel="";
            String time="";
            //System.out.println(json.keySet().toString());
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
            
            
            //System.out.println("Inserisco:"+fileName+" "+warningLevel+" "+time);
        }
    }
    
}


