package com.example.urgenttts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class chatActivity extends AppCompatActivity {

    private EditText messageInput;
    private Button sendButton;

    private List<Message> messageList;

    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, "testID");

        RecyclerView chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageInput.getText().toString().trim();
                if (!messageText.isEmpty()) {
                    String senderId = "exampleSenderId"; // replace with actual sender ID
                    String receiverId = "exampleReceiverId"; // replace with actual receiver ID
                    long timestamp = System.currentTimeMillis();

                    new SendMessageTask().execute(senderId, receiverId, messageText, String.valueOf(timestamp));
                }
                fetchMessages();
            }
        });
    }

    private class SendMessageTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            try {
                String databaseUrl = "https://android-text-speech-default-rtdb.europe-west1.firebasedatabase.app/messages.json"; // Replace with your Firebase database URL
                URL url = new URL(databaseUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST"); // Use POST for adding new data
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                JSONObject messageJson = new JSONObject();
                messageJson.put("senderId", params[0]);
                messageJson.put("receiverId", params[1]);
                messageJson.put("message", params[2]);
                messageJson.put("timestamp", Long.parseLong(params[3]));

                try (OutputStream os = httpURLConnection.getOutputStream()) {
                    byte[] input = messageJson.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                return httpURLConnection.getResponseCode();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            if (responseCode != null && responseCode == HttpURLConnection.HTTP_OK) {
                Toast.makeText(chatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                messageInput.setText(""); // Clear the input field
            } else {
                Toast.makeText(chatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchMessages() {
        new FetchMessagesTask().execute();
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String databaseUrl = "https://android-text-speech-default-rtdb.europe-west1.firebasedatabase.app/messages.json"; // Replace with your Firebase database URL
                URL url = new URL(databaseUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream is = httpURLConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();
                is.close();
                return stringBuilder.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                // Parse the JSON response and update the RecyclerView
                updateRecyclerViewWithMessages(response);
            }
        }
    }

    private void updateRecyclerViewWithMessages(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            List<Message> messages = new ArrayList<>();
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject jsonMessage = jsonObject.getJSONObject(key);
                String senderId = jsonMessage.getString("senderId");
                String receiverId = jsonMessage.getString("receiverId");
                String messageText = jsonMessage.getString("message");
                long timestamp = jsonMessage.getLong("timestamp");

                Message message = new Message(senderId, receiverId, messageText, timestamp);
                messages.add(message);
            }

            Collections.sort(messages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
            messageList.clear();
            messageList.addAll(messages);
            messageAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
