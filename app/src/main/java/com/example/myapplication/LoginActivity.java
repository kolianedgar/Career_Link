package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private long back_press_count;
    private Toast back_toast;
    private int autoSave;
    TextInputEditText Email, Password;
    Button Submit;
    TextView register_button;
    LinearLayout login_layout;
    String email, password;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Email = findViewById(R.id.email_input);
        Password = findViewById(R.id.password);
        Submit = findViewById(R.id.login_submit);
        register_button = (TextView) findViewById(R.id.register);
        login_layout = findViewById(R.id.linearLayout);
        sharedPreferences = getSharedPreferences("autoLogin", Context.MODE_PRIVATE);
        int j = sharedPreferences.getInt("key", 0);

        if(j > 0){
            Intent redirect_homepage = new Intent(getApplicationContext(), HelloActivity.class);
            startActivity(redirect_homepage);
        }
        register_button.setOnClickListener(v -> {
            try{
                Intent redirect_register = new Intent(this, RegisterActivity.class);
                LoginActivity.this.startActivity(redirect_register);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        login_layout.setAlpha(0.8F);

        Submit.setOnClickListener(v -> perform_login());

    }

    private void perform_login() {
        email = String.valueOf(Email.getText());
        password = String.valueOf(Password.getText());

        if(email.isEmpty()){
            Email.setError("Enter email");
            Email.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Email.setError("Email not valid");
            Email.requestFocus();
            return;
        }
        if(password.length() < 8){
            Password.setError("Enter valid password");
            Password.requestFocus();
        }
        else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    mUser = mAuth.getCurrentUser();
                    if(mUser.isEmailVerified()){
                        autoSave = 1;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("key", autoSave);
                        editor.apply();

                        Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_SHORT).show();
                        send_user_to_next_activity();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Please verify your email", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }
    private void send_user_to_next_activity() {
        Intent intent = new Intent(getApplicationContext(), HelloActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if(back_press_count + 2000 > System.currentTimeMillis()){
            back_toast.cancel();
            super.onBackPressed();
            return;
        }
        else{
            back_toast = Toast.makeText(getApplicationContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            back_toast.show();
        }
        back_press_count = System.currentTimeMillis();
    }
}