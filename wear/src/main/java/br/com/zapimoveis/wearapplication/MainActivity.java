package br.com.zapimoveis.wearapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements DataApi.DataListener, ConnectionCallbacks,
        OnConnectionFailedListener, SensorEventListener, NodeApi.NodeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long TIMEOUT_MS = 1000;
    private static final String COUNT_KEY = "COUNT_KEY";
    private static final String IMAGE_RESOURCE = "IMAGE_RESOURCE";
    private static final int SENSOR_TYPE_HEARTRATE = 65562;

    private String nodeId;

    private TextView mTextView;
    private TextView mSecondTextView;
    private ImageView mImageView;
    private Button mOpenButton;
//    private DismissOverlayView mDismissOverlay;

    private GoogleApiClient mGoogleClient;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
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
                mOpenButton = (Button) stub.findViewById(R.id.open_button);

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

                mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

                mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
//                mHeartRateSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE_HEARTRATE);

                mSensorManager.registerListener(MainActivity.this,mHeartRateSensor,3);

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

        if (null != mSensorManager) {
            mSensorManager.unregisterListener(this);
        }

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleClient, this);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(nodeId != null) {
                    Wearable.MessageApi.sendMessage(mGoogleClient, nodeId, "/open_activity", null);
                } else {
                    PutDataMapRequest dataMap = PutDataMapRequest.create("/open_activity");
                    dataMap.getDataMap().putLong("TIMESTAMP", SystemClock.currentThreadTimeMillis());
                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                            .putDataItem(mGoogleClient, dataMap.asPutDataRequest());
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.DataApi.removeListener(mGoogleClient,this);
        mOpenButton.setOnClickListener(null);
    }

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

                    Handler mainHandler = new Handler(MainActivity.this.getMainLooper());

                    if(dataItem.getUri().getPath().equals("/update")) {
                        final long count = dataItem.getDataMap().getLong(COUNT_KEY);
                        Asset profileAsset = dataItem.getDataMap().getAsset(IMAGE_RESOURCE);
                        final Bitmap bitmap = loadBitmapFromAsset(profileAsset);
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                mImageView.setImageBitmap(bitmap);
                                mTextView.setText(String.valueOf(count));
                            }
                        };
                        mainHandler.post(myRunnable);
                    }

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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
        mSecondTextView.setText(String.format("accuracy: %s / value: %s", sensorEvent.accuracy, sensorEvent.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "accuracy changed: " + i);
    }

    @Override
    public void onPeerConnected(Node node) {
        nodeId = node.getId();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        nodeId = null;
    }
}
