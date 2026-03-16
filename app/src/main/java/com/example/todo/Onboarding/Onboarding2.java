package com.example.todo.Onboarding;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Onboarding2 extends Fragment {

    private LinearLayout loginPanel;
    private LinearLayout registerPanel;

    private TextInputLayout loginEmailLayout;
    private TextInputLayout loginPasswordLayout;
    private TextInputEditText loginEmailInput;
    private TextInputEditText loginPasswordInput;
    private Button loginButton;
    private ProgressBar loginProgress;

    private TextInputLayout registerNickLayout;
    private TextInputLayout registerEmailLayout;
    private TextInputLayout registerPasswordLayout;
    private TextInputLayout registerPasswordConfirmLayout;
    private TextInputEditText registerNickInput;
    private TextInputEditText registerEmailInput;
    private TextInputEditText registerPasswordInput;
    private TextInputEditText registerPasswordConfirmInput;
    private Button registerButton;
    private ProgressBar registerProgress;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_onboarding2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loginPanel = view.findViewById(R.id.loginPanel);
        registerPanel = view.findViewById(R.id.registerPanel);

        loginEmailLayout = view.findViewById(R.id.loginEmailLayout);
        loginPasswordLayout = view.findViewById(R.id.loginPasswordLayout);
        loginEmailInput = view.findViewById(R.id.loginEmailInput);
        loginPasswordInput = view.findViewById(R.id.loginPasswordInput);
        loginButton = view.findViewById(R.id.loginButton);
        loginProgress = view.findViewById(R.id.loginProgress);

        registerNickLayout = view.findViewById(R.id.registerNickLayout);
        registerEmailLayout = view.findViewById(R.id.registerEmailLayout);
        registerPasswordLayout = view.findViewById(R.id.registerPasswordLayout);
        registerPasswordConfirmLayout = view.findViewById(R.id.registerPasswordConfirmLayout);
        registerNickInput = view.findViewById(R.id.registerNickInput);
        registerEmailInput = view.findViewById(R.id.registerEmailInput);
        registerPasswordInput = view.findViewById(R.id.registerPasswordInput);
        registerPasswordConfirmInput = view.findViewById(R.id.registerPasswordConfirmInput);
        registerButton = view.findViewById(R.id.registerButton);
        registerProgress = view.findViewById(R.id.registerProgress);

        TextView switchToRegister = view.findViewById(R.id.switchToRegister);
        TextView switchToLogin = view.findViewById(R.id.switchToLogin);

        applyHintColors();

        loginButton.setOnClickListener(v -> handleLogin());
        registerButton.setOnClickListener(v -> handleRegister());

        switchToRegister.setOnClickListener(v -> {
            loginPanel.setVisibility(View.GONE);
            registerPanel.setVisibility(View.VISIBLE);
        });

        switchToLogin.setOnClickListener(v -> {
            registerPanel.setVisibility(View.GONE);
            loginPanel.setVisibility(View.VISIBLE);
        });
    }

    private void applyHintColors() {
        ColorStateList hintColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.brown_medium));

        loginEmailLayout.setDefaultHintTextColor(hintColor);
        loginPasswordLayout.setDefaultHintTextColor(hintColor);
        registerNickLayout.setDefaultHintTextColor(hintColor);
        registerEmailLayout.setDefaultHintTextColor(hintColor);
        registerPasswordLayout.setDefaultHintTextColor(hintColor);
        registerPasswordConfirmLayout.setDefaultHintTextColor(hintColor);
    }

    private void setLoginLoading(boolean loading) {
        loginButton.setText(loading ? "" : getString(R.string.login_button));
        loginButton.setEnabled(!loading);
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setRegisterLoading(boolean loading) {
        registerButton.setText(loading ? "" : getString(R.string.register_button));
        registerButton.setEnabled(!loading);
        registerProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void clearLoginFields() {
        loginEmailInput.setText("");
        loginPasswordInput.setText("");
    }

    private void clearRegisterFields() {
        registerNickInput.setText("");
        registerEmailInput.setText("");
        registerPasswordInput.setText("");
        registerPasswordConfirmInput.setText("");
    }

    private void clearLoginErrors() {
        loginEmailLayout.setError(null);
        loginPasswordLayout.setError(null);
    }

    private void clearRegisterErrors() {
        registerNickLayout.setError(null);
        registerEmailLayout.setError(null);
        registerPasswordLayout.setError(null);
        registerPasswordConfirmLayout.setError(null);
    }

    private void navigateToOnboarding3() {
        if (getActivity() instanceof OnBoarding) {
            ((OnBoarding) getActivity()).goToAppIntro();
        }
    }

    private void handleLogin() {
        clearLoginErrors();

        String email = loginEmailInput.getText() != null ? loginEmailInput.getText().toString().trim() : "";
        String password = loginPasswordInput.getText() != null ? loginPasswordInput.getText().toString().trim() : "";

        boolean hasError = false;

        if (email.isEmpty()) {
            loginEmailLayout.setError(getString(R.string.error_fill_email));
            hasError = true;
        }

        if (password.isEmpty()) {
            loginPasswordLayout.setError(getString(R.string.error_fill_password));
            hasError = true;
        }

        if (hasError) return;

        setLoginLoading(true);

        Login login = new Login();
        login.loginUser(requireContext(), email, password, error -> {
            setLoginLoading(false);
            if (error != null) {
                loginEmailLayout.setError(error);
            } else {
                clearLoginFields();
                navigateToOnboarding3();
            }
        });
    }

    private void handleRegister() {
        clearRegisterErrors();

        String nick = registerNickInput.getText() != null ? registerNickInput.getText().toString().trim() : "";
        String email = registerEmailInput.getText() != null ? registerEmailInput.getText().toString().trim() : "";
        String password = registerPasswordInput.getText() != null ? registerPasswordInput.getText().toString().trim() : "";
        String confirmPassword = registerPasswordConfirmInput.getText() != null ? registerPasswordConfirmInput.getText().toString().trim() : "";

        boolean hasError = false;

        if (nick.isEmpty()) {
            registerNickLayout.setError(getString(R.string.error_fill_nick));
            hasError = true;
        }

        if (email.isEmpty()) {
            registerEmailLayout.setError(getString(R.string.error_fill_email_register));
            hasError = true;
        }

        if (password.isEmpty()) {
            registerPasswordLayout.setError(getString(R.string.error_fill_password_register));
            hasError = true;
        }

        if (confirmPassword.isEmpty()) {
            registerPasswordConfirmLayout.setError(getString(R.string.error_fill_password_confirm));
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            registerPasswordConfirmLayout.setError(getString(R.string.error_passwords_mismatch));
            hasError = true;
        }

        if (hasError) return;

        setRegisterLoading(true);

        Register register = new Register();
        register.registerUser(requireContext(), email, password, nick, error -> {
            setRegisterLoading(false);
            if (error != null) {
                registerEmailLayout.setError(error);
            } else {
                clearRegisterFields();
                navigateToOnboarding3();
            }
        });
    }
}