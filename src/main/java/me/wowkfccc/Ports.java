package me.wowkfccc;

import java.util.List;

public final class Ports {
    private Ports() {}

    // 伺服器清單與容量（含 RAM）
    public interface ServerInventory {
        List<String> listServers();                 // 目前活著的實例 ID
        double capacityCpu(String serverId);        // 可用 CPU 容量（% 或同一單位）
        double capacityRamMb(String serverId);      // ★ 可用 RAM 容量（MB）
    }

    // 玩家操作（Velocity 可跨服搬家）
    public interface PlayerOps {
        record Player(String id, String name, String serverId, String type) {}
        List<Player> listOnlinePlayers();
        void movePlayer(String playerId, String targetServerId);
    }

    // 指標倉儲（從 DB / metrics 拉資料）
    public interface MetricsRepo {
        record CompPoint(String serverId, int total, double[] p) {}
        List<CompPoint> recentComp(int horizon);    // 最近 L 步玩家組成（可多服）
        double latestCpu(String serverId);          // 最新 CPU 使用量（同一單位）
        double latestRamMb(String serverId);        // ★ 最新 RAM 使用量（MB）
    }

    // 擴縮操作（接外部啟停）
    public interface ScalerOps {
        int runningCount();                         // 目前實例數
        void requestScaleTo(int n);                 // 目標實例數（可 no-op）
    }
}

