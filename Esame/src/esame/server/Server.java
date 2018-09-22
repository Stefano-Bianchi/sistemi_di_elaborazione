/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/devices", new MyHandlerDevices()); //     Endpoint utilizzato dai dispositivi remoti. RaspBerry Pi nella prima veriosne
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandlerDevices implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Ciao";
            
            Map<String,String> parametri=getParameters(t);
            if (parametri.get("nome").compareTo(null)==0) {
                response=response+parametri.get("nome");
            }
            if (parametri.get("cognome").compareTo(null)==0){
                response=response+parametri.get("cognome");
            }
            
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        
        static Map<String, String> getParameters(HttpExchange httpExchange) {
            Map<String, String> parameters = new HashMap<>();
           String uri=httpExchange.getRequestURI().toString();
           String[] soloiparametri=uri.split("\\?");
           if (soloiparametri.length>1){
                String[] keyValuePairs = soloiparametri[1].split("&");
                for (String keyValuePair : keyValuePairs) {
                  String[] keyValue = keyValuePair.split("=");
                  if (keyValue.length != 2) {
                    continue;
                  }
                  parameters.put(keyValue[0], keyValue[1]);
                }
           }
            return parameters;
          }
    }

}
