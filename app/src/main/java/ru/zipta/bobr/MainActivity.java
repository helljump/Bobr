package ru.zipta.bobr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getName();
    private FloatingActionButton fab;
    private boolean running = false;
    private Timer timer = new Timer();
    private TickTask tickTask;
    private EventBus eventBus = EventBus.getDefault();
    private ArrayList<Job> jobs = new ArrayList<>();
    private Ringtone ringtone;
    private EditText long_et;
    private EditText pause_et;
    private EditText repeat_et;
    private TextView countdown_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        long_et = (EditText) findViewById(R.id.long_et);
        pause_et = (EditText) findViewById(R.id.pause_et);
        repeat_et = (EditText) findViewById(R.id.repeat_et);
        countdown_tv = (TextView) findViewById(R.id.countdown_tv);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        updateFAB();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (!running) {
                        jobs.clear();
                        int times = Integer.parseInt(repeat_et.getText().toString());
                        String[] spam = long_et.getText().toString().split(":");
                        int longs = Integer.parseInt(spam[0]) * 60 + Integer.parseInt(spam[1]);
                        spam = pause_et.getText().toString().split(":");
                        int pause = Integer.parseInt(spam[0]) * 60 + Integer.parseInt(spam[1]);
                        Job j;
                        for (int i = 0; i < times; i++) {
                            j = new Job(JobType.WORK, longs);
                            jobs.add(j);
                            if (i < times - 1) {
                                j = new Job(JobType.PAUSE, pause);
                                jobs.add(j);
                            }
                        }
                        tickTask = new TickTask();
                        timer.schedule(tickTask, 1000, 1000);
                    } else {
                        tickTask.cancel();
                    }
                    running = !running;
                    updateFAB();
                } catch (NumberFormatException e) {
                    Snackbar.make(view, "Wrong value", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        eventBus.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadRingtone();
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        long_et.setText(preference.getString("long", "1:00"));
        pause_et.setText(preference.getString("pause", "0:05"));
        repeat_et.setText(preference.getString("repeat", "6"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString("long", long_et.getText().toString());
        editor.putString("pause", pause_et.getText().toString());
        editor.putString("repeat", repeat_et.getText().toString());
        editor.apply();
        running = false;
        if(tickTask != null) {
            tickTask.cancel();
        }
        updateFAB();
    }

    private void LoadRingtone() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String strRingtonePreference = preference.getString("notifications_new_message_ringtone", "DEFAULT_SOUND");
        Uri uri = Uri.parse(strRingtonePreference);
        ringtone = RingtoneManager.getRingtone(MainActivity.this, uri);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(TickEvent event) {
        if (jobs.isEmpty()) {
            tickTask.cancel();
            ringtone.play();
            running = false;
            updateFAB();
            Log.d(TAG, "stop");
            return;
        }
        Job t = jobs.get(0);
        t.value--;
        if (t.value == 0) {
            Log.d(TAG, "remove " + t.jobType);
            jobs.remove(t);
            ringtone.play();
        }
        int egg = 0;
        for (Job jj : jobs) {
            egg += jj.value;
        }
        countdown_tv.setText(String.format("%2d:%02d", egg / 60, egg % 60));
    }

    private class TickTask extends TimerTask {
        @Override
        public void run() {
            eventBus.post(new TickEvent());
        }
    }

    private void updateFAB() {
        Drawable pd;
        if (running) {
            pd = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause);
        } else {
            pd = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play);
        }
        fab.setImageDrawable(pd);
    }

    public class TickEvent {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent in = new Intent(this, SettingsActivity.class);
            startActivity(in);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    enum JobType {WORK, PAUSE}

    private class Job {
        private final JobType jobType;
        private int value;

        public Job(JobType jobType, int value) {
            this.jobType = jobType;
            this.value = value;
        }
    }

}
