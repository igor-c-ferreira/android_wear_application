package br.com.zapimoveis.wearapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements DataApi.DataListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long TIMEOUT_MS = 1000;
    private static final String COUNT_KEY = "COUNT_KEY";
    private static final String IMAGE_RESOURCE = "IMAGE_RESOURCE";

    private TextView mTextView;
    private TextView mSecondTextView;
    private ImageView mImageView;
//    private DismissOverlayView mDismissOverlay;

    private GoogleApiClient mGoogleClient;
//    private GestureDetector mDetector;

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleClient, asset).await().getInputStream();
        mGoogleClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mSecondTextView = (TextView) stub.findViewById(R.id.second_text);
                mImageView = (ImageView) stub.findViewById(R.id.image);

//                mDismissOverlay = (DismissOverlayView) stub.findViewById(R.id.dismiss_overlay);
//
//                mDismissOverlay.setIntroText(R.string.long_press_hint);
//                mDismissOverlay.showIntroIfNecessary();
//
//                mDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener(){
//                    @Override
//                    public void onLongPress(MotionEvent event) {
//                        mDismissOverlay.show();
//                        super.onLongPress(event);
//                    }
//                });


                mTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mSecondTextView.getVisibility() == View.VISIBLE) {
                            mSecondTextView.setVisibility(View.INVISIBLE);
                        } else {
                            mSecondTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });

                mGoogleClient = new GoogleApiClient.Builder(MainActivity.this)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(MainActivity.this)
                        .addOnConnectionFailedListener(MainActivity.this)
                        .build();
                mGoogleClient.connect();
            }
        });
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return (super.onTouchEvent(event) || mDetector.onTouchEvent(event));
//    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleClient && mGoogleClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleClient, this);
            mGoogleClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {

            switch (event.getType()) {
                case DataEvent.TYPE_DELETED:
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                    break;
                case DataEvent.TYPE_CHANGED: {
                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());

                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    final long count = dataItem.getDataMap().getLong(COUNT_KEY);

                    Asset profileAsset = dataItem.getDataMap().getAsset(IMAGE_RESOURCE);
                    final Bitmap bitmap = loadBitmapFromAsset(profileAsset);

                    Handler mainHandler = new Handler(MainActivity.this.getMainLooper());

                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(bitmap);
                            mTextView.setText(String.valueOf(count));
                        }
                    };
                    mainHandler.post(myRunnable);

                    break;
                }
                default:
                    Log.d(TAG, "Unknown: " + event.getDataItem().getUri());
                    break;
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult.toString());
    }
}
