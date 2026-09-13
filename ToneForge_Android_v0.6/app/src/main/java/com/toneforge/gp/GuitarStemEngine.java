package com.toneforge.gp;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * On-device guitar extraction for full mixes using HT-Demucs 6S ONNX.
 * It analyzes short representative regions rather than keeping a whole separated song in RAM.
 */
final class GuitarStemEngine {
    static final int SAMPLE_RATE = 44_100;
    static final int N = 343_980; // 7.8 s
    private static final int GUITAR_INDEX = 4; // [drums,bass,other,vocals,guitar,piano]

    interface Progress {
        void onProgress(int percent, String label);
    }

    static final class Result {
        final ToneLogic.AudioProfile profile;
        final int referenceSecond;
        final int segmentsTested;
        final int guitarPresence;
        final String mode;

        Result(ToneLogic.AudioProfile profile, int referenceSecond, int segmentsTested,
               int guitarPresence, String mode) {
            this.profile = profile;
            this.referenceSecond = referenceSecond;
            this.segmentsTested = segmentsTested;
            this.guitarPresence = guitarPresence;
            this.mode = mode;
        }

        String summary() {
            return "AI guitar stem · " + guitarPresence + "% presenza · riferimento " +
                    referenceSecond + " s · " + profile.summary();
        }
    }

    private GuitarStemEngine() {}

    static Result analyze(Context context, Uri uri, String targetSection, Progress progress) throws Exception {
        if (progress != null) progress.onProgress(1, "Preparo AI Guitar Extract");
        File model = AiModelManager.ensure(context, (pct, label) -> {
            if (progress != null) progress.onProgress(Math.min(35, pct * 35 / 100), label);
        });

        long durationUs = durationUs(context, uri);
        long segmentUs = Math.round(N * 1_000_000.0 / SAMPLE_RATE);
        long[] starts = chooseStarts(durationUs, segmentUs, targetSection);

        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        OrtSession session = null;
        try {
            if (progress != null) progress.onProgress(38, "Carico il modello AI sul dispositivo");
            session = env.createSession(model.getAbsolutePath(), options);
            Candidate best = null;
            for (int i = 0; i < starts.length; i++) {
                int base = 40 + (i * 55 / starts.length);
                if (progress != null) progress.onProgress(base,
                        "Estraggo la chitarra · zona " + (i + 1) + "/" + starts.length);
                StereoChunk mix = decodeSegment(context, uri, starts[i], segmentUs);
                Candidate c = inferCandidate(env, session, mix, starts[i]);
                if (best == null || c.score > best.score) best = c;
                if (progress != null) progress.onProgress(Math.min(96, base + 45 / starts.length),
                        "Valuto il timbro della chitarra isolata");
            }
            if (best == null) throw new IllegalStateException("Nessun segmento analizzabile");
            if (progress != null) progress.onProgress(100, "AI Guitar Match pronto");
            return new Result(best.profile, (int)(best.startUs / 1_000_000L), starts.length,
                    best.presencePercent, targetSection == null ? "Auto" : targetSection);
        } catch (OutOfMemoryError oom) {
            throw new IllegalStateException("Memoria insufficiente per il separatore AI. Chiudi altre app e riprova.");
        } finally {
            if (session != null) try { session.close(); } catch (Exception ignored) {}
            try { options.close(); } catch (Exception ignored) {}
        }
    }

