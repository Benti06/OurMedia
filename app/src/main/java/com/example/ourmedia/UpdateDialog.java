package com.example.ourmedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import android.os.Build;
import android.provider.Settings;

public class UpdateDialog {

    // Mostra il dialog di aggiornamento
    public static void showUpdateDialog(Activity activity, String apkUrl) {
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Aggiornamento Disponibile");
            builder.setMessage("È disponibile una nuova versione dell'app. Vuoi aggiornare ora?");

            // Pulsante "Aggiorna"
            builder.setPositiveButton("Aggiorna", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (apkUrl != null && !apkUrl.isEmpty()) {
                        // Inizializza il download e l'installazione dell'APK
                        downloadAndInstallAPK(activity, apkUrl);
                    } else {
                        Toast.makeText(activity, "URL di aggiornamento non disponibile", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Pulsante "Annulla"
            builder.setNegativeButton("Annulla", (dialog, which) -> activity.finish());

            // Creazione e blocco dell'esecuzione
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false); // Impedisce la chiusura accidentale
            dialog.show();
        });
    }

    // Metodo per scaricare e installare l'APK usando Handler e Runnable
    // Funzione principale per scaricare e installare l'APK
    public static void downloadAndInstallAPK(Context context, String apkUrl) {
        // Crea un nuovo thread per eseguire il download
        new Thread(() -> {
            File apkFile = downloadFile(context, apkUrl);

            // Usa Handler per eseguire codice nel thread principale
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if (apkFile != null) {
                    installAPK(context, apkFile);  // Installa l'APK
                } else {
                    Toast.makeText(context, "Errore nel download dell'aggiornamento", Toast.LENGTH_LONG).show();
                }
            });
        }).start();  // Avvia il thread
    }

    // Funzione per scaricare il file APK
    private static File downloadFile(Context context, String apkUrl) {
        File apkFile = null;

        try {
            // Crea la directory di destinazione
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                apkFile = new File(dir, "update.apk");
            }

            // Effettua la connessione al server per scaricare l'APK
            URL url = new URL(apkUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Ottieni il flusso di input dal server
            InputStream inputStream = connection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(apkFile);

            // Scarica il file in blocchi di 1024 byte
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            // Chiudi i flussi
            fileOutputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return apkFile;
    }

    // Funzione per installare l'APK
    private static void installAPK(Context context, File apkFile) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "Il file APK non esiste", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ottieni l'URI per il file APK tramite FileProvider
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);

        // Crea l'Intent per l'installazione dell'APK
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Gestisci permessi per l'installazione dell'APK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Se il permesso per installare APK da fonti sconosciute non è stato concesso
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                // Richiesta per permettere l'installazione da fonti sconosciute
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                settingsIntent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(settingsIntent);
            } else {
                // Se il permesso è già stato concesso, procedi con l'installazione
                context.startActivity(intent);
            }
        } else {
            // Per versioni più vecchie di Android, non è necessario il permesso specifico
            context.startActivity(intent);
        }
    }
}
