package de.kordelle.radio.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ini4j.Ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import de.kordelle.radio.logging.LogHelper;

/**
 * @link https://developer.android.com/training/basics/network-ops/connecting.html
 */
public class DownLoadPlayList extends AsyncTask<String, Void, String>
{
    /**
     * listener interface
     */
    public interface DownLoadPlayListListener {
        public void onPlayListLoaded(final InputStream content);
    }

    public static final String DEFAULT_PLAYLIST_FILENAME = "playlist.pls";
    public static final String SAVED_PLAYLIST_FILENAME = "savedplaylist.pls";

    /* Date format pattern used to parse HTTP date headers in RFC 1123 format.  */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    /* Date format pattern used to parse HTTP date headers in RFC 1036 format. */
    public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
    /* Date format pattern used to parse HTTP date headers in ANSI C asctime() format. */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    private static final String[] DEFAULT_PATTERNS = new String[] {
        PATTERN_RFC1123,
        PATTERN_RFC1036,
        PATTERN_ASCTIME
    };

    /** member */
    private static final String TAG = DownLoadPlayList.class.getSimpleName();
    private static final int WAIT_LOOP_COUNT = 10;
    private static final int CONNECT_TIMEOUT = 10 * 1000;
    private static final int READ_TIMEOUT = 15 * 1000;

    private Context context;
    private DownLoadPlayListListener listener;
    private boolean duringBoot;

    /**
     *
     * @param context
     * @param listener
     */
    public DownLoadPlayList(final Context context, final DownLoadPlayListListener listener, final boolean duringBoot)
    {
        this.context = context;
        this.listener = listener;
        this.duringBoot = duringBoot;
    }

    /**
     * @see AsyncTask#onPostExecute(Object)
     * @param playList
     */
    @Override
    protected void onPostExecute(final String playList)
    {
        InputStream is = null;
        if (StringUtils.isNotBlank(playList))
            is = IOUtils.toInputStream(playList);

        listener.onPlayListLoaded(is);
    }

    /**
     *
     * @param lastMod
     * @return
     * @throws ParseException
     */
    private Date parseHttpDateTime(final String lastMod) throws ParseException
    {
        ParsePosition  pos = new ParsePosition(0);
        SimpleDateFormat df = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH);
        for (final String pattern : DEFAULT_PATTERNS) {
            df.applyPattern(pattern);
            Date date = df.parse(lastMod, pos);
            if ((date != null) && (pos.getErrorIndex() == -1))
                return date;
        }
        throw new ParseException("failed to parse HTTP date time: " + lastMod, pos.getErrorIndex());
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private boolean isNewerFileOnServer(final URL url) throws IOException
    {
        HttpURLConnection conn = null;
        try {
            File file = new File(context.getFilesDir(), SAVED_PLAYLIST_FILENAME);
            if (file.exists()) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setDoInput(true);
                conn.connect();

                LogHelper.d(TAG, "HEAD response: " + conn.getResponseCode());
                String lastMod = conn.getHeaderField("Last-Modified");

                return StringUtils.isBlank(lastMod) || (file.lastModified() < parseHttpDateTime(lastMod).getTime());
            }
        }
        catch (ParseException ex) {
            LogHelper.e(TAG, "error parsing last modified date from server.", ex);
        }
        finally {
            if (conn != null)
                conn.disconnect();
        }
        return true;
    }

    /**
     * @see AsyncTask#doInBackground(Object[])
     * @param params
     * @return
     */
    @Override
    protected String doInBackground(String... params)
    {
        if ((params == null) || (params.length < 1) || StringUtils.isBlank(params[0])) {
            LogHelper.e(TAG, "No url given where to download the playlist.");
            return playListFromLocalFile();
        }

        HttpURLConnection conn = null;
        try {
            if (networkAvailable()) {
                URL url = new URL(params[0]);
                if (isNewerFileOnServer(url)) { // load from server otherwise get local file
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(READ_TIMEOUT);
                    conn.setConnectTimeout(CONNECT_TIMEOUT);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                    conn.setDoInput(true);
                    // Starts the query
                    conn.connect();

                    if (conn.getResponseCode() == 200) {
                        String playList = contentAsString(conn);
                        // try to parse play list file, throws a exception if the file cant be parsed
                        new Ini(IOUtils.toInputStream(playList));
                        // everything is okay
                        return playList;
                    }
                    LogHelper.e(TAG, "Download of playlist failed. Responsecode: " + conn.getResponseCode());
                }
            }
            else
                LogHelper.i(TAG, "Network is NOT available.");
        }
        catch (IOException ex) {
            LogHelper.e(TAG, "Error while fetching playlist from server: " + params[0], ex);
        }
        finally {
            if (conn != null)
                conn.disconnect();
        }
        return playListFromLocalFile();
    }

    /**
     *
     * @param conn
     * @return
     * @throws IOException
     */
    private String contentAsString(final HttpURLConnection conn) throws IOException
    {
        String contentEncoding = conn.getHeaderField("Content-Encoding");

        if (StringUtils.isNotBlank(contentEncoding) && contentEncoding.equalsIgnoreCase("gzip"))
            return IOUtils.toString(new GZIPInputStream(conn.getInputStream()), StandardCharsets.UTF_8);

        return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     *
     * @return
     */
    private boolean networkAvailable()
    {
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // only wait after boot for a network connection. the app is started before network becomes ready
            for (int i = 0; i < WAIT_LOOP_COUNT; i++) {
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected())
                    return true;

                if (!duringBoot)
                    break;

                Thread.sleep(2000); // wait 2 seconds and try again
            }
        }
        catch (Exception ex) {
            LogHelper.e(TAG, "Unable to detect state of network connection.", ex);
        }
        return false;
    }

    /**
     *
     * @return
     */
    private String playListFromLocalFile()
    {
        try {
            LogHelper.i(TAG, "Loading local playlist.");
            try {
                File file = new File(context.getFilesDir(), SAVED_PLAYLIST_FILENAME);
                if (file.exists()) {
                    if (file.length() > 0)
                        return IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
                    else
                        file.delete();
                }
            }
            catch (IOException innerEx) {
                LogHelper.e(TAG, "error loading saved playlist file.", innerEx);
            }

            InputStream is = context.getResources().getAssets().open(DEFAULT_PLAYLIST_FILENAME);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            LogHelper.e(TAG, "Unable to open local playlist", ex);
        }
        return null;
    }

    /**
     *
     * @param context
     * @param is
     * @throws IOException
     */
    public static void storePlayList(final Context context, final InputStream is) throws IOException
    {
        File file = new File(context.getFilesDir(), DownLoadPlayList.SAVED_PLAYLIST_FILENAME);
        IOUtils.copy(is, new FileOutputStream(file));
    }
}
