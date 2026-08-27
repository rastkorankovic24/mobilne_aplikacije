package com.example.popravni_kolokvijum_2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvLocation;
    private CheckBox checkBox;
    private Button btnSnimi;
    private ImageView imageView;

    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private int checkBoxCheckCount = 0;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        checkBox = findViewById(R.id.checkBox);
        btnSnimi = findViewById(R.id.btnSnimi);
        imageView = findViewById(R.id.imageView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fineLocationGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false));
                    boolean coarseLocationGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));
                    if (fineLocationGranted || coarseLocationGranted) {
                        fetchLocation();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            imageView.setImageBitmap(imageBitmap);
                            saveImageToCache(imageBitmap);

                            if (accelerometer != null) {
                                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                            }
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        if (gyroscope != null) {
                            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                        }
                    }
                }
        );

        checkAndRequestPermissions();

        btnSnimi.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxCheckCount++;
                fetchRole(checkBoxCheckCount);
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    tvLocation.setText("Lat: " + lat + ", Lon: " + lon);
                } else {
                    tvLocation.setText("Lokacija nije dostupna");
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void saveImageToCache(Bitmap bitmap) {
        File cacheDir = getCacheDir();
        File imageFile = new File(cacheDir, "slika.jpg");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchRole(int id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app.beeceptor.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Role> call = apiService.getRole(id);

        call.enqueue(new Callback<Role>() {
            @Override
            public void onResponse(@NonNull Call<Role> call, @NonNull Response<Role> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Role role = response.body();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Uloga", role.getName() != null ? role.getName() : role.toString());
                    editor.apply();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Role> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Greška pri preuzimanju uloge", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            Toast.makeText(this, "Accel - X: " + x + ", Y: " + y + ", Z: " + z, Toast.LENGTH_SHORT).show();
            sensorManager.unregisterListener(this, accelerometer);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            btnSnimi.setText("Gyro - X: " + x + ", Y: " + y + ", Z: " + z);
            sensorManager.unregisterListener(this, gyroscope);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // Retrofit Interfejs i Model
    public interface ApiService {
        @GET("mock-server/dummy-json/roles/{id}")
        Call<Role> getRole(@Path("id") int id);
    }

    public static class Role {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}