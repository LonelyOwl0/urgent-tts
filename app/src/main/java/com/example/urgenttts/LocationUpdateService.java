package com.example.urgenttts;


import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.widget.Toast;

public class LocationUpdateService extends Service {
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final String CHANNEL_ID = "LocationUpdateServiceChannel";



    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If location permissions are not granted, stop the service.
            stopSelf();
            return START_NOT_STICKY;
        }


        // Start foreground service with a notification
        Intent notificationIntent = new Intent(this, MainActivity.class);

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service Running")
                .setContentText("Sending location updates")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Fetch user data (e.g., from SharedPreferences)
                SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
                String emergencyContactPhone = sharedPreferences.getString("EmergencyContactPhone", "");

                // Construct the message with the user's location
                String message = "Emergency! I need help. My location: http://maps.google.com/?q=" +
                        location.getLatitude() + "," + location.getLongitude();

                // Send the SMS message
                sendSMS(emergencyContactPhone, message);
            }
        });

        // We want this service to continue running until it is explicitly stopped
        return START_STICKY;
    }

    private void sendSMS(String phoneNo, String message) {
        if (!phoneNo.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
        } else {
            Toast.makeText(this, "Emergency contact phone number is not set.", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);

    }
}
