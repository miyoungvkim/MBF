package creativeLab.samsung.mbf.mbf;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;

public class TextToSpeechTask implements TextToSpeech.OnInitListener {

    private final String TAG = TextToSpeechTask.class.getSimpleName();

    private TextToSpeech textToSpeech;
    private Locale locale;

    public TextToSpeechTask(Context context, Locale locale, UtteranceProgressListener ttsUtteranceProgressListener) {
        this.locale = locale;
        textToSpeech = new TextToSpeech(context, this);
        textToSpeech.setSpeechRate((float) 0.8);
        textToSpeech.setOnUtteranceProgressListener(ttsUtteranceProgressListener);
    }

    public void setSpeechRate(float rate) {
        textToSpeech.setSpeechRate(rate);
    }


    public void speak(String text) {
        if (textToSpeech != null) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "myUtteranceID");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap);
        }
    }

    public void stop() {
        textToSpeech.stop();
    }

    public void shutdown() {
        textToSpeech.shutdown();
    }

    public boolean isSpeaking() {
        return textToSpeech.isSpeaking();
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR)
            textToSpeech.setLanguage(locale);
    }
}