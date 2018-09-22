package esame.riconoscimenti;


import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import static javafx.css.StyleOrigin.USER_AGENT;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONObject;

public class RiconoscimentoAudio {       

    public static void main(String[] args) throws Exception {

        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

//        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
        // Start recognition process pruning previously cached data.
//        recognizer.startRecognition(true);
//        while (true){
//            SpeechResult result = recognizer.getResult();
//            System.out.format("Hypothesis: %s\n", result.getHypothesis());}

        // Pause recognition process. It can be resumed then with startRecognition(false).
//        recognizer.stopRecognition();
        
        
	StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
	InputStream stream = new FileInputStream(new File("./test.wav"));

        recognizer.startRecognition(stream);
	SpeechResult result;
        ArrayList<String> words=new ArrayList();
        while ((result = recognizer.getResult()) != null) {
	    //System.out.format("Hypothesis: %s\n", result.getHypothesis());
            words.add(result.getHypothesis());
	}
	recognizer.stopRecognition();
        JSONObject obj = new JSONObject();
        JSONObject position = new JSONObject();
        
        position.put("type", "Point"); // http://geojson.org/geojson-spec.html
        position.put("coordinates", Arrays.toString((new int[]{10,12})));

        obj.put("deviceId", 1);
        obj.put("deviceName", "Limina 1");
        obj.put("devicePosition", position);
        obj.put("filename","test.wav");
        obj.put("keywords", words);
        System.out.println(obj);
    }
    
    private void sendPost() throws Exception {

		String url = "http://deitest.ivopugliese.it/test.php";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		//print result
		System.out.println(response.toString());

	}
}