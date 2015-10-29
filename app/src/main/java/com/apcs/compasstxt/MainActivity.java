package com.apcs.compasstxt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
    private File file = null;

    private LogCompass logCompass;
    @Bind(R.id.mButton) Button mButton;
    @Bind(R.id.mSetting) Button mSetting;
    @Bind(R.id.textView) TextView textView;
    @Bind(R.id.scrollView) ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        logCompass = new LogCompass();
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        mButton.setBackgroundColor(getResources().getColor(R.color.light_blue));


    }

    @OnClick(R.id.mButton)
    void onClickStartLog(View view){
        if (MODE == STOP_MODE) {
            MODE = LOGGING_MODE;
            logCompass = new LogCompass();
            logCompass.execute();
            mButton.setBackgroundColor(getResources().getColor(R.color.red));

        }
        else {
            MODE = STOP_MODE;

            mButton.setBackgroundColor(getResources().getColor(R.color.light_blue));
        }
    }

    @OnClick(R.id.mSetting)
    void onClickSettingButton(View view){
        final String LOG = "popup menu";
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater()
                .inflate(R.menu.popup_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.send){
                    Log.i(LOG,"send");
                    if (file!= null)
                        Mailer.send(MainActivity.this,"your mail here","Compass","compass",file);
                }
                if (id == R.id.setTimer){
                    Log.i(LOG,"set timer");
                    showDialog();
                }
                return true;
            }
        });

        popup.show(); //showing popup menu
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
            stringBuilder.append("Start!! \n");
            c = Calendar.getInstance();
            Log.i(LOG_TAG, "start compass");
            mSetting.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try{
            while (MODE == LOGGING_MODE){
            c = Calendar.getInstance();
            Date date = c.getTime();
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            float degree = CompassData.degree;
            String degree_str = String.format("%.2f", degree);
            String timestamp = df.format(date);
            stringBuilder.append(timestamp+"\t"+degree_str+"\n");
                publishProgress();
                Log.i(LOG_TAG,"sleep for "+CompassData.interval);
               Thread.sleep(CompassData.interval);
            }
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            String result = stringBuilder.toString();
            Log.i(LOG_TAG, "update compass " + result);
            textView.setText(result);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stringBuilder.append("Stop!! \n");
            String result = stringBuilder.toString();
            Log.i(LOG_TAG, "stop compass " + result);
            textView.setText(result);
            file = SaveFile.save("compass.txt", result);
            mSetting.setEnabled(true);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }
    }



    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Timer");
        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CompassData.interval = Integer.parseInt(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
        input.setText(Integer.toString(CompassData.interval));
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
        float degree = event.values[0];
        CompassData.degree = degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
