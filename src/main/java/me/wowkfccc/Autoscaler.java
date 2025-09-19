package me.wowkfccc;

import java.util.List;

public class Autoscaler {
    private final List<Integer> steps; private final int cooldownMin; private long last=0;
    private int upH=2, downH=3, upCnt=0, downCnt=0;

    public Autoscaler(List<Integer> cpuSteps, int cooldownMin){ this.steps=cpuSteps; this.cooldownMin=cooldownMin; }
    public void setHysteresis(int up, int down){ this.upH=up; this.downH=down; }

    public int decide(double indicator, int running, Ports.ServerInventory inv){
        int target=1; for(int th:steps) if(indicator>th) target++;
        long now=System.currentTimeMillis();
        if (now-last < cooldownMin*60_000L) return running;
        if (target>running){ if(++upCnt>=upH){ last=now; upCnt=downCnt=0; return target; } }
        else if (target<running){ if(++downCnt>=downH){ last=now; upCnt=downCnt=0; return target; } }
        else upCnt=downCnt=0;
        return running;
    }
}

