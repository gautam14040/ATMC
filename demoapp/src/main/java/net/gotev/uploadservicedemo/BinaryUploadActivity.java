package net.gotev.uploadservicedemo;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import butterknife.BindView;

/**
 * @author Aleksandar Gotev
 */

public class BinaryUploadActivity extends UploadActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    public GoogleApiClient mApiClient;
    double lat = 0;
    double log = 0;
    boolean hasLocation = false;
    String activity ;

    String mCurrentPhotoPath;

    CharSequence text = "Hello toast!";
    int duration = Toast.LENGTH_SHORT;
    Context context ;

    MyLocation myLocation = new MyLocation();
    MyLocation.LocationResult locationResult = new MyLocation.LocationResult(){
        @Override
        public void gotLocation(Location location){
            //Got the location!
            lat = location.getLatitude();
            log = location.getLongitude();
            Log.d("MyApp", "gotLocation: "+Double.toString(lat)+" "+Double.toString(log));
            hasLocation = true;

        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent( this, ActivityRecognizedService.class );
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(apiClient, TIMER, pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter(ActivityRecognizedService.LOCAL_BROADCAST_NAME));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }


    private File createImageFile() throws IOException {
        // Create an image file name

//        myLocation.getLocation(this, locationResult);

//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String location = "loc_"+Double.toString(lat)+ "_" + Double.toString(log)+"_";
        String act = activity;
        String imageFileName = "JPEG_act_" + act + "_"+location;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public static void show(BaseActivity activity) {
        activity.startActivity(new Intent(activity, BinaryUploadActivity.class));
    }

    @Override
    public AdapterItem getEmptyItem() {
        return new EmptyItem(R.string.empty_binary_upload);
    }

    public boolean checkGooglePlayServicesAvailable(Activity activity) {
        int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(activity, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public GoogleApiClient apiClient;
    BroadcastReceiver myReceiver;
    int TIMER = 3000; // 3 sec

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if(!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        myLocation.getLocation(this, locationResult);

        apiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        apiClient.connect();

        // verify Play Services is active and up-to-date
        checkGooglePlayServicesAvailable(this);

        myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                activity = intent.getStringExtra(ActivityRecognizedService.LOCAL_BROADCAST_EXTRA);
                Log.d("MyApp", "onReceive: " + activity);
            }
        };


        context = getApplicationContext();
        super.onCreate(savedInstanceState);
        addParameter.setVisibility(View.GONE);
        addFile.setTitleText(getString(R.string.set_file));

        Toast toast = Toast.makeText(context, "Wait till Location availaible",duration);
        toast.show();
    }

    @Override
    public void onAddFile() {
//        galleryAddPic();
        fileParameterName = "file";
        openFilePicker(false);
    }

    @Override
    public void onAddHeader() {
        if(!hasLocation) {
            Toast toast = Toast.makeText(context, "Location not avaible yet, wait ", duration);
            toast.show();
        }
        else
            dispatchTakePictureIntent();
    }

    @Override
    public void onDone(String httpMethod, String serverUrl, UploadItemUtils uploadItemUtils) {

        try {
            final String uploadId = UUID.randomUUID().toString();

            final BinaryUploadRequest request = new BinaryUploadRequest(this, uploadId, serverUrl)
                    .setMethod(httpMethod)
                    .setNotificationConfig(getNotificationConfig(uploadId, R.string.binary_upload))
                    //.setCustomUserAgent(getUserAgent())
                    .setMaxRetries(MAX_RETRIES)
                    .setUsesFixedLengthStreamingMode(FIXED_LENGTH_STREAMING_MODE);


            uploadItemUtils.forEach(new UploadItemUtils.ForEachDelegate() {

                @Override
                public void onHeader(UploadItem item) {
                    request.addHeader(item.getTitle(), item.getSubtitle());
                }

                @Override
                public void onParameter(UploadItem item) {
                    // Binary uploads does not support parameters
                }

                @Override
                public void onFile(UploadItem item) {
                    try {
                        request.setFileToUpload(item.getSubtitle());
                    } catch (IOException exc) {
                        Toast.makeText(BinaryUploadActivity.this,
                                getString(R.string.file_not_found, item.getSubtitle()),
                                Toast.LENGTH_LONG).show();
                    }
                }

            });

            request.startUpload();
            finish();

        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onInfo() {
        openBrowser("https://github.com/gotev/android-upload-service/wiki/Recipes#http-binary-upload-");
    }
}
