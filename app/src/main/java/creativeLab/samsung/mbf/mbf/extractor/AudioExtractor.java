package creativeLab.samsung.mbf.mbf.extractor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;

import creativeLab.samsung.mbf.mbf.MBFInfo;
import creativeLab.samsung.mbf.utils.MBFLog;

public class AudioExtractor {
    private final Context context;
    private int MAX_SAMPLE_SIZE = 256 * 1024;
    private String urlString;
    private int sourceRawResId = -1;
    private String extractedAudioFile = null;

    public AudioExtractor(Context context) {
        this.context = context;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public int startExtractedAudioData(long startTime, long duration, String outputFilename) {
        int ret = MBFInfo.MBF_NO_DATA;
        // "/storage/emulated/0/Android/data/creativeLab.samsung.mbf/cache/pororo01.mp4"
        String dstDirectoryPath = context.getExternalCacheDir().getAbsolutePath() + "/audio";
        if (dstDirectoryPath == null)
            return ret;

        String dstMediaPath = dstDirectoryPath + "/" + outputFilename + ".mp4";
        File dir = new File(dstDirectoryPath);
        if (!dir.exists())
            dir.mkdirs();

        long endTime = startTime + duration;

        // Set up MediaExtractor to read from the source.
        MediaExtractor extractor = new MediaExtractor();
        try {
            if (urlString != null) {
                if (urlString.startsWith("android.resource://"))
                    extractor.setDataSource(context, Uri.parse(urlString), null);
                else extractor.setDataSource(urlString);
            } else {
                AssetFileDescriptor fd = context.getResources().openRawResourceFd(sourceRawResId);
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
                            MBFLog.d("Frame (" + frameCount + ") " +
                                    "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
                                    " Flags:" + bufferInfo.flags +
                                    " TrackIndex:" + trackIndex +
                                    " Size(KB) " + bufferInfo.size / 1024);
                        }
                    }
                    muxer.stop();
                    muxer.release();
                    MBFLog.e("Success to Extract Audio data as a mp4 file. (" + dstMediaPath + ")");
                    this.extractedAudioFile = dstMediaPath;
                    ret = MBFInfo.MBF_SUCCESS;
                }
            }
        } catch (Exception e) {
            MBFLog.e("fail to Extract Audio data. Error : " + e);
            ret = MBFInfo.MBF_ERROR;
        }
        return ret;
    }

    public String getExtractedAudioFile() {
        return extractedAudioFile;
    }

}
