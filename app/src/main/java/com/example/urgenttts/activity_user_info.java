package com.example.urgenttts;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class activity_user_info extends AppCompatActivity {

    private EditText editTextDateOfBirth;

    private Button saveButton;
    private Calendar calendar;
    private int year, month, day;

    private CountryCodePicker ccp,ccpEmergency;
    private EditText editTextPhoneNumber,editTextEmergencyPhoneNumber;

    private Spinner spinnerBloodType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        editTextDateOfBirth = findViewById(R.id.editTextDateOfBirth);
        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);


        editTextDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(activity_user_info.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                // +1 because January is zero
                                String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                                editTextDateOfBirth.setText(selectedDate);
                            }
                        }, year, month, day);
                datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
                datePickerDialog.show();
            }
        });

        ccp = findViewById(R.id.ccp);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        ccp.registerCarrierNumberEditText(editTextPhoneNumber);
        ccp.setAutoDetectedCountry(true);
        ccp.setNumberAutoFormattingEnabled(true);

        ccpEmergency = findViewById(R.id.ccpEmergency);
        editTextEmergencyPhoneNumber = findViewById(R.id.editTextEmergencyPhoneNumber);
        ccpEmergency.registerCarrierNumberEditText(editTextEmergencyPhoneNumber);
        ccpEmergency.setAutoDetectedCountry(true);
        ccpEmergency.setNumberAutoFormattingEnabled(true);


        spinnerBloodType = findViewById(R.id.spinnerBloodType);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item) {
            @Override
            public boolean isEnabled(int position){
                // Disable the first item (Hint)
                return position != 0;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0) {
                    // Set the hint text color
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(getResources().getStringArray(R.array.blood_types));

        // Apply the adapter to the spinner
        spinnerBloodType.setAdapter(adapter);
        saveButton = findViewById(R.id.buttonSaveInfo);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateInputs()) {
                    Toast.makeText(activity_user_info.this, "Please fill all required fields", Toast.LENGTH_LONG).show();
                    return;
                }
                else {
                    saveUserData();
                    saveUserDataToFirebase();
                    Toast.makeText(activity_user_info.this, "Information saved successfully", Toast.LENGTH_LONG).show();
                }
            }
        });

        loadUserData();


    }

    private boolean validateInputs() {
        boolean allInputsValid = true;

        // Validate Full Name
        EditText fullName = findViewById(R.id.editTextFullName);
        if (fullName.getText().toString().trim().isEmpty()) {
            fullName.setError("Full Name is required");
            allInputsValid = false;
        }

        // Validate Date of Birth
        EditText dob = findViewById(R.id.editTextDateOfBirth);
        if (dob.getText().toString().trim().isEmpty()) {
            dob.setError("Date of Birth is required");
            allInputsValid = false;
        }

        // Validate Phone Number
        EditText phoneNumber = findViewById(R.id.editTextPhoneNumber);
        if (phoneNumber.getText().toString().trim().isEmpty()) {
            phoneNumber.setError("Phone Number is required");
            allInputsValid = false;
        }

        // Validate Emergency Contact Name
        EditText emergencyContactName = findViewById(R.id.editTextEmergencyContactName);
        if (emergencyContactName.getText().toString().trim().isEmpty()) {
            emergencyContactName.setError("Emergency Contact Name is required");
            allInputsValid = false;
        }

        // Validate Emergency Contact Phone
        EditText emergencyContactPhone = findViewById(R.id.editTextEmergencyPhoneNumber);
        if (emergencyContactPhone.getText().toString().trim().isEmpty()) {
            emergencyContactPhone.setError("Emergency Contact Phone is required");
            allInputsValid = false;
        }

        return allInputsValid;
    }

    private void saveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("FullName", ((EditText) findViewById(R.id.editTextFullName)).getText().toString());
        editor.putString("HomeAddress", ((EditText) findViewById(R.id.editTextHomeAddress)).getText().toString());
        editor.putString("DateOfBirth", ((EditText) findViewById(R.id.editTextDateOfBirth)).getText().toString());
        editor.putString("PhoneNumber", ccp.getFullNumberWithPlus());
        editor.putString("EmergencyContactName", ((EditText) findViewById(R.id.editTextEmergencyContactName)).getText().toString());
        editor.putString("EmergencyContactPhone", ccpEmergency.getFullNumberWithPlus());
        editor.putString("BloodType", spinnerBloodType.getSelectedItem().toString());
        editor.putString("KnownAllergies", ((EditText) findViewById(R.id.editTextKnownAllergies)).getText().toString());
        editor.putString("MedicalConditions", ((EditText) findViewById(R.id.editTextMedicalConditions)).getText().toString());

        editor.apply();
    }

    private void loadUserDataFromLocal() {

        Toast.makeText(activity_user_info.this, "Loading Data from Local", Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);

        String fullName = sharedPreferences.getString("FullName", "");
        String homeAddress = sharedPreferences.getString("HomeAddress", "");
        String dateOfBirth = sharedPreferences.getString("DateOfBirth", "");
        String phoneNumber = sharedPreferences.getString("PhoneNumber", "");
        String emergencyContactName = sharedPreferences.getString("EmergencyContactName", "");
        String emergencyContactPhone = sharedPreferences.getString("EmergencyContactPhone", "");
        String knownAllergies = sharedPreferences.getString("KnownAllergies", "");
        String medicalConditions = sharedPreferences.getString("MedicalConditions", "");

        ((EditText) findViewById(R.id.editTextFullName)).setText(fullName);
        ((EditText) findViewById(R.id.editTextHomeAddress)).setText(homeAddress);
        ((EditText) findViewById(R.id.editTextDateOfBirth)).setText(dateOfBirth);
        ccp.setFullNumber(phoneNumber);
        ((EditText) findViewById(R.id.editTextEmergencyContactName)).setText(emergencyContactName);
        ccpEmergency.setFullNumber(emergencyContactPhone);

        // For the Spinner, find the position of the saved value and set it
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerBloodType.getAdapter();
        int spinnerPosition = adapter.getPosition(sharedPreferences.getString("BloodType", ""));
        spinnerBloodType.setSelection(spinnerPosition, true);

        ((EditText) findViewById(R.id.editTextKnownAllergies)).setText(knownAllergies);
        ((EditText) findViewById(R.id.editTextMedicalConditions)).setText(medicalConditions);
    }

    private void saveUserDataToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid(); // Unique ID for the logged-in user

            // Create a map to store user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("FullName", ((EditText) findViewById(R.id.editTextFullName)).getText().toString());
            userData.put("DateOfBirth", ((EditText) findViewById(R.id.editTextDateOfBirth)).getText().toString());
            userData.put("PhoneNumber", ccp.getFullNumberWithPlus());
            userData.put("HomeAddress", ((EditText) findViewById(R.id.editTextHomeAddress)).getText().toString());
            userData.put("EmergencyContactName", ((EditText) findViewById(R.id.editTextEmergencyContactName)).getText().toString());
            userData.put("EmergencyContactPhone", ccpEmergency.getFullNumberWithPlus());
            userData.put("BloodType", spinnerBloodType.getSelectedItem().toString());
            userData.put("KnownAllergies", ((EditText) findViewById(R.id.editTextKnownAllergies)).getText().toString());
            userData.put("MedicalConditions", ((EditText) findViewById(R.id.editTextMedicalConditions)).getText().toString());

            // Save data to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully written!"))
                    .addOnFailureListener(e -> Log.w("Firestore", "Error writing document", e));
        }
    }


    private void loadDataFromFirestore() {
        Toast.makeText(activity_user_info.this, "Loading Data from Firestore", Toast.LENGTH_LONG).show();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Fetch data from document
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                ((EditText) findViewById(R.id.editTextFullName)).setText((String) data.get("FullName"));
                                ((EditText) findViewById(R.id.editTextHomeAddress)).setText((String) data.get("HomeAddress"));
                                ((EditText) findViewById(R.id.editTextDateOfBirth)).setText((String) data.get("DateOfBirth"));
                                ccp.setFullNumber((String) data.get("PhoneNumber"));
                                ((EditText) findViewById(R.id.editTextEmergencyContactName)).setText((String) data.get("EmergencyContactName"));
                                ccpEmergency.setFullNumber((String) data.get("EmergencyContactPhone"));

                                // Spinner selection
                                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerBloodType.getAdapter();
                                int spinnerPosition = adapter.getPosition((String) data.get("BloodType"));
                                spinnerBloodType.setSelection(spinnerPosition, true);

                                ((EditText) findViewById(R.id.editTextKnownAllergies)).setText((String) data.get("KnownAllergies"));
                                ((EditText) findViewById(R.id.editTextMedicalConditions)).setText((String) data.get("MedicalConditions"));
                            }
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    })
                    .addOnFailureListener(e -> Log.w("Firestore", "Error fetching document", e));
        }
    }

    private void loadUserData() {
        if (isNetworkAvailable()) {
            loadDataFromFirestore();
        } else {
            loadUserDataFromLocal();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }





}