package tesi.riconoscimenti;


import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.simple.JSONObject;

public class RiconoscimentoAudio {       

    String filename;
    String[] hotWords; 
    
    public RiconoscimentoAudio(String filename){
        this.hotWords = new String[]{"help", "danger", "problem", "warning"};// un elenco di parole per riconoscere uno stato d'emergenza
        this.filename=filename;
    }
    
    public JSONObject decode() throws Exception {

        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
	InputStream stream = new FileInputStream(new File(filename));
        long t1=System.currentTimeMillis();

        recognizer.startRecognition(stream);
        long t2=System.currentTimeMillis();
	SpeechResult result;
        ArrayList<String> words=new ArrayList();
        while ((result = recognizer.getResult()) != null) {
	    String tmp=result.getHypothesis();
            words.add(tmp);
	}
        
	recognizer.stopRecognition();
        long t3=System.currentTimeMillis();
        System.out.println("T1: "+(t2-t1)+"\nT2: "+(t3-t1));
        JSONObject obj = new JSONObject();
        
        obj.put("filename",filename);
        obj.put("time",java.time.Instant.now().toString());
        obj.put("warning-level","green");
        for (String word: words){
            if (Arrays.asList(hotWords).contains(word)) { // scorrendo tutte le parole identificate, riconosco se è una di loro è una hotWord 
                obj.put("warning-level", "red");
            } 
        }
        obj.put("keywords", words);
        return obj;
    }
    
}