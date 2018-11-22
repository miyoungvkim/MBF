package creativeLab.samsung.mbf.activity;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.OverlayView;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import java.util.List;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.mbf.MBFAIController;
import creativeLab.samsung.mbf.mbf.MBFInfo;
import creativeLab.samsung.mbf.utils.MBFAIDebug;

public class PlayActivity_exoplayer extends AppCompatActivity implements VideoRendererEventListener {
    private static final String TAG = "PlayActivity";
    private View decorView;
    private int uiOption;
    private Button btnCapture;
    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    //Produces DataSource instances through which media data is loaded.
    private DefaultDataSourceFactory dataSourceFactory;
    //Produces Extractor instances for parsing the media data.
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionFactory;
    private DefaultBandwidthMeter bandwidthMeterA;

    private MBFAIController MAIC = null;
    private Context context = null;
    //private MultiBoxTracker tracker;
    OverlayView debugTrackingOverlay;
    private MBFAIDebug mDebug;
    private Uri mp4VideoUri = null;
    private String mp4SubTitleURL = null;


    private LottieAnimationView videoFrameMBFLoading;
    private LottieAnimationView videoFrameMBFCharactor;
    private View videoFrameBG;
    private TextView textVideoSubscription;


    private AudioManager audioManager = null;
    private int CurrentVol = 0;
    private int MaxVol = 0;

    private boolean durationSet = false;

    public final Handler videoFrameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int playState = msg.what;
            mbfFrameVisibleStateChange(playState);

            super.handleMessage(msg);
        }
    };

    public final Handler exoPlayerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            boolean exoplayerState = (boolean)msg.obj;
            //mbfFrameVisibleStateChange(exoplayerState);
            player.setPlayWhenReady(exoplayerState);

            super.handleMessage(msg);
        }
    };
    private void mbfFrameVisibleStateChange(int playState) {
        videoFrameMBFLoading.setVisibility(View.GONE);
        videoFrameMBFCharactor.setVisibility(View.GONE);
        textVideoSubscription.setVisibility(View.GONE);
        videoFrameBG.setVisibility(View.GONE);

        String selectedMention = MBFAIController.getSelectedMention();
        if (playState == MBFInfo.MBF_STATE_CONTENTS_PLAY) {
            Log.d(TAG, "MBFInfo.MBF_STATE_CONTENTS_PLAY");

            textVideoSubscription.setVisibility(View.INVISIBLE);
            //int Vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            //if(Vol == MaxVol)
            //{
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,CurrentVol,0);
            //}
            Log.d(TAG, "MBFInfo.MBF_STATE_CONTENTS_PLAY volume" + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            videoFrameBG.setVisibility(View.INVISIBLE);

            videoFrameMBFLoading.setVisibility(View.INVISIBLE);
            videoFrameMBFLoading.pauseAnimation();
            videoFrameMBFLoading.setProgress(0);

            videoFrameMBFCharactor.setVisibility(View.INVISIBLE);
            videoFrameMBFCharactor.pauseAnimation();
            videoFrameMBFCharactor.setProgress(0);

        } else if (playState == MBFInfo.MBF_STATE_MBF_READY) {
            Log.d(TAG, "MBFInfo.MBF_STATE_MBF_READY");

            textVideoSubscription.setVisibility(View.VISIBLE);
            textVideoSubscription.setText(selectedMention);

            //int dur = MAIC.getMediaTTSDuration();
            //videoFrameHandler
            //videoFrameMBFLoading.setAnimation("@raw/pelican_start_bubble3");
            //int Vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            //if(Vol == CurrentVol)
            //{
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,MaxVol,0);
            //}
            Log.d(TAG, "MBFInfo.MBF_STATE_MBF_READY volume" + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            videoFrameBG.setVisibility(View.VISIBLE);

            videoFrameMBFLoading.setVisibility(View.VISIBLE);
            videoFrameMBFLoading.playAnimation();

            videoFrameMBFCharactor.setVisibility(View.INVISIBLE);
            videoFrameMBFCharactor.pauseAnimation();
            videoFrameMBFCharactor.setProgress(0);

        } else if (playState == MBFInfo.MBF_STATE_MBF_PLAY) {
            Log.d(TAG, "MBFInfo.MBF_STATE_MBF_PLAY");

            textVideoSubscription.setVisibility(View.VISIBLE);
            textVideoSubscription.setText(selectedMention);

            videoFrameBG.setVisibility(View.VISIBLE);

            videoFrameMBFLoading.setVisibility(View.INVISIBLE);
            videoFrameMBFLoading.pauseAnimation();
            videoFrameMBFLoading.setProgress(0);

            videoFrameMBFCharactor.setVisibility(View.VISIBLE);
            videoFrameMBFCharactor.loop(false);
            videoFrameMBFCharactor.playAnimation();

            videoFrameMBFCharactor.addAnimatorListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    player.setPlayWhenReady(true);
                    MAIC.setCurrentStatus(MBFInfo.MBF_STATE_CONTENTS_PLAY);
                    mbfFrameVisibleStateChange(MBFInfo.MBF_STATE_CONTENTS_PLAY);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(uiOption);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        MaxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        CurrentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //hide status and navigation bar
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        uiOption = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        setContentView(R.layout.activity_player);
        btnCapture = findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugOnOff();
            }
        });

        videoFrameMBFLoading = findViewById(R.id.animationVideoFrameLoading);
        videoFrameMBFCharactor = findViewById(R.id.animationVideoFrameCharacter);
        textVideoSubscription = findViewById(R.id.textVideoSubscription);
        videoFrameBG = findViewById(R.id.animationVideoFrameBG);

        context = this;

