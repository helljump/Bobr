package ru.zipta.bobr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private FloatingActionButton fab;
    private boolean running = false;
    private Timer timer = new Timer();
    private TickTask tickTask;
    private EventBus eventBus = EventBus.getDefault();
    private ArrayList<Job> jobs = new ArrayList<>();
    private Ringtone ringtone;
    private MyNumberPicker work_min;
    private MyNumberPicker work_sec;
    private MyNumberPicker pause_min;
    private MyNumberPicker pause_sec;
    private MyNumberPicker repeats;
    private TextView countdown_tv;
    private View main_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        main_layout = findViewById(R.id.main_layout);

        work_min = (MyNumberPicker) findViewById(R.id.work_min_np);
        work_sec = (MyNumberPicker) findViewById(R.id.work_sec_np);
        pause_min = (MyNumberPicker) findViewById(R.id.pause_min_np);
        pause_sec = (MyNumberPicker) findViewById(R.id.pause_sec_np);
        repeats = (MyNumberPicker) findViewById(R.id.times_np);

        countdown_tv = (TextView) findViewById(R.id.countdown_tv);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        updateFAB();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (!running) {
                        running = true;
                        jobs.clear();
                        int times = repeats.getValue();
                        int longs = work_min.getValue() * 60 + work_sec.getValue();
                        int pause = pause_min.getValue() * 60 + pause_sec.getValue();
                        savePrefs();
                        Job j;
                        for (int i = 0; i < times; i++) {
                            j = new Job(JobType.PAUSE, pause);
                            jobs.add(j);
                            j = new Job(JobType.WORK, longs);
                            jobs.add(j);
                        }
                        tickTask = new TickTask();
                        timer.schedule(tickTask, 1000, 1000);
                    } else {
                        tickTask.cancel();
                        running = false;
                    }
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
        try {
            repeats.setValue(preference.getInt("repeat", 7));
            work_min.setValue(preference.getInt("work_min", 1));
            work_sec.setValue(preference.getInt("work_sec", 0));
            pause_min.setValue(preference.getInt("pause_min", 0));
            pause_sec.setValue(preference.getInt("pause_sec", 3));
        }catch(ClassCastException ex){
            //nop
        }
    }

    @Override
    protected void onPause() {
        savePrefs();
        if(tickTask != null) {
            tickTask.cancel();
        }
        running = false;
        updateFAB();
        super.onPause();
    }

    private void savePrefs() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("repeat", repeats.getValue());
        editor.putInt("work_min", work_min.getValue());
        editor.putInt("work_sec", work_sec.getValue());
        editor.putInt("pause_min", pause_min.getValue());
        editor.putInt("pause_sec", pause_sec.getValue());
        editor.apply();
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
            if(!jobs.isEmpty()){
                Job tt = jobs.get(0);
                switch (tt.jobType){
                    case PAUSE:
                        main_layout.setBackgroundColor(Color.YELLOW);
                        break;
                    case WORK:
                        main_layout.setBackgroundColor(Color.GREEN);
                        break;
                }
            }
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
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            main_layout.setBackgroundColor(Color.GREEN);
            pd = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause);
        } else {
            pd = ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play);
            main_layout.setBackgroundColor(Color.WHITE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        fab.setImageDrawable(pd);
    }

    public class TickEvent {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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
