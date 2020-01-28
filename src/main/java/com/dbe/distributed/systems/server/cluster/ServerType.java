package com.dbe.distributed.systems.server.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerType {
    private final AtomicBoolean isMaster;

    public ServerType() {
        this.isMaster = new AtomicBoolean(true);
    }

    public boolean isMaster() {
        return isMaster.get();
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster.set(isMaster);
    }
}
