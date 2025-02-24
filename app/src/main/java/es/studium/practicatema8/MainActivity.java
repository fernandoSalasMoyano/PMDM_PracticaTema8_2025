package es.studium.practicatema8;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private AyudanteBaseDeDatos ayudanteBD;
    private Handler handler;
    private Runnable locationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ayudanteBD = new AyudanteBaseDeDatos(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();

        obtenerUbicacion();

        iniciarActualizacionUbicacion();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        cargarMarcadores();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);

            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                }
            });
        } else {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }


    private void iniciarActualizacionUbicacion() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                obtenerUbicacion();
                // Acuerdate que para probarlo mejor cada minuto
                handler.postDelayed(this, 5 * 60 * 1000); // 5 minutos = 5*60*1000
            }
        };
        handler.post(locationRunnable);
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitud = location.getLatitude();
                double longitud = location.getLongitude();
                int bateria = obtenerBateria();
                Log.d(String.valueOf(this), "Ubicación obtenida: " + latitud + ", " + longitud + ", Batería: " + bateria);
                ayudanteBD.insertarUbicacion(latitud, longitud, bateria);
                Toast.makeText(this, "Ubicación guardada: " + latitud + " " + longitud + " Batería: " + bateria, Toast.LENGTH_LONG).show();
                cargarMarcadores();
            }
            else {
            Log.d(String.valueOf(this), "No se pudo obtener la ubicación");
        }
        });
    }
    private void cargarMarcadores() {
        if (googleMap == null) return;

        googleMap.clear(); // Limpiar el mapa antes de añadir nuevos marcadores
        Cursor cursor = ayudanteBD.obtenerUbicaciones();

        if (cursor.getCount() == 0) {
            Log.d("DEBUG", "No hay ubicaciones en la base de datos.");
            return; // No hay ubicaciones en la base de datos
        }

        while (cursor.moveToNext()) {
            double latitud = cursor.getDouble(1);
            double longitud = cursor.getDouble(2);
            int bateria = cursor.getInt(3);
            LatLng posicion = new LatLng(latitud, longitud);

            // Personalizar el marcador con la imagen
            MarkerOptions marcador = new MarkerOptions()
                    .position(posicion)
                    .title("Batería: " + bateria + "%")
                    .icon(bitmapDescriptorFromVector(R.drawable.marcador));

            googleMap.addMarker(marcador);
        }
        cursor.close();
    }
    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), vectorResId);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false); // Ajustar tamaño
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private int obtenerBateria() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(locationRunnable); // Al cerrar la aplicacion, se eliminan las handler
    }
}
