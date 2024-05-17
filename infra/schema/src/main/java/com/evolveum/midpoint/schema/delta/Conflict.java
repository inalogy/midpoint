/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public record Conflict<
        PV extends PrismValue,
        ID extends ItemDefinition<I>,
        I extends Item<PV, ID>,
        V extends ItemTreeDeltaValue<PV, ITD>,
        ITD extends ItemTreeDelta<PV, ID, I, V>,
        ITDV extends ItemTreeDeltaValue<PV, ITD>>
        (ITDV first, ITDV second) implements DebugDumpable {

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpLabelLn(sb, "Conflict", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "first", first, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "second", second, indent + 1);

        return sb.toString();
    }
}