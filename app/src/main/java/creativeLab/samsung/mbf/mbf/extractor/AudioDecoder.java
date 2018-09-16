package creativeLab.samsung.mbf.mbf.extractor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoder {
    private final Context context;
    public short[] data; // data for the AudioTrack playback
    ByteBuffer outputBuffer;
    BufferInfo info = new BufferInfo();
    int maxSize = 0;
    int sampleReadCount = 0;
    private int sourceRawResId = -1;
    private MediaExtractor extractor = null;
    private MediaCodec decoder = null;
    private String urlString = null;
    private MediaFormat inputFormat;
    private boolean end_of_input_file;

    //private ArrayList<short[]> dataArray = new ArrayList<short[]>();
    private int outputBufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER;

    public AudioDecoder(String inputFilename, Context context) {
        this.context = context;
        extractor = new MediaExtractor();
        try {
            if (inputFilename != null) {
                if (inputFilename.startsWith("android.resource://"))
                    extractor.setDataSource(this.context, Uri.parse(inputFilename), null);
                else extractor.setDataSource(inputFilename);
            } else {
                AssetFileDescriptor fd = this.context.getResources().openRawResourceFd(sourceRawResId);
                extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
                fd.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Select the first audio track we find.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                try {
                    decoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, null, null, 0);


                inputFormat = format;
                break;
            }
        }

        if (decoder == null) {
            throw new IllegalArgumentException("No decoder for file format");
        }
        decoder.start();
        end_of_input_file = false;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    // Return the Audio sample rate, in samples/sec.
    public int getSampleRate() {
        return inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }

    public void deInitDecoder() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
    }

    public void getDecodedAudioData(int startTimeMS, int durationMS) {
        extractor.seekTo(startTimeMS, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        maxSize = (durationMS / 1000) * getSampleRate();
        data = new short[maxSize];

        while (!end_of_input_file) {
            for (; ; ) {//Getting data from the file
                if (!end_of_input_file) {
                    int inputBufferIndex = decoder.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                        int size = extractor.readSampleData(inputBuffer, 0);
                        if (size < 0) {// End Of File
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            end_of_input_file = true;
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                outputBuffer = null;

                if (outputBufferIndex >= 0) {
                    outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                    outputBuffer.position(0);//needed?
                }
                outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outputBufferIndex >= 0) {
                    if (info.flags != 0) {
                        decoder.stop();
                        decoder.release();
                        decoder = null;
                    }
                    outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                }

                int samplesRead = info.size / 2;

                if (sampleReadCount >= maxSize) {
                    end_of_input_file = true;
                    break;
                }
                byte[] datadata = new byte[info.size];
                if (outputBuffer != null) {
                    //if (outputBuffer.hasArray())// This doesn't return true in any instance and skipping throws an error on the next line
                    //{
                    //outputBuffer.get(datadata);
                    for (int i = 0; i < samplesRead; i++) {
                        data[i] = outputBuffer.getShort();
                        if (data[i] > 0) {
                            int temp = 0;
                        }
                    }
                    //ByteBuffer.wrap(datadata).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    //System.arraycopy(datadata, 0, data, sampleReadCount, samplesRead);
                    //}
                }
                sampleReadCount = sampleReadCount + samplesRead;

                break;
            }
        }
    }

}
