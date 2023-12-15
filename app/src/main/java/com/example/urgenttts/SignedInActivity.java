package com.example.urgenttts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignedInActivity extends AppCompatActivity {

    private Button btnToggleLocationUpdates;
    private boolean isLocationServiceRunning = false;

    private Handler flashingHandler = new Handler();
    private boolean isRed = false; // To keep track of color state

    private MediaPlayer mediaPlayer;

    private Button signOutButton;
    private Button userInfoButton;
    private GoogleSignInClient mGoogleSignInClient;

    private TextView userNameTextView;
    private TextView userEmailTextView;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        userNameTextView = findViewById(R.id.user_name);
        //userEmailTextView = findViewById(R.id.user_email);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userNameTextView.setText(user.getDisplayName());
            //userEmailTextView.setText(user.getEmail());
            // You can also set user's photo by getting the photo URL from user.getPhotoUrl()
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signOutButton = findViewById(R.id.sign_out_button);
        userInfoButton = findViewById(R.id.user_info_button);

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        userInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), activity_user_info.class);
                startActivity(intent);
            }
        });

        Button emergencyButton = findViewById(R.id.emergencyButton);
        emergencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmergencySMS();
            }
        });

        Button alarmButton = findViewById(R.id.alarmButton); // Replace with your actual button ID
        alarmButton.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                stopAlarmSound();
            } else {
                playAlarmSound();
            }
        });



        btnToggleLocationUpdates = findViewById(R.id.btnToggleLocationUpdates);
        btnToggleLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLocationServiceRunning) {
                    cancelLocationUpdates();
                    isLocationServiceRunning = false;
                    btnToggleLocationUpdates.setText("SMS Location Sharing Off");
                    btnToggleLocationUpdates.setBackgroundColor(ContextCompat.getColor(SignedInActivity.this, R.color.red));
                    Log.d("AAAAAAAAAAAAAAAAAAAA", "WE STOPPED IT");
                } else {
                    scheduleLocationUpdates();
                    isLocationServiceRunning = true;
                    btnToggleLocationUpdates.setText("SMS Location Sharing On");
                    btnToggleLocationUpdates.setBackgroundColor(ContextCompat.getColor(SignedInActivity.this, R.color.green));
                    Log.d("AAAAAAAAAAAAAAAAAAAA", "WE LAUNCHED IT");
                }
            }
        });

    }

    private void playAlarmSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.security_alarm); // Make sure your sound file is in the raw folder
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.setLooping(true); // Loop the sound
            mediaPlayer.start();
            startFlashing();
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            stopFlashing();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendEmergencySMS() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            if (isNetworkAvailable()) {
                fetchNameAndSendSMSFromFirestore();
            } else {
                sendSMSWithLocalData();
            }
        } else {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNameAndSendSMSFromFirestore() {
        Log.d("Debugging", "FirestoreSMS accessed");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("FullName");
                            String emergencyContactPhone = documentSnapshot.getString("EmergencyContactPhone");
                            fetchLocationAndSendSMS(fullName, emergencyContactPhone);
                        } else {
                            // Handle case where user data is not in Firestore
                            fetchLocationAndSendSMS("Unknown", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle Firestore fetch failure
                        fetchLocationAndSendSMS("Unknown", null);
                    });
        }
    }

    private void sendSMSWithLocalData() {
        Log.d("Debugging", "Local SMS accessed");
        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        String fullName = sharedPreferences.getString("FullName", "Unknown");
        String emergencyContactPhone = sharedPreferences.getString("EmergencyContactPhone", "");
        fetchLocationAndSendSMS(fullName, emergencyContactPhone);
    }

    private void fetchLocationAndSendSMS(String fullName, String emergencyContactPhone) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            return;
        }

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && emergencyContactPhone != null && !emergencyContactPhone.isEmpty()) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String message = "Emergency! This is a message from " + fullName + ". I need help. My current location is : http://maps.google.com/?q=" + latitude + "," + longitude;
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(emergencyContactPhone, null, message, null, null);
                Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show();
            } else {
                // Handle case where location or emergency contact phone is null
                Toast.makeText(this, "Failed to get location or emergency contact", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            // Handle failure to get location
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
        });
    }


  /*  private void sendSMS(String fullName) {
        String emergencyContactPhone = getSharedPreferences("UserDetails", MODE_PRIVATE).getString("EmergencyContactPhone", "");
        String message = "Emergency! This is a message from " + fullName + ". I need help.";

        if (!emergencyContactPhone.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyContactPhone, null, message, null, null);
            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Emergency contact not set", Toast.LENGTH_SHORT).show();
        }
    } */


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "SMS permission is required for this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void signOut() {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                task -> {
                    // Google Sign Out was successful, update UI appropriately
                    Toast.makeText(SignedInActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
                    redirectToSignIn();
                });
    }

    private void redirectToSignIn() {
        Intent intent = new Intent(SignedInActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private Runnable flashingRunnable = new Runnable() {
        @Override
        public void run() {
            // Assuming you have a layout with the id `layout_alarm`
            View layout = findViewById(R.id.layout_home);

            if (isRed) {
                layout.setBackgroundColor(ContextCompat.getColor(SignedInActivity.this, R.color.flashing_white));
            } else {
                layout.setBackgroundColor(ContextCompat.getColor(SignedInActivity.this, R.color.flashing_red));
            }
            isRed = !isRed; // Toggle the color state

            // Schedule the next color change
            flashingHandler.postDelayed(this, 500); // Changes color every 500 milliseconds
        }
    };

    private void startFlashing() {
        flashingHandler.post(flashingRunnable);
    }

    private void stopFlashing() {
        flashingHandler.removeCallbacks(flashingRunnable);
        View layout = findViewById(R.id.layout_home);
        layout.setBackgroundColor(ContextCompat.getColor(SignedInActivity.this, R.color.flashing_white));

    }

    private void scheduleLocationUpdates() {
        Intent intent = new Intent(getApplicationContext(), LocationUpdateService.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;

        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, flags);

        long interval = 30 * 1000; // 15 minutes in milliseconds

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        }
    }

    private void cancelLocationUpdates() {
        Intent intent = new Intent(getApplicationContext(), LocationUpdateService.class);
        // The flags should match the PendingIntent used to start the service.
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, flags);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }







}
