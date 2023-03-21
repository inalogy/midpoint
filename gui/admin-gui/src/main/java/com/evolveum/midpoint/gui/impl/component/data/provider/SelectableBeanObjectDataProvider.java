/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.model.SelectableObjectModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanObjectDataProvider<O extends ObjectType> extends SelectableBeanDataProvider<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanObjectDataProvider.class);

    private boolean isMemberPanel = false;

    private Consumer<Task> taskConsumer;

    public SelectableBeanObjectDataProvider(Component component, IModel<Search<O>> search, Set<O> selected) {
        super(component, search, selected, true);
    }

    public SelectableBeanObjectDataProvider(Component component, Set<O> selected) {
        super(component, Model.of(), selected, true);
    }

    public SelectableBean<O> createDataObjectWrapper(O obj) {
        SelectableObjectModel<O> model = new SelectableObjectModel<O>(obj, getOptions()) {
            @Override
            protected O load() {
                PageBase pageBase = getPageBase();
                Task task = pageBase.createSimpleTask("load object");
                OperationResult result = task.getResult();
                PrismObject<O> object = WebModelServiceUtils.loadObject(getType(), getOid(), getOptions(), pageBase, task, result);
                result.computeStatusIfUnknown();
                return object.asObjectable();
            }
        };
        SelectableBean<O> selectable = new SelectableBeanImpl<>(model);
        //TODO result

        for (O s : getSelected()) {
            if (match(s, obj)) {
                selectable.setSelected(true);
                model.setSelected(true);
            }
        }

        return selectable;
    }

    @Override
    protected boolean match(O selectedValue, O foundValue) {
        return selectedValue.getOid().equals(foundValue.getOid());
    }

    @Override
    protected Integer countObjects(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> currentOptions,
            Task task, OperationResult result) throws CommonException {
        return getModelService().countObjects(type, getQuery(), currentOptions, task, result);
    }

    protected boolean isMemberPanel() {
        return isMemberPanel;
    }

    public void setIsMemberPanel(boolean isMemberPanel) {
        this.isMemberPanel = isMemberPanel;
    }


    @Override
    protected List<O> searchObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        if (taskConsumer != null) {
            taskConsumer.accept(task);
        }
        return getModelService().searchObjects(type, query, options, task, result)
                .map(prismObject -> prismObject.asObjectable());
    }

    @Override
    public void detach() {
        super.detach();
        getAvailableData().clear();
    }

    @Override
    protected GetOperationOptionsBuilder postProcessOptions(GetOperationOptionsBuilder optionsBuilder) {
        if (isEmptyListOnNullQuery()) {
            // TODO also for other classes
            if (ShadowType.class.equals(getType())) {
                return optionsBuilder
                        .definitionProcessing(ONLY_IF_EXISTS)
                        .item(ShadowType.F_FETCH_RESULT).definitionProcessing(FULL)
                        .item(ShadowType.F_AUXILIARY_OBJECT_CLASS).definitionProcessing(FULL);
            }
        }
        return optionsBuilder;
    }

    public void setTaskConsumer(Consumer<Task> taskConsumer) {
        this.taskConsumer = taskConsumer;
    }
}