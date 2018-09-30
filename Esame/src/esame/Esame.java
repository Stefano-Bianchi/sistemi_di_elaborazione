/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esame;

import esame.riconoscimenti.RemoteDevice;
import esame.server.Server;
import esame.userclient.Client;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stefano
 */
public class Esame { // Questa classe lancia i tre servizi necessari per lo svolgimento dell'esame

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args){
        Server srv=new Server();
        RemoteDevice remd=new RemoteDevice();
        remd.setId("1");
        remd.setName("Limina 1");
        remd.setPosition(16.188085, 38.391084);
        
        try {
            srv.start();
            System.out.println("Server avviato");
        } catch (IOException ex) {
            System.out.println("Impossibile avviare il server.");
            Logger.getLogger(Esame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            remd.start();
            System.out.println("Dispositivo remoto avviato");
        } catch (IOException ex) {
            Logger.getLogger(Esame.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Client clnt=new Client();
        clnt.setVisible(true);
        
        
    }
    
}
