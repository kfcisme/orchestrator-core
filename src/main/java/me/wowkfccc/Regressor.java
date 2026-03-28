package me.wowkfccc;

import java.util.Map;

public class Regressor {
    public static class Coef {
        public double intercept; public Map<String, Double> beta; public double yLag1, yLag2;
    }
    private final Coef c;
    public Regressor(Coef c){ this.c = c; }

    public double predictCpuNext(double[] pHat, Ports.MetricsRepo repo){
        String[] K = {"AFK","Build","Explorer","Explosive","PvP","Redstone","Social","Survival"};
        double y = c.intercept;
        for(int i=0;i<8;i++) y += c.beta.getOrDefault(K[i], 0.0) * pHat[i];
        double yt  = repo.latestCpu("ALL");
        double yt1 = 0;
        y += c.yLag1 * yt + c.yLag2 * yt1;
        return Math.max(0, y);
    }
}

