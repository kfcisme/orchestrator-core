package me.wowkfccc;
import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;

public class LstmClient {
    private final HttpClient http; private final String base; private final int timeout;
    public LstmClient(String base, int timeoutMs){
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        this.base = base.endsWith("/")? base.substring(0, base.length()-1): base;
        this.timeout = timeoutMs;
    }
    public double[] forecastNext(double[] p){ // baseline：單點也能回傳
        try{
            String body = "{\"server_id\":\"all\",\"comp_seq\":["+toCsv(p)+"],\"horizon\":1}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(base+"/forecast_next_comp"))
                    .timeout(Duration.ofMillis(timeout))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            String res = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            return parseFirstVector(res);
        }catch(Exception e){ return p; } // fallback 原樣
    }
    private static String toCsv(double[] a){
        StringBuilder sb = new StringBuilder(); for(int i=0;i<a.length;i++){ if(i>0) sb.append(','); sb.append(String.format(java.util.Locale.US,"%.6f",a[i])); }
        return sb.toString();
    }
    private static double[] parseFirstVector(String json){
        int i=json.indexOf("\"p_hat\""); if(i<0) return new double[8];
        int lb=json.indexOf("[",i), rb=json.indexOf("]",lb); String[] s=json.substring(lb+1,rb).replace("[","").replace("]","").split(",");
        double[] v=new double[8]; for(int k=0;k<8;k++) v[k]=Double.parseDouble(s[k].trim()); return v;
    }
}
