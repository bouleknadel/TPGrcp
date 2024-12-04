package ma.ensaj.protobufapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.AppCompatEditText;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ma.project.stubs.Bank;
import ma.project.stubs.BankServiceGrpc;

public class ConvertCurrencyActivity extends AppCompatActivity {

    private AppCompatEditText etMontant;
    private AutoCompleteTextView actDeviseSource, actDeviseCible;
    private TextView tvResultat, tvTitre;
    private MaterialButton btnConvertir;
    private ImageButton btnSwap;
    private MaterialCardView cardResult;
    private TextInputLayout tilMontant, tilDeviseSource, tilDeviseCible;

    private ManagedChannel channel;
    private BankServiceGrpc.BankServiceBlockingStub stub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert_currency);

        initializeViews();
        setupGrpcChannel();
        setupListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilMontant = findViewById(R.id.til_montant);
        tilDeviseSource = findViewById(R.id.til_devise_source);
        tilDeviseCible = findViewById(R.id.til_devise_cible);

        // Use instance variables to reference AutoCompleteTextViews
        actDeviseSource = findViewById(R.id.act_devise_source);
        actDeviseCible = findViewById(R.id.act_devise_cible);

        // List of currencies
        String[] currencies = {"USD", "EUR", "MAD"};

        // Create an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, currencies);

        // Set the adapter to the AutoCompleteTextView
        actDeviseSource.setAdapter(adapter);
        actDeviseCible.setAdapter(adapter);

        // EditTexts and AutoCompleteTextViews
        etMontant = findViewById(R.id.et_montant);

        // Other Views
        tvTitre = findViewById(R.id.tv_title);  // Changed to match the XML id
        tvResultat = findViewById(R.id.tv_resultat);
        btnConvertir = findViewById(R.id.btn_convertir);
        btnSwap = findViewById(R.id.btn_switch);  // Changed to match the XML id
        cardResult = findViewById(R.id.card_resultat);  // Changed to match the XML id

        // Initial Setup
        cardResult.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnConvertir.setOnClickListener(v -> {
            if (validateInputs()) {
                convertCurrency();
            }
        });

        btnSwap.setOnClickListener(v -> swapCurrencies());
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String montant = etMontant.getText().toString();
        String source = actDeviseSource.getText().toString();
        String cible = actDeviseCible.getText().toString();

        if (montant.isEmpty()) {
            tilMontant.setError("Veuillez entrer un montant");
            isValid = false;
        } else {
            tilMontant.setError(null);
        }

        if (source.isEmpty()) {
            tilDeviseSource.setError("Veuillez entrer la devise source");
            isValid = false;
        } else {
            tilDeviseSource.setError(null);
        }

        if (cible.isEmpty()) {
            tilDeviseCible.setError("Veuillez entrer la devise cible");
            isValid = false;
        } else {
            tilDeviseCible.setError(null);
        }

        return isValid;
    }

    private void swapCurrencies() {
        String tempDevise = actDeviseSource.getText().toString();
        actDeviseSource.setText(actDeviseCible.getText().toString());
        actDeviseCible.setText(tempDevise);
    }

    private void setupGrpcChannel() {
        try {
            channel = ManagedChannelBuilder.forAddress("192.168.11.117", 5555)
                    .usePlaintext()
                    .build();
            stub = BankServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            showError("Échec de la configuration du canal gRPC");
        }
    }

    private void convertCurrency() {
        double montant;
        try {
            montant = Double.parseDouble(etMontant.getText().toString());
        } catch (NumberFormatException e) {
            tilMontant.setError("Montant invalide");
            return;
        }

        String deviseSource = actDeviseSource.getText().toString().toUpperCase();
        String deviseCible = actDeviseCible.getText().toString().toUpperCase();

        Bank.ConvertCurrencyRequest request = Bank.ConvertCurrencyRequest.newBuilder()
                .setAmount(montant)
                .setCurrencyFrom(deviseSource)
                .setCurrencyTo(deviseCible)
                .build();

        new Thread(() -> {
            try {
                Bank.ConvertCurrencyResponse response = stub.convert(request);
                runOnUiThread(() -> showResult(response.getResult(), deviseSource, deviseCible));
            } catch (StatusRuntimeException e) {
                runOnUiThread(() -> showError("Échec de la conversion : " + e.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Une erreur s'est produite"));
            }
        }).start();
    }

    private void showResult(double result, String from, String to) {
        cardResult.setVisibility(View.VISIBLE);
        String resultText = String.format("%.2f %s = %.2f %s",
                Double.parseDouble(etMontant.getText().toString()),
                from,
                result,
                to);
        tvResultat.setText(resultText);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
