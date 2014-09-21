package br.com.zapimoveis.wearapplication;

import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private static final String COUNT_KEY = "COUNT_KEY";
        private static final String TAG = PlaceholderFragment.class.getSimpleName();
        private static final String IMAGE_RESOURCE = "IMAGE_RESOURCE";
        public static final int NOTIFICATION_ID = 23456;
        public static final String EXTRA_VOICE_REPLY = "EXTRA_VOICE_REPLY";
        private GoogleApiClient mGoogleApiClient;
        private Button mHelloWorldButton;
        private Button mNotificationButton;
        public PlaceholderFragment() {}

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        private static Asset createAssetFromBitmap(Bitmap bitmap) {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mHelloWorldButton = ((Button) rootView.findViewById(R.id.text));
            mNotificationButton = ((Button) rootView.findViewById(R.id.notification_button));
            mNotificationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendNotification();
                }
            });

            return rootView;
        }

        private void sendNotification() {
            Intent response = new Intent(getActivity(), NotificationActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getActivity(),
                    0,
                    response,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText("Exemplo de notificação big Text.");

            String replyLabel = getResources().getString(R.string.reply_label);
            String[] replyChoices = getResources().getStringArray(R.array.reply_choices);

            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                    .setLabel(replyLabel)
                    .setChoices(replyChoices)
                    .build();

            NotificationCompat.Action voiceAction = new NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_share,
                    "Reply",
                    pendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText("New Notification")
                    .setContentTitle("Notification")
                    .setContentIntent(pendingIntent)
                    .setStyle(bigTextStyle)
                    .setAutoCancel(true);

            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(BitmapFactory.decodeResource(
                    getActivity().getResources(),
                    R.drawable.photo
            ));

            Notification secondPage = new NotificationCompat.WearableExtender()
                    .setHintShowBackgroundOnly(true)
                    .extend(new NotificationCompat.Builder(getActivity())
                            .setStyle(bigPictureStyle))
                    .build();

            Notification fullNotification = new NotificationCompat.WearableExtender()
                    .addPage(secondPage)
                    .addAction(voiceAction)
                    .setBackground(BitmapFactory.decodeResource(
                            getActivity().getResources(),
                            R.drawable.photo
                    ))
                    .extend(builder)
                    .build();

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getActivity());
            managerCompat.notify(NOTIFICATION_ID,fullNotification);

        }

        @Override
        public void onConnected(Bundle bundle) {
            mHelloWorldButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo);
                    Asset asset = createAssetFromBitmap(bitmap);

                    PutDataMapRequest dataMap = PutDataMapRequest.create("/update");
                    dataMap.getDataMap().putLong(COUNT_KEY, SystemClock.currentThreadTimeMillis());
                    dataMap.getDataMap().putAsset(IMAGE_RESOURCE, asset);
                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                            .putDataItem(mGoogleApiClient, dataMap.asPutDataRequest());
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                            }
                        }
                    });
                }
            });
        }

        @Override
        public void onConnectionSuspended(int i) {
            mHelloWorldButton.setOnClickListener(null);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            mHelloWorldButton.setOnClickListener(null);
            Log.d(TAG, "onConnectionFailed: " + connectionResult.toString());
        }
    }
}
