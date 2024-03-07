package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    Button register_submit;
    TextInputEditText email_input, password_input, password_confirm, full_name_input;
    TextView login_redirect;
    FirebaseAuth mAuth;
    FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        register_submit = (Button) findViewById(R.id.register_submit);
        email_input = (TextInputEditText) findViewById(R.id.email_input);
        password_input = (TextInputEditText) findViewById(R.id.password);
        password_confirm = (TextInputEditText) findViewById(R.id.confirm_password);
        login_redirect = (TextView) findViewById(R.id.redirect_login);
        full_name_input = (TextInputEditText) findViewById(R.id.fullName_input);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        login_redirect.setOnClickListener(v -> {
            Intent redirect_login = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(redirect_login);
        });

        register_submit.setOnClickListener(v -> auth_user());

    }

    private void auth_user() {
        final String email= String.valueOf(email_input.getText());
        final String password = String.valueOf(password_input.getText());
        final String password_confirm_string = String.valueOf(password_confirm.getText());
        final String full_name_string = String.valueOf(full_name_input.getText());

        if(full_name_string.isEmpty()){
            full_name_input.setError("Please enter your full name");
            full_name_input.requestFocus();
        }
        if(email.isEmpty()){
            email_input.setError("Please enter email");
            email_input.requestFocus();
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            email_input.setError("Please enter valid email");
            email_input.requestFocus();
        }
        if(password.isEmpty()){
            password_input.setError("Please enter password");
            password_input.requestFocus();
        }
//        if(password.length() < 8){
//            password_input.setError("Please enter password with at least 8 characters");
//            password_input.requestFocus();
//        }
        if(!password.matches(".*[a-z].*")){
            password_input.setError("Password should contain at least one lowercase letter");
            password_input.requestFocus();
        }
        if(!password.matches(".*[A-Z].*")){
            password_input.setError("Password should contain at least one uppercase letter");
            password_input.requestFocus();
        }
        if(!password.matches(".*\\d.*")){
            password_input.setError("Password should contain at least one number");
            password_input.requestFocus();
        }
        if(!password.matches(".*[!@#$%^&*()_+{}\\[\\]:;<>,.?~\\\\/-].*")){
            password_input.setError("Password should contain at least one special character");
            password_input.requestFocus();
        }
        if(password.length() < 8){
            password_input.setError("Password should be at least 8 characters long");
            password_input.requestFocus();
        }

        if(password_confirm_string.isEmpty()){
            password_confirm.setError("Please repeat password");
            password_confirm.requestFocus();
        }
        else if(!password.equals(password_confirm_string)){
            password_confirm.setError("Passwords do not match");
            password_confirm.requestFocus();
            password_input.setError("Passwords do not match");
            password_input.requestFocus();
        }
        else{
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    send_verification_email();
                    Toast.makeText(getApplicationContext(), "Registration successful. Please check your email address", Toast.LENGTH_SHORT).show();
                    mUser = mAuth.getCurrentUser();
                    send_user_to_next_activity();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> {
                if(e instanceof FirebaseAuthUserCollisionException)
                {
                    email_input.setError("Email already registered");
                    email_input.requestFocus();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Oops! Something went wrong", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void send_verification_email() {
        if(mAuth.getCurrentUser() != null){
            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(task -> {
                if(!task.isSuccessful()){
                    Toast.makeText(getApplicationContext(), "Oops! Failed to send verification email", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void send_user_to_next_activity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }
}