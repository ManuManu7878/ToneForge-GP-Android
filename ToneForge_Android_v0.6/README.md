# ToneForge GP Android v0.6 — AUTO HYBRID

Questa versione aggiunge un motore **AUTO HYBRID** per GP-5 / GP-50:

- ricerca automatica dei NAM A2 Lite su TONE3000;
- fallback A1 Legacy quando disponibile per il passaggio in Valeton Suite;
- generazione di 3 preset/scena consecutivi: RHYTHM / MAIN / LEAD;
- OD, EQ, delay e reverb differenziati tra le tre scene;
- suggerimento parallelo di **AMP + CAB nativi** realmente disponibili nella famiglia GP-5/GP-50;
- scelta consigliata tra `NATIVO`, `SNAPTONE` oppure `ENTRAMBI` in base al brano.

## Esempi di alternative native

- Plexi / Slash / classic rock: `UK 50JP` o `UK 45` + `UK GRN 4x12`
- AC/DC: `UK 50JP` + `UK GRN 4x12`
- SRV: `Bellman 59B` + `Dark VIT 1x12`
- Fender clean / Mayer: `Dark Twin` + `Dark Twin 2x12`
- Vox: `Foxy 30TB` + `Foxy 2x12`
- Mesa / Metallica: `Mess DualM` + `Mess 4x12`
- Jazz/funk clean: `J-120 CL` + `J-120 2x12`

Il catalogo incorporato è volutamente una piccola mappa tonale curata, non una copia completa dei dati proprietari Valeton.

## Nota sui preset nativi

I `.prst` generati automaticamente in v0.6 sono ancora basati su SnapTone, perché la scrittura binaria affidabile richiede un riferimento SnapTone noto. Il motore AUTO HYBRID suggerisce l'alternativa AMP/CAB nativa e mantiene i valori di OD/EQ/DLY/RVB; per usare la base nativa basta impostare AMP/CAB indicati nella Valeton Suite.

## APK

Il progetto contiene una GitHub Action (`.github/workflows/android.yml`) che genera `app-debug.apk` automaticamente. La toolchain Android/Gradle non è inclusa nel repository.
