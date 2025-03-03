package com.it_nomads.fluttersecurestorage.ciphers;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

class RSACipher18Implementation {

    private static final String KEYSTORE_PROVIDER_ANDROID = "AndroidKeyStore";
    private static final String TYPE_RSA = "RSA";
    private final String KEY_ALIAS;
    private final Context context;


    public RSACipher18Implementation(Context context) throws Exception {
        KEY_ALIAS = context.getPackageName() + ".FlutterSecureStoragePluginKey";
        this.context = context;
        createRSAKeysIfNeeded(context);
    }

    public byte[] wrap(Key key) throws Exception {
        PublicKey publicKey = getPublicKey();
        Cipher cipher = getRSACipher();
        cipher.init(Cipher.WRAP_MODE, publicKey);

        return cipher.wrap(key);
    }

    public Key unwrap(byte[] wrappedKey, String algorithm) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        Cipher cipher = getRSACipher();
        cipher.init(Cipher.UNWRAP_MODE, privateKey);

        return cipher.unwrap(wrappedKey, algorithm, Cipher.SECRET_KEY);
    }

    public byte[] encrypt(byte[] input) throws Exception {
        PublicKey publicKey = getPublicKey();
        Cipher cipher = getRSACipher();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(input);
    }

    public byte[] decrypt(byte[] input) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        Cipher cipher = getRSACipher();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(input);
    }

    private PrivateKey getPrivateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID);
        ks.load(null);

        Key key = ks.getKey(KEY_ALIAS, null);
        if (key == null) {
            throw new Exception("No key found under alias: " + KEY_ALIAS);
        }

        if (!(key instanceof PrivateKey)) {
            throw new Exception("Not an instance of a PrivateKey");
        }

        return (PrivateKey) key;
    }

    private PublicKey getPublicKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID);
        ks.load(null);

        Certificate cert = ks.getCertificate(KEY_ALIAS);
        if (cert == null) {
            throw new Exception("No certificate found under alias: " + KEY_ALIAS);
        }

        PublicKey key = cert.getPublicKey();
        if (key == null) {
            throw new Exception("No key found under alias: " + KEY_ALIAS);
        }

        return key;
    }

    private Cipher getRSACipher() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL"); // error in android 6: InvalidKeyException: Need RSA private or public key
        } else {
            return Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidKeyStoreBCWorkaround"); // error in android 5: NoSuchProviderException: Provider not available: AndroidKeyStoreBCWorkaround
        }
    }

    private void createRSAKeysIfNeeded(Context context) throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID);
        ks.load(null);

        Key privateKey = ks.getKey(KEY_ALIAS, null);
        if (privateKey == null) {
            createKeys(context);
        }
    }

    /**
     * Sets default locale.
     */
    private void setLocale(Locale locale) {
        // Locale.setDefault(locale);
        // Configuration config = context.getResources().getConfiguration();
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //     setSystemLocale(config, locale);
        //     context.createConfigurationContext(config);
        // } else {
        //     setSystemLocaleLegacy(config, locale);
        //     setContextConfigurationLegacy(context, config);
        // }
    }

    @SuppressWarnings("deprecation")
    private void setContextConfigurationLegacy(Context context, Configuration config) {
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    private void setSystemLocaleLegacy(Configuration config, Locale locale) {
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void setSystemLocale(Configuration config, Locale locale) {
        config.setLocale(locale);
    }

    @SuppressWarnings("deprecation")
    private AlgorithmParameterSpec makeAlgorithmParameterSpecLegacy(Context context) {
        return new android.security.KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                .setSerialNumber(BigInteger.valueOf(1))
             //   .setStartDate(start.getTime())
             //   .setEndDate(end.getTime())
                .build();
    }

    private void createKeys(Context context) throws Exception {
        final Locale localeBeforeFakingEnglishLocale = Locale.getDefault();
        try {
            setLocale(Locale.ENGLISH);

            KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(TYPE_RSA, KEYSTORE_PROVIDER_ANDROID);

            AlgorithmParameterSpec spec;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                spec = makeAlgorithmParameterSpecLegacy(context);
            } else {
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                        .setCertificateSubject(new X500Principal("CN=" + KEY_ALIAS))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setCertificateSerialNumber(BigInteger.valueOf(1));
                      //  .setCertificateNotBefore(start.getTime())
                      //  .setCertificateNotAfter(end.getTime());

                spec = builder.build();
            }

            kpGenerator.initialize(spec);
            kpGenerator.generateKeyPair();
        } finally {
            setLocale(localeBeforeFakingEnglishLocale);
        }
    }
}
