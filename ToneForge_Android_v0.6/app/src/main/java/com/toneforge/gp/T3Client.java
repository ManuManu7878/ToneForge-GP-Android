package com.toneforge.gp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

final class T3Client {
    static final String API="https://www.tone3000.com/api/v1";
    static final String REDIRECT="toneforge://oauth/callback";
    private final SharedPreferences prefs;

    static final class SelectResult {
        final boolean canceled;
        final long toneId;
        SelectResult(boolean canceled,long toneId){this.canceled=canceled;this.toneId=toneId;}
    }

    T3Client(Context c){prefs=c.getSharedPreferences("toneforge",Context.MODE_PRIVATE);}

    String clientId(){return prefs.getString("client_id","");}
    void setClientId(String s){prefs.edit().putString("client_id",s==null?"":s.trim()).apply();}
    boolean connected(){return !prefs.getString("access_token","").isEmpty();}
    void logout(){prefs.edit().remove("access_token").remove("refresh_token").remove("expires_at").apply();}

    /**
     * Official TONE3000 Select OAuth flow. The user browses/selects inside TONE3000;
     * ToneForge receives only the selected tone_id, then fetches that tone's models.
     */
    Uri beginSelectTone() throws Exception {
        if(clientId().isEmpty())throw new IllegalStateException("Inserisci il Publishable Client ID TONE3000");
        byte[] random=new byte[48];new SecureRandom().nextBytes(random);
        String verifier=b64url(random);
        String challenge=b64url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
        String state=b64url(new SecureRandom().generateSeed(24));
        prefs.edit().putString("pkce_verifier",verifier).putString("oauth_state",state).apply();

        Uri.Builder b=Uri.parse(API+"/oauth/authorize").buildUpon();
        b.appendQueryParameter("client_id",clientId());
        b.appendQueryParameter("redirect_uri",REDIRECT);
        b.appendQueryParameter("response_type","code");
        b.appendQueryParameter("code_challenge",challenge);
        b.appendQueryParameter("code_challenge_method","S256");
        b.appendQueryParameter("state",state);
        b.appendQueryParameter("prompt","select_tone");
        b.appendQueryParameter("format","nam");
        b.appendQueryParameter("architecture","2");
        b.appendQueryParameter("gears","amp_amp-cab");
        b.appendQueryParameter("preview","true");
        b.appendQueryParameter("menubar","true");
        return b.build();
    }

    SelectResult finishSelectOAuth(Uri callback) throws Exception {
        String expected=prefs.getString("oauth_state","");
        String state=callback.getQueryParameter("state");
        if(state==null||!state.equals(expected))throw new IllegalStateException("Callback OAuth non valido");

        boolean canceled="true".equalsIgnoreCase(callback.getQueryParameter("canceled"));
        String code=callback.getQueryParameter("code");
        long toneId=parseLong(callback.getQueryParameter("tone_id"),-1L);

        // TONE3000 may return a code even when the user exits selection after authenticating.
        if(code!=null&&!code.isEmpty()){
            Map<String,String> f=new LinkedHashMap<>();
            f.put("grant_type","authorization_code");
            f.put("code",code);
            f.put("code_verifier",prefs.getString("pkce_verifier",""));
            f.put("redirect_uri",REDIRECT);
            f.put("client_id",clientId());
            saveToken(postForm(API+"/oauth/token",f));
        }
        if(canceled)return new SelectResult(true,toneId);
        if(toneId<=0)throw new IllegalStateException("TONE3000 non ha restituito alcun tone_id");
        if(!connected())throw new IllegalStateException("Autorizzazione TONE3000 incompleta");
        return new SelectResult(false,toneId);
    }

    JSONObject tone(long toneId,int architecture) throws Exception {
        Map<String,String> p=new LinkedHashMap<>();p.put("architecture",String.valueOf(architecture));
        return get(API+"/tones/"+toneId,p);
    }

    JSONArray models(long toneId, int architecture) throws Exception {
        Map<String,String> p=new LinkedHashMap<>();
        p.put("tone_id",String.valueOf(toneId));p.put("page","1");p.put("page_size","300");p.put("architecture",String.valueOf(architecture));
        JSONObject res=get(API+"/models",p);
        return res.optJSONArray("data")==null?new JSONArray():res.optJSONArray("data");
    }

    byte[] download(String modelUrl) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(modelUrl).openConnection();
        c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setRequestProperty("Authorization","Bearer "+token());
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        byte[] data=readAll(in);
        if(code<200||code>=300)throw new IllegalStateException("Download TONE3000 fallito HTTP "+code+": "+new String(data,StandardCharsets.UTF_8));
        return data;
    }

    private String token() throws Exception {
        String t=prefs.getString("access_token","");
        if(t.isEmpty())throw new IllegalStateException("TONE3000 non collegato");
        long exp=prefs.getLong("expires_at",0);
        if(exp>0&&System.currentTimeMillis()>exp){
            String r=prefs.getString("refresh_token","");
            if(r.isEmpty())throw new IllegalStateException("Sessione TONE3000 scaduta: scegli di nuovo un tone");
            Map<String,String> f=new LinkedHashMap<>();f.put("grant_type","refresh_token");f.put("refresh_token",r);f.put("client_id",clientId());
            saveToken(postForm(API+"/oauth/token",f));t=prefs.getString("access_token","");
        }
        return t;
    }

    private void saveToken(JSONObject j){
        String access=j.optString("access_token","");
        if(access.isEmpty())return;
        SharedPreferences.Editor e=prefs.edit().putString("access_token",access)
                .putString("refresh_token",j.optString("refresh_token",prefs.getString("refresh_token","")));
        long seconds=j.optLong("expires_in",3600);
        e.putLong("expires_at",System.currentTimeMillis()+Math.max(60,seconds-60)*1000L).apply();
    }

    private JSONObject get(String url, Map<String,String> params) throws Exception {
        StringBuilder u=new StringBuilder(url);
        if(params!=null&&!params.isEmpty()){
            u.append('?');boolean first=true;
            for(Map.Entry<String,String>e:params.entrySet()){
                if(!first)u.append('&');first=false;u.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
            }
        }
        HttpURLConnection c=(HttpURLConnection)new URL(u.toString()).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("Authorization","Bearer "+token());c.setRequestProperty("Accept","application/json");
        return readJson(c);
    }

    private JSONObject postForm(String url, Map<String,String> form) throws Exception {
        StringBuilder body=new StringBuilder();boolean first=true;
        for(Map.Entry<String,String>e:form.entrySet()){
            if(!first)body.append('&');first=false;body.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setDoOutput(true);c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.setFixedLengthStreamingMode(bytes.length);
        try(OutputStream out=c.getOutputStream()){out.write(bytes);}return readJson(c);
    }

    private JSONObject readJson(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        byte[] b=readAll(in);String s=new String(b,StandardCharsets.UTF_8);
        if(code<200||code>=300)throw new IllegalStateException("TONE3000 HTTP "+code+": "+s);
        return new JSONObject(s);
    }

    private static byte[] readAll(InputStream in)throws Exception{
        if(in==null)return new byte[0];
        try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[]buf=new byte[16384];int n;while((n=x.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();
        }
    }
    private static long parseLong(String s,long d){try{return Long.parseLong(s);}catch(Exception e){return d;}}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,StandardCharsets.UTF_8.name());}
    private static String b64url(byte[] b){return Base64.encodeToString(b,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
}
