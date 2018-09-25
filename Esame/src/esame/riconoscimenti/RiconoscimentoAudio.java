package esame.riconoscimenti;


import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.json.simple.JSONObject;

public class RiconoscimentoAudio {       

    String filename;
    
    public RiconoscimentoAudio(String filename){
        this.filename=filename;
    }
    
    public JSONObject decode() throws Exception {

        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
// questo è un commento di prova per Manuela
//        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
        // Start recognition process pruning previously cached data.
//        recognizer.startRecognition(true);
//        while (true){
//            SpeechResult result = recognizer.getResult();
//            System.out.format("Hypothesis: %s\n", result.getHypothesis());}

        // Pause recognition process. It can be resumed then with startRecognition(false).
//        recognizer.stopRecognition();
        
        
	StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
	InputStream stream = new FileInputStream(new File(filename));

        recognizer.startRecognition(stream);
	SpeechResult result;
        ArrayList<String> words=new ArrayList();
        while ((result = recognizer.getResult()) != null) {
	    //System.out.format("Hypothesis: %s\n", result.getHypothesis());
            words.add(result.getHypothesis());
	}
	recognizer.stopRecognition();
        JSONObject obj = new JSONObject();
        
        obj.put("filename",filename);
        obj.put("time",java.time.Instant.now().toString());
        obj.put("keywords", words);
        return obj;
    }
    
}