package creativeLab.samsung.mbf.utils;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

public class MBFLog {

    private static String TAG = "MBF ";
    private static int SNACK_BAR_COLOR = Color.BLACK;
    private static StringBuilder stringBuilder = new StringBuilder();

    public static void setTag(String tag) {
        TAG = tag;
    }

    public static void setSnackBarColor(int color) {
        SNACK_BAR_COLOR = color;
    }

    public static void v(String message) {
        Log.v(TAG, buildLog(message));
    }

    public static void d(String message) {
        Log.d(TAG, buildLog(message));
    }

    public static void i(String message) {
        Log.i(TAG, buildLog(message));
    }

    public static void w(String message) {
        Log.w(TAG, buildLog(message));
    }

    public static void e(String message) {
        Log.e(TAG, buildLog(message));
    }

    public static void wtf(String message) {
        Log.wtf(TAG, buildLog(message));
    }

    public static void v(String format, Object... args) {
        Log.v(TAG, buildLog(String.format(format, args)));
    }

    public static void d(String format, Object... args) {
        Log.d(TAG, buildLog(String.format(format, args)));
    }

    public static void i(String format, Object... args) {
        Log.i(TAG, buildLog(String.format(format, args)));
    }

    public static void w(String format, Object... args) {
        Log.w(TAG, buildLog(String.format(format, args)));
    }

    public static void e(String format, Object... args) {
        Log.e(TAG, buildLog(String.format(format, args)));
    }

    public static void wtf(String format, Object... args) {
        Log.wtf(TAG, buildLog(String.format(format, args)));
    }