// 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

// 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

// 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        simpleExoPlayerView = new SimpleExoPlayerView(this);
        simpleExoPlayerView = findViewById(R.id.player_view);
        //TextureView textureView = findViewById(R.id.player_view);

//Set media controller
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        simpleExoPlayerView.setUseController(true);
        simpleExoPlayerView.requestFocus();

// Bind the player to the view.
        simpleExoPlayerView.setPlayer(player);
        //simpleExoPlayerView.getPlayer().setVideoTextureView(textureView);

// I. ADJUST HERE:
//CHOOSE CONTENT: LiveStream / SdCard

//LIVE STREAM SOURCE: * Livestream links may be out of date so find any m3u8 files online and replace:

//        Uri mp4VideoUri =Uri.parse("http://81.7.13.162/hls/ss1/index.m3u8"); //random 720p source
        //Uri mp4VideoUri = Uri.parse("http://54.255.155.24:1935//Live/_definst_/amlst:sweetbcha1novD235L240P/playlist.m3u8"); //Radnom 540p indian channel
        Intent intent = getIntent();
        String tempContentsURL = intent.getExtras().getString("CONTENTS_ADDRESS");
        mp4SubTitleURL = intent.getExtras().getString("SUBTITLE_ADDRESS");
        if(mp4SubTitleURL == null)
        {
            Toast toast = Toast.makeText(this, "수다쟁이 인공지능 기술이 적용되지 않은 영상입니다.", Toast.LENGTH_SHORT);
            toast.show();
        }
        mp4VideoUri = Uri.parse(tempContentsURL); //Radnom 540p indian channel
        //mp4SubTitleURL = "https://kidsvideo.blob.core.windows.net/asset-0c208226-d783-4289-8e0a-eefcf7151230/robocar_poli_s02e02.txt?sv=2015-07-08&sr=c&si=3f5e061f-112d-4ab7-aad2-b640dd0be79b&sig=NUH4%2F4EUDdJ%2B014JG9Hxyb9N5Fw20vZIt8PwIO4KWXc%3D&st=2018-10-23T08%3A05%3A44Z&se=2118-10-23T08%3A05%3A44Z";
//        Uri mp4VideoUri =Uri.parse("FIND A WORKING LINK ABD PLUg INTO HERE"); //PLUG INTO HERE<------------------------------------------


//VIDEO FROM SD CARD: (2 steps. set up file and path, then change videoSource to get the file)
//        String urimp4 = "path/FileName.mp4"; //upload file to device and add path/name.mp4
//        Uri mp4VideoUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+urimp4);


