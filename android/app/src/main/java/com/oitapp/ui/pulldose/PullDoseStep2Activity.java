package com.oitapp.ui.pulldose;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.oitapp.R;
import com.oitapp.utils.SessionManager;

public class PullDoseStep2Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (new SessionManager(this).redirectToLoginIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_pull_dose_step2);
        setTitle(R.string.pull_dose_step2_title);

        final RadioGroup rgDoseForm = findViewById(R.id.rg_dose_form);
        final TextInputEditText etQuantity = findViewById(R.id.et_quantity);
        final TextInputEditText etShelfLife = findViewById(R.id.et_shelf_life);
        final AutoCompleteTextView actvSealType = findViewById(R.id.actv_seal_type);
        MaterialButton btnNext = findViewById(R.id.btn_step2_next);

        String[] sealTypes = new String[]{"Cap", "Tape", "Tamper Seal"};
        actvSealType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sealTypes));
        actvSealType.setText(sealTypes[0], false);

        btnNext.setOnClickListener(view -> {
            int checkedId = rgDoseForm.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(this, R.string.form_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String form = checkedId == R.id.rb_milk ? "milk" : "powder";
            String quantityText = etQuantity.getText() == null ? "" : etQuantity.getText().toString().trim();
            if (quantityText.isEmpty() || Integer.parseInt(quantityText) <= 0) {
                etQuantity.setError(getString(R.string.quantity_required));
                return;
            }
            String shelfLifeText = etShelfLife.getText() == null ? "" : etShelfLife.getText().toString().trim();
            if ("milk".equals(form) && shelfLifeText.isEmpty()) {
                etShelfLife.setError(getString(R.string.shelf_life_required));
                return;
            }
            Intent current = getIntent();
            Intent nextIntent = new Intent(this, PullDoseStep3Activity.class);
            nextIntent.putExtra("patient_id", current.getIntExtra("patient_id", 0));
            nextIntent.putExtra("dose_choice", current.getStringExtra("dose_choice"));
            nextIntent.putExtra("custom_dose", current.getStringExtra("custom_dose"));
            nextIntent.putExtra("current_dose", current.getStringExtra("current_dose"));
            nextIntent.putExtra("dose_form", form);
            nextIntent.putExtra("quantity", Integer.parseInt(quantityText));
            nextIntent.putExtra("shelf_life_hours", shelfLifeText.isEmpty() ? 0 : Integer.parseInt(shelfLifeText));
            nextIntent.putExtra("seal_type", actvSealType.getText() == null ? "Cap" : actvSealType.getText().toString());
            startActivity(nextIntent);
        });
    }
}
