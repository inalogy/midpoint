/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceContentPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class PreviewResourceObjectTypeDataWizardPanel extends AbstractWizardBasicPanel {

    private static final String PANEL_TYPE = "?"; //TODO
    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType;

    public PreviewResourceObjectTypeDataWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType) {
        super(id, model);
        this.resourceObjectType = resourceObjectType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ResourceContentPanel table = new ResourceContentPanel(
                ID_TABLE, resourceObjectType.getObject().getRealValue().getKind(),
                getResourceModel(),
                getConfiguration()) {
            @Override
            protected boolean isIntentAndObjectClassPanelVisible() {
                return false;
            }

            @Override
            protected String getIntent() {
                return resourceObjectType.getObject().getRealValue().getIntent();
            }

            @Override
            protected QName getObjectClass() {
                return resourceObjectType.getObject().getRealValue().getObjectClass();
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getResourceModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectType() {
        return resourceObjectType;
    }

    //    @Override
//    protected IModel<String> getSubTextModel() {
//        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.text");
//    }
//
//    @Override
//    protected IModel<String> getTextModel() {
//        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.subText");
//    }
}