//Measures bandwidth during playback. Can be null if not required.
        bandwidthMeterA = new DefaultBandwidthMeter();
//Produces DataSource instances through which media data is loaded.
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "@string/app_name"), bandwidthMeterA);
//Produces Extractor instances for parsing the media data.
        extractorsFactory = new DefaultExtractorsFactory();

// II. ADJUST HERE:

//This is the MediaSource representing the media to be played:
//FOR SD CARD SOURCE:
//        MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);

//FOR LIVESTREAM LINK:
        MediaSource videoSource = new HlsMediaSource(mp4VideoUri, dataSourceFactory, 1, null, null);
        final LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);

// Prepare the player with the source.
        player.prepare(loopingSource);
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                Log.v(TAG, "Listener-onTimelineChanged...");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                Log.v(TAG, "Listener-onTracksChanged...");
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.v(TAG, "Listener-onLoadingChanged...isLoading:" + isLoading);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.v(TAG, "Listener-onPlayerStateChanged..." + playbackState);
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                    long realDurationMillis = player.getDuration();
                    durationSet = true;
                    MAIC.setFinalDuration(realDurationMillis);
                }
                /*if(playbackState == Player.STATE_ENDED)
                {
                    MediaPlayer mediaEndingMentionTTSVoicePlayer = MediaPlayer.create(context, R.raw.goodbye);
                    mediaEndingMentionTTSVoicePlayer.start();
                }*/
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                Log.v(TAG, "Listener-onRepeatModeChanged...");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.v(TAG, "Listener-onPlayerError...");
                player.stop();
                player.prepare(loopingSource);
                player.setPlayWhenReady(true);
                //MAIC.stop();
            }

            @Override
            public void onPositionDiscontinuity() {
                Log.v(TAG, "Listener-onPositionDiscontinuity...");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                Log.v(TAG, "Listener-onPlaybackParametersChanged...");
            }
        });

        player.setPlayWhenReady(true); //run file/link when ready to play.
        player.setVideoDebugListener(this); //for listening to resolution change and  outputing the resolution


        //tracker = new MultiBoxTracker(this);
        debugTrackingOverlay = (OverlayView)findViewById(R.id.tracking_overlay);

        MAIC = new MBFAIController(context, videoFrameHandler, exoPlayerHandler, player);
        MAIC.start(getAssets(), mp4SubTitleURL, player, simpleExoPlayerView);

        //player.getCurrentPosition();

        mDebug = new MBFAIDebug(simpleExoPlayerView, context, debugTrackingOverlay, MAIC);

    } // End of Create


    private void debugOnOff() {
        //finish();
        // extractorsFactory.
        /*TextureView textureView = (TextureView) simpleExoPlayerView.getVideoSurfaceView();
        Bitmap bitmap = textureView.getBitmap();
        int width = textureView.getWidth();
        int height = textureView.getWidth();*/
        debugTrackingOverlay.postInvalidate();
        if(mDebug.isDebug == false)
        {
            mDebug.isDebug = true;
            if(mDebug.debugT == null)
            {
                mDebug.startDebugThread();
            }

        }else{
            mDebug.isDebug = false;
            mDebug.debugT.interrupt();
            mDebug.debugT = null;
        }
        return;

    }
/*
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }
*/
    /*
    public Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }*/


    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Log.v(TAG, "onVideoSizeChanged [" + " width: " + width + " height: " + height + "]");
        //resolutionTextView.setText("RES:(WxH):"+width+"X"+height +"\n           "+height+"p");
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        /*if(isFisrtProcessing == false)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    processingImage();
                }
            }).start();
        }*/
        Log.v(TAG, "onRenderedFirstFrame ");
        MAIC.setReactionArrange();
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }


//-------------------------------------------------------ANDROID LIFECYCLE---------------------------------------------------------------------------------------------

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()...");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()...");
        player.release();
        if (MAIC != null)
        {
            MAIC.stop();
            MAIC = null;
        }
    }
}