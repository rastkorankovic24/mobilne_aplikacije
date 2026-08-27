package com.example.kp2_2;

import static com.example.kp2_2.R.id.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
    private ImageButton imageButtonCamera;
    private TextView textViewLocation;
    private TextView textViewProximity;
    private ImageView imageViewPhoto;

    private SensorManager sensorManager;
    private Sensor proximitySensor;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Uri photoUri;
    private File photoFile;

    private DatabaseHelper dbHelper;
    private CountryService countryService;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private boolean countriesLoaded = false;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.kp2-2.fileprovider";

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    loadLocation();
                } else {
                    textViewLocation.setText(R.string.location_permission_denied);
                }
            });
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    imageViewPhoto.setImageURI(photoUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBoxStopRecording = findViewById(R.id.checkBoxStopRecording);
        checkBoxCountries = findViewById(R.id.checkBoxCountries);
        imageButtonCamera = findViewById(R.id.imageButtonCamera);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);
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
        imageButtonCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
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

    private void openCamera() {
        try {
            photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Greška pri kreiranju fajla", Toast.LENGTH_SHORT).show();
        }
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
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
