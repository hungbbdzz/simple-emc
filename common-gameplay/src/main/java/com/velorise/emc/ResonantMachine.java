package com.velorise.emc;

/** Shared contract for placed EMC-buffer machines. */
public interface ResonantMachine {
    long getStoredEmc();
    long getMaxStoredEmc();

    /** Returns the amount of EMC actually accepted. */
    long addEmc(long amount);

    boolean isMachineEnabled();
    void setMachineEnabled(boolean enabled);

}
