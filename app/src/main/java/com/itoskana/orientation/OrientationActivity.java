package com.itoskana.orientation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class OrientationActivity extends Activity {
    private LocationManager location_manager;
    private LocationListener location_listener;
    private TextView tv_latitude;
    private TextView tv_longitude;

    private SensorManager sensor_manager;
    private SensorEventListener sensor_event_listener;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor pressure;

    private float gravity[];
    private float geomagnetic[];
    private TextView tv_azimuth;
    private TextView tv_pitch;
    private TextView tv_roll;
    private TextView tv_altitude;
    private MovingAverage azimuth;
    private MovingAverage pitch;
    private MovingAverage roll;
    private MovingAverage altitude;

    private Button bt_freeze;
    private boolean frozen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);

        // GPS
        tv_latitude = (TextView) findViewById(R.id.TextView_gps_latitude);
        tv_longitude = (TextView) findViewById(R.id.TextView_gps_longitude);

        location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        location_listener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if(frozen == false) {
                    tv_latitude.setText(String.valueOf(location.getLatitude()));
                    tv_longitude.setText(String.valueOf(location.getLongitude()));
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, location_listener);

        // Sensors
        final int ma_window_size = 25;
        azimuth = new MovingAverage(ma_window_size);
        pitch = new MovingAverage(ma_window_size);
        roll = new MovingAverage(ma_window_size);
        altitude = new MovingAverage(ma_window_size);

        tv_azimuth = (TextView) findViewById(R.id.TextView_azimuth);
        tv_pitch = (TextView) findViewById(R.id.TextView_pitch);
        tv_roll = (TextView) findViewById(R.id.TextView_roll);
        tv_altitude = (TextView) findViewById(R.id.TextView_altitude);

        sensor_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensor_manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pressure = sensor_manager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        sensor_event_listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                    altitude.input(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]));

                    if(frozen == false)
                        tv_altitude.setText(String.valueOf(altitude.getAverage()));
                }

                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    gravity = event.values;

                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    geomagnetic = event.values;

                if(gravity != null && geomagnetic != null) {
                    float r[] = new float[9];
                    float i[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic);

                    if(success == true) {
                        float orientation[] = new float[3];

                        SensorManager.getOrientation(r, orientation);
                        azimuth.input(orientation[0]);
                        pitch.input(orientation[1]);
                        roll.input(orientation[2]);

                        if(frozen == false) {
                            tv_azimuth.setText(String.valueOf((azimuth.getAverage() * 180 / Math.PI) + 180));
                            tv_pitch.setText(String.valueOf(pitch.getAverage() * 180 / Math.PI));
                            tv_roll.setText(String.valueOf(roll.getAverage() * 180 / Math.PI));
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // Freeze / Unfreeze button
        frozen = false;
        bt_freeze = (Button) findViewById(R.id.Button_freeze);
        bt_freeze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frozen = !frozen;

                if(frozen == false)
                    bt_freeze.setText(getString(R.string.freeze));

                else
                    bt_freeze.setText(getString(R.string.unfreeze));
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Open location dialog if GPS off
        if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
            final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this);
            dialog_builder.setMessage(getString(R.string.enable_gps))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });

            final AlertDialog alert = dialog_builder.create();
            alert.show();
        }

        // Register sensor listeners
        sensor_manager.registerListener(sensor_event_listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensor_manager.registerListener(sensor_event_listener, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensor_manager.registerListener(sensor_event_listener, pressure, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        sensor_manager.unregisterListener(sensor_event_listener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        location_manager.removeUpdates(location_listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }
}