    public static void t(Context context, String format, Object... args) {
        String message = String.format(format, args);
        Log.i(TAG, buildLog(message));
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void tr(Context context, @StringRes int resId, Object... args) {
        String message = context.getString(resId, args);
        Log.i(TAG, buildLog(message));
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void s(View view, String format, Object... args) {
        String message = String.format(format, args);
        Log.i(TAG, buildLog(message));

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(SNACK_BAR_COLOR);
        snackbar.show();
    }

    public static void sr(View view, @StringRes int resId, Object... args) {
        String message = view.getContext().getString(resId, args);
        Log.i(TAG, buildLog(message));

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(SNACK_BAR_COLOR);
        snackbar.show();
    }

    public static void cs(int backgroundColor, View view, String format, Object... args) {
        String message = String.format(format, args);
        Log.i(TAG, buildLog(message));

        Snackbar snackbar = Snackbar.make(view, message, Toast.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(backgroundColor);
        snackbar.show();
    }

    public static void csr(int backgroundColor, View view, @StringRes int resId, Object... args) {
        String message = view.getContext().getString(resId, args);
        Log.i(TAG, buildLog(message));

        Snackbar snackbar = Snackbar.make(view, message, Toast.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(backgroundColor);
        snackbar.show();
    }

    public static void footprint() {
        Log.d(TAG, buildLog("footprint"));
    }

    private static String buildLog(String message) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        stringBuilder.setLength(0);
        stringBuilder.append("[(");
        stringBuilder.append(stackTraceElement.getFileName());
        stringBuilder.append(':');
        stringBuilder.append(stackTraceElement.getLineNumber());
        stringBuilder.append("):");
        stringBuilder.append(stackTraceElement.getMethodName());
        stringBuilder.append("] ");
        stringBuilder.append(message);
        return stringBuilder.toString();
    }

    /**
     * Start {@link Stopwatch}
     * <p>Example</p>
     * <pre><code>
     * LogUtil.Stopwatch stopwatch = LogUtil.startStopwatch("taskName");
     * stopwatch.lap("subTask1");
     * stopwatch.lap("subTask2");
     * stopwatch.lap("subTask3");
     * stopwatch.stop();
     * </code></pre>
     *
     * @param name Used to create {@link Stopwatch}
     * @return New {@link Stopwatch} instance that already started
     * @see Stopwatch
     * @see Stopwatch#start()
     */
    public static Stopwatch startStopwatch(@NonNull String name) {
        Stopwatch stopwatch = new Stopwatch(name);
        stopwatch.start();
        return stopwatch;
    }

    /**
     * Elapsed time checking utility. All messages are logged via {@link MBFLog#d(String, Object...)}
     * <p>Example</p>
     * <pre><code>
     * LogUtil.Stopwatch stopwatch = new LogUtil.Stopwatch("taskName");
     * stopwatch.start();
     * stopwatch.lap("subTask1");
     * stopwatch.lap("subTask2");
     * stopwatch.lap("subTask3");
     * stopwatch.stop();
     * </code></pre>
     *
     * @see #start()
     * @see #lap(String)
     * @see #stop()
     */
    public static class Stopwatch {

        private static final int LOG_TRIM_LENGTH = 25;
        private static final String LOG_TAG_FORMAT = "%" + LOG_TRIM_LENGTH + "s";
        private static final String LOG_PREFIX_FORMAT = LOG_TAG_FORMAT + " | ";
        private static final String LOG_MESSAGE_FORMAT = LOG_TAG_FORMAT + " : %s";
        private static final String STRING_TIME_FORMAT = "%02d:%02d.%03d";
        private String mLogPrefix;
        private long mStartTime;
        private long mLastLapTime;
        private boolean mIsRunning;

        public Stopwatch(@NonNull String name) {
            mLogPrefix = String.format(LOG_PREFIX_FORMAT, trimString(name, LOG_TRIM_LENGTH));
        }

        private static String trimString(@NonNull String str, int size) {
            if (str.length() < size) {
                return str;
            }

            return str.substring(0, size);
        }

        private static long getCurrentTime() {
            return System.currentTimeMillis();
        }

        private static String timeToString(long elapsedTime) {
            long ms = elapsedTime % 1000;
            long sec = elapsedTime / 1000;
            long min = sec / 60;
            sec %= 60;
            return String.format(Locale.getDefault(), STRING_TIME_FORMAT, min, sec, ms);
        }

        /**
         * Start stopwatch. 'start' message logged.
         */
        public void start() {
            mStartTime = getCurrentTime();
            mLastLapTime = mStartTime;
            mIsRunning = true;
            log(LogType.START, null, 0);
        }

        /**
         * Log a elapsed time message with tag
         *
         * @param tag Used to create a log message
         */
        public void lap(String tag) {
            if (mIsRunning) {
                long currentTime = getCurrentTime();
                long elapsedTime = currentTime - mLastLapTime;
                log(LogType.LAP, tag, elapsedTime);
                mLastLapTime = currentTime;
            }
        }

        /**
         * Stop stopwatch. 'total elapsed time' message logged.
         */
        public void stop() {
            if (mIsRunning) {
                long elapsedTime = getCurrentTime() - mStartTime;
                log(LogType.STOP, "total elapsed time", elapsedTime);
                mIsRunning = false;
            }
        }

        /**
         * Check stopwatch running status
         *
         * @return If the stopwatch is running, returns true; else returns false.
         */
        public boolean isRunning() {
            return mIsRunning;
        }

        private void log(LogType logType, String tag, long elapsedTime) {
            switch (logType) {
                case START:
                    log(mLogPrefix + "start --------------------------------");
                    break;
                case LAP:
                    log(mLogPrefix + LOG_MESSAGE_FORMAT, tag, timeToString(elapsedTime));
                    break;
                case STOP:
                    log(mLogPrefix + "--------------------------------------");
                    log(mLogPrefix + LOG_MESSAGE_FORMAT, tag, timeToString(elapsedTime));
                    log(mLogPrefix);
                    break;
                default:
                    break;
            }
        }

        private void log(String format, Object... args) {
            MBFLog.d(format, args);
        }

        private enum LogType {
            START, LAP, STOP
        }
    }
}
