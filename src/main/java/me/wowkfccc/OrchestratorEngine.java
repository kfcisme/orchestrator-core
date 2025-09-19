package me.wowkfccc;

import java.util.*;

public class OrchestratorEngine {
    private final Ports.ServerInventory inv;
    private final Ports.PlayerOps playerOps;
    private final Ports.MetricsRepo repo;
    private final Ports.ScalerOps scaler;
    private final LstmClient lstm;
    private final Regressor reg;
    private final Planner planner;
    private final Autoscaler autoscaler;

    public OrchestratorEngine(Ports.ServerInventory inv, Ports.PlayerOps playerOps, Ports.MetricsRepo repo,
                              Ports.ScalerOps scaler, LstmClient lstm, Regressor reg,
                              Planner planner, Autoscaler autoscaler) {
        this.inv = inv; this.playerOps = playerOps; this.repo = repo;
        this.scaler = scaler; this.lstm = lstm; this.reg = reg;
        this.planner = planner; this.autoscaler = autoscaler;
    }

    public void runTick() {
        // 1) 聚合最近組成
        var hist = repo.recentComp(6);
        if (hist.isEmpty()) return;
        int N = hist.stream().mapToInt(Ports.MetricsRepo.CompPoint::total).sum();
        double[] p = new double[8];
        for (var h : hist) for (int k=0;k<8;k++) p[k] += h.p()[k] * h.total();
        if (N>0) for (int k=0;k<8;k++) p[k] /= N;

        // 2) LSTM → pHat；Regressor → CPU 下一窗
        double[] pHat = lstm.forecastNext(p);
        double cpuHatNext = reg.predictCpuNext(pHat, repo);

        // ★ RAM 下一窗（簡單用當前比例外推；你可換真正的回歸）
        double ramNow  = repo.latestRamMb("ALL");
        double cpuNow  = repo.latestCpu("ALL");
        double ramHatNext = (cpuNow > 1e-6) ? (cpuHatNext * (ramNow / cpuNow)) : ramNow;

        // 3) 換算需要台數（容量含 headroom）
        var servers = inv.listServers();
        String ref = servers.isEmpty()? "default": servers.get(0); // 規格相同假設
        double capCpu = inv.capacityCpu(ref);
        double capRam = inv.capacityRamMb(ref);

        int needCpu = Math.max(1, (int)Math.ceil(cpuHatNext / Math.max(1e-6, capCpu)));
        int needRam = Math.max(1, (int)Math.ceil(ramHatNext / Math.max(1e-6, capRam)));
        int target  = Math.max(needCpu, needRam);

        // 4) 套用冷卻/滯後（用 target 做指標也可以）
        int decided = autoscaler.decide(target, scaler.runningCount(), inv);
        scaler.requestScaleTo(decided);

        // 5) 取得線上玩家＋伺服器容量 → 規劃 → 搬家
        var players = playerOps.listOnlinePlayers();
        var plan = planner.assign(players, servers, inv::capacityCpu, inv::capacityRamMb);
        plan.forEach(playerOps::movePlayer);
    }
}


