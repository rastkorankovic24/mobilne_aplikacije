package com.example.kolokvijum2a;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
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

    private TextView textViewLocation;
    private EditText editTextSearch;
    private ImageButton imageButtonCamera;
    private ImageView imageViewPhoto;
    private Switch switchProducts;
    private Button buttonAction;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private float gyroX;
    private float gyroY;
    private float gyroZ;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseHelper dbHelper;
    private ProductService productService;

    private Uri photoUri;
    private File photoFile;

    private static final String PREFS_NAME = "Kolokvijum2APrefs";
    private static final String PREFS_KEY_LOKACIJA = "lokacija";
    private static final String CHANNEL_ID = "products_channel";
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.kolokvijum2a.fileprovider";
    private static final int PRODUCTS_TO_SAVE = 12;

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

    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showFirstContactInTextView();
                } else {
                    Toast.makeText(this, R.string.contacts_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    imageViewPhoto.setImageURI(photoUri);
                    Toast.makeText(this,
                            getString(R.string.gyro_format, gyroX, gyroY, gyroZ),
                            Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewLocation = findViewById(R.id.textViewLocation);
        editTextSearch = findViewById(R.id.editTextSearch);
        imageButtonCamera = findViewById(R.id.imageButtonCamera);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        switchProducts = findViewById(R.id.switchProducts);
        buttonAction = findViewById(R.id.buttonAction);

        dbHelper = new DatabaseHelper(this);
        productService = RetrofitClient.getProductService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        createNotificationChannel();
        requestNotificationPermission();
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

        switchProducts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                handleSwitchOn();
            } else {
                handleSwitchOff();
            }
        });

        buttonAction.setOnClickListener(v -> handleButtonClick());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // --- Lokacija u TextView ---

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void loadLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        textViewLocation.setText(R.string.location_loading);

        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        showLocation(location);
                    } else {
                        requestSingleLocationUpdate();
                    }
                })
                .addOnFailureListener(e -> requestSingleLocationUpdate());
    }

    private void requestSingleLocationUpdate() {
        if (!hasLocationPermission()) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
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
                    Toast.makeText(MainActivity.this, R.string.location_emulator_hint, Toast.LENGTH_LONG).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void showLocation(Location location) {
        textViewLocation.setText(getString(
                R.string.location_format,
                location.getLatitude(),
                location.getLongitude()));
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // --- Kamera + žiroskop Toast ---

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

    // --- Switch ON: učitaj 12 proizvoda ili prikaži prvi naslov ---

    private void handleSwitchOn() {
        if (hasLocationPermission()) {
            loadLocation();
        }

        if (dbHelper.getProductCount() == 0) {
            Toast.makeText(this, R.string.products_loading, Toast.LENGTH_SHORT).show();
            fetchAndSaveProducts();
        } else {
            String title = dbHelper.getFirstProductTitle();
            if (title != null) {
                Toast.makeText(this, title, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchAndSaveProducts() {
        productService.getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Product> products = response.body();
                    int count = Math.min(products.size(), PRODUCTS_TO_SAVE);
                    dbHelper.insertProducts(products.subList(0, count));
                    Toast.makeText(MainActivity.this, getString(R.string.products_loaded, count), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.products_fetch_error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.products_fetch_error_detail, t.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Switch OFF: sačuvaj lokaciju, prikaži prvi kontakt ---

    private void handleSwitchOff() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(PREFS_KEY_LOKACIJA, textViewLocation.getText().toString()).apply();
        Toast.makeText(this, R.string.location_saved_prefs, Toast.LENGTH_SHORT).show();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            showFirstContactInTextView();
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void showFirstContactInTextView() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                null,
                null,
                ContactsContract.Contacts._ID + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            textViewLocation.setText(name);
            cursor.close();
        } else {
            textViewLocation.setText(R.string.no_contact_found);
            Toast.makeText(this, R.string.contacts_emulator_hint, Toast.LENGTH_LONG).show();
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // --- Dugme: pretraga + brisanje prvog proizvoda ---

    private void handleButtonClick() {
        if (dbHelper.getProductCount() == 0) {
            sendNoProductsNotification();
            Toast.makeText(this, R.string.no_products_notification, Toast.LENGTH_LONG).show();
            return;
        }

        String searchText = editTextSearch.getText().toString();
        int count = dbHelper.countProductsByTitle(searchText);
        Toast.makeText(this, getString(R.string.search_count, count), Toast.LENGTH_SHORT).show();

        dbHelper.deleteFirstProduct();

        if (dbHelper.getProductCount() == 0) {
            sendNoProductsNotification();
            Toast.makeText(this, R.string.no_products_notification, Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_products),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNoProductsNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.no_products_notification))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, builder.build());
        }
    }

    // --- Akcelerometar na dugmetu, žiroskop za Toast ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            buttonAction.setText(getString(R.string.accel_format, x, y, z));
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        if (switchProducts.isChecked() && hasLocationPermission()) {
            loadLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopLocationUpdates();
    }
}
