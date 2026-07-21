package com.ereke.qadam2048;

import android.content.Context;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardPagerAdapter extends RecyclerView.Adapter<LeaderboardPagerAdapter.PageViewHolder> {
    private final String[] modes = {"4x4", "5x5", "6x6"};
    private final Context context;
    private final DatabaseReference db;
    private final Typeface montserrat;
    private OnMyScoreLoadedListener scoreListener;

    // Интерфейс для передачи твоего личного рекорда в Activity
    public interface OnMyScoreLoadedListener {
        void onLoaded(UserScore myData, int myRank);
    }

    public void setOnMyScoreLoadedListener(OnMyScoreLoadedListener listener) {
        this.scoreListener = listener;
    }

    public LeaderboardPagerAdapter(Context context) {
        this.context = context;
        this.db = FirebaseDatabase.getInstance("https://first-project-2easy-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        this.montserrat = Typeface.createFromAsset(context.getAssets(), "fonts/montserrat_bold.ttf");
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.fragment_leaderboard_list, parent, false);
        return new PageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(modes[position]);
    }

    @Override
    public int getItemCount() { return modes.length; }

    class PageViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rv;
        TextView tvEmpty;
        ValueEventListener currentListener;
        DatabaseReference currentRef;

        PageViewHolder(View v) {
            super(v);
            rv = v.findViewById(R.id.rvLeaderboard);
            tvEmpty = v.findViewById(R.id.tvEmpty);
            rv.setLayoutManager(new LinearLayoutManager(context));
        }

        void bind(String mode) {
            // Удаляем старый слушатель, если он был (профилактика утечек памяти)
            if (currentRef != null && currentListener != null) {
                currentRef.removeEventListener(currentListener);
            }

            currentRef = db.child("leaderboard").child(mode);
            currentListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<UserScore> scores = new ArrayList<>();
                    String myId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        UserScore u = ds.getValue(UserScore.class); // Firebase сам заполнит объект
                        if (u != null && u.score > 0) {
                            scores.add(u);
                        }
                    }

                    // Сортируем: сначала самые большие очки
                    Collections.sort(scores, (o1, o2) -> Long.compare(o2.score, o1.score));

                    if (scores.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                        rv.setAdapter(new ItemsAdapter(scores));

                        // 🔥 Ищем тебя в списке для липкой плашки
                        for (int i = 0; i < scores.size(); i++) {
                            if (scores.get(i).userId != null && scores.get(i).userId.equals(myId)) {
                                if (scoreListener != null) {
                                    scoreListener.onLoaded(scores.get(i), i + 1);
                                }
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };

            // Ограничиваем топ-50
            currentRef.orderByChild("score").limitToLast(50).addValueEventListener(currentListener);
        }
    }

    // --- ВНУТРЕННИЙ АДАПТЕР СПИСКА ---
    class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.IVH> {
        private final List<UserScore> list;

        ItemsAdapter(List<UserScore> list) { this.list = list; }

        @NonNull
        @Override
        public IVH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new IVH(LayoutInflater.from(context).inflate(R.layout.item_leaderboard_user, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull IVH h, int p) {
            UserScore u = list.get(p);
            int rankNum = p + 1;

            h.name.setText(u.username);
            h.score.setText(java.text.NumberFormat.getInstance().format(u.score));
            h.name.setTypeface(montserrat);
            h.score.setTypeface(montserrat);
            h.rank.setTypeface(montserrat);

            // Флаг страны
            if (u.country != null && !u.country.isEmpty()) {
                h.flag.setText(getEmojiFlag(u.country));
                h.flag.setVisibility(View.VISIBLE);
            } else {
                h.flag.setVisibility(View.GONE);
            }

            // Дизайн карточек (🥇 🥈 🥉)
            if (rankNum == 1) {
                h.itemView.setBackgroundResource(R.drawable.bg_top_1);
                h.rank.setText("🥇");
            } else if (rankNum == 2) {
                h.itemView.setBackgroundResource(R.drawable.bg_top_2);
                h.rank.setText("🥈");
            } else if (rankNum == 3) {
                h.itemView.setBackgroundResource(R.drawable.bg_top_3);
                h.rank.setText("🥉");
            } else {
                h.itemView.setBackgroundResource(R.drawable.bg_leaderboard_item_default);
                h.rank.setText(String.valueOf(rankNum));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class IVH extends RecyclerView.ViewHolder {
            TextView rank, name, score, flag;
            IVH(View v) {
                super(v);
                rank = v.findViewById(R.id.rankText);
                name = v.findViewById(R.id.nameText);
                score = v.findViewById(R.id.scoreText);
                flag = v.findViewById(R.id.countryFlag);
            }
        }
    }

    public static String getEmojiFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return "🏳️";
        int firstLetter = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }
}