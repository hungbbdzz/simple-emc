package com.velorise.emc.common;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Built-in Default/Balanced policy rules, independent from numeric EMC values. */
public final class EmcPolicyProfile {
    private static final Set<String> BALANCED_RESTRICTED;

    static {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add("minecraft:totem_of_undying");
        ids.add("minecraft:enchanted_golden_apple");
        ids.add("minecraft:elytra");
        ids.add("minecraft:dragon_egg");
        ids.add("minecraft:heavy_core");
        ids.add("minecraft:mace");
        ids.add("minecraft:ominous_trial_key");
        ids.add("minecraft:wither_skeleton_skull");
        ids.add("minecraft:nether_star");
        BALANCED_RESTRICTED = Collections.unmodifiableSet(ids);

    }

    private EmcPolicyProfile() {}

    public static Set<String> balancedRestrictedIds() {
        return BALANCED_RESTRICTED;
    }

    public static boolean isBalancedRestricted(String itemId) {
        return itemId != null && BALANCED_RESTRICTED.contains(itemId);
    }

    public static EmcItemPolicy resolve(
        EmcValueMode mode,
        String itemId,
        boolean hardBlocked,
        boolean hasPositiveEmc,
        EmcPolicyOverride override
    ) {
        if (!hasPositiveEmc) return EmcItemPolicy.disabled("No EMC value");
        // Velorise EMC progression items are never restricted by a built-in profile or a stale
        // server blacklist. Their availability is controlled by recipes and world progression.
        if (itemId != null && itemId.startsWith("velorise_emc:")) {
            return EmcItemPolicy.permissive("Velorise EMC item");
        }
        if (hardBlocked) return EmcItemPolicy.disabled("Hard blacklist");

        boolean balanced = mode == EmcValueMode.BALANCED;
        boolean restricted = balanced && isBalancedRestricted(itemId);
        boolean burnable = true;
        boolean learnable = !restricted;
        boolean materializable = !restricted;
        String source = restricted ? "Balanced restriction" : mode.displayName() + " profile";

        EmcPolicyOverride effectiveOverride = override == null ? EmcPolicyOverride.inherit() : override;
        burnable = effectiveOverride.resolveBurnable(burnable);
        learnable = effectiveOverride.resolveLearnable(learnable);
        materializable = effectiveOverride.resolveMaterializable(materializable);

        // A simple server materialization allow is expected to make a Balanced
        // restriction usable again. Preserve an explicit learn deny, but inherit
        // learning as allowed when materialization itself was explicitly allowed.
        if (materializable
            && effectiveOverride.materializableState() == EmcPolicyOverride.ALLOW
            && effectiveOverride.learnableState() == EmcPolicyOverride.INHERIT) {
            learnable = true;
        }

        // A hidden or non-materializable entry must not remain learnable. This also
        // prevents stale knowledge from becoming a server-side bypass.
        if (!materializable) learnable = false;
        boolean visible = learnable && materializable;

        if (!effectiveOverride.isInherited()) source = "Server override";
        return new EmcItemPolicy(burnable, learnable, materializable, visible, source);
    }
}