    private static Candidate inferCandidate(OrtEnvironment env, OrtSession session, StereoChunk mix, long startUs) throws Exception {
        FloatBuffer fb = ByteBuffer.allocateDirect(2 * N * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(mix.left); fb.put(mix.right); fb.rewind();
        float mixRms = rmsStereo(mix.left, mix.right);
        try (OnnxTensor input = OnnxTensor.createTensor(env, fb, new long[]{1, 2, N});
             OrtSession.Result result = session.run(Collections.singletonMap("mix", input))) {
            OnnxValue value = result.get(0);
            if (!(value instanceof OnnxTensor)) throw new IllegalStateException("Output AI inatteso");
            FloatBuffer out = ((OnnxTensor)value).getFloatBuffer();
            if (out == null || out.remaining() < 6 * 2 * N) throw new IllegalStateException("Output AI incompleto");
            int leftBase = (GUITAR_INDEX * 2) * N;
            int rightBase = leftBase + N;
            float[] mono = new float[N];
            double guitarSq = 0;
            for (int i = 0; i < N; i++) {
                float l = out.get(leftBase + i);
                float r = out.get(rightBase + i);
                float m = 0.5f * (l + r);
                mono[i] = m;
                guitarSq += 0.5 * (l * l + r * r);
            }
            float guitarRms = (float)Math.sqrt(guitarSq / N);
            double ratio = guitarRms / Math.max(1e-7, mixRms);
            int presence = (int)Math.round(Math.max(0, Math.min(100, ratio * 115.0)));
            ToneLogic.AudioProfile raw = AudioAnalyzer.analyzeMonoPcm(mono, SAMPLE_RATE, true);
            double levelQuality = clamp01((db(guitarRms) + 52.0) / 34.0);
            double ratioQuality = clamp01(ratio / 0.32);
            double q = 0.35 + 0.65 * Math.sqrt(levelQuality * ratioQuality);
            ToneLogic.AudioProfile adjusted = new ToneLogic.AudioProfile(
                    raw.brightness, raw.gain, raw.body, raw.space, raw.echoMs, raw.echoConfidence,
                    Math.min(0.96, raw.confidence * q), true, raw.seconds);
            double score = guitarRms * (0.45 + 0.55 * ratioQuality);
            return new Candidate(adjusted, startUs, presence, score);
        }
    }

    private static long durationUs(Context context, Uri uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(context, uri);
            String ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (ms != null) return Math.max(1, Long.parseLong(ms)) * 1000L;
        } catch (Exception ignored) {
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
        return 180_000_000L;
    }

    private static long[] chooseStarts(long durationUs, long segmentUs, String section) {
        long max = Math.max(0, durationUs - segmentUs);
        String s = section == null ? "" : section.toLowerCase();
        if (s.contains("intro")) return new long[]{clampStart((long)(durationUs * 0.04), max)};
        if (s.contains("rhythm") || s.contains("ritmica")) return new long[]{clampStart((long)(durationUs * 0.34), max)};
        if (s.contains("lead") || s.contains("solo")) return new long[]{clampStart((long)(durationUs * 0.73), max)};
        if (durationUs <= segmentUs * 2) return new long[]{0};
        return new long[]{
                clampStart((long)(durationUs * 0.12), max),
                clampStart((long)(durationUs * 0.48), max),
                clampStart((long)(durationUs * 0.78), max)
        };
    }

    private static long clampStart(long v, long max) { return Math.max(0, Math.min(max, v)); }

    private static StereoChunk decodeSegment(Context context, Uri uri, long startUs, long durationUs) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(context, uri, null);
            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) { track = i; format = f; break; }
            }
            if (track < 0 || format == null) throw new IllegalArgumentException("Nessuna traccia audio");
            extractor.selectTrack(track);
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) throw new IllegalArgumentException("Formato audio non riconosciuto");
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44_100;
            int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
            int pcmEncoding = AudioFormat.ENCODING_PCM_16BIT;

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();
            FloatCollector left = new FloatCollector((int)Math.min(Integer.MAX_VALUE - 8L, durationUs * sampleRate / 1_000_000L + 8192));
            FloatCollector right = new FloatCollector(left.capacity());
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inDone = false, outDone = false;
            long endUs = startUs + durationUs;
            while (!outDone) {
                if (!inDone) {
                    int ii = codec.dequeueInputBuffer(10_000);
                    if (ii >= 0) {
                        ByteBuffer in = codec.getInputBuffer(ii);
                        if (in == null) throw new IllegalStateException("Decoder input non disponibile");
                        int size = extractor.readSampleData(in, 0);
                        long pts = extractor.getSampleTime();
                        if (size < 0 || pts < 0 || pts > endUs + 500_000L) {
                            codec.queueInputBuffer(ii, 0, 0, Math.max(0, pts), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inDone = true;
                        } else {
                            codec.queueInputBuffer(ii, 0, size, pts, 0);
                            extractor.advance();
                        }
                    }
                }
                int oi = codec.dequeueOutputBuffer(info, 10_000);
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat of = codec.getOutputFormat();
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (android.os.Build.VERSION.SDK_INT >= 24 && of.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcmEncoding = of.getInteger(MediaFormat.KEY_PCM_ENCODING);
                } else if (oi >= 0) {
                    ByteBuffer out = codec.getOutputBuffer(oi);
                    if (out != null && info.size > 0) {
                        ByteBuffer d = out.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                        d.position(info.offset); d.limit(info.offset + info.size);
                        int bytes = pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT ? 4 : 2;
                        int frames = d.remaining() / Math.max(bytes, bytes * channels);
                        for (int f = 0; f < frames; f++) {
                            long pts = info.presentationTimeUs + Math.round(f * 1_000_000.0 / Math.max(1, sampleRate));
                            float l = 0, r = 0;
                            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                if (channels <= 1) { l = clip(d.getFloat()); r = l; }
                                else {
                                    l = clip(d.getFloat()); r = clip(d.getFloat());
                                    for (int c = 2; c < channels; c++) d.getFloat();
                                }
                            } else {
                                if (channels <= 1) { l = d.getShort() / 32768f; r = l; }
                                else {
                                    l = d.getShort() / 32768f; r = d.getShort() / 32768f;
                                    for (int c = 2; c < channels; c++) d.getShort();
                                }
                            }
                            if (pts >= startUs && pts < endUs) { left.add(l); right.add(r); }
                        }
                    }
                    outDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || info.presentationTimeUs > endUs + 100_000L;
                    codec.releaseOutputBuffer(oi, false);
                }
                if (left.size() > (long)sampleRate * 9L) outDone = true;
            }
            if (left.size() < Math.max(2048, sampleRate)) throw new IllegalArgumentException("Segmento audio troppo breve");
            return new StereoChunk(resample(left.toArray(), sampleRate, N), resample(right.toArray(), sampleRate, N));
        } finally {
            try { extractor.release(); } catch (Exception ignored) {}
            if (codec != null) {
                try { codec.stop(); } catch (Exception ignored) {}
                try { codec.release(); } catch (Exception ignored) {}
            }
        }
    }

    private static float[] resample(float[] src, int sr, int outN) {
        float[] out = new float[outN];
        if (src.length == 0) return out;
        double step = sr / (double)SAMPLE_RATE;
        for (int i = 0; i < outN; i++) {
            double pos = i * step;
            int a = (int)pos;
            if (a >= src.length - 1) { out[i] = a < src.length ? src[a] : 0; continue; }
            double t = pos - a;
            out[i] = (float)(src[a] * (1 - t) + src[a + 1] * t);
        }
        return out;
    }

    private static float rmsStereo(float[] l, float[] r) {
        double s = 0; int n = Math.min(l.length, r.length);
        for (int i = 0; i < n; i++) s += 0.5 * (l[i] * l[i] + r[i] * r[i]);
        return (float)Math.sqrt(s / Math.max(1, n));
    }

    private static double db(double rms) { return 20.0 * Math.log10(Math.max(1e-9, rms)); }
    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
    private static float clip(float v) { return Math.max(-1f, Math.min(1f, v)); }

    private static final class Candidate {
        final ToneLogic.AudioProfile profile; final long startUs; final int presencePercent; final double score;
        Candidate(ToneLogic.AudioProfile p, long startUs, int presencePercent, double score) {
            this.profile = p; this.startUs = startUs; this.presencePercent = presencePercent; this.score = score;
        }
    }

    private static final class StereoChunk {
        final float[] left, right;
        StereoChunk(float[] left, float[] right) { this.left = left; this.right = right; }
    }

    private static final class FloatCollector {
        private float[] a; private int n;
        FloatCollector(int initial) { a = new float[Math.max(4096, Math.min(initial, 1_000_000))]; }
        int capacity() { return a.length; }
        int size() { return n; }
        void add(float v) { if (n == a.length) grow(); a[n++] = v; }
        float[] toArray() { float[] o = new float[n]; System.arraycopy(a, 0, o, 0, n); return o; }
        private void grow() { float[] b = new float[Math.min(Integer.MAX_VALUE - 8, a.length + Math.max(4096, a.length / 2))]; System.arraycopy(a, 0, b, 0, n); a = b; }
    }
}
