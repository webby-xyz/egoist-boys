package your.package.name;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int CONTACT_PERMISSION = 101;
    private static final int LOCATION_PERMISSION = 102;
    private static final int MEDIA_PICKER = 103;

    private CheckBox contactCheck;
    private CheckBox locationCheck;

    private TextView statusText;
    private TextView selectedMediaText;

    private final ArrayList<Uri> selectedMedia =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        contactCheck =
                findViewById(R.id.contactCheck);

        locationCheck =
                findViewById(R.id.locationCheck);

        statusText =
                findViewById(R.id.statusText);

        selectedMediaText =
                findViewById(R.id.selectedMediaText);

        Button mediaButton =
                findViewById(R.id.photoVideoButton);

        Button backupButton =
                findViewById(R.id.backupButton);

        Button restoreButton =
                findViewById(R.id.restoreButton);

        contactCheck.setOnClickListener(v -> {

            if (contactCheck.isChecked()) {

                if (checkSelfPermission(
                        Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.READ_CONTACTS
                            },
                            CONTACT_PERMISSION
                    );
                }
            }
        });

        locationCheck.setOnClickListener(v -> {

            if (locationCheck.isChecked()) {

                if (checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            LOCATION_PERMISSION
                    );
                }
            }
        });

        mediaButton.setOnClickListener(v ->
                openMediaPicker()
        );

        backupButton.setOnClickListener(v ->
                createBackup()
        );

        restoreButton.setOnClickListener(v ->
                restoreBackup()
        );
    }

    private void openMediaPicker() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("image/* video/*");

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                true
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                intent,
                MEDIA_PICKER
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != MEDIA_PICKER ||
                resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        selectedMedia.clear();

        if (data.getClipData() != null) {

            int count =
                    data.getClipData().getItemCount();

            for (int i = 0; i < count; i++) {

                Uri uri =
                        data.getClipData()
                                .getItemAt(i)
                                .getUri();

                selectedMedia.add(uri);
            }

        } else if (data.getData() != null) {

            selectedMedia.add(data.getData());
        }

        selectedMediaText.setText(
                selectedMedia.size()
                        + " media file(s) selected"
        );
    }

    private void createBackup() {

        try {

            EditText passwordInput =
                    new EditText(this);

            passwordInput.setHint(
                    "Backup password"
            );

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Create Backup")
                    .setMessage(
                            "A recovery password is required."
                    )
                    .setView(passwordInput)
                    .setPositiveButton(
                            "BACKUP",
                            (dialog, which) -> {

                                String password =
                                        passwordInput
                                                .getText()
                                                .toString();

                                if (password.length() < 8) {

                                    Toast.makeText(
                                            this,
                                            "Password कम से कम 8 characters का रखें",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                buildEncryptedBackup(
                                        password
                                );
                            }
                    )
                    .setNegativeButton(
                            "CANCEL",
                            null
                    )
                    .show();

        } catch (Exception e) {

            showError();
        }
    }

    private void buildEncryptedBackup(
            String password
    ) {

        try {

            StringBuilder backup =
                    new StringBuilder();

            backup.append(
                    "PERSONAL_BACKUP\n"
            );

            backup.append(
                    "VERSION=1\n\n"
            );

            if (contactCheck.isChecked()) {

                if (checkSelfPermission(
                        Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED) {

                    backup.append(
                            "=== CONTACTS ===\n"
                    );

                    backup.append(
                            readContacts()
                    );

                    backup.append("\n");
                }
            }

            if (locationCheck.isChecked()) {

                if (checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                    backup.append(
                            "=== LOCATION ===\n"
                    );

                    Location location =
                            getLastKnownLocation();

                    if (location != null) {

                        backup.append(
                                "Latitude="
                        );

                        backup.append(
                                location.getLatitude()
                        );

                        backup.append(
                                "\nLongitude="
                        );

                        backup.append(
                                location.getLongitude()
                        );

                        backup.append("\n");

                    } else {

                        backup.append(
                                "Location unavailable\n"
                        );
                    }
                }
            }

            backup.append(
                    "\n=== SELECTED MEDIA ===\n"
            );

            backup.append(
                    "Count="
            );

            backup.append(
                    selectedMedia.size()
            );

            backup.append("\n");

            byte[] plainData =
                    backup.toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            byte[] encrypted =
                    BackupCrypto.encrypt(
                            plainData,
                            password
                    );

            File backupFile =
                    new File(
                            getFilesDir(),
                            "personal_backup.enc"
                    );

            FileOutputStream output =
                    new FileOutputStream(
                            backupFile
                    );

            output.write(encrypted);
            output.close();

            statusText.setText(
                    "✅ Encrypted backup created\n\n"
                    + backupFile.getAbsolutePath()
            );

            Toast.makeText(
                    this,
                    "Backup created successfully",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            showError();
        }
    }

    private String readContacts() {

        StringBuilder result =
                new StringBuilder();

        ContentResolver resolver =
                getContentResolver();

        Cursor cursor =
                resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        },
                        null,
                        null,
                        null
                );

        if (cursor == null) {
            return "No contacts available\n";
        }

        try {

            while (cursor.moveToNext()) {

                String name =
                        cursor.getString(0);

                String number =
                        cursor.getString(1);

                result.append(name);
                result.append(" : ");
                result.append(number);
                result.append("\n");
            }

        } finally {

            cursor.close();
        }

        return result.toString();
    }

    private Location getLastKnownLocation() {

        android.location.LocationManager manager =
                (android.location.LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        Location best = null;

        try {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                return null;
            }

            Location gps =
                    manager.getLastKnownLocation(
                            android.location.LocationManager.GPS_PROVIDER
                    );

            Location network =
                    manager.getLastKnownLocation(
                            android.location.LocationManager.NETWORK_PROVIDER
                    );

            if (gps != null) {
                best = gps;
            }

            if (network != null &&
                    (best == null ||
                     network.getTime() > best.getTime())) {

                best = network;
            }

        } catch (Exception ignored) {
        }

        return best;
    }

    private void restoreBackup() {

        EditText passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Recovery password"
        );

        new android.app.AlertDialog.Builder(this)
                .setTitle("Restore Backup")
                .setView(passwordInput)
                .setPositiveButton(
                        "RESTORE",
                        (dialog, which) -> {

                            try {

                                String password =
                                        passwordInput
                                                .getText()
                                                .toString();

                                File backupFile =
                                        new File(
                                                getFilesDir(),
                                                "personal_backup.enc"
                                        );

                                if (!backupFile.exists()) {

                                    Toast.makeText(
                                            this,
                                            "Backup नहीं मिला",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                java.io.FileInputStream input =
                                        new java.io.FileInputStream(
                                                backupFile
                                        );

                                ByteArrayOutputStream buffer =
                                        new ByteArrayOutputStream();

                                byte[] temp =
                                        new byte[8192];

                                int length;

                                while ((length =
                                        input.read(temp)) != -1) {

                                    buffer.write(
                                            temp,
                                            0,
                                            length
                                    );
                                }

                                input.close();

                                byte[] decrypted =
                                        BackupCrypto.decrypt(
                                                buffer.toByteArray(),
                                                password
                                        );

                                String result =
                                        new String(
                                                decrypted,
                                                StandardCharsets.UTF_8
                                        );

                                new android.app.AlertDialog.Builder(
                                        this
                                )
                                        .setTitle(
                                                "Backup Restored"
                                        )
                                        .setMessage(result)
                                        .setPositiveButton(
                                                "OK",
                                                null
                                        )
                                        .show();

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Wrong password या invalid backup",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void showError() {

        Toast.makeText(
                this,
                "Operation failed",
                Toast.LENGTH_LONG
        ).show();
    }
}
