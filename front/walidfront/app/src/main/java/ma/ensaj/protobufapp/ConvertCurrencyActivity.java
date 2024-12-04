package ma.ensaj.protobufapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ma.project.stubs.Bank;
import ma.project.stubs.BankServiceGrpc;

public class ConvertCurrencyActivity extends AppCompatActivity {

    private EditText etMontant, etDeviseSource, etDeviseCible;
    private TextView tvResultat;
    private Button btnConvertir;

    private ManagedChannel channel;
    private BankServiceGrpc.BankServiceBlockingStub stub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_currency);

        // Initialisation des éléments d'interface utilisateur
        etMontant = findViewById(R.id.et_montant);
        etDeviseSource = findViewById(R.id.et_devise_source);
        etDeviseCible = findViewById(R.id.et_devise_cible);
        tvResultat = findViewById(R.id.tv_resultat);
        btnConvertir = findViewById(R.id.btn_convertir);

        // Configuration du canal gRPC
        setupGrpcChannel();

        // Écouteur de clic sur le bouton
        btnConvertir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertCurrency();
            }
        });
    }

    private void setupGrpcChannel() {
        try {
            // Utilisation de 10.0.2.2 pour l'émulateur Android afin d'accéder à la machine hôte
            channel = ManagedChannelBuilder.forAddress("10.0.2.2", 5555)
                    .usePlaintext() // Cela garantit HTTP/2 sans TLS
                    .build();

            // Création d'un stub bloquant pour les appels synchrones
            stub = BankServiceGrpc.newBlockingStub(channel);

        } catch (Exception e) {
            Log.e("gRPC Setup Error", "Error setting up gRPC channel: " + e.getMessage(), e);
            Toast.makeText(this, "Échec de la configuration du canal gRPC", Toast.LENGTH_SHORT).show();
        }
    }

    private void convertCurrency() {
        // Récupération des valeurs d'entrée
        String montantStr = etMontant.getText().toString();
        String deviseSource = etDeviseSource.getText().toString();
        String deviseCible = etDeviseCible.getText().toString();

        if (montantStr.isEmpty() || deviseSource.isEmpty() || deviseCible.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        // Création de la requête gRPC
        Bank.ConvertCurrencyRequest request = Bank.ConvertCurrencyRequest.newBuilder()
                .setAmount(montant)
                .setCurrencyFrom(deviseSource)
                .setCurrencyTo(deviseCible)
                .build();

        new Thread(() -> {
            try {
                Bank.ConvertCurrencyResponse response = stub.convert(request);

                runOnUiThread(() -> tvResultat.setText("Montant converti : " + response.getResult()));

            } catch (StatusRuntimeException e) {
                Log.e("gRPC Error", "Error calling gRPC: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Échec de la conversion : " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e("gRPC Error", "Unexpected error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}