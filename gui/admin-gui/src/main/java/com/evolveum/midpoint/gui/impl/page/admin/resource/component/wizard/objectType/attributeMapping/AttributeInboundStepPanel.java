/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.AbstractFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractInboundStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "attributeInboundWizard")
@PanelInstance(identifier = "attributeInboundWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.attributes.step.inbound", icon = "fa fa-circle"),
        containerPath = "schemaHandling/objectType/attribute/inbound",
        expanded = true)
public class AttributeInboundStepPanel extends AbstractInboundStepPanel<ResourceAttributeDefinitionType> {

    private static final String PANEL_TYPE = "attributeInboundWizard";

    public AttributeInboundStepPanel(ResourceDetailsModel model,
                                            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.attributes.step.inbound");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.attributes.inbound.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.attributes.inbound.subText");
    }
}