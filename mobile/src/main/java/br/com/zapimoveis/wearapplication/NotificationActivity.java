package br.com.zapimoveis.wearapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.widget.TextView;

public class NotificationActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        TextView mTextView = ((TextView) findViewById(R.id.text));

        CharSequence message = getMessageText(getIntent());
        if(message != null) {
            mTextView.setText(message);
        }
    }

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(MainActivity.PlaceholderFragment.EXTRA_VOICE_REPLY);
        }
        return null;
    }
}
