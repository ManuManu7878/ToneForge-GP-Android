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
import android.view.Gravity;
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
import java.nio.charset.StandardCharsets;
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
    private Button connect, auto, importTemplate, sendA2, sendFallback, sendScene1, sendScene2, sendScene3, openValeton;
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
        setBusy("Completo il collegamento TONE3000…");
        io.execute(() -> {
            try { t3.finishOAuth(u); runOnUiThread(() -> { status.setText("✓ TONE3000 collegato"); refreshState(); }); }
            catch(Exception e){showError("OAuth: "+e.getMessage());}
        });
    }

    private View buildUi() {
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(Color.rgb(13,16,22));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(28)); sc.addView(root);

        TextView title=tv("ToneForge GP",30,Color.rgb(238,244,251)); title.setTypeface(null,1); root.addView(title);
        TextView sub=tv("Android • AUTO HYBRID: TONE3000 + AMP/CAB nativi → 3 scene",13,Color.rgb(155,168,187)); root.addView(sub);
        status=panel("Controllo configurazione…"); root.addView(status,lpTop(12));

        root.addView(section("TONE3000"),lpTop(18));
        root.addView(label("Publishable Client ID")); clientId=input("t3k_… / client ID"); clientId.setText(t3.clientId()); root.addView(clientId);
        connect=button("Collega TONE3000"); root.addView(connect,lpTop(8));
        connect.setOnClickListener(v -> {
            String id=clientId.getText().toString().trim(); t3.setClientId(id);
            try { startActivity(new Intent(Intent.ACTION_VIEW,t3.beginOAuth())); }
            catch(Exception e){showError(e.getMessage());}
        });

        root.addView(section("Pedale e slot"),lpTop(18));
        device=new Spinner(this); String[] devs={"GP50","GP5"}; ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,devs);device.setAdapter(a); root.addView(device);
        LinearLayout slots=new LinearLayout(this);slots.setOrientation(LinearLayout.HORIZONTAL);slots.setWeightSum(2);
        snapSlot=input("SnapTone utente 51–80"); snapSlot.setInputType(InputType.TYPE_CLASS_NUMBER); snapSlot.setText(String.valueOf(prefs.getInt("snap_slot",51)));
        presetSlot=input("Preset utente"); presetSlot.setInputType(InputType.TYPE_CLASS_NUMBER); presetSlot.setText(String.valueOf(prefs.getInt("preset_slot",55)));
        slots.addView(boxed("SnapTone 51–80",snapSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        slots.addView(boxed("Preset: GP50 55–99 / GP5 50–99",presetSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); root.addView(slots,lpTop(8));

        root.addView(section("Template preset"),lpTop(18));
        TextView explain=tv("Una volta sola: esporta dalla Valeton Suite Android un tuo .prst con SnapTone attivo. ToneForge lo usa come base e genera 3 preset consecutivi (Rhythm / Main / Lead) con OD, EQ, delay e reverb indipendenti.",13,Color.rgb(203,213,225)); root.addView(explain);
        importTemplate=button("Importa template .prst"); root.addView(importTemplate,lpTop(8));
        templateStatus=panel(""); root.addView(templateStatus,lpTop(8));
        importTemplate.setOnClickListener(v -> pickTemplate());

        root.addView(section("Artista / brano"),lpTop(18));
        query=input("es. Slash - Sweet Child O' Mine solo");root.addView(query);
        auto=button("AUTO HYBRID • confronta SnapTone / nativo + crea 3 scene");root.addView(auto,lpTop(8));
        auto.setOnClickListener(v -> runAuto());

        result=panel("Pronto.");result.setTextIsSelectable(true);root.addView(result,lpTop(12));
        sendA2=button("1 • Apri A2 Lite scelto nella Valeton Suite");
        sendFallback=button("Fallback • Apri A1 Legacy corrispondente");
        sendScene1=button("2A • Importa SCENA 1 · RHYTHM");
        sendScene2=button("2B • Importa SCENA 2 · MAIN");
        sendScene3=button("2C • Importa SCENA 3 · LEAD");
        openValeton=button("Apri Valeton Suite");
        root.addView(sendA2,lpTop(8));root.addView(sendFallback,lpTop(8));root.addView(sendScene1,lpTop(8));root.addView(sendScene2,lpTop(8));root.addView(sendScene3,lpTop(8));root.addView(openValeton,lpTop(8));
        sendA2.setVisibility(View.GONE);sendFallback.setVisibility(View.GONE);sendScene1.setVisibility(View.GONE);sendScene2.setVisibility(View.GONE);sendScene3.setVisibility(View.GONE);
        sendA2.setOnClickListener(v -> shareToValeton(lastA2Uri,lastA2Name));
        sendFallback.setOnClickListener(v -> shareToValeton(lastFallbackNamUri,lastFallbackName));
        sendScene1.setOnClickListener(v -> shareToValeton(lastSceneUris[0],lastSceneNames[0]));
        sendScene2.setOnClickListener(v -> shareToValeton(lastSceneUris[1],lastSceneNames[1]));
        sendScene3.setOnClickListener(v -> shareToValeton(lastSceneUris[2],lastSceneNames[2]));
        openValeton.setOnClickListener(v -> launchValeton());

        TextView note=tv("AUTO HYBRID confronta sempre SnapTone e base nativa. I .prst generati automaticamente restano SnapTone-based; per l'alternativa nativa ToneForge indica AMP/CAB reali del GP-5/GP-50 da impostare in Valeton Suite, mantenendo le stesse tre scene OD/EQ/DLY/RVB.",12,Color.rgb(120,134,154)); root.addView(note,lpTop(18));
        return sc;
    }

    private void runAuto() {
        final String q=query.getText().toString().trim(); if(q.isEmpty()){toast("Scrivi artista e brano");return;}
        final String dev=(String)device.getSelectedItem(); final int snap=parseInt(snapSlot,51), pslot=parseInt(presetSlot,55);
        if(snap<51||snap>80){toast("Slot SnapTone utente: 51–80");return;}
        int minPreset="GP5".equals(dev)?50:55;
        if(pslot<minPreset||pslot>97){toast("Per 3 scene consecutive, slot iniziale "+dev+": "+minPreset+"–97");return;}
        prefs.edit().putInt("snap_slot",snap).putInt("preset_slot",pslot).putString("device",dev).apply();
        if(!t3.connected()){toast("Prima collega TONE3000");return;}
        setBusy("Confronto AMP/CAB nativi e migliori NAM A2 Lite su TONE3000…");
        io.execute(() -> {
            try {
                ToneLogic.Profile profile=ToneLogic.profileFor(q);
                List<JSONObject> tones=t3.search(q,profile); if(tones.isEmpty())throw new IllegalStateException("Nessun NAM A2 Lite trovato");
                JSONObject tone=tones.get(0); long toneId=tone.optLong("id");
                JSONArray a2s=t3.models(toneId,2); JSONObject a2=ToneLogic.chooseLite(a2s); if(a2==null)throw new IllegalStateException("Il tone selezionato non contiene A2 Lite");

                byte[] a2Bytes=t3.download(a2.getString("model_url"));
                String base=ToneLogic.safeName(q);String a2File=base+"_A2_Lite.nam";
                Uri a2Uri=saveDownload(a2File,a2Bytes,"application/octet-stream");

                JSONArray a1s=t3.models(toneId,1); JSONObject a1=ToneLogic.matchA1(a1s,a2.optString("name"));
                byte[] transferBytes=a1!=null?t3.download(a1.getString("model_url")):a2Bytes;
                String transferFile=base+(a1!=null?"_A1_Valeton.nam":"_A2_Lite.nam");
                Uri transferUri=a1!=null?saveDownload(transferFile,transferBytes,"application/octet-stream"):a2Uri;

                String presetInfo; StringBuilder sceneInfo=new StringBuilder();
                File tpl=templateFile(dev);
                for(int i=0;i<3;i++){lastSceneUris[i]=null;lastSceneNames[i]="";}
                if(tpl.exists()) {
                    byte[] raw=readFile(tpl);
                    List<ToneLogic.GeneratedScene> scenes=ToneLogic.buildThreeScenes(raw,dev,snap,q,profile.name+" "+profile.recipe+" "+ToneLogic.blob(tone));
                    for(int i=0;i<scenes.size();i++) {
                        ToneLogic.GeneratedScene scene=scenes.get(i); int slot=pslot+i;
                        String fileName=String.format(Locale.ROOT,"%02d-%s_%s_%s.prst",slot,ToneLogic.safeName(q),scene.shortLabel,dev);
                        Uri uri=saveDownload(fileName,scene.bytes,"application/octet-stream");
                        lastSceneUris[i]=uri; lastSceneNames[i]=fileName;
                        sceneInfo.append("\n").append(slot).append(" · ").append(scene.summary);
                    }
                    presetInfo="3 scene create dal tuo template, tutte collegate allo SnapTone "+snap+":"+sceneInfo;
                } else presetInfo="Scene non create: importa una volta un template .prst reale per "+dev+".";

                lastA2Uri=a2Uri;lastA2Name=a2File;lastFallbackNamUri=a1!=null?transferUri:null;lastFallbackName=a1!=null?transferFile:"";
                double score=ToneLogic.scoreTone(tone,q+" "+profile.searchHint);
                ToneLogic.NativeRig nativeRig=ToneLogic.nativeRigFor(q);
                String sourceChoice=ToneLogic.sourceRecommendation(q,score,nativeRig);
                String compat=a1!=null?"✓ trovato A1 Legacy corrispondente: uso questo per Valeton":"⚠ nessun A1 Legacy corrispondente: salvo l'A2 Lite. La compatibilità diretta dipende dalla versione Valeton Suite";
                String out="AUTO HYBRID → "+sourceChoice+"\n\n"+
                        "ALTERNATIVA NATIVA GP-5/GP-50\n"+nativeRig.summary()+"\n\n"+
                        "ALTERNATIVA TONE3000 / SNAPTONE\n"+
                        "Scelto: "+tone.optString("title",tone.optString("name","Tone"))+"\n"+
                        "A2 Lite: "+a2.optString("name")+"\n"+
                        "Match score: "+String.format(Locale.ROOT,"%.1f",score)+"\n"+
                        compat+"\n\n"+
                        "Profilo scene: "+profile.name+"\n"+profile.recipe+"\n\n"+
                        presetInfo+"\n\n"+
                        "Uso pratico: se scegli SnapTone, carica il NAM nello slot "+snap+" e importa RHYTHM/MAIN/LEAD in "+pslot+"–"+(pslot+2)+". Se preferisci il nativo, replica AMP/CAB indicati nella Valeton Suite mantenendo gli stessi OD/EQ/DLY/RVB delle tre scene.";
                runOnUiThread(() -> {
                    result.setText(out);status.setText("✓ Pacchetto mobile creato in Download/ToneForge");
                    sendA2.setVisibility(View.VISIBLE);sendFallback.setVisibility(lastFallbackNamUri==null?View.GONE:View.VISIBLE);
                    sendScene1.setVisibility(lastSceneUris[0]==null?View.GONE:View.VISIBLE);sendScene2.setVisibility(lastSceneUris[1]==null?View.GONE:View.VISIBLE);sendScene3.setVisibility(lastSceneUris[2]==null?View.GONE:View.VISIBLE);
                });
            } catch(Exception e){showError(e.getMessage());}
        });
    }

    private void pickTemplate(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_TEMPLATE);}

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data); if(req!=PICK_TEMPLATE||res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();io.execute(() -> {
            try {byte[] b=readUri(u);String dev=b.length==507?"GP5":b.length==552?"GP50":null;if(dev==null)throw new IllegalArgumentException(".prst non riconosciuto: "+b.length+" byte");
                try(FileOutputStream out=new FileOutputStream(templateFile(dev))){out.write(b);}String msg="✓ Template "+dev+" salvato ("+b.length+" byte)";runOnUiThread(() -> {templateStatus.setText(msg);if("GP5".equals(dev))device.setSelection(1);else device.setSelection(0);});
            }catch(Exception e){showError("Template: "+e.getMessage());}
        });
    }

    private void refreshState(){
        String dev=prefs.getString("device","GP50");device.setSelection("GP5".equals(dev)?1:0);
        status.setText((t3.connected()?"✓ TONE3000 collegato":"TONE3000 non collegato")+" • Android standalone");
        StringBuilder s=new StringBuilder();for(String d:new String[]{"GP50","GP5"})if(templateFile(d).exists())s.append(d).append(" ✓  ");templateStatus.setText(s.length()==0?"Nessun template importato.":s.toString());
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
