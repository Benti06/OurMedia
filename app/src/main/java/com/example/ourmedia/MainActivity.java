package com.example.ourmedia;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;

import java.util.List;
import android.graphics.Typeface;
import android.graphics.Typeface;


public class MainActivity extends AppCompatActivity implements Esplora.OnActionListener{

    FrameLayout frameLayout;
    TabLayout tabLayout;
    Esplora frament;
    private SharedViewModel viewModel;
    private int userId;
    GestoreSQL database;
    private Runnable addpost;

    private boolean giavisto=false;

    int stanzaprecedente=GestoreSQL.Nessuno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        database= new GestoreSQL();
        try {

            if (!database.versione(getPackageManager().getPackageInfo(getPackageName(), 0).versionName)) {
                checkForUpdate();
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }


        TextView nomeUtente = ((TextView) findViewById(R.id.nomeUtente));
        Esplora esplora = new Esplora();
        frament = esplora;


        frameLayout = (FrameLayout) findViewById(R.id.framelayout);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        if(leggiId()!=-1) {
            nomeUtente.setText(leggiUsername());
        }
        if(!leggiFiltro())
            database.FiltroViewOFF();
        addpost = () -> {
            try {
                List<MyItem> post = database.newPost(userId, 3);
                if(post.size()<3){
                    if(!post.isEmpty()) {
                        ((Esplora) frament).addPost(post);
                        post = database.newPost(userId, 3 - post.size());
                    }else{
                        if(!giavisto) {
                            Toast.makeText(this, "Hai visto tutti i post", Toast.LENGTH_LONG).show();
                            giavisto = true;
                            salvaFiltro(false);
                        }
                    }

                }

                ((Esplora) frament).addPost(post);

            } catch (NullPointerException e){


            }
        };
        // Inizializzazione del Fragment Esplora
        if (savedInstanceState == null) {  // solo se non esiste già un fragment salvato
            frament = new Esplora();  // Crea una nuova istanza di Esplora
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.framelayout, frament)
                    .runOnCommit(() -> { // Aspetta che il Fragment sia pronto

                        if (frament instanceof Esplora) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (leggiId() == -1) {
                                    showUsernameDialog(() -> {
                                        addpost.run();
                                        nomeUtente.setText(leggiUsername());
                                    });
                                }else {
                                    addpost.run();
                                }
                            });
                        }
                        ScrollView scrollView = findViewById(R.id.scrollView);
                        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                            @Override
                            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                                View child = scrollView.getChildAt(0);
                                if (child != null) {
                                    int diff = (child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
                                    if (diff < 300) {
                                        addpost.run();
                                    }
                                }
                            }
                        });
                    })
                    .commit();
        }


        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Cambia il Fragment in base alla Tab selezionata
                switch (tab.getPosition()) {
                    case 0:
                        frament = esplora;  // Creazione di un nuovo fragment
                        break;
                    case 1:
                        //frament = new Like();  // Creazione di un altro fragment
                        break;
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.framelayout, frament)  // Aggiungi il nuovo Fragment
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)  // Imposta una transizione
                        .commit();
            }



            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Puoi fare altre azioni qui quando il tab viene deselezionato
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Puoi fare altre azioni qui quando il tab viene ricalcato
            }
        });





        nomeUtente.setOnLongClickListener((View v) -> {
            showUsernameDialog(() -> {
                ((Esplora)frament).clear();
                addpost.run();
            });
            return true;
        });

        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        viewModel.getActionParameter().observe(this, action -> {
            // Esegui qualcosa quando il fragment ha invocato l'azione
        });


        ImageButton btnSettings = findViewById(R.id.btnSettings);

        // Quando viene cliccato, apre il dialog
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterDialog();
            }
        });
    }

    private void showUsernameDialog(Runnable onComplete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Inserisci Username");
        builder.setCancelable(false); // Impedisce la chiusura toccando fuori

        EditText input = new EditText(this);
        input.setHint("Username");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Iscrivi", (dialog, which) -> {
            String username = input.getText().toString().trim();
            //new Thread(() -> { // Esegui operazioni di database in background
                int supp = database.setId(username);
                //runOnUiThread(() -> {
                    if (supp == -1) {
                        dialog.dismiss();
                        Toast.makeText(this, "Qualcosa è andato storto", Toast.LENGTH_SHORT).show();
                        showUsernameDialog(onComplete); // Riapri se l'ID non è valido
                    } else {
                        salvaId();
                        ((TextView) findViewById(R.id.nomeUtente)).setText(username);
                        salvaUsername(username);
                        dialog.dismiss();
                        if (onComplete != null) onComplete.run(); // Esegui il codice dopo la chiusura
                    }
                //});
            //}).start();
        });

        builder.setNegativeButton("Accedi", (dialog, which) -> {
            String username = input.getText().toString().trim();
            //new Thread(() -> {
                userId = database.getID(username);
                //runOnUiThread(() -> {
                    if (userId == -1) {
                        Toast.makeText(MainActivity.this, "Qualcosa è andato storto", Toast.LENGTH_LONG).show();
                        showUsernameDialog(onComplete);
                        dialog.dismiss();
                    } else {
                        salvaId();
                        ((TextView) findViewById(R.id.nomeUtente)).setText(username);
                        salvaUsername(username);
                        if (onComplete != null) onComplete.run(); // Esegui il codice dopo la chiusura
                        dialog.dismiss();
                    }
                //});
            //}).start();
        });

        builder.setNeutralButton("Annulla", (dialog, which) -> {
            if(userId==-1)
                showUsernameDialog(onComplete);
            dialog.dismiss();
        });

        builder.show();
    }

    private int leggiId(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);
        return userId;
    }

    private void salvaId(int s){
        userId = s;
        salvaId();
    }

    private void salvaId(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("id", userId);
        editor.apply(); // Oppure editor.commit();
    }

    private void salvaUsername(String username){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply(); // Oppure editor.commit();
    }

    private String leggiUsername(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("username", null);
    }

    private void salvaFiltro(boolean b){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("filtro", b);
        editor.apply(); // Oppure editor.commit();
    }
    private boolean leggiFiltro(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("filtro", true);
    }

    public void Aggungi(int n){
        for(int i=0; i<n; i++) {
            View item = getLayoutInflater().inflate(R.layout.item_layout, null);
            TextView autore = item.findViewById(R.id.autore);
        }
    }


    @Override
    public int onLike(int id_post) {
        database.setLike(userId, id_post, !database.isLiked(userId, id_post));
        return database.getNLikes(id_post);
    }

    // Metodo per simulare il controllo di un aggiornamento
    private void checkForUpdate() {
        // URL dell'APK per l'aggiornamento
        String apkUrl = database.apk();

        // Chiamata al dialogo di aggiornamento
        showUpdateDialog(apkUrl);
    }

    // Mostra il dialogo di aggiornamento
    private void showUpdateDialog(String apkUrl) {
        UpdateDialog.showUpdateDialog(this, apkUrl);
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleziona le opzioni di visualizzazione");

        // Layout principale verticale
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        // Creazione di un RadioGroup per le stanze
        TextView stanzaTitle = new TextView(this);
        stanzaTitle.setText("Filtra per stanza:");
        stanzaTitle.setTextSize(16);
        stanzaTitle.setTypeface(null, Typeface.BOLD);
        stanzaTitle.setPadding(0, 0, 0, 20);

        RadioGroup radioGroupStanza = new RadioGroup(this);
        radioGroupStanza.setOrientation(RadioGroup.VERTICAL);

        RadioButton none = new RadioButton(this);
        none.setText("Nessun filtro");
        none.setId(View.generateViewId());

        RadioButton red = new RadioButton(this);
        red.setText("Stanza Rossa");
        red.setId(View.generateViewId());

        RadioButton yellow = new RadioButton(this);
        yellow.setText("Stanza Gialla");
        yellow.setId(View.generateViewId());

        RadioButton blue = new RadioButton(this);
        blue.setText("Stanza Blu");
        blue.setId(View.generateViewId());

        RadioButton green = new RadioButton(this);
        green.setText("Stanza Verde");
        green.setId(View.generateViewId());

        // Aggiunta dei RadioButton al primo RadioGroup
        radioGroupStanza.addView(none);
        radioGroupStanza.addView(red);
        radioGroupStanza.addView(yellow);
        radioGroupStanza.addView(blue);
        radioGroupStanza.addView(green);

        // Creazione di un RadioGroup per la visualizzazione (Con/Senza ripetizioni)
        TextView ripetizioniTitle = new TextView(this);
        ripetizioniTitle.setText("Opzioni di visualizzazione:");
        ripetizioniTitle.setTextSize(16);
        ripetizioniTitle.setTypeface(null, Typeface.BOLD);
        ripetizioniTitle.setPadding(0, 30, 0, 20);

        RadioGroup radioGroupRipetizioni = new RadioGroup(this);
        radioGroupRipetizioni.setOrientation(RadioGroup.VERTICAL);

        RadioButton withRepeats = new RadioButton(this);
        withRepeats.setText("Con ripetizioni");
        withRepeats.setId(View.generateViewId());

        RadioButton withoutRepeats = new RadioButton(this);
        withoutRepeats.setText("Senza ripetizioni");
        withoutRepeats.setId(View.generateViewId());

        radioGroupRipetizioni.addView(withRepeats);
        radioGroupRipetizioni.addView(withoutRepeats);

        if(stanzaprecedente == GestoreSQL.Nessuno)
            none.setChecked(true);
        else if (stanzaprecedente == GestoreSQL.StanzaGialla)
            yellow.setChecked(true);
        else if (stanzaprecedente == GestoreSQL.StanzaRossa) {
            red.setChecked(true);
        }else if (stanzaprecedente == GestoreSQL.StanzaBlu) {
            blue.setChecked(true);
        }else if (stanzaprecedente == GestoreSQL.StanzaVerde) {
            green.setChecked(true);
        }



        if(leggiFiltro())
            withoutRepeats.setChecked(true);
        else
            withRepeats.setChecked(true);

        // Aggiunta degli elementi al layout
        layout.addView(stanzaTitle);
        layout.addView(radioGroupStanza);
        layout.addView(ripetizioniTitle);
        layout.addView(radioGroupRipetizioni);

        builder.setView(layout);

        // Aggiunge il pulsante "OK"
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedStanzaId = radioGroupStanza.getCheckedRadioButtonId();
                int selectedRipetizioniId = radioGroupRipetizioni.getCheckedRadioButtonId();

                // Filtraggio per stanza

                // Filtraggio per ripetizioni
                if (selectedRipetizioniId == withRepeats.getId()) {
                    database.FiltroViewOFF();
                    giavisto=false;
                } else if (selectedRipetizioniId == withoutRepeats.getId()) {
                    database.FiltroViewON();
                    salvaFiltro(false);
                    giavisto=false;
                }

                if(selectedStanzaId == none.getId() && stanzaprecedente==-1){
                    database.FiltroCategoriaOFF();
                    stanzaprecedente = GestoreSQL.Nessuno;
                }else if(stanzaprecedente!=selectedStanzaId) {

                    if (selectedStanzaId == red.getId()) {
                        database.FiltroCategoriaON(GestoreSQL.StanzaRossa);
                        stanzaprecedente=GestoreSQL.StanzaRossa;
                    } else if (selectedStanzaId == green.getId()) {
                        database.FiltroCategoriaON(GestoreSQL.StanzaVerde);
                        stanzaprecedente=GestoreSQL.StanzaVerde;
                    } else if (selectedStanzaId == blue.getId()) {
                        database.FiltroCategoriaON(GestoreSQL.StanzaBlu);
                        stanzaprecedente=GestoreSQL.StanzaBlu;
                    } else if (selectedStanzaId == yellow.getId()) {
                        database.FiltroCategoriaON(GestoreSQL.StanzaGialla);
                        stanzaprecedente = GestoreSQL.StanzaGialla;
                    }



                }
                frament.clear();
                addpost.run();

                // Mostra un messaggio di conferma
                Toast.makeText(MainActivity.this, "Filtro applicato", Toast.LENGTH_SHORT).show();
            }
        });

        // Aggiunge il pulsante "Annulla"
        builder.setNegativeButton("Annulla", null);

        // Mostra il dialog
        builder.create().show();
    }


}