package com.sumirelabs.sum1rem0d.game;

//? if neoforge {
/*import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
*///?}

public final class NeoFurnaceTransactions {
    //? if neoforge {
    /*private final InstantFurnace furnace;
    private final SnapshotJournal<InstantFurnace.CostSnapshot> journal;
    public NeoFurnaceTransactions(InstantFurnace furnace) {
        this.furnace = furnace;
        journal = new SnapshotJournal<>() {
            @Override protected InstantFurnace.CostSnapshot createSnapshot() { return furnace.sum1rem0d$snapshotCosts(); }
            @Override protected void revertToSnapshot(InstantFurnace.CostSnapshot snapshot) { furnace.sum1rem0d$restoreCosts(snapshot); }
            @Override protected void onRootCommit(InstantFurnace.CostSnapshot original) { furnace.sum1rem0d$finishCommit(); }
        };
    }
    public void extraction(TransactionContext transaction) {
        if (!furnace.sum1rem0d$isPreview()) return;
        journal.updateSnapshots(transaction);
        if (!furnace.sum1rem0d$consume(true)) throw new IllegalStateException("Furnace preview became invalid during extraction");
    }
    *///?}
}
