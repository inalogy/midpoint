/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationsGuiUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationsGuiUtil.class);

    private static final String DOT_CLASS = SimulationsGuiUtil.class.getName() + ".";
    private static final String OPERATION_PARSE_PROCESSED_OBJECT = DOT_CLASS + "parserProcessedObject";

    public static Label createProcessedObjectStateLabel(String id, IModel<SimulationResultProcessedObjectType> model) {
        Label label = new Label(id, () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            return LocalizationUtil.translate(WebComponentUtil.createEnumResourceKey(state));
        });

        label.add(AttributeModifier.append("class", () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            switch (state) {
                case ADDED:
                    return Badge.State.SUCCESS.getCss();
                case DELETED:
                    return Badge.State.DANGER.getCss();
                case MODIFIED:
                    return Badge.State.INFO.getCss();
                case UNMODIFIED:
                default:
                    return Badge.State.SECONDARY.getCss();
            }
        }));

        return label;
    }

    public static String getProcessedObjectType(@NotNull IModel<SimulationResultProcessedObjectType> model) {
        SimulationResultProcessedObjectType object = model.getObject();
        if (object == null || object.getType() == null) {
            return null;
        }

        QName type = object.getType();
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);
        String key = WebComponentUtil.createEnumResourceKey(ot);

        return LocalizationUtil.translate(key);
    }

    public static IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createProcessedObjectIconColumn() {
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                SimulationResultProcessedObjectType object = model.getObject().getValue();
                ObjectType obj = object.getBefore() != null ? object.getBefore() : object.getAfter();
                if (obj == null || obj.asPrismObject() == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(WebComponentUtil.createDefaultColoredIcon(object.getType())));
                }

                return new DisplayType()
                        .icon(new IconType().cssClass(WebComponentUtil.createDefaultIcon(obj.asPrismObject())));
            }
        };
    }

    public static Visualization createVisualization(ObjectDeltaType objectDelta, PageBase page) {
        if (objectDelta == null) {
            return null;
        }

        try {
            ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDelta);

            Task task = page.getPageTask();
            OperationResult result = task.getResult();

            return page.getModelInteractionService().visualizeDelta(delta, true, false, task, result);
        } catch (SchemaException | ExpressionEvaluationException e) {
            LOGGER.debug("Couldn't convert and visualize delta", e);

            throw new SystemException(e);
        }
    }

    public static VisualizationDto createVisualizationDto(ObjectDeltaType objectDelta, PageBase page) {
        if (objectDelta == null) {
            return null;
        }

        Visualization visualization = createVisualization(objectDelta, page);
        return createVisualizationDto(visualization);
    }

    public static VisualizationDto createVisualizationDto(Visualization visualization) {
        if (visualization == null) {
            return null;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating dto for deltas:\n{}", DebugUtil.debugDump(visualization));
        }

        final WrapperVisualization wrapper =
                new WrapperVisualization(new SingleLocalizableMessage("PageSimulationResultObject.changes"), Arrays.asList(visualization));

        return new VisualizationDto(wrapper);
    }

    public static String getProcessedObjectName(ProcessedObject<?> object, PageBase page) {
        if (object == null || object.getName() == null) {
            return page.getString("ProcessedObjectsPanel.unnamed");
        }

        ObjectType obj = ObjectProcessingStateType.DELETED.equals(object.getState()) ? object.getBefore() : object.getAfter();
        if (obj == null) {
            return LocalizationUtil.translatePolyString(obj.getName());
        }

        if (obj instanceof ShadowType) {
            try {
                return getProcessedShadowName((ShadowType) obj, page);
            } catch (SystemException ex) {
                LOGGER.debug("Couldn't create processed shadow name", ex);
            }
        }

        return WebComponentUtil.getDisplayNameAndName(obj.asPrismObject());
    }

    private static String getProcessedShadowName(ShadowType shadow, PageBase page) {
        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;

        ResourceAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute(shadow);
        Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
        String name = realName != null ? realName.toString() : "";

        String intent = shadow.getIntent() != null ? shadow.getIntent() : "";

        ObjectReferenceType resourceRef = shadow.getResourceRef();

        Object resourceName = WebModelServiceUtils.resolveReferenceName(resourceRef, page, false);
        if (resourceName == null) {
            resourceName = new SingleLocalizableMessage("ProcessedObjectsPanel.unknownResource",
                    new Object[] { resourceRef != null ? resourceRef.getOid() : null });
        }

        LocalizableMessage msg = new SingleLocalizableMessage("ProcessedObjectsPanel.shadow", new Object[] {
                new SingleLocalizableMessage("ShadowKindType." + kind.name()),
                name,
                intent,
                resourceName
        });

        return LocalizationUtil.translateMessage(msg);
    }

    public static ProcessedObject parseProcessedObject(@NotNull SimulationResultProcessedObjectType obj, @NotNull PageBase page) {
        Task task = page.createSimpleTask(OPERATION_PARSE_PROCESSED_OBJECT);
        OperationResult result = task.getResult();

        try {
            return page.getModelService().parseProcessedObject(obj, task, result);
        } catch (Exception ex) {
            result.computeStatusIfUnknown();
            result.recordFatalError("Couldn't parse processed object", ex);

            page.showResult(result);

            return null;
        }
    }

    public static Map<BuiltInSimulationMetricType, Integer> getBuiltInMetrics(SimulationResultType result) {
        List<SimulationMetricValuesType> metrics = result.getMetric();
        List<SimulationMetricValuesType> builtIn = metrics.stream().filter(m -> m.getRef() != null && m.getRef().getBuiltIn() != null)
                .collect(Collectors.toList());

        Map<BuiltInSimulationMetricType, Integer> map = new HashMap<>();

        for (SimulationMetricValuesType metric : builtIn) {
            BuiltInSimulationMetricType identifier = metric.getRef().getBuiltIn();

            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(metric);
            map.put(identifier, value != null ? value.intValue() : 0);
        }

        return map;
    }

    public static int getUnmodifiedProcessedObjectCount(SimulationResultType result, Map<BuiltInSimulationMetricType, Integer> builtInMetrics) {
        int totalCount = SimulationResultTypeUtil.getObjectsProcessed(result);
        int unmodifiedCount = totalCount;

        for (Map.Entry<BuiltInSimulationMetricType, Integer> entry : builtInMetrics.entrySet()) {
            BuiltInSimulationMetricType identifier = entry.getKey();
            int value = entry.getValue();

            unmodifiedCount = unmodifiedCount - value;
        }

        return unmodifiedCount;
    }
}