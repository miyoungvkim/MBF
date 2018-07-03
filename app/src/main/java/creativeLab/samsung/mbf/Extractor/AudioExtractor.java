package creativeLab.samsung.mbf.Extractor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class AudioExtractor {
    final String TAG = "AudioExtrator";
    final Context mContext;
    int MAX_SAMPLE_SIZE = 256 * 1024;
    long mstartTime = 0;
    long mduration = 0;
    private String mUrlString;
    private int sourceRawResId = -1;

    public AudioExtractor(Context c) {
        this.mContext = c;
    }

    // ex :  AudioExtractor adioExtractor = new AudioExtractor(mContext, R.raw.A
    public AudioExtractor(Context context, int resourceId) {
        this.mContext = context;
        this.sourceRawResId = resourceId;
    }

    public void setUrlString(String mUrlString) {
        this.mUrlString = mUrlString;
    }

    public void setResourceId(int resid) {
        this.sourceRawResId = resid;
    }

    public void setTime(long startTime, long duration) {
        this.mstartTime = startTime;
        this.mduration = duration;
    }

    // getExtracted_Audio("pororo01")
    public String getExtractedAudioDataPath(String outputFilename) {
        // "/storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4"
        String dstDirectoryPath = mContext.getExternalCacheDir().getAbsolutePath();
        String dstMediaPath = dstDirectoryPath + "/" + outputFilename + ".mp4";
        File dir = new File(dstDirectoryPath);
        if (!dir.exists()) dir.mkdirs();

        // Set up MediaExtractor to read from the source.
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
            int trackCount = extractor.getTrackCount();

            // Set up MediaMuxer for the destination.
            MediaMuxer muxer;
            muxer = new MediaMuxer(dstMediaPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Set up the tracks.
            HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith("audio/")) { //audio/mp4a-latm
                    int dstIndex = muxer.addTrack(format);
                    indexMap.put(i, dstIndex);
                    extractor.selectTrack(i);

                    long startTime = mstartTime;
                    long endTime = mstartTime + mduration;

                    extractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

                    // Copy the samples from MediaExtractor to MediaMuxer.
                    boolean sawEOS = false;
                    int bufferSize = MAX_SAMPLE_SIZE;
                    int frameCount = 0;
                    int offset = 100;

                    ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    muxer.start();
                    while (!sawEOS) {
                        bufferInfo.offset = offset;
                        bufferInfo.size = extractor.readSampleData(dstBuf, offset);
                        if (bufferInfo.size < 0) {
                            sawEOS = true;
                            bufferInfo.size = 0;
                        } else {
                            bufferInfo.presentationTimeUs = extractor.getSampleTime();
                            bufferInfo.flags = extractor.getSampleFlags();
                            if (bufferInfo.presentationTimeUs > endTime) {
                                break;
                            }
                            int trackIndex = extractor.getSampleTrackIndex();
                            muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                            extractor.advance();
                            frameCount++;
                            Log.d(TAG, "Frame (" + frameCount + ") " +
                                    "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
                                    " Flags:" + bufferInfo.flags +
                                    " TrackIndex:" + trackIndex +
                                    " Size(KB) " + bufferInfo.size / 1024);
                        }
                    }
                    muxer.stop();
                    muxer.release();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "fail to Extract Audio data. Error : " + e);
            dstMediaPath = null;
        }
        Log.d(TAG, "Success to Extract Audio data as a mp4 file. (" + dstMediaPath + ")");
        return dstMediaPath;
    }
}
