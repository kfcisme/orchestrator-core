package me.wowkfccc;

import java.util.List;

public final class Ports {
    private Ports() {}

    public interface ServerInventory {
        List<String> listServers();
        double capacityCpu(String serverId);
        double capacityRamMb(String serverId);
    }

    public interface PlayerOps {
        record Player(String id, String name, String serverId, String type) {}
        List<Player> listOnlinePlayers();
        void movePlayer(String playerId, String targetServerId);
    }

    public interface MetricsRepo {
        record CompPoint(String serverId, int total, double[] p) {}
        List<CompPoint> recentComp(int horizon);
        double latestCpu(String serverId);
        double latestRamMb(String serverId);
    }

    public interface ScalerOps {
        int runningCount();
        void requestScaleTo(int n);
    }
}

