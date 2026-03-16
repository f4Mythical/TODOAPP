package com.example.todo.Onboarding;

import android.content.Context;

import com.example.todo.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register {

    public interface AuthCallback {
        void onResult(String error);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void registerUser(Context context, String email, String password, String nick, AuthCallback callback) {
        checkNickAvailability(context, email, password, nick, callback);
    }

    private void checkNickAvailability(Context context, String email, String password, String nick, AuthCallback callback) {
        db.collection("nicks")
                .whereEqualTo("nick", nick)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onResult(context.getString(R.string.error_nick_taken));
                    } else {
                        createAccount(context, email, password, nick, callback);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onResult(context.getString(R.string.error_nick_check_failed, e.getMessage())));
    }

    private void createAccount(Context context, String email, String password, String nick, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserData(context, user.getUid(), email, nick, callback);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onResult(context.getString(R.string.error_register_failed, e.getMessage())));
    }

    private void saveUserData(Context context, String uid, String email, String nick, AuthCallback callback) {
        Timestamp now = Timestamp.now();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("createdAt", now);

        Map<String, Object> nickData = new HashMap<>();
        nickData.put("email", email);
        nickData.put("nick", nick);

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid ->
                        db.collection("nicks").document(uid)
                                .set(nickData)
                                .addOnSuccessListener(aVoid2 -> callback.onResult(null))
                                .addOnFailureListener(e ->
                                        callback.onResult(context.getString(R.string.error_save_nick_failed, e.getMessage()))))
                .addOnFailureListener(e ->
                        callback.onResult(context.getString(R.string.error_save_user_failed, e.getMessage())));
    }
}