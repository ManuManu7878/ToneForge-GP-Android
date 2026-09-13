package com.toneforge.gp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ToneLogic {
    static final class Profile {
        final String name;
        final String searchHint;
        final String recipe;
        Profile(String name, String searchHint, String recipe) {
            this.name = name;
            this.searchHint = searchHint;
            this.recipe = recipe;
        }
    }


    static final class NativeRig {
        final String amp, cab, character, why;
        final int confidence;
        NativeRig(String amp,String cab,String character,int confidence,String why){
            this.amp=amp;this.cab=cab;this.character=character;this.confidence=confidence;this.why=why;
        }
        String summary(){
            return "AMP nativo: "+amp+" • CAB nativo: "+cab+" • "+character+" • confidenza "+confidence+"%\n"+why;
        }
    }

    /**
     * Curated native GP-5 / GP-50 alternatives using model names exposed by Valeton's effect list.
     * This is deliberately a compact tonal map, not a redistributed copy of Valeton's full catalog.
     */
    static NativeRig nativeRigFor(String query) {
        String q=norm(query);
        if(has(q,"slash","sweet child","guns n","gary moore","bonamassa"))
            return new NativeRig("UK 50JP","UK GRN 4x12","Plexi/JMP dinamico, medi avanti",88,"Ottima base nativa. Per il lead molto specifico di Slash, un buon SnapTone 1959SLP/Jubilee resta spesso più fedele.");
        if(has(q,"ac/dc","angus","back in black"))
            return new NativeRig("UK 50JP","UK GRN 4x12","crunch Plexi asciutto e aperto",95,"Qui sceglierei spesso il nativo: poco gain, forte dinamica e pochissimi effetti.");
        if(has(q,"hendrix"))
            return new NativeRig("UK 45","UK GRN 4x12","JTM/Plexi edge-of-breakup",92,"Il nativo è molto adatto; aggiungi fuzz solo quando il brano lo richiede.");
        if(has(q,"srv","stevie ray","pride and joy","texas blues"))
            return new NativeRig("Bellman 59B","Dark VIT 1x12","Fender/Bassman brillante, edge breakup",86,"Per SRV è una buona alternativa nativa; il capture Vibroverb/Super Reverb può essere più specifico.");
        if(has(q,"mayer"))
            return new NativeRig("Dark Twin","Dark Twin 2x12","clean Fender ampio e pedal-platform",84,"Molto valido per la base clean; per Two-Rock/Dumble il SnapTone è più mirato.");
        if(has(q,"knopfler","sultans"))
            return new NativeRig("Dark Twin","Dark VIT 1x12","clean brillante e molto dinamico",90,"Il nativo funziona molto bene; compressione leggera e poco ambiente contano più del capture.");
        if(has(q,"gilmour","pink floyd"))
            return new NativeRig("Dark Twin","Dark Twin 2x12","clean pedal-platform",76,"È una base valida, ma per il carattere Hiwatt preferirei uno SnapTone dedicato.");
        if(has(q,"robben ford","carlton"))
            return new NativeRig("L-Star CL","SUP Star 2x12","clean/drive morbido, medi fluidi",74,"È la migliore approssimazione nativa; per Dumble/ODS preferirei SnapTone.");
        if(has(q,"santana"))
            return new NativeRig("L-Star CL","L-Star 1x12","lead Mesa caldo e compresso",80,"Buona base nativa con OD davanti; un capture Mesa Mark può essere più centrato.");
        if(has(q,"metallica","mesa mark","rectifier","high gain","metal"))
            return new NativeRig("Mess DualM","Mess 4x12","high-gain Mesa stretto e aggressivo",94,"Per ritmiche moderne il nativo è già molto convincente; usa Mess EQ per scolpire il V.");
        if(has(q,"evh","van halen"))
            return new NativeRig("UK 50JP","UK GRN 4x12","hot British / brown-style",82,"Base nativa credibile, ma un capture Plexi variac è più specifico.");
        if(has(q,"kotzen"))
            return new NativeRig("Bad-KT OD","UK GRN 4x12","boutique British hot-rodded",78,"Buona alternativa per il mid-gain; SnapTone Cornford/Victory può essere più preciso.");
        if(has(q,"vox","ac30","queen","brian may"))
            return new NativeRig("Foxy 30TB","Foxy 2x12","AC30 Top Boost brillante",95,"Qui il modello nativo è una prima scelta molto forte.");
        if(has(q,"clean","funk","jazz chorus"))
            return new NativeRig("J-120 CL","J-120 2x12","clean molto definito e neutro",93,"Per clean/funk conviene spesso restare sul nativo.");
        return new NativeRig("UK 45","UK GRN 4x12","base rock/blues versatile",62,"Suggerimento generico: ToneForge confronterà questa base con il NAM trovato su TONE3000.");
    }

    static String sourceRecommendation(String query,double tone3000Score,NativeRig nativeRig){
        String q=norm(query);
        if(has(q,"ac/dc","angus","back in black","knopfler","sultans","vox","ac30","clean","funk") && nativeRig.confidence>=88)
            return "NATIVO consigliato";
        if(has(q,"gilmour","robben ford","carlton","mayer","slash","dumble","hiwatt") && tone3000Score>=18)
            return "SNAPTONE consigliato";
        if(nativeRig.confidence>=92 && tone3000Score<18) return "NATIVO consigliato";
        if(tone3000Score>=22) return "SNAPTONE consigliato";
        return "ENTRAMBI validi: prova prima il nativo";
    }

    static Profile profileFor(String query) {
        String q = norm(query);
        if (has(q,"slash","sweet child","guns n")) return new Profile("PLEXI LEAD","Marshall Plexi Super Lead 1959 hot rodded",
                "SnapTone Marshall/Plexi • OD leggero/boost • medi presenti • delay 320–380 ms • plate corto");
        if (has(q,"srv","stevie ray","pride and joy")) return new Profile("TEXAS BLUES","Fender Super Reverb Vibroverb Texas blues edge breakup",
                "SnapTone Fender/Vibroverb/Super Reverb • Tube Screamer low gain • bass controllati • spring reverb");
        if (has(q,"knopfler","sultans")) return new Profile("BRIGHT CLEAN","Fender clean edge breakup bright",
                "SnapTone Fender clean/edge breakup • compressor leggero • bright EQ • reverb corto");
        if (has(q,"gilmour","pink floyd")) return new Profile("HIWATT","Hiwatt clean pedal platform",
                "SnapTone Hiwatt clean • drive/fuzz secondo parte • delay lungo • plate/hall");
        if (has(q,"robben ford")) return new Profile("DUMBLE","Dumble Overdrive Special smooth mid gain",
                "SnapTone Dumble ODS • boost trasparente • medi fluidi • delay breve • plate");
        if (has(q,"mayer")) return new Profile("DUMBLE CLEAN","Dumble Two Rock Fender clean edge breakup",
                "SnapTone Two-Rock/Dumble/Fender • compressor lieve • OD morbido • spring/plate");
        if (has(q,"bonamassa")) return new Profile("THICK LEAD","Marshall Plexi Dumble thick lead",
                "SnapTone Plexi/Dumble • boost medio • low end stretto • delay 300–360 ms • plate");
        if (has(q,"ac/dc","angus","back in black")) return new Profile("PLEXI CRUNCH","Marshall Plexi Super Lead crunch",
                "SnapTone Plexi • niente compressione forte • gain medio • presence aperta • reverb minimo");
        if (has(q,"evh","van halen")) return new Profile("BROWN","Marshall Plexi variac brown sound",
                "SnapTone Plexi hot/variac • phaser opzionale • plate corto • gain alto ma dinamico");
        if (has(q,"santana")) return new Profile("SINGING LEAD","Mesa Boogie Mark singing lead",
                "SnapTone Mesa/Boogie lead • sustain • medi cantanti • delay breve • plate");
        if (has(q,"carlton")) return new Profile("FUSION","Dumble smooth fusion clean lead",
                "SnapTone Dumble • compressor lieve • OD smooth • delay corto • plate");
        if (has(q,"kotzen")) return new Profile("HOT BRITISH","Marshall Cornford Victory hot rodded mid gain",
                "SnapTone British hot-rodded • boost leggero • gain medio-alto • delay corto");
        if (has(q,"hendrix")) return new Profile("PLEXI FUZZ","Marshall Super Lead Plexi edge breakup fuzz platform",
                "SnapTone Plexi edge breakup • fuzz/boost secondo brano • room/spring");
        if (has(q,"gary moore")) return new Profile("HOT PLEXI","Marshall JTM Plexi hot lead",
                "SnapTone JTM/Plexi • boost • sustain • delay 300 ms • plate");
        if (has(q,"metallica")) return new Profile("TIGHT HIGH GAIN","Mesa Boogie Mark high gain tight",
                "SnapTone Mesa Mark high gain • gate • bass stretto • EQ a V moderata • room minimo");
        return new Profile("AUTO","guitar amp tone " + query,
                "SnapTone scelto automaticamente • 3 scene Rhythm/Main/Lead con OD, EQ, delay e reverb generati");
    }

    static boolean has(String q, String... keys) {
        for (String k : keys) if (q.contains(norm(k))) return true;
        return false;
    }

    static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.replace('’','\'');
    }

    static Set<String> tokens(String s) {
        Set<String> stop = new HashSet<>(Arrays.asList("the","and","with","for","tone","guitar","sound","style","di","il","la","un","una","e","of"));
        Set<String> out = new HashSet<>();
        for (String x : norm(s).split("[^a-z0-9]+")) if (x.length() > 1 && !stop.contains(x)) out.add(x);
        return out;
    }

    static String blob(JSONObject t) {
        StringBuilder b = new StringBuilder();
        for (String k : new String[]{"title","name","description","url"}) b.append(' ').append(t.optString(k,""));
        for (String k : new String[]{"tags","makes"}) {
            JSONArray a = t.optJSONArray(k);
            if (a == null) continue;
            for (int i=0;i<a.length();i++) {
                Object x = a.opt(i);
                if (x instanceof String) b.append(' ').append(x);
                else if (x instanceof JSONObject) {
                    JSONObject o=(JSONObject)x;
                    for (String kk : new String[]{"name","model","make","title"}) b.append(' ').append(o.optString(kk,""));
                }
            }
        }
        JSONObject u=t.optJSONObject("user"); if(u!=null)b.append(' ').append(u.optString("username",""));
        return norm(b.toString());
    }

    static double scoreTone(JSONObject t, String query) {
        String blob=blob(t); double s=0;
        for(String tok:tokens(query)) if(blob.contains(tok)) s+=7;
        s+=Math.min(t.optDouble("downloads_count",0)/5000.0,6);
        s+=Math.min(t.optDouble("favorites_count",0)/500.0,4);
        JSONObject u=t.optJSONObject("user");
        if(t.optBoolean("verified",false)||(u!=null&&u.optBoolean("is_verified",false)))s+=4;
        JSONArray sizes=t.optJSONArray("sizes");
        if(sizes!=null)for(int i=0;i<sizes.length();i++)if("lite".equalsIgnoreCase(sizes.optString(i)))s+=5;
        if(t.optInt("a2_models_count",0)>0)s+=3;
        return s;
    }

    static JSONObject chooseLite(JSONArray models) {
        JSONObject best=null; int bestScore=Integer.MIN_VALUE;
        List<String> pref=Arrays.asList("normal","mid","medium","default","5","6","7","crunch","drive","clean","high");
        for(int i=0;i<models.length();i++) {
            JSONObject m=models.optJSONObject(i); if(m==null||!"lite".equalsIgnoreCase(m.optString("size")))continue;
            String n=norm(m.optString("name")); int s=0;
            for(int j=0;j<pref.size();j++)if(n.contains(pref.get(j)))s+=pref.size()-j;
            if(best==null||s>bestScore){best=m;bestScore=s;}
        }
        return best;
    }

    static JSONObject matchA1(JSONArray models, String a2Name) {
        JSONObject best=null; double bestScore=-1; Set<String> target=tokens(a2Name);
        for(int i=0;i<models.length();i++) {
            JSONObject m=models.optJSONObject(i); if(m==null)continue;
            String name=m.optString("name",""); Set<String> mt=tokens(name);
            int inter=0; for(String t:target)if(mt.contains(t))inter++;
            int union=target.size()+mt.size()-inter;
            double j=union==0?0:(double)inter/union;
            if(norm(name).equals(norm(a2Name)))j+=2;
            if(j>bestScore){best=m;bestScore=j;}
        }
        return bestScore>=0.15?best:null;
    }

    // Fixed storage order in GP-5/GP-50 .prst files.
    static final int BLK_NR=0, BLK_PRE=1, BLK_DST=2, BLK_AMP=3, BLK_CAB=4,
            BLK_EQ=5, BLK_MOD=6, BLK_DLY=7, BLK_RVB=8, BLK_NS=9;

    private static final byte[] REC_MODELS=new byte[]{0x03,0x30,0x28,0x00};
    private static final byte[] REC_BYPASS=new byte[]{0x01,0x30,0x04,0x00};
    private static final byte[] REC_PARAMS=new byte[]{0x04,0x30,0x40,0x01};

    static final class SceneRecipe {
        final String label, shortLabel;
        final boolean odOn, delayOn, reverbOn;
        final int odGain, odTone, odVol;
        final int[] eq;
        final int delayModel, delayMix, delayMs, delayFeedback;
        final int reverbModel, reverbMix, reverbDecay, reverbDamp;
        SceneRecipe(String label,String shortLabel,boolean odOn,int odGain,int odTone,int odVol,int[] eq,
                    boolean delayOn,int delayModel,int delayMix,int delayMs,int delayFeedback,
                    boolean reverbOn,int reverbModel,int reverbMix,int reverbDecay,int reverbDamp) {
            this.label=label; this.shortLabel=shortLabel; this.odOn=odOn; this.odGain=odGain; this.odTone=odTone; this.odVol=odVol;
            this.eq=eq; this.delayOn=delayOn; this.delayModel=delayModel; this.delayMix=delayMix; this.delayMs=delayMs;
            this.delayFeedback=delayFeedback; this.reverbOn=reverbOn; this.reverbModel=reverbModel; this.reverbMix=reverbMix;
            this.reverbDecay=reverbDecay; this.reverbDamp=reverbDamp;
        }
        String summary() {
            return label+": OD "+(odOn?("G"+odGain+" T"+odTone+" V"+odVol):"OFF")+
                    " • EQ ["+eq[0]+","+eq[1]+","+eq[2]+","+eq[3]+","+eq[4]+"]"+
                    " • Delay "+(delayOn?(delayName(delayModel)+" "+delayMs+"ms M"+delayMix+" F"+delayFeedback):"OFF")+
                    " • Reverb "+(reverbOn?(reverbName(reverbModel)+" M"+reverbMix+" D"+reverbDecay):"OFF");
        }
    }

    static final class GeneratedScene {
        final String label, shortLabel, patchName, summary;
        final byte[] bytes;
        GeneratedScene(SceneRecipe r,String patchName,byte[] bytes){
            this.label=r.label;this.shortLabel=r.shortLabel;this.patchName=patchName;this.summary=r.summary();this.bytes=bytes;
        }
    }

    /**
     * Build 3 complete patch variants. We deliberately use consecutive presets instead of pretending
     * the device stores three independent parameter snapshots inside a single .prst: that lets OD,
     * EQ, delay and reverb all change independently between Rhythm/Main/Lead.
     */
    static List<GeneratedScene> buildThreeScenes(byte[] template,String device,int displaySnapSlot,String query) {
        return buildThreeScenes(template,device,displaySnapSlot,query,"");
    }

    static List<GeneratedScene> buildThreeScenes(byte[] template,String device,int displaySnapSlot,String query,String styleHint) {
        validateTemplate(template,device);
        if(displaySnapSlot<51||displaySnapSlot>80) throw new IllegalArgumentException("Slot SnapTone utente: 51–80");
        SceneRecipe[] recipes=sceneRecipes(query+" "+(styleHint==null?"":styleHint));
        List<GeneratedScene> out=new ArrayList<>();
        for(SceneRecipe r:recipes) {
            String name=shortPatchName(query,r.shortLabel);
            out.add(new GeneratedScene(r,name,patchScene(template,device,displaySnapSlot,name,r)));
        }
        return out;
    }

    // Kept for backward compatibility with v0.4 callers/tests: returns the MAIN scene.
    static byte[] patchSnapTone(byte[] data,String device,int displaySlot,String patchName) {
        SceneRecipe main=sceneRecipes(patchName)[1];
        return patchScene(data,device,displaySlot,shortPatchName(patchName,"MAIN"),main);
    }

    private static byte[] patchScene(byte[] data,String device,int displaySlot,String patchName,SceneRecipe r) {
        validateTemplate(data,device);
        int internalSlot=displaySlot-1; // displayed user slots 51..80 => model indices 50..79
        byte[] b=Arrays.copyOf(data,data.length);
        int mb=find(b,REC_MODELS); int bb=find(b,REC_BYPASS); int pb=find(b,REC_PARAMS);
        if(mb<0||bb<0||pb<0) throw new IllegalArgumentException("Template .prst incompleto: mancano record modelli/bypass/parametri");
        mb+=4; bb+=4; pb+=4;

        // SnapTone: model record #9. The reverse-engineered format uses fixed block storage order.
        setModelLowIndex(b,mb,BLK_NS,internalSlot);
        // Green OD = type/index 0; Guitar EQ 1 = 0; delay/reverb indices follow the device list.
        setModelLowIndex(b,mb,BLK_DST,0);
        setModelLowIndex(b,mb,BLK_EQ,0);
        setModelLowIndex(b,mb,BLK_DLY,r.delayModel);
        setModelLowIndex(b,mb,BLK_RVB,r.reverbModel);

        // SnapTone replaces AMP+CAB in this patch. Preserve PRE/NR/MOD state from the template.
        int mask=readLe32(b,bb);
        mask=setBit(mask,BLK_NS,true);
        mask=setBit(mask,BLK_AMP,false);
        mask=setBit(mask,BLK_CAB,false);
        mask=setBit(mask,BLK_DST,r.odOn);
        mask=setBit(mask,BLK_EQ,true);
        mask=setBit(mask,BLK_DLY,r.delayOn);
        mask=setBit(mask,BLK_RVB,r.reverbOn);
        writeLe32(b,bb,mask);

        // Green OD: Gain / Tone / VOL.
        setParam(b,pb,BLK_DST,0,r.odGain);
        setParam(b,pb,BLK_DST,1,r.odTone);
        setParam(b,pb,BLK_DST,2,r.odVol);

        // Guitar EQ 1: five bands (125/400/800/1.6k/4k) + VOL. Valeton stores these as 0..100 values.
        for(int i=0;i<5;i++) setParam(b,pb,BLK_EQ,i,r.eq[i]);
        setParam(b,pb,BLK_EQ,5,50f);

        // Common delay models: Mix / Time(ms) / Feedback / Trail.
        setParam(b,pb,BLK_DLY,0,r.delayMix);
        setParam(b,pb,BLK_DLY,1,r.delayMs);
        setParam(b,pb,BLK_DLY,2,r.delayFeedback);
        setParam(b,pb,BLK_DLY,3,1f);

        // Reverbs share Mix + Decay. Air/Plate add Damp before Trail; Room/Hall/Spring put Trail at algId 2.
        setParam(b,pb,BLK_RVB,0,r.reverbMix);
        setParam(b,pb,BLK_RVB,1,r.reverbDecay);
        if(r.reverbModel==0||r.reverbModel==5) {
            setParam(b,pb,BLK_RVB,2,r.reverbDamp);
            setParam(b,pb,BLK_RVB,3,1f);
        } else {
            setParam(b,pb,BLK_RVB,2,1f);
        }

        writePatchName(b,patchName);
        b[0x14]=(byte)crc8(b,0x15,b.length);
        return b;
    }

    static SceneRecipe[] sceneRecipes(String query) {
        String q=norm(query);
        // Delay: 0 Pure, 1 Analog, 2 Slapback, 4 Tape. Reverb: 1 Room, 2 Hall, 5 Plate, 6 Spring.
        if(has(q,"srv","stevie ray","pride and joy","texas blues")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",true,8,58,62, eq(47,49,53,55,51),false,1,10,105,8,true,6,20,38,50),
                sc("MAIN","MAIN",true,14,60,66, eq(46,49,54,56,52),true,2,10,105,8,true,6,24,44,50),
                sc("LEAD","LEAD",true,20,62,73, eq(45,49,56,59,53),true,1,15,250,18,true,6,27,50,50)
        };
        if(has(q,"slash","sweet child","guns n","gary moore","bonamassa")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,8,52,60, eq(46,49,54,56,52),false,4,10,280,14,true,1,12,28,55),
                sc("MAIN","MAIN",true,10,54,66, eq(45,49,55,58,53),true,4,12,285,16,true,5,14,34,58),
                sc("LEAD","LEAD",true,16,56,75, eq(44,49,57,61,54),true,4,20,350,25,true,5,18,42,58)
        };
        if(has(q,"knopfler","sultans")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(48,49,51,54,57),false,2,8,95,5,true,1,13,26,50),
                sc("MAIN","MAIN",false,0,50,50, eq(47,49,52,55,58),true,2,9,105,6,true,1,16,30,50),
                sc("LEAD","LEAD",true,7,53,62, eq(46,49,54,57,58),true,1,14,190,14,true,5,17,34,58)
        };
        if(has(q,"gilmour","pink floyd")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(47,49,52,55,54),true,4,16,330,25,true,2,16,42,50),
                sc("MAIN","MAIN",true,12,54,65, eq(46,49,54,57,55),true,4,21,410,34,true,2,20,50,50),
                sc("LEAD","LEAD",true,22,57,74, eq(44,49,56,60,56),true,4,26,470,42,true,5,24,58,55)
        };
        if(has(q,"robben ford","carlton","mayer","santana")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(47,50,53,55,51),false,1,8,180,8,true,1,14,30,50),
                sc("MAIN","MAIN",true,9,50,63, eq(46,50,55,57,52),true,1,11,220,12,true,5,16,34,58),
                sc("LEAD","LEAD",true,17,53,72, eq(45,50,57,60,53),true,1,18,310,22,true,5,20,42,58)
        };
        if(has(q,"metallica")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",true,4,61,72, eq(45,47,48,55,57),false,0,8,260,8,true,1,8,22,50),
                sc("MAIN","MAIN",true,5,62,75, eq(44,47,49,56,58),false,0,8,280,8,true,1,9,24,50),
                sc("LEAD","LEAD",true,6,63,78, eq(44,48,52,59,58),true,0,17,330,22,true,5,13,30,60)
        };
        if(has(q,"ac/dc","angus","back in black")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(47,49,53,56,54),false,0,8,240,8,true,1,7,20,50),
                sc("MAIN","MAIN",false,0,50,50, eq(46,49,54,57,55),false,0,8,240,8,true,1,9,23,50),
                sc("LEAD","LEAD",true,7,54,66, eq(45,49,56,59,56),true,1,12,260,13,true,5,11,28,58)
        };
        if(has(q,"high gain","metal","mesa mark","rectifier")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",true,4,61,72, eq(45,47,48,55,57),false,0,8,260,8,true,1,8,22,50),
                sc("MAIN","MAIN",true,5,62,75, eq(44,47,49,56,58),false,0,8,280,8,true,1,9,24,50),
                sc("LEAD","LEAD",true,6,63,78, eq(44,48,52,59,58),true,0,17,330,22,true,5,13,30,60)
        };
        if(has(q,"plexi","marshall","hot british","british hot")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,8,52,60, eq(46,49,54,56,52),false,4,10,280,14,true,1,12,28,55),
                sc("MAIN","MAIN",true,10,54,66, eq(45,49,55,58,53),true,4,12,285,16,true,5,14,34,58),
                sc("LEAD","LEAD",true,16,56,75, eq(44,49,57,61,54),true,4,20,350,25,true,5,18,42,58)
        };
        if(has(q,"dumble","fusion","smooth lead")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(47,50,53,55,51),false,1,8,180,8,true,1,14,30,50),
                sc("MAIN","MAIN",true,9,50,63, eq(46,50,55,57,52),true,1,11,220,12,true,5,16,34,58),
                sc("LEAD","LEAD",true,17,53,72, eq(45,50,57,60,53),true,1,18,310,22,true,5,20,42,58)
        };
        if(has(q,"bright clean","clean","country")) return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(48,49,51,54,57),false,2,8,95,5,true,1,13,26,50),
                sc("MAIN","MAIN",false,0,50,50, eq(47,49,52,55,58),true,2,9,105,6,true,1,16,30,50),
                sc("LEAD","LEAD",true,7,53,62, eq(46,49,54,57,58),true,1,14,190,14,true,5,17,34,58)
        };
        // Generic musical default: progressively more push, mids, ambience and delay.
        return new SceneRecipe[]{
                sc("RHYTHM","RHY",false,0,50,50, eq(47,49,52,54,52),false,1,8,220,10,true,1,10,25,50),
                sc("MAIN","MAIN",true,8,52,62, eq(46,49,54,56,53),true,1,10,240,12,true,5,13,31,58),
                sc("LEAD","LEAD",true,15,55,72, eq(45,49,56,59,54),true,4,18,330,22,true,5,18,40,58)
        };
    }

    private static SceneRecipe sc(String l,String sl,boolean od,int g,int t,int v,int[] eq,
                                  boolean dly,int dm,int mix,int ms,int fb,
                                  boolean rvb,int rm,int rmix,int dec,int damp) {
        return new SceneRecipe(l,sl,od,g,t,v,eq,dly,dm,mix,ms,fb,rvb,rm,rmix,dec,damp);
    }
    private static int[] eq(int a,int b,int c,int d,int e){return new int[]{a,b,c,d,e};}

    private static String shortPatchName(String query,String suffix) {
        String base=safeName(query).replace('_',' ');
        int max=Math.max(1,15-suffix.length());
        if(base.length()>max)base=base.substring(0,max);
        return (base+"-"+suffix).substring(0,Math.min(16,base.length()+1+suffix.length()));
    }

    private static void validateTemplate(byte[] data,String device) {
        int expected="GP5".equals(device)?507:552;
        byte[] magic="GP5".equals(device)?new byte[]{0x47,0x50,0x2d,0x35,0x00}:new byte[]{0x47,0x50,0x2d,0x35,0x30};
        if(data.length!=expected) throw new IllegalArgumentException("Template "+device+" non valido: "+data.length+" byte, attesi "+expected);
        for(int i=0;i<magic.length;i++) if(data[i]!=magic[i]) throw new IllegalArgumentException("Header "+device+" non valido");
    }

    private static void setModelLowIndex(byte[] b,int modelsBase,int block,int index) {
        int off=modelsBase+block*4;
        if(off+3>=b.length) throw new IllegalArgumentException("Record modello fuori range");
        b[off]=(byte)(index&0xff); b[off+1]=0; b[off+2]=0; // preserve category byte at off+3
    }

    private static void setParam(byte[] b,int paramsBase,int block,int alg,float value) {
        int slot=block*8+alg; if(slot<0||slot>=80)return;
        int off=paramsBase+slot*4; int bits=Float.floatToIntBits(value);
        b[off]=(byte)bits;b[off+1]=(byte)(bits>>>8);b[off+2]=(byte)(bits>>>16);b[off+3]=(byte)(bits>>>24);
    }

    private static int readLe32(byte[] b,int off){return (b[off]&255)|((b[off+1]&255)<<8)|((b[off+2]&255)<<16)|((b[off+3]&255)<<24);}
    private static void writeLe32(byte[] b,int off,int v){b[off]=(byte)v;b[off+1]=(byte)(v>>>8);b[off+2]=(byte)(v>>>16);b[off+3]=(byte)(v>>>24);}
    private static int setBit(int mask,int bit,boolean on){return on?(mask|(1<<bit)):(mask&~(1<<bit));}

    private static void writePatchName(byte[] b,String patchName) {
        if(patchName==null)return;
        byte[] n=patchName.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        Arrays.fill(b,0x19,0x29,(byte)0);System.arraycopy(n,0,b,0x19,Math.min(16,n.length));
    }

    private static String delayName(int i){switch(i){case 1:return "Analog";case 2:return "Slapback";case 4:return "Tape";default:return "Pure";}}
    private static String reverbName(int i){switch(i){case 1:return "Room";case 2:return "Hall";case 5:return "Plate";case 6:return "Spring";default:return "Air";}}

    static int find(byte[] hay, byte[] needle) {
        outer: for(int i=0;i<=hay.length-needle.length;i++) {
            for(int j=0;j<needle.length;j++) if(hay[i+j]!=needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    static int crc8(byte[] data, int start, int end) {
        int c=0;
        for(int i=start;i<end;i++) {
            c ^= data[i]&0xff;
            for(int bit=0;bit<8;bit++) c=(c&0x80)!=0?((c<<1)^0x07)&0xff:(c<<1)&0xff;
        }
        return c;
    }

    static String safeName(String s) {
        String n=Normalizer.normalize(s==null?"ToneForge":s,Normalizer.Form.NFD).replaceAll("\\p{M}","");
        n=n.replaceAll("[^A-Za-z0-9._ -]+","").trim().replace(' ','_');
        if(n.isEmpty())n="ToneForge"; return n.substring(0,Math.min(40,n.length()));
    }

    static List<JSONObject> arrayToList(JSONArray a){
        List<JSONObject> out=new ArrayList<>(); if(a==null)return out;
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)out.add(o);} return out;
    }
}
