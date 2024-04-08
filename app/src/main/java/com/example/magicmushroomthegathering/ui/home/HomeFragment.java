package com.example.magicmushroomthegathering.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.magicmushroomthegathering.Login;
import com.example.magicmushroomthegathering.R;
import com.example.magicmushroomthegathering.databinding.FragmentHomeBinding;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private TextView userDetails;
    private Button btnLogout;
    private SwitchMaterial swtDarkMode;
    private boolean isUserInitiatedChange = false;

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        btnLogout = view.findViewById(R.id.btnLogout);
        userDetails = view.findViewById(R.id.user_details);
        swtDarkMode = view.findViewById(R.id.darkMode);

        if (user == null) {
            navigateToLogin();
        } else {
            userDetails.setText(user.getEmail());

            db.collection(getString(R.string.usersettings)).document(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {

                    DocumentSnapshot document = task.getResult();
                    Boolean darkTheme = document.getBoolean("darkTheme");

                    isUserInitiatedChange = false;
                    swtDarkMode.setChecked(darkTheme != null && darkTheme);
                    isUserInitiatedChange = true;
                }
            });
        }

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            navigateToLogin();
        });

        swtDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUserInitiatedChange) {
                saveThemePreference(isChecked);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void navigateToLogin() {

        Intent intent = new Intent(getActivity(), Login.class);
        getActivity().startActivity(intent);
        getActivity().finish();
    }

    private void saveThemePreference(boolean isDarkModeEnabled) {
        Map<String, Boolean> userSetting = new HashMap<>();
        userSetting.put("darkTheme", isDarkModeEnabled);

        db.collection(getString(R.string.usersettings)).document(user.getUid())
                .set(userSetting)
                .addOnSuccessListener(aVoid -> {
                    setTheme(isDarkModeEnabled);
                    saveThemePreferenceLocally(isDarkModeEnabled);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save settings", Toast.LENGTH_SHORT).show();
                });
    }

    private void setTheme(boolean darkMode) {
        AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void saveThemePreferenceLocally(boolean isDarkModeEnabled) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("IsDarkMode", isDarkModeEnabled);
        editor.apply();
    }
}