package creativeLab.samsung.mbf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.OverlayView;
import org.tensorflow.demo.env.BorderedText;

import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.activity.PlayActivity_exoplayer;
import creativeLab.samsung.mbf.mbf.MBFAIController;

public class MBFAIDebug {
    private static final String TAG = "MBFAIDebug";

    private Paint boxPaint = new Paint();
    private BorderedText borderedText = null;
    private float textSizePx = 0.0f;
    private long processingTime = 0;
    private boolean isFisrtProcessing = false;
    private  OverlayView debugTrackingOverlay;
    private MBFAIController MAIC;
    public Thread debugT = null;// = new debugThread();

    public boolean isDebug = false;
    private SimpleExoPlayerView simpleExoPlayerView;
    long currTimestamp = 0;
    private List<Classifier.Recognition> results = null;

    public MBFAIDebug(SimpleExoPlayerView sepv, Context context, OverlayView ov, MBFAIController mfac)
    {
        simpleExoPlayerView = sepv;
        debugTrackingOverlay = ov;
        MAIC = mfac;

        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);
        boxPaint.setStrokeMiter(100);

        //debugTrackingOverlay = (OverlayView)findViewById(R.id.tracking_overlay);

        debugTrackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(Canvas canvas) {
                        draw(canvas);
                    }
                }
        );
    }

    public void startDebugThread()
    {
        debugT = new debugThread();
        debugT.start();
    }

    private synchronized void draw(final Canvas canvas) {
        //final boolean rotated = true;//sensorOrientation % 180 == 90;
        //final float multiplier = 1.0f;
        //Math.min(canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
        //        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
        /*frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);*/
        //int numBoxes = results.size();
        if(results == null)
        {

        }else {
            for (final Classifier.Recognition result : results) {
                final RectF trackedPos = result.getLocation();
                final float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
                canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
                final String labelString =
                        !TextUtils.isEmpty(result.getTitle())
                                ? String.format("%s %.2f", result.getTitle(), result.getConfidence())
                                : String.format("%.2f", result.getConfidence());
                borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.bottom, labelString);
            }
        }
    }

    private void processingImage()
    {
        while (isDebug == true) {

            debugTrackingOverlay.postInvalidate();
            currTimestamp++;
            TextureView textureView = (TextureView) simpleExoPlayerView.getVideoSurfaceView();
            Bitmap bitmap = textureView.getBitmap();
            int width = textureView.getWidth();
            int height = textureView.getWidth();


            final long startTime = SystemClock.uptimeMillis();
            results = MAIC.getObjectDetectorResult(bitmap);
            final long stopTime = SystemClock.uptimeMillis();
            final long dur = stopTime - startTime;
            processingTime = dur;
            //getWindowManager().getDefaultDisplay().getRotation();
            //tracker.onFrame(width,height,0,getScreenOrientation(),null,currTimestamp);
            //tracker.trackResults(results, null, currTimestamp);

            debugTrackingOverlay.postInvalidate();
        }
    }


    public class debugThread extends Thread implements Runnable
    {
        public void run(){
            try {
                while (!Thread.currentThread().isInterrupted())
                {
                    processingImage();
                }
            }catch (Exception ee){

            }finally {
                Log.v(TAG,"debug Thread is interrupted");
            }
        }
    }



}
