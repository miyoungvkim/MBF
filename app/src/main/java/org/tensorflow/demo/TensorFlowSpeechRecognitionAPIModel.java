/**
 * geonhui83.lee modified speech recognition on Android Device using wavenet with tensorflow
 */
package org.tensorflow.demo;

import android.content.res.AssetManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.demo.mfcc.MFCC;

import java.util.Arrays;

import creativeLab.samsung.mbf.utils.MBFLog;

public class TensorFlowSpeechRecognitionAPIModel {
    private static final char[] map = new char[]{'0', ' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    // UI elements.
    //private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String OUTPUT_SCORES_NAME = "output";
    public boolean shouldContinueRecognition = true;
    public Thread recognitionThread;
    // Working variables.
    private int sampleRate = 0;
    private int sampleDurationMS = 0;
    private int recordingLen = 0;
    //private short[] recordingBuffer;// = new short[RECORDING_LENGTH];
    //private int recordingOffset = 0;
    //private boolean shouldContinue = true;
    //public Thread recordingThread;
    //private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private TensorFlowInferenceInterface inferenceInterface;
    private String inputDataname;

    public void create(
            final AssetManager assetManager,
            final String modelFilename,
            final String iDataname,
            final int sRate,
            final int sDurationMS
    ) {
        sampleRate = sRate;
        sampleDurationMS = sDurationMS;
        recordingLen = (int) (sampleRate * sampleDurationMS / 1000);
        //recordingBuffer = new short[recordingLen];
        inputDataname = iDataname;

        inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
    }

    public void setSampleData(int sRate, int sDurationMS) {
        sampleRate = sRate;
        sampleDurationMS = sDurationMS;
        recordingLen = (int) (sampleRate * sampleDurationMS / 1000);
    }

    public String recognize(short[] inputAudio) {
        MBFLog.v("Start recognition");
        //Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[recordingLen];
        double[] doubleInputBuffer = new double[recordingLen];
        long[] outputScores = new long[157];
        String[] outputScoresNames = new String[]{OUTPUT_SCORES_NAME};

        int maxLength = inputAudio.length;
        System.arraycopy(inputAudio, 0, inputBuffer, 0, maxLength);

        /*
        recordingBufferLock.lock();
        try {
            int maxLength = recordingBuffer.length;
            System.arraycopy(recordingBuffer, 0, inputBuffer, 0, maxLength);
        } finally {
            recordingBufferLock.unlock();
        }*/

        // We need to feed in float values between -1.0 and 1.0, so divide the
        // signed 16-bit inputs.
        for (int i = 0; i < recordingLen; ++i) {
            doubleInputBuffer[i] = inputBuffer[i] / 32767.0;
        }

        //MFCC java library.
        MFCC mfccConvert = new MFCC();
        float[] mfccInput = mfccConvert.process(doubleInputBuffer);
        MBFLog.v("MFCC Input======> " + Arrays.toString(mfccInput));

        // Run the model.
        inferenceInterface.feed(inputDataname, mfccInput, 1, 157, 20);
        inferenceInterface.run(outputScoresNames);
        inferenceInterface.fetch(OUTPUT_SCORES_NAME, outputScores);
        MBFLog.v("OUTPUT======> " + Arrays.toString(outputScores));


        //Output the result.
        String result = "";
        for (int i = 0; i < outputScores.length; i++) {
            if (outputScores[i] == 0)
                break;
            result += map[(int) outputScores[i]];
        }
        MBFLog.v("End recognition: " + result);
        return result;
        /*this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputText.setText(r);
            }
        });*/


    }
}
