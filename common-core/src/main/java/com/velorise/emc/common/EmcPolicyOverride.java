package com.velorise.emc.common;

import java.util.Objects;

/**
 * Tri-state server override for EMC permissions.
 * -1 inherits the active profile, 0 denies and 1 allows.
 */
public final class EmcPolicyOverride {
    public static final int INHERIT = -1;
    public static final int DENY = 0;
    public static final int ALLOW = 1;

    private int burnable;
    private int learnable;
    private int materializable;

    public EmcPolicyOverride() {
        this(INHERIT, INHERIT, INHERIT);
    }

    public EmcPolicyOverride(int burnable, int learnable, int materializable) {
        this.burnable = normalize(burnable);
        this.learnable = normalize(learnable);
        this.materializable = normalize(materializable);
    }

    public static EmcPolicyOverride inherit() {
        return new EmcPolicyOverride();
    }

    public static EmcPolicyOverride materialization(boolean allowed) {
        return new EmcPolicyOverride(INHERIT, INHERIT, allowed ? ALLOW : DENY);
    }

    public static EmcPolicyOverride blockAll() {
        return new EmcPolicyOverride(DENY, DENY, DENY);
    }

    public static EmcPolicyOverride allowAll() {
        return new EmcPolicyOverride(ALLOW, ALLOW, ALLOW);
    }

    public int burnableState() {
        return burnable;
    }

    public int learnableState() {
        return learnable;
    }

    public int materializableState() {
        return materializable;
    }

    public boolean isInherited() {
        return burnable == INHERIT && learnable == INHERIT && materializable == INHERIT;
    }

    public EmcPolicyOverride withBurnable(int state) {
        return new EmcPolicyOverride(state, learnable, materializable);
    }

    public EmcPolicyOverride withLearnable(int state) {
        return new EmcPolicyOverride(burnable, state, materializable);
    }

    public EmcPolicyOverride withMaterializable(int state) {
        return new EmcPolicyOverride(burnable, learnable, state);
    }

    public boolean resolveBurnable(boolean inherited) {
        return resolve(burnable, inherited);
    }

    public boolean resolveLearnable(boolean inherited) {
        return resolve(learnable, inherited);
    }

    public boolean resolveMaterializable(boolean inherited) {
        return resolve(materializable, inherited);
    }

    private static boolean resolve(int state, boolean inherited) {
        return state == INHERIT ? inherited : state == ALLOW;
    }

    private static int normalize(int state) {
        if (state == DENY || state == ALLOW) return state;
        return INHERIT;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EmcPolicyOverride other)) return false;
        return burnable == other.burnable
            && learnable == other.learnable
            && materializable == other.materializable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(burnable, learnable, materializable);
    }
}
