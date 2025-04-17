package com.example.ourmedia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.bumptech.glide.request.transition.Transition;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Esplora#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Esplora extends Fragment {

    private List<MyItem> post;
    private LinearLayout lista;
    private OnActionListener mListener;



    // Interfaccia che definisce il metodo che l'Activity dovrà implementare

    public interface OnActionListener {
        int onLike(int id_post);
    }



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1"; private String mParam1;
    //                                   ↓--------↑


    public Esplora() {
        // Required empty public constructor
        post = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verifica che l'Activity abbia implementato l'interfaccia
        if (context instanceof OnActionListener) {
            mListener = (OnActionListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement OnActionListener");
        }
    }

    public void clear(){
        lista.removeAllViews();
        post.clear();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Esplora.
     */
    // TODO: Rename and change types and number of parameters
    public static Esplora newInstance(String param1, String param2) {
        Esplora fragment = new Esplora();
        Bundle args = new Bundle();

        //                             ↓↑
        //args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //                                     -----
        }

        post = new ArrayList<>();

    }

    public void setLista(LinearLayout l){
        lista=l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_esplora, container, false);
        lista = view.findViewById(R.id.lista);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lista = view.findViewById(R.id.lista);
    }

    public void addPost(MyItem daaggungere){

        CountDownLatch latch = new CountDownLatch(1); // 🔹 Blocco il thread finché UI non è pronta


        //getActivity().runOnUiThread(() -> {
            View nuovaView = generaView(daaggungere);
            lista.addView(nuovaView);
            post.add(daaggungere);
            latch.countDown();
        //});


        try {
            latch.await(); // 🔹 Aspetta che runOnUiThread finisca
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
    public void addPost(List<MyItem> daaggungere) throws NullPointerException{
        for(MyItem item : daaggungere){
            try{
                addPost(item);
            }catch (NullPointerException e){
                throw new NullPointerException();
            }
        }
    }
    private View generaView(MyItem myitem){

        View layout = getLayoutInflater().inflate(R.layout.item_layout, lista, false);
        ((TextView)layout.findViewById(R.id.autore)).setText(myitem.getAutore());
        ((TextView)layout.findViewById(R.id.descrizione)).setText(myitem.getDescrizione());


        TextView like = ((TextView)layout.findViewById(R.id.like));
        String s = myitem.getLike()+"";
        like.setText(s);
        like.setOnClickListener((view)->{
            String s3 = ""+triggerAction(myitem.getId());
            like.setText(s3);
        });

        ((ImageView)layout.findViewById(R.id.likeIcon)).setOnClickListener(v ->{
            String s3 = ""+triggerAction(myitem.getId());
            like.setText(s3);
        });

        /*Bitmap supp = Bitmap.createBitmap()
        Glide.with(this).load(myitem.getImagePath()).into(supp);//setImageBitmap(myitem.getImagePath());
PhotoView photoView = ((PhotoView) layout.findViewById(R.id.immagine));
*/
        PhotoView photoView = ((PhotoView) layout.findViewById(R.id.immagine));
        Glide.with(this)
                .asBitmap()  // Usa .asBitmap() per ottenere un Bitmap
                .load(myitem.getImagePath())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                        photoView.post(() -> {

                            int photoViewWidth = photoView.getWidth();  // Ottieni la larghezza del PhotoView
                            Bitmap scaledBitmap = scaleBitmapToWidth(resource, photoViewWidth);

                            int height = 1000;
                            if(scaledBitmap.getHeight()>height){
                                scaledBitmap = scaleBitmapToHeight(resource, height);
                            }

                            photoView.setImageBitmap(scaledBitmap);

                            // Imposta le dimensioni del PhotoView in base alla bitmap
                            ViewGroup.LayoutParams params = photoView.getLayoutParams();
                            params.height = scaledBitmap.getHeight();
                            photoView.setLayoutParams(params);
                        });

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Gestisci quando l'immagine non è più disponibile
                    }
                });
        photoView.setOnLongClickListener(v -> {
            // Creiamo un'icona di cuore sopra l'immagine
            ImageView heart = new ImageView(v.getContext());
            heart.setImageResource(R.drawable.heart); // Usa un'icona di cuore
            heart.setColorFilter(Color.RED); // Colore rosso

            // Aggiungiamo la vista sopra il PhotoView
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

            ViewGroup viewGroup = ((ViewGroup) v.getParent());
            viewGroup.addView(heart, params);
            heart.setX(viewGroup.findViewById(R.id.immagine).getWidth()/2f);
            heart.setY(viewGroup.findViewById(R.id.immagine).getHeight()/2f);
            params.gravity = Gravity.CENTER; // Posizioniamo il cuore al centro


            // Creiamo l'animazione
            heart.setScaleX(0f);
            heart.setScaleY(0f);
            heart.setAlpha(0f);
            int durata = 500;
            heart.animate()
                    .scaleX(1.5f).scaleY(1.5f) // Ingrandisce il cuore
                    .alpha(1f) // Appare gradualmente
                    .setDuration(durata) // Durata 300ms
                    .withEndAction(() -> heart.animate()
                            .scaleX(0f).scaleY(0f) // Riduce di nuovo la dimensione
                            .alpha(0f) // Scompare
                            .setDuration(durata)
                            .withEndAction(() -> {
                                ((ViewGroup) v.getParent()).removeView(heart);
                            })
                    ).start();

            String s2 = ""+triggerAction(myitem.getId());

            like.setText(s2);

            return true; // Indica che il long click è gestito
        });
        return layout;
    }
    // Chiamata al metodo dell'Activity

    public int triggerAction(int id_post) {
        if (mListener != null) {
            return mListener.onLike(id_post);
        }
        return 0;
    }


    private Bitmap scaleBitmapToWidth(Bitmap originalBitmap, int targetWidth) {
        if (originalBitmap == null || targetWidth <= 0) {
            return originalBitmap;
        }

        // Calcola la nuova altezza mantenendo il rapporto d'aspetto
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        float scaleFactor = (float) targetWidth / originalWidth;
        int newHeight = Math.round(originalHeight * scaleFactor);

        // Crea una nuova bitmap scalata
        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, newHeight, true);
    }

    private Bitmap scaleBitmapToHeight(Bitmap originalBitmap, int tagetHeight) {
        if (originalBitmap == null || tagetHeight <= 0) {
            return originalBitmap;
        }

        // Calcola la nuova altezza mantenendo il rapporto d'aspetto
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        float scaleFactor = (float) tagetHeight / originalHeight;
        int newWodth = Math.round(originalWidth * scaleFactor);

        // Crea una nuova bitmap scalata
        return Bitmap.createScaledBitmap(originalBitmap, newWodth, tagetHeight, true);
    }
}