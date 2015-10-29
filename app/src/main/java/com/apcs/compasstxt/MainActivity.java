package com.apcs.compasstxt;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;

import com.hahattpro.meowdebughelper.Mailer;
import com.hahattpro.meowdebughelper.SaveFile;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {
    private SensorManager mSensorManager;
    private int LOGGING_MODE = 1;
    private int STOP_MODE = 0;
    private int MODE = STOP_MODE;

    private LogCompass logCompass;
    @Bind(R.id.mButton)
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        logCompass = new LogCompass();
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mButton.setBackgroundColor(getResources().getColor(R.color.white));

        //wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }

    @OnClick(R.id.mButton)
    void onClickStartLog(View view){
        if (MODE == STOP_MODE) {
            MODE = LOGGING_MODE;
            logCompass.execute();
            mButton.setBackgroundColor(getResources().getColor(R.color.red));

        }
        else {
            MODE = STOP_MODE;
            mButton.setBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MODE = STOP_MODE;
        logCompass.cancel(true);
    }

    private class LogCompass extends AsyncTask<Void,Void,Void>{
        StringBuilder stringBuilder;
        Calendar c;
        String LOG_TAG = "compass";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            stringBuilder = new StringBuilder();
            c = Calendar.getInstance();
            Log.i(LOG_TAG,"start compass");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try{
            while (MODE == LOGGING_MODE){
            Date date = c.getTime();
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            float degree = CompassData.degree;
            String degree_str = String.format("%.1f", degree);
            String timestamp = df.format(date);
            stringBuilder.append(timestamp+"\t"+degree_str+"\n");
               Thread.sleep(CompassData.interval);
            }
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            String result = stringBuilder.toString();
            Log.i(LOG_TAG, "stop compass " + result);
            File file = SaveFile.save("compass.txt", result);
            Mailer.send(MainActivity.this,"your mail here","Compass","compass",file);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
        CompassData.degree = degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
