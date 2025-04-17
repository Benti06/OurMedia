package com.example.ourmedia;

//import static androidx.appcompat.graphics.drawable.DrawableContainerCompat.Api21Impl.getResources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class MyItem {
    private int id;
    private String autore;
    private String descrizione;
    private String imagePath;  // path dell'immagine salvato nel DB
    private int like;
    //private Bitmap immagine;

    public MyItem(int id, String autore, String descrizione, String imagePath) {
        this.id = id;
        this.autore = autore;
        this.descrizione = descrizione;
        //immagine = loadImage(imagePath);
        this.imagePath =imagePath;
    }

    public int getLike() {
        return like;
    }

    public void setLike(int like) {
        this.like = like;
    }
    private Bitmap loadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getAutore() {
        return autore;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getImagePath() {
        return imagePath;
    }

    /*
    public Bitmap getImmagine(){

        return imagePath;
    }
*/
    @Override
    public String toString() {
        return "Item:{" + "id=" + id + '\n' + ", autore='" + autore + '\n' + ", descrizione='" + descrizione + '\n' + ", imagePath='" + imagePath + "\n}";
    }

    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("ID", id);
            json.put("Autore", autore);
            json.put("Descrizione", descrizione);
            json.put("Immagine", imagePath);

            // Aggiunge "Like" solo se ha un valore valido
            if (like > 0) {
                json.put("Like", like);
            }

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MyItem fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            int id = json.getInt("ID");
            String autore = json.getString("Autore");
            String descrizione = json.getString("Descrizione");
            String imagePath = json.getString("Immagine");

            MyItem item = new MyItem(id, autore, descrizione, imagePath);

            // Controlla se il campo "Like" esiste
            if (json.has("Like")) {
                item.setLike(json.getInt("Like"));
            }

            return item;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

