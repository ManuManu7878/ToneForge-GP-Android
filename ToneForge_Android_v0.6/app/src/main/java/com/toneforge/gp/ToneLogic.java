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
    // ---------------------------------------------------------------------
    // v0.8 native .prst builder
    // Format details/blank preset/conversion logic derived from
    // drewmerc302/valeton-gp50 (MIT), Copyright (c) 2026 Andrew Mercurio.
    // The project reverse-engineered GP-50 / GP-5 .prst files and verified its
    // codec byte-for-byte against a preset corpus. ToneForge ports only the
    // small subset needed to create valid presets from a factory blank.
    // ---------------------------------------------------------------------

    static final String OPEN_SOURCE_NOTICE =
            "MIT License\n\nCopyright (c) 2026 Andrew Mercurio\n\n" +
            "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the Software), " +
            "to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, " +
            "and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: " +
            "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. " +
            "THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, " +
            "WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";


    // Canonical factory-empty GP-50 preset published by valeton-gp50.
    private static final String BLANK_GP50_B64 =
            "R1AtNTAAAAAAAAAAAAAAAAAAAQCv/////0dQLTUwAAAAAAAAAAAAAAD/ABAAAQAEAAEAAAACAAQAR1A1MAAAEAABEAQACgAAAAIQBAAIAAAAAQA7AAEgAQAyAiAEAHgAAAADIAEAAAQgBAAAAAAABSAEAGQAAAAGIAEAAAcgAQAACCABAGQJIAEAAAogAQAAAgCGAQEwBAAAAAAAAjAKAAABAgkDBAUGBwgDMCgAGwAAAAAAAAAAAAADAQAABwEAAAo1AAABAAAABAAAAAsLAAAMAAAADwQwQAEAAKBBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoEEAAEhCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgQgAAjEIAAEhCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPBBAABIQgAASEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAASEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEhCAAAAAAAAAAAAAEhCAAAAPwAASEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoEEAAPpDAADwQQAAAAAAAAAAAAAAAAAAAAAAAAAAAADwQQAAAAAAAEhCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEhCAABIQgAASEIAAEhCAABIQgAAAAAAAAAAAAAAAAMACgAAAAAAAAAAAAUF";

    private static final byte[] REC_ORDER_V08=new byte[]{0x02,0x30,0x0a,0x00};
    private static final int NAME_OFF_V08=0x19, NAME_LEN_V08=16, BODY_OFF_V08=0x29, CRC_OFF_V08=0x14, SETTINGS_OFF_V08=0x55;
    private static final byte[] HEADER_GP5_V08=new byte[]{0x47,0x50,0x2d,0x35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
    private static final byte[] SENTINEL_V08=new byte[]{(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
    private static final byte[] BLK_FF_PREFIX_V08=new byte[]{0x01,0x00,0x04,0x00,0x01,0x00,0x00,0x00,0x02,0x00,0x04,0x00};
    private static final byte[] DEVTAG_GP5_V08=new byte[]{0x0a,0x45,0x4d,0x51};
    private static final byte[] BLK_00_PAYLOAD_V08=new byte[]{0x01,0x10,0x04,0x00,0x0a,0x00,0x00,0x00,0x02,0x10,0x04,0x00,0x08,0x00,0x00,0x00};
    private static final byte[] FS_TRAILER_V08=new byte[]{0x03,0x00,0x0a,0x00};
    private static final byte[] FS_TRAILER_GP5_V08=new byte[]{0x03,0x00,0x08,0x00};

    // Exact FXIDs from the GP-50/GP-5 reverse-engineered effect rings.
    private static final int FX_GATE=27;
    private static final int FX_COMP=0;
    private static final int FX_GREEN_OD=50331648;
    private static final int FX_RED_HAZE=50331684;
    private static final int FX_SORA_FUZZ=50331682;
    private static final int FX_DARK_TWIN=117440516;
    private static final int FX_J120=117440532;
    private static final int FX_LSTAR_CL=117440537;
    private static final int FX_BELLMAN_59B=117440548;
    private static final int FX_FOXY_30TB=117440551;
    private static final int FX_UK45=117440554;
    private static final int FX_UK50JP=117440559;
    private static final int FX_UK800=117440565;
    private static final int FX_BAD_KT_OD=117440587;
    private static final int FX_EV51=117440602;
    private static final int FX_FLAGMAN_PLUS=117440605;
    private static final int FX_MESS_DUALM=117440617;
    private static final int FX_DARK_VIT_1X12=167772164;
    private static final int FX_LSTAR_1X12=167772169;
    private static final int FX_FOXY_2X12=167772175;
    private static final int FX_J120_2X12=167772177;
    private static final int FX_DARK_TWIN_2X12=167772178;
    private static final int FX_SUP_STAR_2X12=167772185;
    private static final int FX_EV_4X12=167772192;
    private static final int FX_UK_GRN_4X12=167772194;
    private static final int FX_MESS_4X12=167772196;
    private static final int FX_GUITAR_EQ1=16777269;
    private static final int FX_MESS_EQ=16777276;
    private static final int FX_A_CHORUS=67108864;
    private static final int FX_O_PHASE=67108889;
    private static final int FX_M_VIBE=67108895;
    private static final int FX_DELAY_PURE=184549376;
    private static final int FX_DELAY_ANALOG=184549377;
    private static final int FX_DELAY_TAPE=184549378;
    private static final int FX_DELAY_SLAP=184549381;
    private static final int FX_RVB_ROOM=201326592;
    private static final int FX_RVB_HALL=201326593;
    private static final int FX_RVB_SPRING=201326596;
    private static final int FX_RVB_PLATE=201326607;
    private static final int FX_RVB_PLATE_L=201326608;
    private static final int FX_SNAPTONE_1=251658240;

    static final class NativePreset {
        final String label, shortLabel, patchName, summary;
        final byte[] bytes;
        NativePreset(String label,String shortLabel,String patchName,String summary,byte[] bytes){
            this.label=label;this.shortLabel=shortLabel;this.patchName=patchName;this.summary=summary;this.bytes=bytes;
        }
    }

    private static final class NativeModels {
        final int amp,cab,dst,eq,mod,delay,reverb;
        final String ampName,cabName,dstName,modName,style;
        final boolean compressor,gate;
        NativeModels(int amp,String ampName,int cab,String cabName,int dst,String dstName,int eq,int mod,String modName,
                     int delay,int reverb,boolean compressor,boolean gate,String style){
            this.amp=amp;this.ampName=ampName;this.cab=cab;this.cabName=cabName;this.dst=dst;this.dstName=dstName;
            this.eq=eq;this.mod=mod;this.modName=modName;this.delay=delay;this.reverb=reverb;this.compressor=compressor;this.gate=gate;this.style=style;
        }
    }

    static List<NativePreset> buildNativePresets(String query,String device) {
        if(query==null||query.trim().isEmpty()) throw new IllegalArgumentException("Scrivi artista e brano");
        String dev="GP5".equalsIgnoreCase(device)?"GP5":"GP50";
        NativeModels rig=nativeModelsFor(query);
        SceneRecipe[] scenes=sceneRecipes(query);
        List<NativePreset> out=new ArrayList<>();
        for(int i=0;i<3;i++) {
            SceneRecipe scene=scenes[i];
            byte[] gp50=java.util.Base64.getDecoder().decode(BLANK_GP50_B64);
            int mb=find(gp50,REC_MODELS);int bb=find(gp50,REC_BYPASS);int pb=find(gp50,REC_PARAMS);
            if(mb<0||bb<0||pb<0)throw new IllegalStateException("Preset base non valido");
            mb+=4;bb+=4;pb+=4;

            setModelFxid(gp50,mb,BLK_NR,FX_GATE);
            setModelFxid(gp50,mb,BLK_PRE,FX_COMP);
            setModelFxid(gp50,mb,BLK_DST,rig.dst);
            setModelFxid(gp50,mb,BLK_AMP,rig.amp);
            setModelFxid(gp50,mb,BLK_CAB,rig.cab);
            setModelFxid(gp50,mb,BLK_EQ,rig.eq);
            setModelFxid(gp50,mb,BLK_MOD,rig.mod);
            setModelFxid(gp50,mb,BLK_DLY,delayForScene(query,rig,i));
            setModelFxid(gp50,mb,BLK_RVB,reverbForScene(query,rig,i));
            setModelFxid(gp50,mb,BLK_NS,FX_SNAPTONE_1);

            int mask=0;
            mask=setBit(mask,BLK_NR,rig.gate);
            mask=setBit(mask,BLK_PRE,rig.compressor);
            boolean odOn=scene.odOn || forceDriveForScene(query,i);
            mask=setBit(mask,BLK_DST,odOn);
            mask=setBit(mask,BLK_AMP,true);mask=setBit(mask,BLK_CAB,true);mask=setBit(mask,BLK_EQ,true);
            boolean modOn=modActive(query,i);
            mask=setBit(mask,BLK_MOD,modOn);
            mask=setBit(mask,BLK_DLY,scene.delayOn || forceDelayForScene(query,i));
            mask=setBit(mask,BLK_RVB,true);
            mask=setBit(mask,BLK_NS,false);
            writeLe32(gp50,bb,mask);

            // Gate / compressor.
            setParam(gp50,pb,BLK_NR,0,rig.gate?(has(norm(query),"metallica","metal","high gain")?38f:30f):50f);
            setParam(gp50,pb,BLK_PRE,0,rig.compressor?18f:20f);
            setParam(gp50,pb,BLK_PRE,1,50f);

            // OD/fuzz parameters. Fuzz models expose Fuzz + VOL; Green OD has Gain/Tone/VOL.
            if(rig.dst==FX_RED_HAZE||rig.dst==FX_SORA_FUZZ){
                setParam(gp50,pb,BLK_DST,0, i==0?48f:(i==1?60f:72f));
                setParam(gp50,pb,BLK_DST,1, i==2?62f:56f);
            } else {
                setParam(gp50,pb,BLK_DST,0,scene.odGain);
                setParam(gp50,pb,BLK_DST,1,scene.odTone);
                setParam(gp50,pb,BLK_DST,2,scene.odVol);
            }

            applyAmpParams(gp50,pb,rig.amp,query,i);
            setParam(gp50,pb,BLK_CAB,0,50f);

            // Guitar EQs are true +/- 50 dB-style values in the file, not 0..100.
            for(int band=0;band<5;band++) {
                float v=Math.max(-50,Math.min(50,scene.eq[band]-50));
                if(rig.eq==FX_MESS_EQ && band==2) v += (i<2?-8f:-3f); // classic restrained V, not cartoonishly scooped
                setParam(gp50,pb,BLK_EQ,band,v);
            }
            if(rig.eq==FX_GUITAR_EQ1)setParam(gp50,pb,BLK_EQ,5,50f);

            applyModParams(gp50,pb,rig.mod,query,i);
            int dlyId=delayForScene(query,rig,i);
            setParam(gp50,pb,BLK_DLY,0,scene.delayOn?scene.delayMix:(forceDelayForScene(query,i)?12f:8f));
            setParam(gp50,pb,BLK_DLY,1,delayTimeForScene(query,scene,i));
            setParam(gp50,pb,BLK_DLY,2,scene.delayFeedback);
            setParam(gp50,pb,BLK_DLY,4,1f); // Trail is algId 4 on Pure/Analog/Tape/Slapback

            int rvbId=reverbForScene(query,rig,i);
            setParam(gp50,pb,BLK_RVB,0,reverbMixForScene(query,scene,i));
            if(rvbId==FX_RVB_SPRING){
                setParam(gp50,pb,BLK_RVB,1,Math.max(20,scene.reverbDecay));
                setParam(gp50,pb,BLK_RVB,3,1f);
            } else {
                setParam(gp50,pb,BLK_RVB,2,Math.max(20,scene.reverbDecay));
                if(rvbId==FX_RVB_PLATE){setParam(gp50,pb,BLK_RVB,4,55f);setParam(gp50,pb,BLK_RVB,6,1f);}
                else if(rvbId==FX_RVB_PLATE_L){setParam(gp50,pb,BLK_RVB,6,1f);}
                else setParam(gp50,pb,BLK_RVB,3,1f);
            }

            String patchName=nativePatchName(query,scene.shortLabel);
            writePatchName(gp50,patchName);
            gp50[CRC_OFF_V08]=(byte)crc8(gp50,CRC_OFF_V08+1,gp50.length);
            byte[] finalBytes="GP5".equals(dev)?convertGp50ToGp5(gp50):gp50;
            String summary=scene.label+" · "+rig.ampName+" → "+rig.cabName+
                    " · "+(odOn?rig.dstName+" ON":"drive OFF")+
                    (modOn?" · "+rig.modName:"")+" · "+rig.style;
            out.add(new NativePreset(scene.label,scene.shortLabel,patchName,summary,finalBytes));
        }
        return out;
    }

    private static NativeModels nativeModelsFor(String query){
        String q=norm(query);
        if(has(q,"metallica","enter sandman","master of puppets","metal","rectifier","high gain"))
            return new NativeModels(FX_MESS_DUALM,"Mess DualM",FX_MESS_4X12,"Mess 4x12",FX_GREEN_OD,"Green OD",FX_MESS_EQ,FX_A_CHORUS,"A-Chorus",FX_DELAY_PURE,FX_RVB_ROOM,false,true,"tight modern/high-gain");
        if(has(q,"srv","stevie ray","pride and joy","texas flood"))
            return new NativeModels(FX_BELLMAN_59B,"Bellman 59B",FX_DARK_VIT_1X12,"Dark VIT 1x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_SPRING,false,false,"Texas blues edge-of-breakup");
        if(has(q,"knopfler","sultans","dire straits"))
            return new NativeModels(FX_DARK_TWIN,"Dark Twin",FX_DARK_VIT_1X12,"Dark VIT 1x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_SLAP,FX_RVB_ROOM,true,false,"bright compressed clean");
        if(has(q,"gilmour","pink floyd","comfortably numb","time solo"))
            return new NativeModels(FX_DARK_TWIN,"Dark Twin",FX_DARK_TWIN_2X12,"Dark Twin 2x12",FX_SORA_FUZZ,"Sora Fuzz",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_PURE,FX_RVB_HALL,false,false,"wide pedal-platform lead");
        if(has(q,"robben ford","larry carlton","carlton","dumble","fusion"))
            return new NativeModels(FX_LSTAR_CL,"L-Star CL",FX_SUP_STAR_2X12,"SUP Star 2x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_PLATE,false,false,"smooth fusion / D-style approximation");
        if(has(q,"mayer","john mayer","slow dancing","gravity"))
            return new NativeModels(FX_DARK_TWIN,"Dark Twin",FX_DARK_TWIN_2X12,"Dark Twin 2x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_SPRING,true,false,"large clean pedal-platform");
        if(has(q,"santana"))
            return new NativeModels(FX_LSTAR_CL,"L-Star CL",FX_LSTAR_1X12,"L-Star 1x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_PLATE,false,false,"singing mid-forward lead");
        if(has(q,"evh","van halen","eruption","panama"))
            return new NativeModels(FX_UK50JP,"UK 50JP",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_O_PHASE,"O-Phase",FX_DELAY_TAPE,FX_RVB_PLATE,false,false,"brown-style hot British");
        if(has(q,"hendrix","little wing","purple haze","voodoo child"))
            return new NativeModels(FX_UK45,"UK 45",FX_UK_GRN_4X12,"UK GRN 4x12",FX_RED_HAZE,"Red Haze",FX_GUITAR_EQ1,FX_M_VIBE,"M-Vibe",FX_DELAY_TAPE,FX_RVB_SPRING,false,false,"JTM/Plexi fuzz platform");
        if(has(q,"kotzen","richie kotzen"))
            return new NativeModels(FX_BAD_KT_OD,"Bad-KT OD",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_PLATE,false,false,"hot-rodded boutique British");
        if(has(q,"queen","brian may","bohemian rhapsody"))
            return new NativeModels(FX_FOXY_30TB,"Foxy 30TB",FX_FOXY_2X12,"Foxy 2x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_TAPE,FX_RVB_PLATE,false,false,"AC30 Top Boost lead");
        if(has(q,"clean","funk","jazz chorus","chic","nile rodgers"))
            return new NativeModels(FX_J120,"J-120 CL",FX_J120_2X12,"J-120 2x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_SLAP,FX_RVB_ROOM,true,false,"clean/funk");
        if(has(q,"slash","sweet child","guns n","gary moore","bonamassa"))
            return new NativeModels(FX_UK50JP,"UK 50JP",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_TAPE,FX_RVB_PLATE,false,false,"Plexi/JMP lead");
        if(has(q,"ac/dc","angus","back in black","highway to hell"))
            return new NativeModels(FX_UK50JP,"UK 50JP",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_ROOM,false,false,"dry open Plexi crunch");
        if(has(q,"jcm800","80s rock","hard rock"))
            return new NativeModels(FX_UK800,"UK 800",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_PLATE,false,false,"JCM800 hard rock");
        return new NativeModels(FX_UK45,"UK 45",FX_UK_GRN_4X12,"UK GRN 4x12",FX_GREEN_OD,"Green OD",FX_GUITAR_EQ1,FX_A_CHORUS,"A-Chorus",FX_DELAY_ANALOG,FX_RVB_PLATE,false,false,"versatile rock/blues");
    }

    private static void applyAmpParams(byte[] b,int pb,int amp,String query,int scene){
        String q=norm(query);
        float g=scene==0?35f:(scene==1?45f:55f);
        if(has(q,"ac/dc","angus","back in black"))g=scene==0?34f:(scene==1?40f:46f);
        if(has(q,"slash","sweet child","gary moore","bonamassa"))g=scene==0?44f:(scene==1?52f:58f);
        if(has(q,"metallica","metal","high gain"))g=scene==0?54f:(scene==1?58f:62f);
        if(amp==FX_DARK_TWIN){
            setParam(b,pb,BLK_AMP,0,has(q,"gilmour")?35f:30f);setParam(b,pb,BLK_AMP,1,52f);
            setParam(b,pb,BLK_AMP,2,45f);setParam(b,pb,BLK_AMP,3,scene==2?48f:42f);setParam(b,pb,BLK_AMP,4,62f);setParam(b,pb,BLK_AMP,5,1f);
        } else if(amp==FX_J120){
            setParam(b,pb,BLK_AMP,0,55f);setParam(b,pb,BLK_AMP,1,48f);setParam(b,pb,BLK_AMP,2,50f);setParam(b,pb,BLK_AMP,3,62f);setParam(b,pb,BLK_AMP,4,1f);
        } else if(amp==FX_UK50JP){
            setParam(b,pb,BLK_AMP,0,g);setParam(b,pb,BLK_AMP,1,58f);setParam(b,pb,BLK_AMP,2,52f);
            setParam(b,pb,BLK_AMP,3,44f);setParam(b,pb,BLK_AMP,4,scene==2?62f:56f);setParam(b,pb,BLK_AMP,5,58f);setParam(b,pb,BLK_AMP,6,scene==2?55f:48f);
        } else if(amp==FX_UK45||amp==FX_UK800||amp==FX_LSTAR_CL||amp==FX_BELLMAN_59B||amp==FX_BAD_KT_OD||amp==FX_MESS_DUALM||amp==FX_FLAGMAN_PLUS){
            setParam(b,pb,BLK_AMP,0,g);setParam(b,pb,BLK_AMP,1,55f);setParam(b,pb,BLK_AMP,2,52f);
            setParam(b,pb,BLK_AMP,3,amp==FX_MESS_DUALM?44f:46f);setParam(b,pb,BLK_AMP,4,amp==FX_MESS_DUALM?42f:(scene==2?58f:52f));setParam(b,pb,BLK_AMP,5,57f);
        } else if(amp==FX_FOXY_30TB){
            setParam(b,pb,BLK_AMP,0,scene==0?32f:(scene==1?40f:48f));setParam(b,pb,BLK_AMP,1,46f);setParam(b,pb,BLK_AMP,2,52f);setParam(b,pb,BLK_AMP,3,48f);setParam(b,pb,BLK_AMP,4,60f);setParam(b,pb,BLK_AMP,5,0f);
        } else if(amp==FX_EV51){
            setParam(b,pb,BLK_AMP,0,g);setParam(b,pb,BLK_AMP,1,52f);setParam(b,pb,BLK_AMP,2,45f);setParam(b,pb,BLK_AMP,3,48f);setParam(b,pb,BLK_AMP,4,58f);setParam(b,pb,BLK_AMP,6,55f);
        }
    }

    private static boolean forceDriveForScene(String query,int i){
        String q=norm(query);
        if(has(q,"hendrix","purple haze","voodoo child"))return i>0;
        if(has(q,"gilmour","comfortably numb"))return i>0;
        return false;
    }
    private static boolean modActive(String query,int i){
        String q=norm(query);
        if(has(q,"evh","van halen"))return i>0;
        if(has(q,"hendrix","voodoo child"))return i>0;
        if(has(q,"clean","funk","jazz chorus"))return i==1;
        return false;
    }
    private static void applyModParams(byte[] b,int pb,int mod,String query,int i){
        if(mod==FX_O_PHASE){setParam(b,pb,BLK_MOD,0,i==2?0.7f:0.5f);}
        else if(mod==FX_M_VIBE){setParam(b,pb,BLK_MOD,0,i==2?58f:45f);setParam(b,pb,BLK_MOD,1,i==2?1.1f:0.7f);}
        else {setParam(b,pb,BLK_MOD,0,28f);setParam(b,pb,BLK_MOD,1,0.6f);setParam(b,pb,BLK_MOD,2,52f);}
    }
    private static boolean forceDelayForScene(String query,int i){return has(norm(query),"gilmour","comfortably numb")&&i>=0;}
    private static int delayForScene(String query,NativeModels rig,int i){
        if(has(norm(query),"knopfler","sultans")&&i<2)return FX_DELAY_SLAP;
        return rig.delay;
    }
    private static int reverbForScene(String query,NativeModels rig,int i){
        String q=norm(query);
        if(has(q,"srv","mayer","knopfler","hendrix"))return FX_RVB_SPRING;
        if(has(q,"gilmour"))return i==2?FX_RVB_PLATE_L:FX_RVB_HALL;
        if(has(q,"ac/dc","metallica","clean","funk"))return FX_RVB_ROOM;
        return rig.reverb;
    }
    private static float delayTimeForScene(String query,SceneRecipe scene,int i){
        String q=norm(query);
        if(has(q,"gilmour","comfortably numb"))return i==0?360f:(i==1?420f:470f);
        if(has(q,"knopfler","sultans")&&i<2)return i==0?95f:110f;
        return Math.max(20,scene.delayMs);
    }
    private static float reverbMixForScene(String query,SceneRecipe scene,int i){
        String q=norm(query);
        if(has(q,"ac/dc"))return i==2?10f:7f;
        if(has(q,"metallica"))return i==2?13f:7f;
        return Math.max(8,Math.min(32,scene.reverbMix));
    }

    private static String nativePatchName(String query,String suffix){
        String base=safeName(query).replace('_',' ');
        int max=Math.max(1,15-suffix.length());if(base.length()>max)base=base.substring(0,max);
        String n=base+"-"+suffix;return n.substring(0,Math.min(16,n.length()));
    }

    private static void setModelFxid(byte[] b,int modelsBase,int block,int fxid){
        int off=modelsBase+block*4;
        b[off]=(byte)fxid;b[off+1]=(byte)(fxid>>>8);b[off+2]=(byte)(fxid>>>16);b[off+3]=(byte)(fxid>>>24);
    }

    private static byte[] convertGp50ToGp5(byte[] gp50){
        byte[] tone=findTlvData(gp50,0x0002);
        if(tone.length!=390)throw new IllegalArgumentException("Tone block inatteso: "+tone.length);
        int[] vb=readVolBpmV08(gp50);int[] fs=readFootswitchesV08(gp50);
        byte[] settingsPayload=concatV08(new byte[]{0x01,0x20,0x04,0x00},u32leV08(vb[0]),new byte[]{0x02,0x20,0x04,0x00},u32leV08(vb[1]));
        byte[] body=concatV08(
                tlvV08(0x00ff,concatV08(BLK_FF_PREFIX_V08,DEVTAG_GP5_V08)),
                tlvV08(0x0000,BLK_00_PAYLOAD_V08),
                tlvV08(0x0001,settingsPayload),
                tlvV08(0x0002,tone),
                tlvV08(0x0003,concatV08(u32leV08(fs[0]),u32leV08(fs[1])))
        );
        byte[] out=concatV08(HEADER_GP5_V08,new byte[]{0},SENTINEL_V08,new byte[NAME_LEN_V08],body);
        String name=readPatchNameV08(gp50);writePatchName(out,name);out[CRC_OFF_V08]=(byte)crc8(out,CRC_OFF_V08+1,out.length);
        if(out.length!=507)throw new IllegalStateException("GP5 .prst generato con lunghezza "+out.length+", attesi 507");
        return out;
    }

    private static byte[] findTlvData(byte[] b,int wanted){
        int off=BODY_OFF_V08;
        while(off+4<=b.length){
            int tag=(b[off]&255)|((b[off+1]&255)<<8);int len=(b[off+2]&255)|((b[off+3]&255)<<8);
            if(off+4+len>b.length)break;
            if(tag==wanted)return Arrays.copyOfRange(b,off+4,off+4+len);
            off+=4+len;
        }
        return new byte[0];
    }
    private static int[] readVolBpmV08(byte[] b){
        int vol=50,bpm=120,i=SETTINGS_OFF_V08;
        while(i+4<=b.length && (b[i+1]&255)==0x20){
            int rid=b[i]&255,len=(b[i+2]&255)|((b[i+3]&255)<<8);if(len!=1&&len!=2&&len!=4)break;
            int v=0;for(int k=len-1;k>=0;k--)v=v*256+(b[i+4+k]&255);
            if(rid==1)vol=v;else if(rid==2)bpm=v;i+=4+len;
        }
        return new int[]{vol,bpm};
    }
    private static int[] readFootswitchesV08(byte[] b){
        int off=lastFindV08(b,FS_TRAILER_V08);if(off<0)off=lastFindV08(b,FS_TRAILER_GP5_V08);if(off<0)return new int[]{0,0};
        off+=4;return new int[]{readLe32(b,off),readLe32(b,off+4)};
    }
    private static int lastFindV08(byte[] b,byte[] n){
        outer:for(int i=b.length-n.length;i>=0;i--){for(int j=0;j<n.length;j++)if(b[i+j]!=n[j])continue outer;return i;}return -1;
    }
    private static String readPatchNameV08(byte[] b){
        StringBuilder s=new StringBuilder();for(int i=NAME_OFF_V08;i<NAME_OFF_V08+NAME_LEN_V08;i++){if(b[i]==0)break;s.append((char)(b[i]&255));}return s.toString().trim();
    }
    private static byte[] tlvV08(int tag,byte[] payload){return concatV08(u16leV08(tag),u16leV08(payload.length),payload);}
    private static byte[] u16leV08(int v){return new byte[]{(byte)v,(byte)(v>>>8)};}
    private static byte[] u32leV08(int v){return new byte[]{(byte)v,(byte)(v>>>8),(byte)(v>>>16),(byte)(v>>>24)};}
    private static byte[] concatV08(byte[]... xs){int n=0;for(byte[]x:xs)n+=x.length;byte[]o=new byte[n];int p=0;for(byte[]x:xs){System.arraycopy(x,0,o,p,x.length);p+=x.length;}return o;}

    static List<GeneratedScene> buildSnapTonePresetsAutomatic(String device,int displaySnapSlot,String query,String styleHint){
        if(displaySnapSlot<51||displaySnapSlot>80)throw new IllegalArgumentException("Slot SnapTone utente: 51–80");
        String dev="GP5".equalsIgnoreCase(device)?"GP5":"GP50";
        SceneRecipe[] recipes=sceneRecipes((query==null?"":query)+" "+(styleHint==null?"":styleHint));
        List<GeneratedScene> out=new ArrayList<>();
        for(int i=0;i<3;i++){
            SceneRecipe r=recipes[i];
            byte[] gp50=java.util.Base64.getDecoder().decode(BLANK_GP50_B64);
            int mb=find(gp50,REC_MODELS)+4,bb=find(gp50,REC_BYPASS)+4,pb=find(gp50,REC_PARAMS)+4;
            if(mb<4||bb<4||pb<4)throw new IllegalStateException("Preset base non valido");
            setModelFxid(gp50,mb,BLK_DST,FX_GREEN_OD);
            setModelFxid(gp50,mb,BLK_EQ,FX_GUITAR_EQ1);
            setModelFxid(gp50,mb,BLK_DLY,has(norm(query),"slash","gilmour","evh")?FX_DELAY_TAPE:FX_DELAY_ANALOG);
            setModelFxid(gp50,mb,BLK_RVB,has(norm(query),"srv","mayer","knopfler")?FX_RVB_SPRING:FX_RVB_PLATE);
            setModelFxid(gp50,mb,BLK_NS,FX_SNAPTONE_1+(displaySnapSlot-1));
            int mask=0;
            mask=setBit(mask,BLK_DST,r.odOn);mask=setBit(mask,BLK_EQ,true);mask=setBit(mask,BLK_DLY,r.delayOn);mask=setBit(mask,BLK_RVB,true);mask=setBit(mask,BLK_NS,true);
            writeLe32(gp50,bb,mask);
            setParam(gp50,pb,BLK_DST,0,r.odGain);setParam(gp50,pb,BLK_DST,1,r.odTone);setParam(gp50,pb,BLK_DST,2,r.odVol);
            for(int band=0;band<5;band++)setParam(gp50,pb,BLK_EQ,band,Math.max(-50,Math.min(50,r.eq[band]-50)));
            setParam(gp50,pb,BLK_EQ,5,50f);
            setParam(gp50,pb,BLK_DLY,0,r.delayMix);setParam(gp50,pb,BLK_DLY,1,Math.max(20,r.delayMs));setParam(gp50,pb,BLK_DLY,2,r.delayFeedback);setParam(gp50,pb,BLK_DLY,4,1f);
            int rvb=has(norm(query),"srv","mayer","knopfler")?FX_RVB_SPRING:FX_RVB_PLATE;
            setParam(gp50,pb,BLK_RVB,0,Math.max(8,Math.min(30,r.reverbMix)));
            if(rvb==FX_RVB_SPRING){setParam(gp50,pb,BLK_RVB,1,r.reverbDecay);setParam(gp50,pb,BLK_RVB,3,1f);}else{setParam(gp50,pb,BLK_RVB,2,r.reverbDecay);setParam(gp50,pb,BLK_RVB,4,55f);setParam(gp50,pb,BLK_RVB,6,1f);}
            setParam(gp50,pb,BLK_NS,0,50f);setParam(gp50,pb,BLK_NS,1,50f);setParam(gp50,pb,BLK_NS,2,50f);setParam(gp50,pb,BLK_NS,3,50f);setParam(gp50,pb,BLK_NS,4,50f);
            String name=nativePatchName(query,r.shortLabel);writePatchName(gp50,name);gp50[CRC_OFF_V08]=(byte)crc8(gp50,CRC_OFF_V08+1,gp50.length);
            byte[] bytes="GP5".equals(dev)?convertGp50ToGp5(gp50):gp50;
            out.add(new GeneratedScene(r,name,bytes));
        }
        return out;
    }

}
