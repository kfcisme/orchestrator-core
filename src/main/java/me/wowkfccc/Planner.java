package me.wowkfccc;


import java.util.*;
import java.util.function.Function;

public class Planner {
    private final Map<String, Double> costCpu;      // 類型→CPU 成本/人
    private final Map<String, Double> costRamMb;    // 類型→RAM 成本/人（MB）
    public double migratePenalty = 0.5;             // 遷移懲罰
    public double allowOver = 0.05;                 // 允許輕微超量
    public double wCpu = 0.6, wRam = 0.4;           // 評分權重

    public Planner(Map<String, Double> unitCostCpu, Map<String, Double> unitCostRamMb) {
        this.costCpu = unitCostCpu;
        this.costRamMb = unitCostRamMb;
    }

    public Map<String, String> assign(
            List<Ports.PlayerOps.Player> players, List<String> servers,
            Function<String, Double> cpuCapFn,
            Function<String, Double> ramCapFn) {

        class Bin { String id; double cpuCap, ramCap, cpuLoad=0, ramLoad=0; }
        Map<String, Bin> bins = new HashMap<>();
        for (String s : servers) {
            Bin b = new Bin();
            b.id = s; b.cpuCap = cpuCapFn.apply(s); b.ramCap = ramCapFn.apply(s);
            bins.put(s, b);
        }

        // 由“重”到“輕”排；你也可改 max(uCpu, uRam/100) 做排序基準
        players.sort((a, b) -> Double.compare(maxCost(b), maxCost(a)));

        Map<String, String> plan = new HashMap<>();
        for (Ports.PlayerOps.Player u : players) {
            double uCpu = cpuCostOf(u), uRam = ramCostOf(u);
            Bin best = null; double bestScore = Double.POSITIVE_INFINITY;

            for (Bin b : bins.values()) {
                boolean okCpu = (b.cpuLoad + uCpu) <= b.cpuCap * (1 + allowOver);
                boolean okRam = (b.ramLoad + uRam) <= b.ramCap * (1 + allowOver);
                if (!okCpu || !okRam) continue;

                double mig = (u.serverId()!=null && u.serverId().equals(b.id)) ? 0.0 : migratePenalty;
                double cpuRatio = (b.cpuLoad + uCpu) / Math.max(1e-6, b.cpuCap);
                double ramRatio = (b.ramLoad + uRam) / Math.max(1e-6, b.ramCap);
                double score = wCpu * cpuRatio + wRam * ramRatio + mig;

                if (score < bestScore) { bestScore = score; best = b; }
            }

            if (best == null) {
                // 硬塞到綜合負載最低的 bin
                best = bins.values().stream()
                        .min(Comparator.comparingDouble(b ->
                                wCpu * (b.cpuLoad / Math.max(1e-6, b.cpuCap)) +
                                        wRam * (b.ramLoad / Math.max(1e-6, b.ramCap))))
                        .orElse(null);
            }
            if (best != null) {
                best.cpuLoad += uCpu; best.ramLoad += uRam;
                plan.put(u.id(), best.id);
            }
        }
        return plan;
    }

    private double cpuCostOf(Ports.PlayerOps.Player u){ return costCpu.getOrDefault(u.type(), 0.5); }
    private double ramCostOf(Ports.PlayerOps.Player u){ return costRamMb.getOrDefault(u.type(), 16.0); }
    private double maxCost(Ports.PlayerOps.Player u){ return Math.max(cpuCostOf(u), ramCostOf(u)/100.0); }
}

