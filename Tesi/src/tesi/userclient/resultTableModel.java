/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tesi.userclient;

import tesi.strutturedati.Riconoscimento;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author elsoft
 */
public class resultTableModel extends javax.swing.table.DefaultTableModel {
    
    Object[][] tableData;
    String[] header;

    public resultTableModel(JSONArray results) {
        header = new String[] {"Warning", "Time", "New"};
        if (results.size()>0){
            tableData = new Object[results.size()][3];
            for (int i=0;i<results.size();i++){
                   Riconoscimento ric=new Riconoscimento((JSONObject) results.get(i));
                   tableData[i][0]=ric.getWarning();
                   tableData[i][1]=ric;
                   tableData[i][2]=(ric.getNew()==1)?"TRUE":"FALSE";
                };



            super.setDataVector(tableData,header);
        }
    }

  
    
}
