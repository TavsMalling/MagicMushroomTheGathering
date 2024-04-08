package com.example.magicmushroomthegathering.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.magicmushroomthegathering.R;
import com.example.magicmushroomthegathering.databinding.FragmentMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private IMapController mapController;

    private FragmentMapBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MapViewModel mapViewModel =
                new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(18.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        getStartLocation();

        getDBMarkers();

        view.findViewById(R.id.fab_CreateMarker).setOnClickListener(v -> addMapMarker());
    }

    private void getStartLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.setCenter(startPoint);
            } else {
                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMapMarker() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                com.google.firebase.firestore.GeoPoint firestoreGeoPoint =
                        new com.google.firebase.firestore.GeoPoint(location.getLatitude(), location.getLongitude());

                GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                Marker currentMarker = new Marker(map);
                currentMarker.setPosition(startPoint);
                currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                map.getOverlays().add(currentMarker);
                addToDB(firestoreGeoPoint);
                map.invalidate();
                Toast.makeText(getContext(), "Location:" + startPoint.toDoubleString(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToDB(com.google.firebase.firestore.GeoPoint fireStoreGeoPoint) {
        Map<String, com.google.firebase.firestore.GeoPoint> markerCoordinates = new HashMap<>();
        markerCoordinates.put("coordinates", fireStoreGeoPoint);

        db.collection(getString(R.string.usermarkers)).document(user.getUid()).collection("markers").add(markerCoordinates);
    }

    private void getDBMarkers() {
        db.collection(getString(R.string.usermarkers))
                .document(user.getUid())
                .collection("markers")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            com.google.firebase.firestore.GeoPoint firestoreGeoPoint = document.getGeoPoint("coordinates");

                            if (firestoreGeoPoint != null) {
                                Marker marker = new Marker(map);
                                marker.setPosition(new GeoPoint(firestoreGeoPoint.getLatitude(), firestoreGeoPoint.getLongitude()));
                                map.getOverlays().add(marker);
                            }
                        }
                    }
                    else {
                        Log.d("FirestoreError", "Error getting documents: ", task.getException());
                    }
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}