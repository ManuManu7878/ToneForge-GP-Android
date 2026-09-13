package com.toneforge.gp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 9002;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private T3Client t3;
    private SharedPreferences prefs;

    private EditText query, clientId, snapSlot, presetSlot;
    private Spinner device, pickup, sourceMode, sectionTarget;
    private TextView status, audioStatus, analysisCard, result;
    private ProgressBar busy;
    private Button forge, pickAudio, chooseTone, sendA2, sendFallback, sendScene1, sendScene2, sendScene3, openValeton;
    private Uri audioUri, lastFallbackNamUri, lastA2Uri;
    private ToneLogic.AudioProfile audioProfile;
    private final Uri[] lastSceneUris = new Uri[3];
    private final String[] lastSceneNames = new String[]{"","",""};
    private String lastFallbackName="", lastA2Name="";

    private static final int BG=Color.rgb(8,11,16), CARD=Color.rgb(17,23,32), CARD2=Color.rgb(22,30,42);
    private static final int TEXT=Color.rgb(241,246,252), MUTED=Color.rgb(146,160,181);
    private static final int CYAN=Color.rgb(91,226,255), AMBER=Color.rgb(255,188,92), GREEN=Color.rgb(98,226,154);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        t3=new T3Client(this);
        prefs=getSharedPreferences("toneforge",MODE_PRIVATE);
        setContentView(buildUi());
        refreshState();
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}

    private void handleIntent(Intent intent){
        Uri u=intent==null?null:intent.getData();
        if(u==null||!"toneforge".equals(u.getScheme())||!"oauth".equals(u.getHost()))return;
        setBusy("Ricevo il tone scelto da TONE3000…");
        io.execute(() -> {
            try{
                T3Client.SelectResult selection=t3.finishSelectOAuth(u);
                if(selection.canceled){runOnUiThread(() -> finishBusy("TONE3000 chiuso • AI Tone Engine pronto"));return;}
                processSelectedTone(selection.toneId);
            }catch(Exception e){showError("TONE3000: "+e.getMessage());}
        });
    }

    private View buildUi(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(34));sc.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=tv("TF",20,BG);mark.setTypeface(Typeface.DEFAULT,Typeface.BOLD);mark.setGravity(Gravity.CENTER);mark.setBackground(circle(CYAN));
        hero.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout heroText=new LinearLayout(this);heroText.setOrientation(LinearLayout.VERTICAL);heroText.setPadding(dp(12),0,0,0);
        TextView title=tv("ToneForge GP",28,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heroText.addView(title);
        heroText.addView(tv("AI TONE MATCH  ·  v1.0",11,CYAN));hero.addView(heroText,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        TextView beta=pill("STUDIO",CYAN);hero.addView(beta);
        root.addView(hero);
        TextView tagline=tv("AI guitar isolation + reference matching + real Valeton preset generation.",13,MUTED);tagline.setPadding(0,dp(10),0,0);root.addView(tagline);

        LinearLayout statRow=new LinearLayout(this);statRow.setOrientation(LinearLayout.HORIZONTAL);statRow.setGravity(Gravity.CENTER_VERTICAL);statRow.setPadding(0,dp(14),0,0);
        busy=new ProgressBar(this);busy.setIndeterminate(true);busy.setVisibility(View.GONE);statRow.addView(busy,new LinearLayout.LayoutParams(dp(24),dp(24)));
        status=tv("AI Tone Engine pronto",13,GREEN);status.setPadding(dp(8),0,0,0);statRow.addView(status,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));root.addView(statRow);

        LinearLayout toneCard=card();
        toneCard.addView(kicker("01  TARGET TONE"));
        toneCard.addView(section("Che suono vuoi?"));
        toneCard.addView(body("Scrivi artista + brano. ToneForge combina conoscenza del brano, rig plausibile e — se aggiungi l’audio — analisi della chitarra estratta dal mix."));
        query=input("es. Slash - Sweet Child O' Mine · solo");toneCard.addView(query,lpTop(10));
        root.addView(toneCard,lpTop(18));

        LinearLayout audioCard=card();
        audioCard.addView(kicker("02  REFERENCE AUDIO"));
        audioCard.addView(section("Fagli ascoltare il riferimento"));
        audioCard.addView(body("Aggiungi un file audio. Se è un brano completo, ToneForge usa un separatore AI on-device per isolare prima la chitarra. L’audio non viene caricato online."));
        sourceMode=spinner(new String[]{"Brano completo · AI isola la chitarra","Chitarra isolata / stem"});audioCard.addView(sourceMode,lpTop(10));
        sectionTarget=spinner(new String[]{"Auto · cerca la zona più chitarristica","Intro","Rhythm / ritmica","Lead / Solo"});audioCard.addView(sectionTarget,lpTop(8));
        pickAudio=secondaryButton("＋  Aggiungi audio di riferimento");audioCard.addView(pickAudio,lpTop(8));pickAudio.setOnClickListener(v->pickAudio());
        audioStatus=tv(AiModelManager.isReady(this)?"AI Guitar Extract pronto · nessun audio selezionato":"AI Guitar Extract: al primo mix scarica 136 MB · nessun audio selezionato",12,MUTED);audioStatus.setPadding(0,dp(10),0,0);audioCard.addView(audioStatus);
        analysisCard=panel("Audio Match non ancora analizzato");analysisCard.setVisibility(View.GONE);audioCard.addView(analysisCard,lpTop(10));
        root.addView(audioCard,lpTop(12));

        LinearLayout rigCard=card();
        rigCard.addView(kicker("03  YOUR RIG"));
        rigCard.addView(section("Adatta il preset al tuo setup"));
        LinearLayout two=new LinearLayout(this);two.setOrientation(LinearLayout.HORIZONTAL);two.setWeightSum(2);two.addView(field("PEDALE",device=spinner(new String[]{"GP50","GP5"})),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));two.addView(field("PICKUP",pickup=spinner(new String[]{"Auto","Humbucker","Single coil","P90"})),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));rigCard.addView(two,lpTop(8));
        root.addView(rigCard,lpTop(12));

        forge=primaryButton("CREATE 3 TONES  →");root.addView(forge,lpTop(16));forge.setOnClickListener(v->runNative());
        TextView forgeHint=tv("RHYTHM  ·  MAIN  ·  LEAD   •   AI-matched .prst per Valeton",11,MUTED);forgeHint.setGravity(Gravity.CENTER);forgeHint.setPadding(0,dp(7),0,0);root.addView(forgeHint);

        result=panel("Il risultato apparirà qui: rig scelto, grado di Audio Match e tre preset pronti da aprire nella Valeton Suite.");result.setTextIsSelectable(true);root.addView(result,lpTop(16));

        sendScene1=secondaryButton("Apri RHYTHM in Valeton");sendScene2=secondaryButton("Apri MAIN in Valeton");sendScene3=secondaryButton("Apri LEAD in Valeton");
        sendScene1.setVisibility(View.GONE);sendScene2.setVisibility(View.GONE);sendScene3.setVisibility(View.GONE);
        root.addView(sendScene1,lpTop(8));root.addView(sendScene2,lpTop(7));root.addView(sendScene3,lpTop(7));
        sendScene1.setOnClickListener(v->shareToValeton(lastSceneUris[0],lastSceneNames[0]));sendScene2.setOnClickListener(v->shareToValeton(lastSceneUris[1],lastSceneNames[1]));sendScene3.setOnClickListener(v->shareToValeton(lastSceneUris[2],lastSceneNames[2]));

        LinearLayout pro=card();
        pro.addView(kicker("PRO  ·  SNAPTone / TONE3000"));
        pro.addView(section("NAM opzionale"));
        pro.addView(body("Per chi vuole SnapTone: usa il flusso OAuth ufficiale TONE3000. Il motore nativo sopra non ne ha bisogno."));
        clientId=input("Publishable Client ID TONE3000");clientId.setText(t3.clientId());pro.addView(clientId,lpTop(10));
        chooseTone=secondaryButton("Sfoglia TONE3000");pro.addView(chooseTone,lpTop(8));chooseTone.setOnClickListener(v->startTone3000Select());
        LinearLayout slots=new LinearLayout(this);slots.setOrientation(LinearLayout.HORIZONTAL);slots.setWeightSum(2);
        snapSlot=input("51–80");snapSlot.setInputType(InputType.TYPE_CLASS_NUMBER);snapSlot.setText(String.valueOf(prefs.getInt("snap_slot",51)));
        presetSlot=input("55");presetSlot.setInputType(InputType.TYPE_CLASS_NUMBER);presetSlot.setText(String.valueOf(prefs.getInt("preset_slot",55)));
        slots.addView(field("SNAPTONE SLOT",snapSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));slots.addView(field("PRESET INIZIALE",presetSlot),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));pro.addView(slots,lpTop(8));
        sendA2=secondaryButton("Apri A2 Lite nella Valeton Suite");sendFallback=secondaryButton("Apri A1 Legacy");openValeton=secondaryButton("Apri Valeton Suite");
        sendA2.setVisibility(View.GONE);sendFallback.setVisibility(View.GONE);pro.addView(sendA2,lpTop(8));pro.addView(sendFallback,lpTop(7));pro.addView(openValeton,lpTop(7));
        sendA2.setOnClickListener(v->shareToValeton(lastA2Uri,lastA2Name));sendFallback.setOnClickListener(v->shareToValeton(lastFallbackNamUri,lastFallbackName));openValeton.setOnClickListener(v->launchValeton());
        root.addView(pro,lpTop(18));

        TextView foot=tv("ToneForge v1.0: sui mix completi isola la chitarra on-device con HT-Demucs 6S/ONNX, poi fonde il fingerprint con la knowledge base del brano. Nessun audio viene caricato. Il risultato resta una ricostruzione musicale, non una misura fisica univoca del rig originale. .prst: progetto valeton-gp50 (MIT). AI model + ONNX Runtime: MIT.",11,Color.rgb(104,119,140));foot.setPadding(dp(2),dp(18),dp(2),0);root.addView(foot);
        return sc;
    }

    private void pickAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");startActivityForResult(i,PICK_AUDIO);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req!=PICK_AUDIO||res!=RESULT_OK||data==null||data.getData()==null)return;
        audioUri=data.getData();
        try{getContentResolver().takePersistableUriPermission(audioUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        if(sourceMode.getSelectedItemPosition()==0 && !AiModelManager.isReady(this)){
            new AlertDialog.Builder(this)
                    .setTitle("Installa AI Guitar Extract")
                    .setMessage("Per isolare la chitarra da un brano completo serve il modello HT-Demucs 6S (136 MB). Viene scaricato una sola volta e poi l'analisi resta sul dispositivo. Nessun audio viene inviato online.")
                    .setPositiveButton("Scarica e analizza",(d,w)->analyzeAudio(audioUri))
                    .setNegativeButton("Non ora",null)
                    .show();
        } else analyzeAudio(audioUri);
    }

    private void analyzeAudio(Uri uri){
        final boolean isolated=sourceMode.getSelectedItemPosition()==1;
        final String target=(String)sectionTarget.getSelectedItem();
        audioProfile=null;analysisCard.setVisibility(View.GONE);
        setBusy(isolated?"Analizzo lo stem di chitarra…":"Preparo l'AI Guitar Extract…");
        audioStatus.setText("Analisi: "+displayName(uri));
        io.execute(() -> {
            try{
                if(isolated){
                    ToneLogic.AudioProfile p=AudioAnalyzer.analyze(this,uri,true);audioProfile=p;
                    runOnUiThread(() -> {
                        finishBusy("Stem analizzato");
                        audioStatus.setText("✓ "+displayName(uri)+" · stem diretto");
                        analysisCard.setText("GUITAR FINGERPRINT\n"+p.summary()+"\n\nSorgente: chitarra isolata. Il fingerprint entra direttamente nel motore ToneForge.");
                        analysisCard.setVisibility(View.VISIBLE);
                    });
                } else {
                    GuitarStemEngine.Result r=GuitarStemEngine.analyze(this,uri,target,(pct,label) ->
                            runOnUiThread(() -> { status.setText(label+(pct>0&&pct<100?" · "+pct+"%":"")); status.setTextColor(AMBER); }));
                    audioProfile=r.profile;
                    runOnUiThread(() -> {
                        finishBusy("AI Guitar Match pronto");
                        audioStatus.setText("✓ "+displayName(uri)+" · chitarra estratta dal mix");
                        analysisCard.setText("AI GUITAR STEM\n"+r.summary()+"\n\nIl preset viene corretto sullo stem di chitarra, non sul mix intero. Il motore ha analizzato "+r.segmentsTested+" zona/e e ha scelto il riferimento più utile.");
                        analysisCard.setVisibility(View.VISIBLE);
                    });
                }
            }catch(Exception e){audioProfile=null;showError("Analisi audio: "+e.getMessage());}
        });
    }

    private void runNative(){
        final String q=query.getText().toString().trim();if(q.isEmpty()){toast("Scrivi artista e brano");return;}
        final String dev=(String)device.getSelectedItem();final String pu=(String)pickup.getSelectedItem();final ToneLogic.AudioProfile ap=audioProfile;
        prefs.edit().putString("device",dev).putString("pickup",pu).apply();
        setBusy(ap==null?"Costruisco il tone dalla knowledge base…":"Fondo knowledge base + Audio Match + hardware Valeton…");
        io.execute(() -> {
            try{
                List<ToneLogic.NativePreset> presets=ToneLogic.buildNativePresets(q,dev,ap,pu);
                StringBuilder out=new StringBuilder();
                out.append(ap==null?"KNOWLEDGE MATCH":"AI GUITAR MATCH").append("  ·  ").append(dev).append("\n");
                if(ap!=null)out.append(ap.summary()).append("\n");
                out.append("Pickup: ").append(pu).append("\n\n");
                for(int i=0;i<3;i++){
                    ToneLogic.NativePreset p=presets.get(i);String fileName="ToneForge_"+ToneLogic.safeName(q)+"_"+p.shortLabel+"_"+dev+".prst";
                    Uri uri=saveDownload(fileName,p.bytes,"application/octet-stream");lastSceneUris[i]=uri;lastSceneNames[i]=fileName;
                    out.append(i==0?"RHYTHM  ":i==1?"MAIN     ":"LEAD     ").append(p.summary).append("\n");
                }
                out.append("\n✓ 3 file salvati in Download/ToneForge");
                runOnUiThread(() -> {result.setText(out.toString());finishBusy("3 preset pronti");sendScene1.setVisibility(View.VISIBLE);sendScene2.setVisibility(View.VISIBLE);sendScene3.setVisibility(View.VISIBLE);});
            }catch(Exception e){showError("Preset: "+e.getMessage());}
        });
    }

    private void startTone3000Select(){
        String id=clientId.getText().toString().trim();t3.setClientId(id);if(id.isEmpty()){toast("Inserisci il Publishable Client ID TONE3000");return;}saveSlots();
        try{startActivity(new Intent(Intent.ACTION_VIEW,t3.beginSelectTone()));}catch(Exception e){showError(e.getMessage());}
    }

    private void saveSlots(){String dev=(String)device.getSelectedItem();prefs.edit().putInt("snap_slot",parseInt(snapSlot,51)).putInt("preset_slot",parseInt(presetSlot,55)).putString("device",dev).apply();}

    private void processSelectedTone(long toneId)throws Exception{
        final String q=query.getText().toString().trim().isEmpty()?"Tone":query.getText().toString().trim();
        final String dev=prefs.getString("device",(String)device.getSelectedItem());final int snap=prefs.getInt("snap_slot",parseInt(snapSlot,51));final int pslot=prefs.getInt("preset_slot",parseInt(presetSlot,55));
        if(snap<51||snap>80)throw new IllegalArgumentException("Slot SnapTone: 51–80");int minPreset="GP5".equals(dev)?50:55;if(pslot<minPreset||pslot>97)throw new IllegalArgumentException("Preset iniziale "+dev+": "+minPreset+"–97");
        setBusy("Scarico il NAM scelto su TONE3000…");
        JSONObject tone=t3.tone(toneId,2);JSONArray a2s=t3.models(toneId,2);JSONObject a2=ToneLogic.chooseLite(a2s);if(a2==null){List<JSONObject> any=ToneLogic.arrayToList(a2s);if(any.isEmpty())throw new IllegalStateException("Nessun modello A2");a2=any.get(0);}
        byte[] a2Bytes=t3.download(a2.getString("model_url"));String toneName=tone.optString("title",tone.optString("name",q));String base=ToneLogic.safeName(toneName);String a2File=base+"_A2.nam";Uri a2Uri=saveDownload(a2File,a2Bytes,"application/octet-stream");
        JSONArray a1s=t3.models(toneId,1);JSONObject a1=ToneLogic.matchA1(a1s,a2.optString("name"));Uri a1Uri=null;String a1File="";if(a1!=null){byte[] a1Bytes=t3.download(a1.getString("model_url"));a1File=base+"_A1_Valeton.nam";a1Uri=saveDownload(a1File,a1Bytes,"application/octet-stream");}
        ToneLogic.Profile profile=ToneLogic.profileFor(q);for(int i=0;i<3;i++){lastSceneUris[i]=null;lastSceneNames[i]="";}
        List<ToneLogic.GeneratedScene> scenes=ToneLogic.buildSnapTonePresetsAutomatic(dev,snap,q,profile.name+" "+profile.recipe+" "+ToneLogic.blob(tone));StringBuilder sceneInfo=new StringBuilder();
        for(int i=0;i<scenes.size();i++){ToneLogic.GeneratedScene scene=scenes.get(i);int slot=pslot+i;String fileName=String.format(Locale.ROOT,"%02d-%s_%s_%s.prst",slot,ToneLogic.safeName(q),scene.shortLabel,dev);Uri uri=saveDownload(fileName,scene.bytes,"application/octet-stream");lastSceneUris[i]=uri;lastSceneNames[i]=fileName;sceneInfo.append("\n").append(slot).append(" · ").append(scene.summary);}
        lastA2Uri=a2Uri;lastA2Name=a2File;lastFallbackNamUri=a1Uri;lastFallbackName=a1File;final String a1Info=a1!=null?"A1 Legacy disponibile":"Nessun A1 Legacy corrispondente";final String out="TONE3000 · "+toneName+"\nA2: "+a2.optString("name")+"\n"+a1Info+"\n\n3 preset SnapTone:"+sceneInfo;
        runOnUiThread(() -> {result.setText(out);finishBusy("TONE3000 importato");sendA2.setVisibility(View.VISIBLE);sendFallback.setVisibility(lastFallbackNamUri==null?View.GONE:View.VISIBLE);sendScene1.setVisibility(View.VISIBLE);sendScene2.setVisibility(View.VISIBLE);sendScene3.setVisibility(View.VISIBLE);});
    }

    private void refreshState(){
        device.setSelection("GP5".equals(prefs.getString("device","GP50"))?1:0);String pu=prefs.getString("pickup","Auto");for(int i=0;i<pickup.getCount();i++)if(pu.equals(pickup.getItemAtPosition(i)))pickup.setSelection(i);
        status.setText("✓ AI Tone Engine pronto"+(AiModelManager.isReady(this)?" · Guitar Extract installato":"")+(t3.connected()?" · TONE3000 collegato":""));
    }

    private Uri saveDownload(String name,byte[] bytes,String mime)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,name);v.put(MediaStore.MediaColumns.MIME_TYPE,mime);v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ToneForge");Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new IllegalStateException("Impossibile creare "+name);try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException("Output non disponibile");out.write(bytes);}return uri;}
    private void shareToValeton(Uri uri,String name){if(uri==null){toast("File non disponibile");return;}Intent s=new Intent(Intent.ACTION_SEND);s.setType("application/octet-stream");s.putExtra(Intent.EXTRA_STREAM,uri);s.setClipData(ClipData.newRawUri(name,uri));s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);s.setPackage("com.sonicake.gp_5");try{startActivity(s);}catch(ActivityNotFoundException e){s.setPackage(null);startActivity(Intent.createChooser(s,"Apri file con…"));}}
    private void launchValeton(){Intent i=getPackageManager().getLaunchIntentForPackage("com.sonicake.gp_5");if(i!=null)startActivity(i);else try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.sonicake.gp_5")));}catch(Exception e){toast("Valeton Suite non trovata");}}
    private String displayName(Uri u){String name="audio";try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)name=c.getString(i);}}catch(Exception ignored){}return name;}

    private void setBusy(String s){runOnUiThread(() -> {busy.setVisibility(View.VISIBLE);status.setText(s);status.setTextColor(AMBER);forge.setEnabled(false);});}
    private void finishBusy(String s){busy.setVisibility(View.GONE);status.setText("✓ "+s);status.setTextColor(GREEN);forge.setEnabled(true);}
    private void showError(String s){runOnUiThread(() -> {busy.setVisibility(View.GONE);status.setText("⚠ "+s);status.setTextColor(Color.rgb(255,120,120));result.setText("⚠ "+s);forge.setEnabled(true);});}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private int parseInt(EditText e,int d){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return d;}}

    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(15),dp(16),dp(16));l.setBackground(round(CARD,18));return l;}
    private TextView kicker(String s){TextView t=tv(s,10,CYAN);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLetterSpacing(.13f);t.setPadding(0,0,0,dp(6));return t;}
    private TextView section(String s){TextView t=tv(s,19,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView body(String s){TextView t=tv(s,12,MUTED);t.setPadding(0,dp(5),0,0);return t;}
    private TextView tv(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.15f);return t;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(103,119,140));e.setTextColor(TEXT);e.setTextSize(14);e.setSingleLine(true);e.setPadding(dp(13),dp(11),dp(13),dp(11));e.setBackground(round(CARD2,12));return e;}
    private Spinner spinner(String[] items){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){@Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convertView,parent);v.setTextColor(TEXT);v.setTextSize(13);v.setPadding(dp(12),dp(11),dp(12),dp(11));return v;}};s.setAdapter(a);s.setBackground(round(CARD2,12));return s;}
    private LinearLayout field(String name,View v){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(0,0,dp(7),0);TextView lab=tv(name,10,MUTED);lab.setTypeface(Typeface.DEFAULT,Typeface.BOLD);lab.setPadding(0,0,0,dp(4));l.addView(lab);l.addView(v);return l;}
    private Button primaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BG);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{CYAN,Color.rgb(136,240,255)});g.setCornerRadius(dp(16));b.setBackground(g);b.setPadding(dp(14),dp(12),dp(14),dp(12));return b;}
    private Button secondaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);GradientDrawable g=round(CARD2,12);g.setStroke(dp(1),Color.rgb(48,62,82));b.setBackground(g);return b;}
    private TextView panel(String s){TextView t=tv(s,12,Color.rgb(202,214,229));t.setText(s);t.setPadding(dp(14),dp(13),dp(14),dp(13));GradientDrawable g=round(Color.rgb(12,17,24),14);g.setStroke(dp(1),Color.rgb(35,49,67));t.setBackground(g);return t;}
    private TextView pill(String s,int color){TextView t=tv(s,10,color);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(dp(9),dp(5),dp(9),dp(5));GradientDrawable g=round(Color.argb(38,Color.red(color),Color.green(color),Color.blue(color)),99);g.setStroke(dp(1),color);t.setBackground(g);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable circle(int color){GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(color);return g;}
    private LinearLayout.LayoutParams lpTop(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(top);return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}
}
