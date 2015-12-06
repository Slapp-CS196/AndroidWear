package cs196slapp.slapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    Date currentDate;
    SimpleDateFormat timeFormat, dateFormat;

    private SensorManager sensorManager;
    private Sensor linear_acc;

    private ArrayList<Float> logx = new ArrayList<Float>();
    private ArrayList<Float> logy = new ArrayList<Float>();
    private ArrayList<Float> logz = new ArrayList<Float>();

    private float maxx = (float) 0.0, maxy = (float) 0.0, maxz = (float) 0.0;
    private int slaps = 0;
    private boolean slappActive = false, detection = true, training = false;


    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set date/time formats and zones
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSSZ");
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));


        //Store current date & time
        currentDate = new Date();

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        linear_acc =sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, linear_acc, SensorManager.SENSOR_DELAY_NORMAL);

        Button reset_button = (Button) findViewById(R.id.reset_button);
        Button slapp_toggle = (Button) findViewById(R.id.slapp_toggle);
        Button train_toggle = (Button) findViewById(R.id.training_toggle);

        reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slaps = 0;
                ((TextView)findViewById(R.id.slapp_count)).setText("Slapps: " + slaps);

                maxx = (float)0.0;
                maxy = (float)0.0;
                maxz = (float)0.0;
            }
        });

        slapp_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detection = !detection;
                ((Button)findViewById(R.id.slapp_toggle)).setText("Turn slapping " + ((detection)?"off" : "on"));
            }
        });

        train_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                training = !training;
                ((Button)findViewById(R.id.training_toggle)).setText("" + ((training)? "Stop" : "Start") + "training");
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event){

        if(detection) {
            if (Math.abs(event.values[2]) > 9.0 || Math.abs(event.values[0]) < 7.0 || Math.abs(event.values[1]) < 7.0){
                if (!slappActive) {
                    slaps++;
                    slappActive = true;
                    ((TextView) findViewById(R.id.slapp_count)).setText("Slapps: " + slaps);
                    sendTestSlapp();
                    logEvent();
                }
            }else if (event.values[2] <= 9.0 && slappActive) {
                slappActive = false;
                Toast.makeText(this, "Slapp!", Toast.LENGTH_SHORT).show();
            }
            if(event.values[0] > maxx)
                ((TextView) findViewById(R.id.x)).setText("X: " + event.values[0]);
            if(event.values[1] > maxy)
                ((TextView) findViewById(R.id.y)).setText("Y: " + event.values[1]);
            if(event.values[2] > maxz)
                ((TextView) findViewById(R.id.z)).setText("Z: " + event.values[2]);
        }

        if(training){
            if(Math.abs(event.values[0]) > 9.0 || Math.abs(event.values[1]) > 9.0 || Math.abs(event.values[2]) > 9.0){

            }
        }
    }

    public void onAccuracyChanged(Sensor s, int a){

    }

    public void sendTestSlapp(){
        //Phone app integration goes here.
    }

    public void logEvent(){

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        sensorManager.unregisterListener(this);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        sensorManager.registerListener(this, linear_acc, SensorManager.SENSOR_DELAY_NORMAL);
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mClockView.setVisibility(View.GONE);
        }
    }

    public void trainSlapp(){

    }

    public boolean checkSlapp() {
        return true;
    }
}
