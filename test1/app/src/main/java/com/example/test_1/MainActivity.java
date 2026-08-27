package com.example.test_1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    // UI elementi
    private CheckBox cbStopAudio, cbFetchCountries;
    private Button btnSnimi;
    private TextView tvLocation, tvProximity;

    // Hardver i senzori
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor proximitySensor;

    // Audio snimanje
    private MediaRecorder mediaRecorder;
    private String audioFilePath;

    // Baza i Mreža
    private AppDatabase db;
    private ApiService apiService;
    private boolean isFirstCheck = true; // Prati da li je prvi put čekiran drugi CheckBox

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicijalizacija UI komponenti
        cbStopAudio = findViewById(R.id.cbStopAudio);
        cbFetchCountries = findViewById(R.id.cbFetchCountries);
        btnSnimi = findViewById(R.id.btnSnimi);
        tvLocation = findViewById(R.id.tvLocation);
        tvProximity = findViewById(R.id.tvProximity);

        // Inicijalizacija baze i Retrofit-a
        db = AppDatabase.getInstance(this);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app.beeceptor.com/") // Obavezno kosi crta na kraju baznog URL-a
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Putanja za čuvanje zvuka u files direktorijumu aplikacije
        audioFilePath = getFilesDir().getAbsolutePath() + "/snimak.3gp";

        // Traženje permisija pri pokretanju
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        }, 100);

        // --- LOKACIJA (Tačka 3) ---
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        }

        // --- PROXIMITY SENZOR (Tačka 8) ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        // --- SNIMANJE ZVUKA KLIČNOM NA DUGME (Tačka 4) ---
        btnSnimi.setOnClickListener(v -> {
            startRecording();
            btnSnimi.setEnabled(false);
            cbStopAudio.setChecked(false); // Resetujemo checkbox ako je bio čekiran
        });

        // --- ZAUSTAVLJANJE ZVUKA PREKO CHECKBOX-A (Tačka 4) ---
        cbStopAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                stopRecording();
                btnSnimi.setEnabled(true);
            }
        });

        // --- RETROFIT I ROOM PREKO DRUGOG CHECKBOX-A (Tačka 6 i 7) ---
        cbFetchCountries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (isFirstCheck) {
                    // Tačka 6: Prvi put preuzmi sa API-ja i spasi u bazu
                    fetchCountriesFromApi();
                    isFirstCheck = false;
                } else {
                    // Tačka 7: Svaki naredni put briši poslednju državu i prikaži Toast
                    deleteLastCountryAndToast();
                }
            }
        });
    }

    // --- LOGIKA ZA AUDIO RECORDING ---
    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(this, "Snimanje počelo...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                Toast.makeText(this, "Snimak sačuvan u files direktorijumu", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException stopException) {
                // Rešava potencijalni problem ako se klikne prebrzo
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
    }

    // --- LOGIKA ZA API (Retrofit) ---
    private void fetchCountriesFromApi() {
        apiService.getCountries().enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Country> countries = response.body();
                    db.countryDao().insertAll(countries);
                    Toast.makeText(MainActivity.this, "Države uspešno sačuvane u bazu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Greška sa API-jem!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LOGIKA ZA BRISANJE IZ BAZE ---
    private void deleteLastCountryAndToast() {
        Country lastCountry = db.countryDao().getLastCountry();
        if (lastCountry != null) {
            db.countryDao().deleteById(lastCountry.id);
        }
        int remaining = db.countryDao().getCount();
        Toast.makeText(this, "Ostalo država u bazi: " + remaining, Toast.LENGTH_SHORT).show();
    }

    // --- SENSOR EVENT LISTENER METODE ---
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            tvProximity.setText("Proximity senzor: " + distance + " cm");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- LOCATION LISTENER METODE ---
    @Override
    public void onLocationChanged(@NonNull Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        tvLocation.setText("Lokacija:\nLat: " + lat + "\nLon: " + lon);
    }

    // --- LIFECYCLE ZA SENZORE ---
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
        sensorManager.unregisterListener(this);
    }
}