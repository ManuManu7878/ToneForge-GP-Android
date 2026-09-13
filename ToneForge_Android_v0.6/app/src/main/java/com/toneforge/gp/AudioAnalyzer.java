package com.toneforge.gp;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight on-device reference-audio analyser.
 *
 * This deliberately does not pretend to recover an exact amp model from a mastered mix.
 * It extracts robust-ish timbral cues (brightness/body/saturation proxy), ambience and a
 * conservative echo-period candidate, then ToneLogic uses those features as bounded
 * corrections around the artist/song prior.
 */
final class AudioAnalyzer {
    private static final long MAX_ANALYZE_US = 240_000_000L; // 4 min cap

    private AudioAnalyzer() {}

    static ToneLogic.AudioProfile analyze(Context context, Uri uri, boolean isolatedGuitar) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(context, uri, null);
            int track = -1;
            MediaFormat inputFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    inputFormat = f;
                    break;
                }
            }
            if (track < 0 || inputFormat == null) throw new IllegalArgumentException("Nessuna traccia audio trovata");
            extractor.selectTrack(track);

            String mime = inputFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null) throw new IllegalArgumentException("Formato audio non riconosciuto");
            int sampleRate = inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(inputFormat, null, null, 0);
            codec.start();

            AnalyzerStats stats = new AnalyzerStats(sampleRate, isolatedGuitar);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inDone = false, outDone = false;
            int pcmEncoding = AudioFormat.ENCODING_PCM_16BIT;
            long lastPtsUs = 0;

            while (!outDone) {
                if (!inDone) {
                    int inIndex = codec.dequeueInputBuffer(10_000);
                    if (inIndex >= 0) {
                        ByteBuffer in = codec.getInputBuffer(inIndex);
                        if (in == null) throw new IllegalStateException("Decoder input non disponibile");
                        int size = extractor.readSampleData(in, 0);
                        long pts = extractor.getSampleTime();
                        if (size < 0 || (pts >= 0 && pts > MAX_ANALYZE_US)) {
                            codec.queueInputBuffer(inIndex, 0, 0, Math.max(0, pts), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inDone = true;
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, Math.max(0, pts), 0);
                            lastPtsUs = Math.max(lastPtsUs, pts);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = codec.dequeueOutputBuffer(info, 10_000);
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat of = codec.getOutputFormat();
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (android.os.Build.VERSION.SDK_INT >= 24 && of.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding = of.getInteger(MediaFormat.KEY_PCM_ENCODING);
                    }
                    stats.updateSampleRate(sampleRate);
                } else if (outIndex >= 0) {
                    ByteBuffer out = codec.getOutputBuffer(outIndex);
                    if (out != null && info.size > 0) {
                        ByteBuffer data = out.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                        data.position(info.offset);
                        data.limit(info.offset + info.size);
                        consumePcm(data, pcmEncoding, Math.max(1, channels), stats);
                    }
                    outDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    codec.releaseOutputBuffer(outIndex, false);
                }
            }
            return stats.finish(lastPtsUs / 1000L);
        } finally {
            try { extractor.release(); } catch (Exception ignored) {}
            if (codec != null) {
                try { codec.stop(); } catch (Exception ignored) {}
                try { codec.release(); } catch (Exception ignored) {}
            }
        }
    }

    static ToneLogic.AudioProfile analyzeMonoPcm(float[] mono, int sampleRate, boolean isolatedGuitar) {
        if (mono == null || mono.length < Math.max(4096, sampleRate / 2))
            throw new IllegalArgumentException("Audio troppo breve");
        AnalyzerStats stats = new AnalyzerStats(sampleRate, isolatedGuitar);
        for (float v : mono) stats.addSample(clampSample(v));
        long ms = Math.round(1000.0 * mono.length / Math.max(1, sampleRate));
        return stats.finish(ms);
    }

    private static void consumePcm(ByteBuffer b, int encoding, int channels, AnalyzerStats stats) {
        if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
            int frameBytes = 4 * channels;
            while (b.remaining() >= frameBytes) {
                double mono = 0;
                for (int c = 0; c < channels; c++) mono += clampSample(b.getFloat());
                stats.addSample(mono / channels);
            }
            return;
        }
        // Android decoders normally output 16-bit PCM unless float is explicitly advertised.
        int frameBytes = 2 * channels;
        while (b.remaining() >= frameBytes) {
            double mono = 0;
            for (int c = 0; c < channels; c++) mono += b.getShort() / 32768.0;
            stats.addSample(mono / channels);
        }
    }

    private static double clampSample(double v) { return Math.max(-1.0, Math.min(1.0, v)); }

    private static final class AnalyzerStats {
        private int sampleRate;
        private final boolean isolated;
        private final int fftSize = 2048;
        private final double[] frame = new double[fftSize];
        private int framePos = 0, frameCounter = 0, analyzedFrames = 0;
        private double centroidSum = 0, flatnessSum = 0, lowSum = 0, bodySum = 0, presenceSum = 0, highSum = 0;
        private double rmsSum = 0, crestSum = 0, zcrSum = 0;
        private long totalSamples = 0;

        private int envBlockSamples;
        private int envCount = 0;
        private double envSq = 0;
        private final List<Double> envelope = new ArrayList<>();

        AnalyzerStats(int sampleRate, boolean isolated) {
            this.sampleRate = Math.max(8000, sampleRate);
            this.isolated = isolated;
            this.envBlockSamples = Math.max(80, this.sampleRate / 100); // ~10 ms
        }

        void updateSampleRate(int sr) {
            sampleRate = Math.max(8000, sr);
            envBlockSamples = Math.max(80, sampleRate / 100);
        }

        void addSample(double x) {
            totalSamples++;
            envSq += x * x;
            envCount++;
            if (envCount >= envBlockSamples) {
                envelope.add(Math.sqrt(envSq / envCount));
                envSq = 0;
                envCount = 0;
            }

            frame[framePos++] = x;
            if (framePos == fftSize) {
                // Analyze roughly every 8 frames: enough coverage, modest CPU use.
                if ((frameCounter++ & 7) == 0) analyzeFrame(frame);
                framePos = 0;
            }
        }

        private void analyzeFrame(double[] src) {
            double[] re = new double[fftSize];
            double[] im = new double[fftSize];
            double sq = 0, peak = 0;
            int zc = 0;
            double prev = src[0];
            for (int i = 0; i < fftSize; i++) {
                double x = src[i];
                sq += x * x;
                peak = Math.max(peak, Math.abs(x));
                if (i > 0 && ((x >= 0) != (prev >= 0))) zc++;
                prev = x;
                double w = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (fftSize - 1));
                re[i] = x * w;
            }
            double rms = Math.sqrt(sq / fftSize) + 1e-9;
            fft(re, im);

            double total = 1e-12, weighted = 0, low = 0, body = 0, presence = 0, high = 0;
            double logSum = 0;
            int bins = fftSize / 2;
            for (int k = 1; k < bins; k++) {
                double mag2 = re[k] * re[k] + im[k] * im[k] + 1e-15;
                double hz = (double) k * sampleRate / fftSize;
                total += mag2;
                weighted += hz * mag2;
                logSum += Math.log(mag2);
                if (hz < 250) low += mag2;
                else if (hz < 900) body += mag2;
                else if (hz < 3000) presence += mag2;
                else high += mag2;
            }
            double arithmetic = total / Math.max(1, bins - 1);
            double geometric = Math.exp(logSum / Math.max(1, bins - 1));
            centroidSum += weighted / total;
            flatnessSum += geometric / arithmetic;
            lowSum += low / total;
            bodySum += body / total;
            presenceSum += presence / total;
            highSum += high / total;
            rmsSum += rms;
            crestSum += peak / rms;
            zcrSum += (double) zc / fftSize;
            analyzedFrames++;
        }

        ToneLogic.AudioProfile finish(long decodedMs) {
            if (envCount > 0) envelope.add(Math.sqrt(envSq / Math.max(1, envCount)));
            if (analyzedFrames < 3) throw new IllegalArgumentException("Audio troppo breve per un'analisi affidabile");
            double n = analyzedFrames;
            double centroid = centroidSum / n;
            double flatness = flatnessSum / n;
            double low = lowSum / n;
            double body = bodySum / n;
            double presence = presenceSum / n;
            double high = highSum / n;
            double crest = crestSum / n;
            double zcr = zcrSum / n;

            int brightness = clamp100((centroid - 900) / 28.0 + high * 95 + presence * 28);
            int bodyScore = clamp100(20 + body * 120 + low * 45 - high * 30);
            int gain = clamp100(42 + flatness * 120 + zcr * 90 + (2.8 - crest) * 11 + high * 35);

            Echo echo = detectEcho(envelope);
            int space = clamp100(22 + echo.confidence * 65 + Math.max(0, 3.2 - crest) * 7);
            double coverage = Math.min(1.0, analyzedFrames / 90.0);
            double confidence = (isolated ? 0.86 : 0.56) * (0.65 + 0.35 * coverage);
            // Master mixes are inherently ambiguous because cymbals/bass/vocals contaminate guitar features.
            confidence = Math.min(isolated ? 0.94 : 0.64, confidence);

            long ms = decodedMs > 0 ? decodedMs : (long) (1000.0 * totalSamples / Math.max(1, sampleRate));
            return new ToneLogic.AudioProfile(brightness, gain, bodyScore, space, echo.ms, echo.confidence,
                    confidence, isolated, Math.max(1, ms / 1000));
        }
    }

    private static final class Echo {
        final int ms; final double confidence;
        Echo(int ms, double confidence) { this.ms = ms; this.confidence = confidence; }
    }

    private static Echo detectEcho(List<Double> env) {
        int n = env.size();
        if (n < 120) return new Echo(0, 0);
        double mean = 0;
        for (double v : env) mean += v;
        mean /= n;
        double var = 0;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) { x[i] = env.get(i) - mean; var += x[i] * x[i]; }
        if (var < 1e-10) return new Echo(0, 0);

        int bestLag = 0;
        double best = 0;
        // Envelope bins are ~10 ms. Search 80–650 ms; deliberately conservative.
        for (int lag = 8; lag <= 65 && lag < n / 2; lag++) {
            double num = 0, denA = 0, denB = 0;
            for (int i = lag; i < n; i++) {
                double a = x[i], b = x[i - lag];
                num += a * b; denA += a * a; denB += b * b;
            }
            double corr = num / (Math.sqrt(denA * denB) + 1e-12);
            // Penalize very long lags and common beat-period false positives slightly.
            double score = corr - (lag > 50 ? 0.03 : 0);
            if (score > best) { best = score; bestLag = lag; }
        }
        double conf = Math.max(0, Math.min(1, (best - 0.18) / 0.42));
        return conf > 0.05 ? new Echo(bestLag * 10, conf) : new Echo(0, 0);
    }

    private static int clamp100(double v) { return (int) Math.round(Math.max(0, Math.min(100, v))); }

    private static void fft(double[] re, double[] im) {
        int n = re.length;
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) { j ^= bit; bit >>= 1; }
            j ^= bit;
            if (i < j) {
                double tr = re[i]; re[i] = re[j]; re[j] = tr;
                double ti = im[i]; im[i] = im[j]; im[j] = ti;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            double wlenR = Math.cos(ang), wlenI = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double wr = 1, wi = 0;
                for (int k = 0; k < len / 2; k++) {
                    int u = i + k, v = i + k + len / 2;
                    double vr = re[v] * wr - im[v] * wi;
                    double vi = re[v] * wi + im[v] * wr;
                    re[v] = re[u] - vr; im[v] = im[u] - vi;
                    re[u] += vr; im[u] += vi;
                    double nwr = wr * wlenR - wi * wlenI;
                    wi = wr * wlenI + wi * wlenR; wr = nwr;
                }
            }
        }
    }
}
