package com.toneforge.gp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_TEMPLATE = 9001;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private T3Client t3;
    private SharedPreferences prefs;

    private EditText clientId, query, snapSlot, presetSlot;
    private Spinner device;
    private TextView status, templateStatus, result;
    private Button nativeAuto, chooseTone, importTemplate, sendA2, sendFallback, sendScene1, sendScene2, sendScene3, openValeton;
    private Uri lastFallbackNamUri, lastA2Uri;
    private final Uri[] lastSceneUris=new Uri[3];
    private final String[] lastSceneNames=new String[]{"","",""};
    private String lastFallbackName="", lastA2Name="";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        t3 = new T3Client(this);
        prefs = getSharedPreferences("toneforge", MODE_PRIVATE);
        setContentView(buildUi());
        refreshState();
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri u=intent==null?null:intent.getData();
        if(u==null||!"toneforge".equals(u.getScheme())||!"oauth".equals(u.getHost()))return;
        setBusy("Ricevo il tone scelto da TONE3000…");
        io.execute(() -> {
            try {
                T3Client.SelectResult selection=t3.finishSelectOAuth(u);
                if(selection.canceled){
                    runOnUiThread(() -> {status.setText("TONE3000 chiuso • modalità nativa sempre disponibile"); result.setText("Nessun tone selezionato. Puoi continuare a generare preset nativi senza TONE3000.");});
                    return;
                }
                processSelectedTone(selection.toneId);
            } catch(Exception e){showError("TONE3000: "+e.getMessage());}
        });
    }

    private View buildUi() {
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(Color.rgb(13,16,22));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(28)); sc.addView(root);

        TextView title=tv("ToneForge GP · v0.8",30,Color.rgb(238,244,251)); title.setTypeface(null,1); root.addView(title);
        TextView sub=tv("GP-5 / GP-50 • genera .prst NATIVI pronti • TONE3000 opzionale",13,Color.rgb(155,168,187)); root.addView(sub);
        status=panel("Modalità nativa pronta."); root.addView(status,lpTop(12));

        root.addView(section("Artista / brano"),lpTop(18));
        query=input("es. Slash - Sweet Child O' Mine solo");root.addView(query);
        nativeAuto=button("GENERA 3 PRESET PRONTI (.prst)");root.addView(nativeAuto,lpTop(8));
        nativeAuto.setOnClickListener(v -> runNative());

        root.addView(section("Pedale"),lpTop(18));
        device=new Spinner(this); String[] devs={"GP50","GP5"}; ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,devs);device.setAdapter(a); root.addView(device);

        result=panel("Scrivi artista e brano: ToneForge crea RHYTHM / MAIN / LEAD pronti da importare nella Valeton Suite.");result.setTextIsSelectable(true);root.addView(result,lpTop(12));

        root.addView(section("TONE3000 · opzionale"),lpTop(20));
        TextView t3Info=tv("Usalo solo se vuoi un NAM/SnapTone. ToneForge apre il catalogo ufficiale TONE3000: scegli tu il tone, poi l'app scarica il modello selezionato. La modalità nativa non richiede TONE3000.",13,Color.rgb(203,213,225));root.addView(t3Info);
        root.addView(label("Publishable Client ID TONE3000")); clientId=input("client_id / publishable key"); clientId.setText(t3.clientId()); root.addView(clientId);
        chooseTone=button("Scegli un NAM su TONE3000"); root.addView(chooseTone,lpTop(8));
        chooseTone.setOnClickListener(v -> startTone3000Select());

        LinearLayout slots=new LinearLayout(this);slots.setOrientation(LinearLayout.HORIZONTAL);slots.setWeightSum(2);
        snapSlot=input("51–80"); snapSlot.setInputType(InputType.TYPE_CLASS_NUMBER); snapSlot.setText(String.valueOf(prefs.getInt("snap_slot",51)));
        presetSlot=input("Preset iniziale"); presetSlot.setInputType(InputType.TYPE_CLASS_NUMBER); presetSlot.setText(String.valueOf(prefs.getInt("preset_slot",55)));
        slots.addView(boxed("SnapTone slot",snapSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        slots.addView(boxed("3 preset consecutivi",presetSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); root.addView(slots,lpTop(8));

        root.addView(section("SnapTone automatico"),lpTop(18));
        TextView explain=tv("Nessun template .prst richiesto: se scegli un NAM su TONE3000, ToneForge genera da zero i tre preset RHYTHM / MAIN / LEAD che puntano allo slot SnapTone scelto.",13,Color.rgb(203,213,225)); root.addView(explain);
        templateStatus=panel("✓ Generatore .prst integrato v0.8"); root.addView(templateStatus,lpTop(8));

        sendA2=button("Apri A2 Lite scelto nella Valeton Suite");
        sendFallback=button("Apri A1 Legacy corrispondente");
        sendScene1=button("Importa SCENA 1 · RHYTHM");
        sendScene2=button("Importa SCENA 2 · MAIN");
        sendScene3=button("Importa SCENA 3 · LEAD");
        openValeton=button("Apri Valeton Suite");
        root.addView(sendA2,lpTop(12));root.addView(sendFallback,lpTop(8));root.addView(sendScene1,lpTop(8));root.addView(sendScene2,lpTop(8));root.addView(sendScene3,lpTop(8));root.addView(openValeton,lpTop(8));
        sendA2.setVisibility(View.GONE);sendFallback.setVisibility(View.GONE);sendScene1.setVisibility(View.GONE);sendScene2.setVisibility(View.GONE);sendScene3.setVisibility(View.GONE);
        sendA2.setOnClickListener(v -> shareToValeton(lastA2Uri,lastA2Name));
        sendFallback.setOnClickListener(v -> shareToValeton(lastFallbackNamUri,lastFallbackName));
        sendScene1.setOnClickListener(v -> shareToValeton(lastSceneUris[0],lastSceneNames[0]));
        sendScene2.setOnClickListener(v -> shareToValeton(lastSceneUris[1],lastSceneNames[1]));
        sendScene3.setOnClickListener(v -> shareToValeton(lastSceneUris[2],lastSceneNames[2]));
        openValeton.setOnClickListener(v -> launchValeton());

        TextView note=tv("v0.8 genera direttamente preset NATIVI .prst validi per GP-50 e GP-5: AMP, CAB, drive, EQ, modulazioni, delay e reverb vengono scritti nel file con CRC corretto. TONE3000 resta un'alternativa opzionale per SnapTone/NAM. Formato .prst basato sul progetto open-source valeton-gp50 (MIT).",12,Color.rgb(120,134,154)); root.addView(note,lpTop(18));
        return sc;
    }

    private void runNative() {
        final String q=query.getText().toString().trim();
        if(q.isEmpty()){toast("Scrivi artista e brano");return;}
        final String dev=(String)device.getSelectedItem();
        prefs.edit().putString("device",dev).apply();
        setBusy("Genero 3 preset "+dev+" con modelli nativi Valeton…");
        io.execute(() -> {
            try {
                List<ToneLogic.NativePreset> presets=ToneLogic.buildNativePresets(q,dev);
                StringBuilder out=new StringBuilder();
                out.append("PRESET NATIVI ").append(dev).append(" · pronti da importare\n\n");
                for(int i=0;i<3;i++){
                    ToneLogic.NativePreset p=presets.get(i);
                    String fileName="ToneForge_"+ToneLogic.safeName(q)+"_"+p.shortLabel+"_"+dev+".prst";
                    Uri uri=saveDownload(fileName,p.bytes,"application/octet-stream");
                    lastSceneUris[i]=uri;lastSceneNames[i]=fileName;
                    out.append(i+1).append(". ").append(p.summary).append("\n   file: ").append(fileName).append("\n\n");
                }
                out.append("Tocca uno dei tre pulsanti qui sotto per inviare il .prst alla Valeton Suite. Non serve TONE3000 per questi preset.");
                runOnUiThread(() -> {
                    result.setText(out.toString());
                    status.setText("✓ 3 preset .prst creati in Download/ToneForge");
                    sendScene1.setText("Apri RHYTHM in Valeton");
                    sendScene2.setText("Apri MAIN in Valeton");
                    sendScene3.setText("Apri LEAD in Valeton");
                    sendScene1.setVisibility(View.VISIBLE);sendScene2.setVisibility(View.VISIBLE);sendScene3.setVisibility(View.VISIBLE);
                });
            } catch(Exception e){showError("Preset nativo: "+e.getMessage());}
        });
    }

    private void startTone3000Select(){
        String id=clientId.getText().toString().trim();t3.setClientId(id);
        if(id.isEmpty()){toast("Inserisci prima il Publishable Client ID TONE3000");return;}
        saveSlots();
        try {startActivity(new Intent(Intent.ACTION_VIEW,t3.beginSelectTone()));}
        catch(Exception e){showError(e.getMessage());}
    }

    private void saveSlots(){
        String dev=(String)device.getSelectedItem();int snap=parseInt(snapSlot,51),pslot=parseInt(presetSlot,55);
        prefs.edit().putInt("snap_slot",snap).putInt("preset_slot",pslot).putString("device",dev).apply();
    }

    private void processSelectedTone(long toneId) throws Exception {
        final String q=query.getText().toString().trim().isEmpty()?"Tone":query.getText().toString().trim();
        final String dev=prefs.getString("device",(String)device.getSelectedItem());
        final int snap=prefs.getInt("snap_slot",parseInt(snapSlot,51));
        final int pslot=prefs.getInt("preset_slot",parseInt(presetSlot,55));
        if(snap<51||snap>80)throw new IllegalArgumentException("Slot SnapTone utente: 51–80");
        int minPreset="GP5".equals(dev)?50:55;
        if(pslot<minPreset||pslot>97)throw new IllegalArgumentException("Per 3 scene consecutive, preset iniziale "+dev+": "+minPreset+"–97");

        setBusy("Scarico il NAM scelto su TONE3000…");
        JSONObject tone=t3.tone(toneId,2);
        JSONArray a2s=t3.models(toneId,2);
        JSONObject a2=ToneLogic.chooseLite(a2s);
        if(a2==null){
            List<JSONObject> any=ToneLogic.arrayToList(a2s);
            if(any.isEmpty())throw new IllegalStateException("Il tone selezionato non contiene modelli A2");
            a2=any.get(0);
        }

        byte[] a2Bytes=t3.download(a2.getString("model_url"));
        String toneName=tone.optString("title",tone.optString("name",q));
        String base=ToneLogic.safeName(toneName);
        String a2File=base+"_A2.nam";
        Uri a2Uri=saveDownload(a2File,a2Bytes,"application/octet-stream");

        JSONArray a1s=t3.models(toneId,1);
        JSONObject a1=ToneLogic.matchA1(a1s,a2.optString("name"));
        Uri a1Uri=null;String a1File="";
        if(a1!=null){
            byte[] a1Bytes=t3.download(a1.getString("model_url"));
            a1File=base+"_A1_Valeton.nam";
            a1Uri=saveDownload(a1File,a1Bytes,"application/octet-stream");
        }

        ToneLogic.Profile profile=ToneLogic.profileFor(q);
        ToneLogic.NativeRig nativeRig=ToneLogic.nativeRigFor(q);
        StringBuilder sceneInfo=new StringBuilder();
        for(int i=0;i<3;i++){lastSceneUris[i]=null;lastSceneNames[i]="";}
        List<ToneLogic.GeneratedScene> scenes=ToneLogic.buildSnapTonePresetsAutomatic(dev,snap,q,profile.name+" "+profile.recipe+" "+ToneLogic.blob(tone));
        for(int i=0;i<scenes.size();i++) {
            ToneLogic.GeneratedScene scene=scenes.get(i);int slot=pslot+i;
            String fileName=String.format(Locale.ROOT,"%02d-%s_%s_%s.prst",slot,ToneLogic.safeName(q),scene.shortLabel,dev);
            Uri uri=saveDownload(fileName,scene.bytes,"application/octet-stream");
            lastSceneUris[i]=uri;lastSceneNames[i]=fileName;
            sceneInfo.append("\n").append(slot).append(" · ").append(scene.summary);
        }
        String presetInfo="3 preset SnapTone creati automaticamente per lo slot "+snap+":"+sceneInfo;

        lastA2Uri=a2Uri;lastA2Name=a2File;lastFallbackNamUri=a1Uri;lastFallbackName=a1File;
        final String a1Info=a1!=null?"✓ disponibile anche A1 Legacy: "+a1.optString("name"):"Nessun A1 Legacy corrispondente trovato; resta disponibile l'A2 selezionato.";
        final String out="TONE3000 · tone scelto da te\n\n"+
                "Tone: "+toneName+"\nA2: "+a2.optString("name")+"\n"+a1Info+"\n\n"+
                presetInfo+"\n\n"+
                "CONFRONTO NATIVO\n"+nativeRig.summary()+"\n\n"+
                "ToneForge non ha fatto una ricerca automatica nel catalogo: il NAM è quello selezionato da te nel flusso ufficiale TONE3000.";
        runOnUiThread(() -> {
            result.setText(out);status.setText("✓ TONE3000 importato • file salvati in Download/ToneForge");
            sendA2.setVisibility(View.VISIBLE);sendFallback.setVisibility(lastFallbackNamUri==null?View.GONE:View.VISIBLE);
            sendScene1.setVisibility(lastSceneUris[0]==null?View.GONE:View.VISIBLE);sendScene2.setVisibility(lastSceneUris[1]==null?View.GONE:View.VISIBLE);sendScene3.setVisibility(lastSceneUris[2]==null?View.GONE:View.VISIBLE);
        });
    }

    private void pickTemplate(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_TEMPLATE);}

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(req!=PICK_TEMPLATE||res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();io.execute(() -> {
            try {
                byte[] b=readUri(u);String dev=b.length==507?"GP5":b.length==552?"GP50":null;
                if(dev==null)throw new IllegalArgumentException(".prst non riconosciuto: "+b.length+" byte");
                try(FileOutputStream out=new FileOutputStream(templateFile(dev))){out.write(b);}
                String msg="✓ Template "+dev+" salvato ("+b.length+" byte)";
                runOnUiThread(() -> {templateStatus.setText(msg);if("GP5".equals(dev))device.setSelection(1);else device.setSelection(0);});
            }catch(Exception e){showError("Template: "+e.getMessage());}
        });
    }

    private void refreshState(){
        String dev=prefs.getString("device","GP50");device.setSelection("GP5".equals(dev)?1:0);
        status.setText("✓ Modalità nativa pronta"+(t3.connected()?" • sessione TONE3000 disponibile":" • TONE3000 opzionale"));
        if(templateStatus!=null)templateStatus.setText("✓ Generatore .prst integrato • nessun template richiesto");
    }

    private File templateFile(String dev){return new File(getFilesDir(),"template_"+dev+".prst");}
    private byte[] readFile(File f)throws Exception{try(InputStream in=new java.io.FileInputStream(f)){return readAll(in);}}
    private byte[] readUri(Uri u)throws Exception{try(InputStream in=getContentResolver().openInputStream(u)){return readAll(in);}}
    private static byte[] readAll(InputStream in)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[16384];int n;while((n=in.read(b))!=-1)o.write(b,0,n);return o.toByteArray();}

    private Uri saveDownload(String name, byte[] bytes, String mime)throws Exception{
        ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,name);v.put(MediaStore.MediaColumns.MIME_TYPE,mime);v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ToneForge");
        Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new IllegalStateException("Impossibile creare "+name);
        try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException("Output non disponibile");out.write(bytes);}return uri;
    }

    private void shareToValeton(Uri uri,String name){
        if(uri==null){toast("File non disponibile");return;}
        Intent s=new Intent(Intent.ACTION_SEND);s.setType("application/octet-stream");s.putExtra(Intent.EXTRA_STREAM,uri);s.setClipData(ClipData.newRawUri(name,uri));s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);s.setPackage("com.sonicake.gp_5");
        try{startActivity(s);}catch(ActivityNotFoundException e){s.setPackage(null);startActivity(Intent.createChooser(s,"Apri file con…"));}
    }

    private void launchValeton(){
        Intent i=getPackageManager().getLaunchIntentForPackage("com.sonicake.gp_5");if(i!=null)startActivity(i);else try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.sonicake.gp_5")));}catch(Exception e){toast("Valeton Suite non trovata");}
    }

    private void setBusy(String s){runOnUiThread(() -> {status.setText(s);result.setText(s);});}
    private void showError(String s){runOnUiThread(() -> {status.setText("⚠ "+s);result.setText("⚠ "+s);});}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private int parseInt(EditText e,int d){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return d;}}

    private TextView tv(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.12f);return t;}
    private TextView label(String s){TextView t=tv(s,12,Color.rgb(155,168,187));t.setPadding(0,dp(8),0,dp(4));return t;}
    private TextView section(String s){TextView t=tv(s,18,Color.WHITE);t.setTypeface(null,1);return t;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(100,116,139));e.setTextColor(Color.WHITE);e.setSingleLine(true);e.setPadding(dp(10),dp(9),dp(10),dp(9));e.setBackgroundColor(Color.rgb(16,21,29));return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private TextView panel(String s){TextView t=tv(s,13,Color.rgb(203,213,225));t.setText(s);t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setBackgroundColor(Color.rgb(16,21,29));return t;}
    private LinearLayout boxed(String label,EditText e){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);TextView t=label(label);l.addView(t);l.addView(e);l.setPadding(0,0,dp(6),0);return l;}
    private LinearLayout.LayoutParams lpTop(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(top);return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}
}
