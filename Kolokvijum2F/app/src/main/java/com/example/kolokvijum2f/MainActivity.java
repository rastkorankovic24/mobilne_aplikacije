package com.example.kolokvijum2f;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private CheckBox checkBoxStopRecording;
    private CheckBox checkBoxCountries;
    private Button buttonSnimi;
    private TextView textViewLocation;
    private TextView textViewProximity;

    private SensorManager sensorManager;
    private Sensor proximitySensor;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private DatabaseHelper dbHelper;
    private CountryService countryService;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private boolean countriesLoaded = false;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    loadLocation();
                } else {
                    textViewLocation.setText(R.string.location_permission_denied);
                }
            });

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    Toast.makeText(this, R.string.audio_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBoxStopRecording = findViewById(R.id.checkBoxStopRecording);
        checkBoxCountries = findViewById(R.id.checkBoxCountries);
        buttonSnimi = findViewById(R.id.buttonSnimi);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewProximity = findViewById(R.id.textViewProximity);

        dbHelper = new DatabaseHelper(this);
        countryService = RetrofitClient.getCountryService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor == null) {
                textViewProximity.setText(R.string.proximity_unavailable);
            }
        }

        requestLocationPermission();
        setupListeners();
    }

    private void setupListeners() {
        buttonSnimi.setOnClickListener(v -> {
            if (isRecording) {
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        checkBoxStopRecording.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                stopRecording();
                checkBoxStopRecording.setChecked(false);
            }
        });

        checkBoxCountries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (!countriesLoaded) {
                fetchAndSaveCountries();
            } else {
                dbHelper.deleteLastCountry();
                int count = dbHelper.getCountryCount();
                Toast.makeText(this, getString(R.string.countries_remaining, count), Toast.LENGTH_SHORT).show();
            }
            checkBoxCountries.setChecked(false);
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void loadLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                showLocation(location);
            } else {
                requestSingleLocationUpdate();
            }
        });
    }

    private void requestSingleLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    showLocation(location);
                } else {
                    textViewLocation.setText(R.string.location_unavailable);
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
                        new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        showLocation(location);
                    } else {
                        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
                    }
                });
    }

    private void showLocation(Location location) {
        textViewLocation.setText(getString(R.string.location_format,
                location.getLatitude(), location.getLongitude()));
    }

    private void startRecording() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            audioFile = new File(getFilesDir(), "audio_" + timeStamp + ".3gp");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            buttonSnimi.setEnabled(false);
            Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.recording_error, Toast.LENGTH_SHORT).show();
            resetRecordingState();
        }
    }

    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            Toast.makeText(this, R.string.recording_saved, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
            Toast.makeText(this, R.string.recording_error, Toast.LENGTH_SHORT).show();
        } finally {
            mediaRecorder = null;
            resetRecordingState();
        }
    }

    private void resetRecordingState() {
        isRecording = false;
        buttonSnimi.setEnabled(true);
    }

    private void fetchAndSaveCountries() {
        Toast.makeText(this, R.string.countries_loading, Toast.LENGTH_SHORT).show();
        countryService.getCountries().enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(@NonNull Call<List<Country>> call, @NonNull Response<List<Country>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    dbHelper.insertCountries(response.body());
                    countriesLoaded = true;
                    Toast.makeText(MainActivity.this,
                            getString(R.string.countries_loaded, response.body().size()),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.countries_fetch_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Country>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, R.string.countries_fetch_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            textViewProximity.setText(getString(R.string.proximity_format, event.values[0]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
