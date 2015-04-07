package org.slamon.android.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.slamon.android.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.slamon.Agent.ConnectionState;
import static org.slamon.android.SLAMonAndroidAgentService.IntentConstants.*;

/**
 * SLAMon Android Agent application base activity.
 */

public class SlamonActivity extends Activity {

    private TextView textView;
    private RelativeLayout backgroundView;
    private int animationDuration;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private LocalBroadcastManager broadcastManager;
    private IntentFilter filter = new SLAMonIntentFilter();
    private BroadcastReceiver receiver = new SLAMonBroadcastReceiver();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slamon_main);

        // Get views by id and relevant variables for further use
        backgroundView = (RelativeLayout) findViewById(R.id.background_layout);
        textView = (TextView) findViewById(R.id.debug_text_window);
        textView.setAlpha(0f);
        textView.setMovementMethod(new ScrollingMovementMethod());
        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Create filter for receiving broadcasted Intents and instantiate broadcaster
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        broadcastManager.unregisterReceiver(receiver);
        super.onDestroy();
    }

    // TextView visibility toggle
    public void toggleDebugWindow(View view) {
        float alpha = textView.getAlpha();
        if (alpha == 0f) {
            textView.animate().alpha(1f).setDuration(animationDuration);
        } else {
            textView.animate().alpha(0f).setDuration(animationDuration);
        }
    }

    // Update TextView text
    private void updateTextView(final String message, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String formattedString = String.format("%s - %s\n", dateFormat.format(new Date().getTime()), message);
                SpannableString string = new SpannableString(formattedString);
                string.setSpan(new ForegroundColorSpan(color), 0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.append(string);
            }
        });
    }

    // Update background image
    private void updateBackground(final ConnectionState state) {
        runOnUiThread(new Runnable() {
            Drawable background;

            @Override
            public void run() {
                switch (state) {
                    case DISCONNECTED:
                    case CONNECTING:
                        background = getResources().getDrawable(R.drawable.slamon_android_disconnected);
                        backgroundView.setBackground(background);
                        break;
                    case CONNECTED:
                        background = getResources().getDrawable(R.drawable.slamon_android_connected);
                        backgroundView.setBackground(background);
                        break;
                }
            }
        });
    }

    /**
     * Custom broadcast receiver class that handles SLAMon Android Agent Service event Intents
     */
    class SLAMonBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case CONNECTED_TO_AFM:
                    long returnTime = intent.getLongExtra("returnTime", 5000);
                    int secondsUntil = Math.round((float) (returnTime - new Date().getTime()) / 1000);
                    updateTextView(String.format("Connected to AFM. Next connection attempt after %d seconds",
                            secondsUntil), Color.WHITE);
                    break;
                case CONNECTION_STATE_CHANGE:
                    ConnectionState state = ConnectionState.valueOf(intent.getStringExtra("status"));
                    switch (state) {
                        case DISCONNECTED:
                            updateTextView("Disconnected from Agent Fleet Manager", Color.RED);
                            break;
                        case CONNECTING:
                            updateTextView("Attempting to connect to Agent Fleet Manager", Color.WHITE);
                            break;
                        case CONNECTED:
                            updateTextView("Connected to Agent Fleet Manager", Color.WHITE);
                            break;
                    }
                    updateBackground(state);
                    break;
                case FATAL_ERROR:
                    updateTextView(String.format("The Agent encountered a fatal error: %s",
                            intent.getStringExtra("message")), Color.RED);
                    break;
                case TEMPORARY_ERROR:
                    updateTextView(String.format("The Agent encountered a temporary error: %s",
                            intent.getStringExtra("message")), Color.RED);
                    break;
                case TASK_STARTED:
                    updateTextView("The Agent has started executing a task", Color.WHITE);
                    break;
                case TASK_COMPLETED:
                    updateTextView("The Agent successfully completed a task", Color.GREEN);
                    break;
                case TASK_ERROR:
                    updateTextView(String.format("The Agent completed a task with error: %s",
                            intent.getStringExtra("message")), Color.MAGENTA);
                    break;
                case PUSH_NOTIFICATION:
                    updateTextView(intent.getStringExtra("message"), Color.CYAN);
                    break;
            }
        }
    }

    class SLAMonIntentFilter extends IntentFilter {
        public SLAMonIntentFilter() {
            this.addAction(CONNECTED_TO_AFM);
            this.addAction(CONNECTION_STATE_CHANGE);
            this.addAction(FATAL_ERROR);
            this.addAction(TEMPORARY_ERROR);
            this.addAction(TASK_STARTED);
            this.addAction(TASK_COMPLETED);
            this.addAction(TASK_ERROR);
            this.addAction(PUSH_NOTIFICATION);
        }
    }
}
