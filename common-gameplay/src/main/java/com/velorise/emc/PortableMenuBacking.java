package com.velorise.emc;

/** Marks a detached block entity that is editing data stored on an inventory item. */
public interface PortableMenuBacking {
    boolean isPortableMenuBacking();
    int lockedPlayerSlot();
    void savePortableBacking();
}
