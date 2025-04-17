package com.example.ourmedia;

import android.transition.Visibility;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;

import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


public class GestoreSQL {
    // Imposta qui i tuoi parametri di connessione
    private final String url = "https://bentisocial.altervista.org/"; // verifica che il DB sia in esecuzione
    private boolean supp=false;
    private int[] filtri={1, -1};
    public static final int StanzaRossa = 1;
    public static final int StanzaGialla = 2;
    public static final int StanzaBlu = 3;
    public static final int StanzaVerde = 4;

    public static final int Nessuno =-1;

    // Apre la connessione al database
    public boolean openConnection() {
        try {
            URL u = new URL(url);
            if(u.openConnection()!=null)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean versione(String version){
        return Invia("version", version).contains("1");
    }
    public String apk(){
        return url+"OurMedia.apk";
    }

    // Chiude la connessione al database
    public void closeConnection() {
            closeConnection();
    }

    // Restituisce il numero di like per un dato post
    public Integer getNLikes(int postId) {
        try{
            String query = "SELECT COUNT(*) AS numLikes FROM View WHERE ID_Post =" + postId + " AND Like_Post = true";
            JSONArray supp = new JSONArray(Invia(query));
            JSONObject risposta = supp.getJSONObject(0);
            String valore = risposta.getString("numLikes");
            return Integer.parseInt(valore);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Verifica se un utente ha messo il like ad un determinato post
    public Boolean isLiked(int userId, int postId) {
        try {
            String query = "SELECT Like_Post FROM View WHERE ID_User = " + userId + " AND ID_Post = " + postId;
            JSONArray supp = new JSONArray(Invia(query));
            JSONObject risposta = supp.getJSONObject(0);
            return risposta.getInt("Like_Post")==1;
        } catch (JSONException e) {
            return false;
        }
    }

    // Restituisce un post che l'utente non ha ancora visto, e registra la view
    public MyItem newPost(int userId) {
        try {
            return newPost(userId, 1).get(0);
        }catch (IndexOutOfBoundsException | NullPointerException e){
            return null;
        }
    }


    public List<MyItem> newPost(int userId, int quantita){

        List<MyItem> lista=new ArrayList<>();
        try {
            // Query per selezionare un post non ancora visualizzato dall'utente, ordinato casualmente
            String s1 = filtro1(userId);
            String s2 = filtro2();
            String query = "SELECT * FROM Post "+  s1+ ((s2!="" && s1=="")? s2 : s2.replace("WHERE", " AND ")) +" ORDER BY RAND() LIMIT "+quantita;

            JSONArray supp = new JSONArray(Invia(query));

            for (int i=0; i<supp.length(); i++) {
                JSONObject risposta = supp.getJSONObject(i);
                Integer id = risposta.getInt("ID");

                MyItem post = new MyItem(id, risposta.getString("Autore"), risposta.getString("Descrizione"), risposta.getString("Immagine"));
                post.setLike(getNLikes(id));

                lista.add(post);

            }



                // Registra la visualizzazione con Like_Post impostato a false e nessun commento
            for (MyItem post : lista) {
                String insertView = "INSERT INTO View (ID_User, ID_Post, Like_Post, Commento) VALUES (" + userId + ", " + post.getId() + ", false, NULL)";
                JSONObject insert = new JSONObject(Invia(insertView));
                if(s1!="") {
                    if (insert.isNull("result") || !insert.getBoolean("result")) {
                        lista.remove(post);
                        lista.add(null);
                    }
                }
            }
            return lista;
        }catch (JSONException e){
            return null;
        }
    }
    public void FiltroViewON(){
        filtri[0]=1;
    }
    public void FiltroViewOFF(){
        filtri[0]=0;
    }

    public void FiltroCategoriaON(int categoria){
        filtri[1]=categoria;
    }
    public void FiltroCategoriaOFF(){
        FiltroCategoriaON(1);
    }


    // Aggiunge il like ad un post. Se esiste già una view per quell'utente, la aggiorna, altrimenti la inserisce.
    public boolean addLike(int userId, int postId) {
        try {
            // Prova ad aggiornare se esiste già una riga per quella view
            String query = "UPDATE View SET Like_Post = true WHERE ID_User = "+userId+" AND ID_Post = "+postId;
            return new JSONObject(Invia(query)).getBoolean("result");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Funzione setLike: simile ad addLike, imposta il like per un video specifico visto da un utente
    public boolean setLike(int userId, int postId, boolean b) {
        try {
            String query = "UPDATE View SET Like_Post = \"" + (b ? 1 : 0) + "\" WHERE View.ID_User = " + userId + " AND View.ID_Post = " + postId;
            return new JSONObject(Invia(query)).getBoolean("result");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Aggiunge un commento ad un post visualizzato dall'utente
    public boolean addComment(int userId, int postId, String comment) {
        try {
            // Se esiste già la view, aggiorna il commento
            String query = "UPDATE View SET Commento = " + comment + " WHERE ID_User = "+userId+" AND ID_Post = "+postId;
            return (new JSONObject(Invia(query))).getBoolean("result");

        } catch (JSONException e) {
        throw new RuntimeException(e);
        }
    }

    // Visualizza tutti i commenti relativi ad un determinato post (di tutti gli utenti, incluso quello corrente)
    public List<String> viewComments(int postId) {

        try{
            List<String> comments = new ArrayList<>();
            // Query aggiornata per includere anche il nome utente
            String query = "SELECT U.Username, V.Commento " +
                           "FROM View V JOIN User U ON V.ID_User = U.ID " +
                           "WHERE V.ID_Post = "+postId+" AND V.Commento IS NOT NULL";

            JSONArray array = new JSONArray(Invia(query));

            JSONObject object;
            int i=0;
            while (!array.isNull(i)){
                object = array.getJSONObject(i);
                comments.add(object.getString("Username") + ": " + object.getString("Username"));
                i++;
            }

            return comments;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MyItem> getPost(int userId){
        try{
            List<MyItem> comments = new ArrayList<>();
            // Query aggiornata per includere anche il nome utente
            String query = "SELECT P.* " +
                    "FROM Post P JOIN View V ON P.ID = V.ID_Post" +
                    "WHERE V.ID_User = "+userId+" AND Like_Post = true";

            JSONArray array = new JSONArray(Invia(query));

            JSONObject object;
            int i=0;
            while (!array.isNull(i)){
                object = array.getJSONObject(i);
                int id= object.getInt("ID");
                comments.add(new MyItem(id, object.getString("Autore"), object.getString("Descrizione"), object.getString("Immagine")));
                i++;
            }

            return comments;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String Invia(String query)  {
        return Invia("query", query);
    }

    private String Invia(String key, String value){
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            try {
                URL u = new URL(url + "?" + key + "="  + value);
                HttpsURLConnection connection = (HttpsURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder s = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    s.append(line).append("\n");
                }
                return s.toString();
            } catch (IOException e) {
                return "Errore: " + e.getMessage();
            }
        });

        Thread thread = new Thread(futureTask);
        thread.start();

        try {
            return futureTask.get(); // Attende il risultato
        } catch (ExecutionException | InterruptedException e) {
            return "Errore nel thread: " + e.getMessage();
        }
    }

    public int getID(String username) {

        try {
            String query = "SELECT ID FROM User WHERE Username = \"" + username + "\"";
            JSONArray arr = new JSONArray(Invia(query));
            JSONObject risposta = arr.getJSONObject(0);
            return risposta.getInt("ID");

        } catch (JSONException e) {
            return -1;
        }
    }
    public int setId(String username){
        try {
            String query = "INSERT INTO User (Username) VALUES (\"" + username + "\")";
            JSONObject risposta = new JSONObject(Invia(query));
            if(!risposta.isNull("result") && risposta.getBoolean("result"))
                return getID(username);
            return -1;
        } catch (JSONException e) {
            return -1;
        }
    }

    private String filtro1(Integer userId){
            if(filtri[0]==1 && userId>0)
                return " WHERE ID NOT IN (SELECT ID_Post FROM View WHERE ID_User = " + userId + ")";
            return "";
    }
    private String filtro2(){
        if(filtri[1]>0 && filtri[1]<5){
            return " WHERE categoria = "+filtri[1];
        }
        return "";
    }
}