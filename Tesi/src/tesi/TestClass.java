/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tesi;

import tesi.riconoscimenti.RemoteDevice;
import tesi.server.Server;
import tesi.userclient.Client;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stefano
 */
public class TestClass { // Questa classe lancia i tre servizi necessari per lo svolgimento dell'esame

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    
    static Server srv;
    static RemoteDevice remd;
    static Client clnt;
    
    public static void main(String[] args){
        TestClass.srv=new Server();
        TestClass.remd=new RemoteDevice();
        remd.setId("1");
        remd.setName("Limina 1");
        remd.setPosition(16.188085, 38.391084);
        
        try {
            srv.start();
            System.out.println("Server avviato");
        } catch (IOException ex) {
            System.out.println("Impossibile avviare il server.");
            Logger.getLogger(TestClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            remd.start();
            System.out.println("Dispositivo remoto avviato");
        } catch (IOException ex) {
            Logger.getLogger(TestClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        TestClass.clnt=new Client();
        clnt.setVisible(true);
        
        
    }
    
}
