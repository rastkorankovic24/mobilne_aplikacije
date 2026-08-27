package com.example.popravni;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvLocation;
    private CheckBox checkBoxRole;
    private Button btnSnap;
    private ImageView ivPhoto;

    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private Uri photoUri;
    private File photoFile;
    private int checkCount = 0;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        checkBoxRole = findViewById(R.id.checkBoxRole);
        btnSnap = findViewById(R.id.btnSnap);
        ivPhoto = findViewById(R.id.ivPhoto);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean locGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean camGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
            if (locGranted != null && locGranted) {
                getLastLocation();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                ivPhoto.setImageURI(photoUri);
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }
            } else {
                if (gyroscope != null) {
                    sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        });

        checkPermissions();

        btnSnap.setOnClickListener(v -> dispatchTakePictureIntent());

        checkBoxRole.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkCount++;
                fetchRole(checkCount);
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
            });
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompatCheck()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    String locText = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
                    tvLocation.setText(locText);
                } else {
                    tvLocation.setText("Lokacija nije dostupna");
                }
            });
        }
    }

    private boolean ActivityCompatCheck() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            photoFile = null;
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getCacheDir();
        return File.createTempFile("JPEG_" + System.currentTimeMillis() + "_", ".jpg", storageDir);
    }

    private void fetchRole(int id) {
        ApiService apiService = ApiClient.getApiService();
        Call<Role> call = apiService.getRoleById(id);
        call.enqueue(new Callback<Role>() {
            @Override
            public void onResponse(Call<Role> call, Response<Role> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Role role = response.body();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Uloga", role.getName());
                    editor.apply();
                }
            }

            @Override
            public void onFailure(Call<Role> call, Throwable t) {
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            Toast.makeText(this, "X: " + x + ", Y: " + y + ", Z: " + z, Toast.LENGTH_SHORT).show();
            sensorManager.unregisterListener(this, accelerometer);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            btnSnap.setText("Gyro: " + x + ", " + y + ", " + z);
            sensorManager.unregisterListener(this, gyroscope);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}