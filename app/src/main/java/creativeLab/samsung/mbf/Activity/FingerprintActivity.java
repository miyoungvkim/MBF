package creativeLab.samsung.mbf.Activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import creativeLab.samsung.mbf.R;
import creativeLab.samsung.mbf.Utils.Animation;
import creativeLab.samsung.mbf.Utils.FingerprintHandler;

public class FingerprintActivity extends AppCompatActivity {

    // Variable used for storing the key in the Android Keystore container
    private static final String KEY_NAME = "MBF";
    private KeyStore keyStore;
    private Cipher cipher;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_fingerprint);

        // Initializing both Android Keyguard Manager and Fingerprint Manager
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        textView = findViewById(R.id.errorText);

        // Check whether the device has a Fingerprint sensor.
        if (!fingerprintManager.isHardwareDetected()) {
            /**
             * An error message will be displayed if the device does not contain the fingerprint hardware.
             * However if you plan to implement a default authentication method,
             * you can redirect the user to a default authentication activity from here.
             * Example:
             * Intent intent = new Intent(this, DefaultAuthenticationActivity.class);
             * startActivity(intent);
             */
            textView.setText("Your Device does not have a Fingerprint Sensor");
        } else {
            // Checks whether fingerprint permission is set on manifest
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                textView.setText("Fingerprint authentication permission not enabled");
            } else {
                // Check whether at least one fingerprint is registered
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    textView.setText("Register at least one fingerprint in Settings");
                } else {
                    // Checks whether lock screen security is enabled or not
                    if (!keyguardManager.isKeyguardSecure()) {
                        textView.setText("Lock screen security not enabled in Settings");
                    } else {
                        generateKey();

                        if (cipherInit()) {
                            FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                            FingerprintHandler helper = new FingerprintHandler(this);
                            helper.startAuth(fingerprintManager, cryptoObject);
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }

        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }

        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /**
     * Created by Ravi Tamada on 18/05/16.
     */
    public static class EpisodeListAdapter extends RecyclerView.Adapter<EpisodeListAdapter.MyViewHolder> {

        private Context mContext;
        private List<Animation> albumList;

        public EpisodeListAdapter(Context mContext, List<Animation> albumList) {
            this.mContext = mContext;
            this.albumList = albumList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.animation_card, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            Animation album = albumList.get(position);
            holder.title.setText(album.getName());
            holder.count.setText(album.getNumOfSongs() + " songs");
            // loading album cover using Glide library
            Glide.with(mContext).load(album.getThumbnail()).into(holder.thumbnail);

            holder.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(holder.overflow);
                }
            });

            holder.thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(mContext, "click " + holder.title.getText(), Toast.LENGTH_SHORT).show();
                    mContext.startActivity(new Intent(mContext, PlayActivity_with_tensorflow.class));
                }
            });

        }

        /**
         * Showing popup menu when tapping on 3 dots
         */
        private void showPopupMenu(View view) {
            // inflate menu
            PopupMenu popup = new PopupMenu(mContext, view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_album, popup.getMenu());
            popup.setOnMenuItemClickListener(new MyMenuItemClickListener());
            popup.show();
        }

        @Override
        public int getItemCount() {
            return albumList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView title, count;
            public ImageView thumbnail, overflow;

            public MyViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.title);
                count = view.findViewById(R.id.count);
                thumbnail = view.findViewById(R.id.thumbnail);
                overflow = view.findViewById(R.id.overflow);
            }
        }

        /**
         * Click listener for popup menu items
         */
        class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            public MyMenuItemClickListener() {
            }

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_add_favourite:
                        Toast.makeText(mContext, "Add to favourite", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.action_play_next:
                        Toast.makeText(mContext, "Play next", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                }
                return false;
            }
        }
    }
}