package com.example.todo.Barpanel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.todo.FirestoreHelper;
import com.example.todo.GeminiPlanner;
import com.example.todo.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FragmentMow extends Fragment {

    private TextView tvStatus;
    private ImageButton btnMic;
    private ProgressBar progress;

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            ArrayList<String> matches = result.getData()
                                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            if (matches != null && !matches.isEmpty()) {
                                processCommand(matches.get(0));
                            }
                        } else {
                            setStatus("Naciśnij mikrofon i powiedz co zaplanować");
                            setLoading(false);
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mow, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStatus = view.findViewById(R.id.tvMowStatus);
        btnMic   = view.findViewById(R.id.btnMic);
        progress = view.findViewById(R.id.progressMow);

        btnMic.setOnClickListener(v -> startVoiceInput());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvStatus = null;
        btnMic   = null;
        progress = null;
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz co zaplanować...");
        setLoading(true);
        speechLauncher.launch(intent);
    }

    private void processCommand(String rawText) {
        setStatus("Rozpoznano:\n\"" + rawText + "\"\n\nPrzetwarzam przez AI...");

        GeminiPlanner.process(requireContext(), rawText, new GeminiPlanner.Callback() {
            @Override
            public void onResult(String task, int offsetDays, List<String> reminders) {
                if (!isAdded()) return;
                setLoading(false);
                buildAndSavePlan(task, offsetDays, reminders);

                String info = "✓ Dodano: " + task;
                if (!reminders.isEmpty())
                    info += "\nPowiadomienia: " + String.join(", ", reminders);
                setStatus(info);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setLoading(false);
                setStatus("Naciśnij mikrofon i powiedz co zaplanować");
                Toast.makeText(requireContext(),
                        "Błąd: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void buildAndSavePlan(String task, int offsetDays, List<String> reminders) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Calendar due = Calendar.getInstance();
        due.add(Calendar.DAY_OF_MONTH, offsetDays);
        due.set(Calendar.HOUR_OF_DAY, 23);
        due.set(Calendar.MINUTE, 59);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        List<Timestamp> ts = new ArrayList<>();
        for (String t : reminders) {
            String[] p = t.split(":");
            if (p.length != 2) continue;
            try {
                Calendar c = (Calendar) due.clone();
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0].trim()));
                c.set(Calendar.MINUTE, Integer.parseInt(p[1].trim()));
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                ts.add(new Timestamp(c.getTime()));
            } catch (NumberFormatException ignored) {}
        }

        String email = user.getEmail() != null ? user.getEmail() : "";
        FirebaseFirestore.getInstance()
                .collection("nicks")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String nick = doc.getString("nick");
                    if (nick == null || nick.isEmpty()) nick = email;
                    new FirestoreHelper().addPlan(
                            requireContext(),
                            user.getUid(),
                            email,
                            nick,
                            task,
                            "",
                            ts.isEmpty() ? null : ts.get(0),
                            ts,
                            new Timestamp(due.getTime()),
                            "medium"
                    );
                });
    }

    private void setStatus(String text) {
        if (tvStatus != null) tvStatus.setText(text);
    }

    private void setLoading(boolean loading) {
        if (btnMic != null) btnMic.setEnabled(!loading);
        if (progress != null)
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}