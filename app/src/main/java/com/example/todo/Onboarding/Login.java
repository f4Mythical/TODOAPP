package com.example.todo.Onboarding;

import android.content.Context;

import com.example.todo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login {

    public interface AuthCallback {
        void onResult(String error);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loginUser(Context context, String emailOrNick, String password, AuthCallback callback) {
        if (emailOrNick.contains("@")) {
            signIn(context, emailOrNick, password, callback);
        } else {
            resolveNickToEmail(context, emailOrNick, password, callback);
        }
    }

    private void resolveNickToEmail(Context context, String nick, String password, AuthCallback callback) {
        db.collection("nicks")
                .whereEqualTo("nick", nick)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String email = querySnapshot.getDocuments().get(0).getString("email");
                        if (email != null) {
                            signIn(context, email, password, callback);
                        } else {
                            callback.onResult(context.getString(R.string.error_login_nick_not_found));
                        }
                    } else {
                        callback.onResult(context.getString(R.string.error_login_nick_no_exist));
                    }
                })
                .addOnFailureListener(e ->
                        callback.onResult(context.getString(R.string.error_generic, e.getMessage())));
    }

    private void signIn(Context context, String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onResult(context.getString(R.string.error_login_failed, e.getMessage())));
    }
}