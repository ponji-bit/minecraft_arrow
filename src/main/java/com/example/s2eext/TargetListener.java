package com.example.s2eext;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class TargetListener implements Listener {

    private final TargetManager targetManager;

    public TargetListener(TargetManager targetManager) {
        this.targetManager = targetManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        targetManager.handleTargetBlockBreak(event.getBlock());
    }
}
