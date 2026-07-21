package com.ereke.qadam2048;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardListFragment extends Fragment {

    private String gameMode;
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<UserScore> scoreList = new ArrayList<>();
    private TextView tvEmpty;

    public static LeaderboardListFragment newInstance(String mode) {
        LeaderboardListFragment fragment = new LeaderboardListFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard_list, container, false);

        if (getArguments() != null) {
            gameMode = getArguments().getString("mode");
        }

        recyclerView = view.findViewById(R.id.rvLeaderboard);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        // Отключаем вложенный скролл, чтобы ViewPager2 видел горизонтальные свайпы
        recyclerView.setNestedScrollingEnabled(false);

        adapter = new LeaderboardAdapter(scoreList, getContext());
        recyclerView.setAdapter(adapter);

        loadScoresFromFirebase();
        return view;
    }

    private void loadScoresFromFirebase() {
        if (gameMode == null || getContext() == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance("https://first-project-2easy-default-rtdb.europe-west1.firebasedatabase.app/").getReference("leaderboard").child(gameMode);
        ref.orderByChild("score").limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                scoreList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Извлекаем userId — именно это поле мы сравниваем в адаптере
                    String uId = ds.child("userId").getValue(String.class);
                    String name = ds.child("username").getValue(String.class);
                    Long score = ds.child("score").getValue(Long.class);

                    if (name != null) {
                        scoreList.add(new UserScore(name, score != null ? score : 0, uId));
                    }
                }
                Collections.reverse(scoreList);

                // Пересоздаем адаптер или обновляем, если он уже есть
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                if (tvEmpty != null) {
                    tvEmpty.setVisibility(scoreList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}