package lniu.animatedgifcreator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;


import com.loopj.android.http.*;
import org.apache.http.Header;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends Activity {

    private static final int ACTION_TAKE_VIDEO = 3;
    private Uri mVideoUri;
    private GifRun gifRun;
    private SurfaceView surfaceView;
    private AsyncHttpClient client;
    private String hostname;
    private EditText editText;
    private File currFile;

    Twitter twitter;
    public final String consumer_key = "Replace your KEY";
    public final String secret_key = "Replace your KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gifRun = new GifRun();
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        editText = (EditText)findViewById(R.id.editText);
        mVideoUri = null;
        client = new AsyncHttpClient();
        client.setEnableRedirects(true);
        client.setTimeout(40*1000);
        hostname = this.getResources().getString(R.string.hostname);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("MbzWeYPTeT9eBQquDNR2GKV3i")
                .setOAuthConsumerSecret("VTkJM55J5yqtHHZyXAUbO2gKSK8IwnSHQOtW13SPvkTkf6uHUf")
                .setOAuthAccessToken("756372378-LQ6SGl0s9L6cqiMS05OQvTFQTX5c2VIE6udfGRj5")
                .setOAuthAccessTokenSecret("BTwqEi4QNriK4cbv8xpbSwXrIxtZM5JSE9C0wqVMfhFBR");
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_VIDEO: {
                if (resultCode == RESULT_OK) {
                    handleCameraVideo(data);
                }
                break;
            } // ACTION_TAKE_VIDEO
        } // switch
    }

    private Boolean isNetworkAvailable() {
        return true;
    }

    public void uploadPic(File file, String message,Twitter twitter) throws Exception  {
        try{
            StatusUpdate status = new StatusUpdate(message);
            status.setMedia(file);
            twitter.updateStatus(status);}
        catch(TwitterException e){
            Log.d("TAG", "Pic Upload error" + e.getErrorMessage());
            throw e;
        }
    }

    public void ShareGif(View view) {
        if(currFile != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ConfigurationBuilder cb = new ConfigurationBuilder();
                        cb.setDebugEnabled(true)
                                .setOAuthConsumerKey("MbzWeYPTeT9eBQquDNR2GKV3i")
                                .setOAuthConsumerSecret("VTkJM55J5yqtHHZyXAUbO2gKSK8IwnSHQOtW13SPvkTkf6uHUf")
                                .setOAuthAccessToken("756372378-LQ6SGl0s9L6cqiMS05OQvTFQTX5c2VIE6udfGRj5")
                                .setOAuthAccessTokenSecret("BTwqEi4QNriK4cbv8xpbSwXrIxtZM5JSE9C0wqVMfhFBR");
                        TwitterFactory tf = new TwitterFactory(cb.build());
                        Twitter twitter = tf.getInstance();
                        uploadPic(currFile, "", twitter);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Succeed to post on twitter!", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Curr file is empty!", Toast.LENGTH_LONG).show();
        }
    }

    public void dispatchTakeVideoIntent(View view) {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
    }


    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getGifFileNameFromPath(String path) {
        String[] substrs = path.split("/");
        return substrs[substrs.length - 1].replace("mp4", "gif");
    }

    private void handleCameraVideo(Intent intent) {
        mVideoUri = intent.getData();
        String movie_path = getRealPathFromURI(getApplicationContext(), mVideoUri);
        System.out.println(movie_path);
        Toast.makeText(getApplicationContext(), movie_path,
                Toast.LENGTH_LONG).show();

        // Upload file to server.
        File myFile = new File(movie_path);
        System.out.println("Found file " + movie_path);
        final String gifFileName = getGifFileNameFromPath(movie_path);
        RequestParams params = new RequestParams();
        try {
            params.put("file", myFile, "video/mp4");
        } catch(FileNotFoundException e) {}
        String want2put = editText.getText().toString();
        String encoded = "";
        try {
            encoded = URLEncoder.encode(want2put, "utf-8");
        }catch(Exception e) {
            e.printStackTrace();
        }
        client.post(hostname + "/upload" + (want2put.equals("") ? "" : "?words=" + encoded),
                params, new FileAsyncHttpResponseHandler(this) {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                // called when response HTTP status is "200 OK"
                String output = String.format("response is NULL with statusCode: %d.", statusCode);
                if(response == null) {
                    Toast.makeText(getApplicationContext(), output, Toast.LENGTH_LONG).show();
                    return;
                }
                // Toast.makeText(getApplicationContext(), "Upload file succeed." + gifFileName,
                //       Toast.LENGTH_LONG).show();
                currFile = null;
                File outputFile = new File(((Context)MainActivity.this).getExternalFilesDir(null), gifFileName);
                Toast.makeText(getApplicationContext(), outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                try {
                    InputStream inputStream = new FileInputStream(response);
                    OutputStream outputStream = new FileOutputStream(outputFile);
                    int read = 0;
                    byte[] bytes = new byte[1024];
                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }

                    InputStream gifStream = new FileInputStream(response);
                    gifRun.LoadGiff(surfaceView, getApplicationContext(), gifStream);
                    currFile = outputFile;
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
                Toast.makeText(getApplicationContext(), "Retry is called.", Toast.LENGTH_LONG).show();
            }
        });

    }
}
