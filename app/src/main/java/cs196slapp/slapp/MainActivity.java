package cs196slapp.slapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TIME_KEY = "xyz.slapp.slapp_android";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    Date currentDate;
    SimpleDateFormat timeFormat, dateFormat;

    private SensorManager sensorManager;
    private Sensor linear_acc;

/*  private ArrayList<Float> logx = new ArrayList<Float>();         //Leftovers for training implementation
    private ArrayList<Float> logy = new ArrayList<Float>();
    private ArrayList<Float> logz = new ArrayList<Float>();
*/
    private float maxx = (float) 0.0, maxy = (float) 0.0, maxz = (float) 0.0;
    private int slapps = 0;
    private boolean slappActive = false, detection = false, training = false;

    private GoogleApiClient mGoogleApiClient;

    private BoxInsetLayout mContainerView;
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
        linear_acc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, linear_acc, SensorManager.SENSOR_DELAY_NORMAL);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Button reset_button = (Button) findViewById(R.id.reset_button);
        Button slapp_toggle = (Button) findViewById(R.id.slapp_toggle);
        Button train_toggle = (Button) findViewById(R.id.training_toggle);
        ImageView logo = (ImageView) findViewById(R.id.logo);

        reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slapps = 0;
                ((TextView)findViewById(R.id.slapp_count)).setText("Slapps: " + slapps);

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
                ((ImageView)findViewById(R.id.logo)).setImageResource(((detection) ? R.drawable.slapp_320_blue : R.drawable.slapp_320_grey));
            }
        });

        train_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                training = !training;
                ((Button)findViewById(R.id.training_toggle)).setText("" + ((training)? "Stop" : "Start") + "training");
            }
        });

        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                detection = !detection;
                ((ImageView)findViewById(R.id.logo)).setImageResource(((detection) ? R.drawable.slapp_320_blue : R.drawable.slapp_320_grey));
                ((Button)findViewById(R.id.slapp_toggle)).setText("Turn slapping " + ((detection) ? "off" : "on"));
            }
        });

        logo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate((long) 50);
                slapps = 0;
                maxx = maxy = maxz = (float)0.0;
                return true;
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event){

        if(detection) {
            if (Math.abs(event.values[2]) > 9.0 && Math.abs(event.values[0]) < 6.0 && Math.abs(event.values[1]) < 6.0){
                if (!slappActive) {
                    slapps++;
                    slappActive = true;
                    ((TextView) findViewById(R.id.slapp_count)).setText("Slapps: " + slapps);
                    sendSlapp();
                    logEvent();
                }
            }else if (event.values[2] <= 9.0 && slappActive) {
                slappActive = false;
                Toast.makeText(this, "Slapp!", Toast.LENGTH_SHORT).show();
                updateDataLayer();
                try {
                    Thread.sleep(500);                               //To allow accelerometer values to normalize.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(Math.abs(event.values[0]) > maxx) {
                ((TextView) findViewById(R.id.x)).setText("X: " + event.values[0]);
                maxx = Math.abs(event.values[0]);
            }
            if(Math.abs(event.values[1]) > maxy) {
                ((TextView) findViewById(R.id.y)).setText("Y: " + event.values[1]);
                maxy = Math.abs(event.values[1]);
            }
            if(Math.abs(event.values[2]) > maxz) {
                ((TextView) findViewById(R.id.z)).setText("Z: " + event.values[2]);
                maxz = Math.abs(event.values[2]);
            }
        }

        if(training){
            if(Math.abs(event.values[0]) > 9.0 || Math.abs(event.values[1]) > 9.0 || Math.abs(event.values[2]) > 9.0){
                //Just in case we need to train Slapp.
            }
        }
    }

    public void onAccuracyChanged(Sensor s, int a){

    }

    public void sendSlapp(){
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
            ((TextView)findViewById(R.id.slapp_count)).setText("Slapps: " + slapps);
        }
    }

    private void updateDataLayer(){
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/time");
        long time = System.currentTimeMillis();
        putDataMapReq.getDataMap().putLong(TIME_KEY, time);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        System.out.println("Data should be updating with this: " + time);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    public void trainSlapp(){
        //Just in case we need to train Slapp.
    }

    public boolean checkSlapp() {
        //Just in case Slapp ever gets trained.
        return true;
    }

    public void onConnectionSuspended (int a) { }

    public void onConnected (Bundle b){ }

    public void onConnectionFailed(ConnectionResult c) {
        Toast.makeText(this, "Connection Failed!", Toast.LENGTH_SHORT);
    }
}
