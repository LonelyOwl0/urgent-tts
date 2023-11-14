package com.example.urgenttts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignedInActivity extends AppCompatActivity {

    private Button signOutButton;
    private GoogleSignInClient mGoogleSignInClient;

    private TextView userNameTextView;
    private TextView userEmailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);

        userNameTextView = findViewById(R.id.user_name);
        userEmailTextView = findViewById(R.id.user_email);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userNameTextView.setText(user.getDisplayName());
            userEmailTextView.setText(user.getEmail());
            // You can also set user's photo by getting the photo URL from user.getPhotoUrl()
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signOutButton = findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }

    public void signOut() {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Google Sign Out was successful, update UI appropriately
                        Toast.makeText(SignedInActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
                        redirectToSignIn();
                    }
                });
    }

    private void redirectToSignIn() {
        Intent intent = new Intent(SignedInActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Add a sign-out button and its logic here if needed
}
