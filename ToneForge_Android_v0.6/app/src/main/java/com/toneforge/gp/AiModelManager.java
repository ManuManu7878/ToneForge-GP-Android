package com.toneforge.gp;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Manages the optional on-device guitar source-separation model. */
final class AiModelManager {
    static final String MODEL_FILE = "htdemucs_6s_fp16weights.onnx";
    static final String MODEL_URL = "https://huggingface.co/StemSplitio/htdemucs-6s-onnx/resolve/main/htdemucs_6s_fp16weights.onnx?download=true";
    static final String MODEL_SHA256 = "7ce55792e2231c93fbf92de95f5fd5b3a5e6c89f7db690dfd693e8f1dce56869";
    static final long EXPECTED_MIN_BYTES = 130L * 1024L * 1024L;

    interface Progress {
        void onProgress(int percent, String label);
    }

    private AiModelManager() {}

    static File modelFile(Context context) {
        File dir = new File(context.getNoBackupFilesDir(), "ai_models");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MODEL_FILE);
    }

    static boolean isReady(Context context) {
        File f = modelFile(context);
        if (!f.isFile() || f.length() < EXPECTED_MIN_BYTES) return false;
        SharedPreferences p = context.getSharedPreferences("toneforge_ai", Context.MODE_PRIVATE);
        return MODEL_SHA256.equals(p.getString("verified_sha256", ""));
    }

    static File ensure(Context context, Progress progress) throws Exception {
        File target = modelFile(context);
        SharedPreferences prefs = context.getSharedPreferences("toneforge_ai", Context.MODE_PRIVATE);
        if (target.isFile() && target.length() >= EXPECTED_MIN_BYTES &&
                MODEL_SHA256.equals(prefs.getString("verified_sha256", ""))) {
            if (progress != null) progress.onProgress(100, "AI Guitar Extract pronto");
            return target;
        }

        File tmp = new File(target.getParentFile(), MODEL_FILE + ".part");
        if (tmp.exists()) tmp.delete();
        if (progress != null) progress.onProgress(0, "Scarico AI Guitar Extract · 136 MB");

        HttpURLConnection c = (HttpURLConnection) new URL(MODEL_URL).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(60_000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "ToneForgeGP/1.0 Android");
        c.connect();
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("Download modello AI: HTTP " + code);
        long total = c.getContentLengthLong();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long done = 0;
        int lastPercent = -1;
        try (BufferedInputStream in = new BufferedInputStream(c.getInputStream(), 1024 * 1024);
             FileOutputStream fos = new FileOutputStream(tmp);
             BufferedOutputStream out = new BufferedOutputStream(fos, 1024 * 1024)) {
            byte[] buf = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (n == 0) continue;
                out.write(buf, 0, n);
                sha.update(buf, 0, n);
                done += n;
                int pct = total > 0 ? (int)Math.min(99, done * 100L / total) : (int)Math.min(99, done / (1400L * 1024L));
                if (pct != lastPercent) {
                    lastPercent = pct;
                    if (progress != null) progress.onProgress(pct, String.format(Locale.ROOT, "Scarico AI Guitar Extract · %d%%", pct));
                }
            }
            out.flush();
            fos.getFD().sync();
        } finally {
            c.disconnect();
        }

        String digest = hex(sha.digest());
        if (!MODEL_SHA256.equalsIgnoreCase(digest)) {
            tmp.delete();
            throw new IllegalStateException("Verifica modello AI fallita");
        }
        if (tmp.length() < EXPECTED_MIN_BYTES) {
            tmp.delete();
            throw new IllegalStateException("Modello AI incompleto");
        }
        if (target.exists() && !target.delete()) throw new IllegalStateException("Impossibile aggiornare il modello AI");
        if (!tmp.renameTo(target)) {
            copy(tmp, target);
            tmp.delete();
        }
        prefs.edit().putString("verified_sha256", MODEL_SHA256).apply();
        if (progress != null) progress.onProgress(100, "AI Guitar Extract pronto");
        return target;
    }

    static void remove(Context context) {
        File f = modelFile(context);
        if (f.exists()) f.delete();
        context.getSharedPreferences("toneforge_ai", Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static void copy(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] b = new byte[1024 * 1024]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            out.getFD().sync();
        }
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte v : b) s.append(String.format(Locale.ROOT, "%02x", v & 0xff));
        return s.toString();
    }
}
