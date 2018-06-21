package creativeLab.samsung.mbf.Extractor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioExtractorTask extends Thread {
    final String TAG = "AudioExtrator";
    final Context mContext;
    MediaCodec codec;
    long timeoutUs = 1000;
    AudioTrack audioTrack;
    long mstartTime = 0;
    long mDuration = 0;
    private String mUrlString;
    private int sourceRawResId = -1;

    public AudioExtractorTask(Context c) {
        mContext = c;
    }

    public AudioExtractorTask(Context c, int resid) {
        mContext = c;
        sourceRawResId = resid;
    }

    public void setUrlString(String mUrlString) {
        this.mUrlString = mUrlString;
    }

    public void setTime(long startTime, long duration) {
        this.mstartTime = startTime;
        this.mDuration = duration;
    }

    @Override
    public void run() {
        MediaExtractor extractor = new MediaExtractor();
        try {
            if (mUrlString != null) {
                if (mUrlString.startsWith("android.resource://"))
                    extractor.setDataSource(mContext, Uri.parse(mUrlString), null);
                else extractor.setDataSource(mUrlString);
            } else {
                AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(sourceRawResId);
                extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
                fd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime.startsWith("audio/")) { //audio/mp4a-latm
                extractor.selectTrack(i);

                try {
                    codec = MediaCodec.createDecoderByType(mime);
                    codec.configure(format, null, null, 0);
                    codec.start();


                    int TIMEOUT_US = 1000;
                    int inputIndex = codec.dequeueInputBuffer(TIMEOUT_US);
                    if (inputIndex >= 0) {
                        int sampleSize = extractor.readSampleData(codec.getInputBuffer(inputIndex), 0);
                        int Channels = extractor.getTrackFormat(i).getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        int sampleRate = extractor.getTrackFormat(i).getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        int bitRate = extractor.getTrackFormat(i).getInteger(MediaFormat.KEY_BIT_RATE);

                        int channels;
                        switch (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                            case 1: {
                                channels = AudioFormat.CHANNEL_OUT_MONO;
                                break;
                            }
                            case 2: {
                                channels = AudioFormat.CHANNEL_OUT_STEREO;
                                break;
                            }
                            case 4: {
                                channels = AudioFormat.CHANNEL_OUT_QUAD;
                                break;
                            }
                            default:
                                Log.d(TAG, "WAV channels error \n");
                                return;
                        }

                        audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC, sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT, // BitsPerSample를 알수있는 방법이 없다.
                                AudioTrack.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT),
                                AudioTrack.MODE_STREAM);

                        try {
                            codec = MediaCodec.createDecoderByType(mime);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                        codec.configure(format, null, null, 0);
                        codec.start();

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        boolean inputEos = false;
                        boolean outputEos = false;

                        int inputBufIndex, outputBufIndex;
                        ByteBuffer readBuffer, writeBuffer;
                        long presentationTimeUs;

                        audioTrack.play();

                        long startTime = mstartTime;
                        long endTime = mstartTime + mDuration;

                        extractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

                        while (!outputEos) {
                            if (!inputEos) {
                                inputBufIndex = codec.dequeueInputBuffer(timeoutUs);
                                if (inputBufIndex >= 0) {
                                    readBuffer = codec.getInputBuffer(inputBufIndex);
                                    sampleSize = extractor.readSampleData(readBuffer, 0);

                                    if (sampleSize < 0) {
                                        inputEos = true;
                                        sampleSize = 0;
                                        presentationTimeUs = 0;
                                    } else {
                                        presentationTimeUs = extractor.getSampleTime();
                                    }
                                    if (presentationTimeUs > endTime) {
                                        audioTrack.stop();
                                        break;
                                    }

                                    codec.queueInputBuffer(
                                            inputBufIndex,
                                            0,
                                            sampleSize,
                                            presentationTimeUs,
                                            inputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                                    if (!inputEos) {
                                        extractor.advance();
                                    }
                                } else {
                                    Log.e(TAG, "inputBufIndex " + inputBufIndex);
                                }
                            }

                            outputBufIndex = codec.dequeueOutputBuffer(info, timeoutUs);
                            if (outputBufIndex >= 0) {
                                writeBuffer = codec.getOutputBuffer(outputBufIndex);

                                writeBuffer.position(info.offset);
                                writeBuffer.limit(info.offset + info.size);

                                audioTrack.write(writeBuffer, writeBuffer.remaining(), AudioTrack.WRITE_BLOCKING);
                                writeBuffer.clear();
                                codec.releaseOutputBuffer(outputBufIndex, false);
                                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    Log.d(TAG, "saw output EOS.");
                                    outputEos = true;
                                }
                            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat oformat = codec.getOutputFormat();
                                Log.d(TAG, "output format has changed to " + oformat);
                            } else {
                                Log.d(TAG, "dequeueOutputBuffer returned " + outputBufIndex);
                            }
                        }
                    }
                    codec.stop();
                    codec.release();
                    audioTrack.release();
                } catch (IOException e) {
                    Log.e(TAG, "error !!!!!! " + e);
                }

            }
        }
        extractor.release();
    }
}
