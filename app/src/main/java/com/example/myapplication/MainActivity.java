package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final String AES_KEY = "aB2#xY8qW4zH9!pD3^sF6gV5rT7@jK1v";
    private static final Key symmetricKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");

    private final String disconnected = "Disconnected!";
    private final String connected = "Connected!";
    private EditText nameEditText, messageEditText;
    private Button connectButton, sendButton;
    private TextView chatTextView;
    private ScrollView chatScrollView;

    private String userName;

    private DatabaseReference databaseReference;

    private ChildEventListener childEventListener;

    private Queue<Message> messageQueue = new LinkedList<>();
    private boolean isConnected = true;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        messageEditText = findViewById(R.id.messageEditText);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        chatTextView = findViewById(R.id.chatTextView);
        chatScrollView = findViewById(R.id.chatScrollView);

        messageEditText.setEnabled(false);
        sendButton.setEnabled(false);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                String userName = dataSnapshot.child("userName").getValue(String.class);
                String message = decrypt(dataSnapshot.child("message").getValue(String.class));
                appendMessageToChat(userName, message);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        cm.registerNetworkCallback(networkRequest,
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        if(!isConnected){
                            Toast.makeText(getApplicationContext(), "Network connected", Toast.LENGTH_SHORT).show();
                        }
                        isConnected = true;
                        while (!messageQueue.isEmpty()) {
                            Message message = messageQueue.poll();
                            writeMessageToDatabase(message.getUserName(), message.getMessage(), success -> {
                                if(success){
                                    messageEditText.setText("");
                                }else{
                                    Toast.makeText(getApplicationContext(),"Error Occurred!", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        if(isConnected){
                            Toast.makeText(getApplicationContext(), "Network disconnected", Toast.LENGTH_LONG).show();
                        }
                        isConnected = false;
                    }
                });

        connectButton.setOnClickListener(view -> {
            String connectButtonText = connectButton.getText().toString();
            if(connectButtonText.equals("Connect")){
                userName = nameEditText.getText().toString();

                if (userName.isEmpty()) return;

                writeMessageToDatabase(userName, connected, success -> {
                    if(success){
                        databaseReference.child("messages").addChildEventListener(childEventListener);
                        nameEditText.setEnabled(false);
                        connectButton.setText("Disconnect");
                        chatTextView.setText("");
                        messageEditText.setEnabled(true);
                        sendButton.setEnabled(true);
                    }else{
                        Toast.makeText(getApplicationContext(),"Error Occurred!", Toast.LENGTH_LONG).show();
                    }
                });
            } else if (connectButtonText.equals("Disconnect")) {
                writeMessageToDatabase(userName, disconnected, success -> {
                    if(success){
                        databaseReference.child("messages").removeEventListener(childEventListener);
                        nameEditText.setText("");
                        nameEditText.setEnabled(true);
                        connectButton.setText("Connect");
                        messageEditText.setEnabled(false);
                        sendButton.setEnabled(false);
                    }else{
                        Toast.makeText(getApplicationContext(),"Error Occurred!", Toast.LENGTH_LONG).show();
                    }
                });

            }

        });

        sendButton.setOnClickListener(view -> {
            String message = messageEditText.getText().toString ();

            if(message.isEmpty()) return;

            writeMessageToDatabase(userName, message, success -> {
                if(success){
                    messageEditText.setText("");
                }else{
                    Toast.makeText(getApplicationContext(),"Error Occurred!", Toast.LENGTH_LONG).show();
                }
            });

        });

        ImageButton logout_btn = findViewById(R.id.logout_button);
        logout_btn.setEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("autoLogin", Context.MODE_PRIVATE);

        logout_btn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("key", 0);
            editor.apply();

            mAuth.signOut();
            Intent redirect_login = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(redirect_login);
        });
    }

    private void writeMessageToDatabase(String userName, String message, MessageWriteCallback callback){
        String encrypted_message = encrypt(message);

        HashMap<String, Object> messageHashMap = new HashMap<>();
        messageHashMap.put("userName", userName);
        messageHashMap.put("message", encrypted_message);

        Date localDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String formattedDateTime = sdf.format(localDate);

        if(isConnected){
            databaseReference.child("messages").child(formattedDateTime).setValue(messageHashMap)
                    .addOnSuccessListener(aVoid -> callback.isSuccess(true))
                    .addOnFailureListener(e -> callback.isSuccess(false));
        }else{
            messageQueue.offer(new Message(userName, message));
            messageEditText.setText("");
            Toast.makeText(getApplicationContext(), "Message queued for sending", Toast.LENGTH_SHORT).show();
        }

    }

    private String encrypt(String message){
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedMessage);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e)
        {
            throw new RuntimeException("Encryption algorithm not available", e);
        }
        catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decrypt(String encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Decryption algorithm not available", e);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private void appendMessageToChat(String userName, String message) {
        String userNameText =  userName + " : ";
        String fullText = userNameText + message;

        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(userNameText);
        int endIndex = startIndex + userNameText.length();
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if(!chatTextView.getText().toString().isEmpty()){
            chatTextView.append("\n");
        }
        chatTextView.append(spannableString + "\n");

        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    interface MessageWriteCallback {
        void isSuccess(boolean success);
    }

    class Message {
        private String userName;
        private String message;

        public Message(String userName, String message) {
            this.userName = userName;
            this.message = message;
        }

        public String getUserName() {
            return userName;
        }

        public String getMessage() {
            return message;
        }
    }
}