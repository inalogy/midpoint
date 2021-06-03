/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;

public class WorkDefinitionUtil {

    @NotNull
    public static List<WorkDefinitionWrapper> getWorkDefinitions(WorkDefinitionsType definitions) throws SchemaException {
        List<WorkDefinitionWrapper> values = new ArrayList<>();
        if (definitions == null) {
            return values;
        }
        addTypedParameters(values, definitions.getReconciliation());
        addTypedParameters(values, definitions.getLiveSynchronization());
        addUntypedParameters(values, definitions.getExtension());
        return values;
    }

    private static void addUntypedParameters(List<WorkDefinitionWrapper> values, ExtensionType extension) {
        if (extension == null) {
            return;
        }
        SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();
        PrismContainerValue<?> pcv = extension.asPrismContainerValue();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemDefinition<?> definition = item.getDefinition();
            if (definition != null &&
                    schemaRegistry.isAssignableFromGeneral(AbstractWorkDefinitionType.COMPLEX_TYPE, definition.getTypeName())) {
                for (PrismValue value : item.getValues()) {
                    if (value instanceof PrismContainerValue<?>) {
                        values.add(new WorkDefinitionWrapper.UntypedWorkDefinitionWrapper((PrismContainerValue<?>) value));
                    }
                }
            }
        }
    }

    private static void addTypedParameters(List<WorkDefinitionWrapper> values, AbstractWorkDefinitionType realValue) {
        if (realValue != null) {
            values.add(new WorkDefinitionWrapper.TypedWorkDefinitionWrapper(realValue));
        }
    }

    public static Collection<QName> getWorkDefinitionTypeNames(WorkDefinitionsType definitions)
            throws SchemaException {
        List<WorkDefinitionWrapper> workDefinitionWrapperCollection = getWorkDefinitions(definitions);
        return workDefinitionWrapperCollection.stream()
                .map(WorkDefinitionWrapper::getBeanTypeName)
                .collect(Collectors.toSet());
    }

}