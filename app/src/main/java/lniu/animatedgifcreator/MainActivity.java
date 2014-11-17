package lniu.animatedgifcreator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.Toast;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import com.loopj.android.http.*;
import org.apache.http.Header;


public class MainActivity extends Activity {

    private static final int ACTION_TAKE_VIDEO = 3;
    private Uri mVideoUri;
    private GifRun gifRun;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gifRun = new GifRun();
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mVideoUri = null;


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

    private void dispatchTakeVideoIntent() {
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

    private void handleCameraVideo(Intent intent) {
        mVideoUri = intent.getData();
        String movie_path = getRealPathFromURI(getApplicationContext(), mVideoUri);
        System.out.println(movie_path);
        Toast.makeText(getApplicationContext(), movie_path,
                Toast.LENGTH_LONG).show();

        // Upload file to server.

        File myFile = new File(movie_path);
        //  String str = movie_path.split("/")[movie_path.length()-1];
        //Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
        RequestParams params = new RequestParams();
        try {
            params.put("file", myFile, "video/mp4");
        } catch(FileNotFoundException e) {}

        AsyncHttpClient client = new AsyncHttpClient();
        client.setEnableRedirects(true);
        client.post("http://10.90.114.82:80/upload", params, new FileAsyncHttpResponseHandler(this) {

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
                File outputFile = new File(((Context)MainActivity.this).getExternalFilesDir(null), response.getName());
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
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });

    }
}
