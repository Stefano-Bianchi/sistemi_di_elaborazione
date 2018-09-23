/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.fileupload.FileItem;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/upload", new MyHandlerDevices()); //     Endpoint utilizzato dai dispositivi remoti. RaspBerry Pi nella prima veriosne
        server.setExecutor(null); // creates a default executor
        server.start();
    }
    // https://stackoverflow.com/questions/33732110/file-upload-using-httphandler
    
    static class MyHandlerDevices implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            for (Iterator<Entry<String, List<String>>> it = t.getRequestHeaders().entrySet().iterator(); it.hasNext();) {
                Entry<String, List<String>> header = it.next();
                System.out.println(header.getKey() + ": " + header.getValue().get(0)); // solo per debug
            }
                DiskFileItemFactory d = new DiskFileItemFactory();      

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
                    t.sendResponseHeaders(200, 0);
                    OutputStream os = t.getResponseBody();               
                    for(FileItem fi : result) {
                        switch(fi.getFieldName()){
                            case ("json"):
                                BufferedReader bri=new BufferedReader(new InputStreamReader(fi.getInputStream()));
                                String json=bri.readLine();
                                System.out.println("Ricevuto: "+json);
                                // per il debug lo visualizziamo
                                // va quì inserita successivamente la
                                // porzione di codice che inserisce il file JSON nel db locale
                                break;
                            case ("audio"):
                                byte[] buffer = new byte[4096];
                                InputStream is=fi.getInputStream();
                                // quì fa uso del nomefile originario, andrebbe valutato un criterio
                                // diverso.
                                // ad esempio generare un nomefile temporaneo, così da salvare
                                // tutti i file in una unica cartella,
                                // memorizzare nel db oltre che il file json anche il file con
                                // cui si è salvato il file audio
                                // così da recuperarlo quando l'operatore lo richiede
                                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fi.getName()));
                                while(-1 != is.read(buffer)) {
                                    outputStream.write(buffer);
                                }
                                break;
                            default:
                                System.out.println("Formato imprevisto");
                                break;
                        }
                        //System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName()+" size="+fi.getSize());
                    }
                    os.write("OK\r\n".getBytes());
                    os.close();

                } catch (Exception e) {
                    e.printStackTrace();
                } 


        }
    }
}
