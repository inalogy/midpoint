/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class OutlierResultPanel extends BasePanel<String> implements Popupable {

    private static final String ID_CARD = "card";
    private static final String ID_CARD_HEADER = "card-header";
    private static final String ID_CARD_BODY = "card-body";
    private static final String ID_CARD_FOOTER = "card-footer";

    private static final String ID_CARD_HEADER_BODY = "card-header-body";
    private static final String ID_CARD_BODY_BODY = "card-body-body";
    private static final String ID_CARD_FOOTER_BODY = "card-footer-body";

    private static final String ID_CARD_BODY_COMPONENT = "card-body-component";
    private static final String ID_CARD_BODY_COMPONENT_CONTAINER = "card-body-component-container";

    private static final String ID_BODY_TITLE = "body-title";
    private static final String ID_BODY_SUBTITLE = "body-subtitle";

    public OutlierResultPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
        card.setOutputMarkupId(true);
        card.add(AttributeAppender.replace("class", getCardCssClass()));
        add(card);

        WebMarkupContainer cardHeader = new WebMarkupContainer(ID_CARD_HEADER);
        cardHeader.setOutputMarkupId(true);
        card.add(cardHeader);

        WebMarkupContainer cardFooter = new WebMarkupContainer(ID_CARD_FOOTER);
        cardFooter.setOutputMarkupId(true);
        card.add(cardFooter);

        WebMarkupContainer cardBody = new WebMarkupContainer(ID_CARD_BODY);
        cardBody.setOutputMarkupId(true);
        card.add(cardBody);

        WebMarkupContainer cardBodyBody = new WebMarkupContainer(ID_CARD_BODY_BODY);
        cardBodyBody.setOutputMarkupId(true);
        cardBody.add(cardBodyBody);

        cardHeader.add(getCardHeaderBody(ID_CARD_HEADER_BODY));

        Component cardBodyComponent = getCardBodyComponent(ID_CARD_BODY_COMPONENT);
        cardBodyBody.add(cardBodyComponent);


        cardFooter.add(getCardFooterBody(ID_CARD_FOOTER_BODY));

        Label bodyTitle = new Label(ID_BODY_TITLE, getBodyTitle());
        bodyTitle.setOutputMarkupId(true);
        bodyTitle.add(new VisibleBehaviour(this::isBodyTitleVisible));
        cardBody.add(bodyTitle);

        Label bodySubtitle = new Label(ID_BODY_SUBTITLE, getBodySubtitle());
        bodySubtitle.setOutputMarkupId(true);
        bodySubtitle.add(new VisibleBehaviour(this::isBodySubtitleVisible));
        cardBody.add(bodySubtitle);
    }

    protected boolean isBodyTitleVisible() {
        return true;
    }

    protected boolean isBodySubtitleVisible() {
        return true;
    }

    public String getCardCssClass() {
        return "card m-0 p-0";
    }

    public String getBodyTitle() {
        return "Outlier factors";
    }

    public String getBodySubtitle() {
        return "This is the description of the outlier object factors";
    }

    public Component getCardHeaderBody(String componentId) {
        WebMarkupContainer cardHeaderBody = new WebMarkupContainer(componentId);
        cardHeaderBody.setOutputMarkupId(true);
        return cardHeaderBody;
    }

    public Component getCardBodyComponent(String componentId) {
        RepeatingView cardBodyComponent = new RepeatingView(componentId);
        cardBodyComponent.setOutputMarkupId(true);
        return cardBodyComponent;
    }

    public Component getCardFooterBody(String componentId) {
        AjaxIconButton linkButton = new AjaxIconButton(componentId,
                Model.of("fa fa-cog"), Model.of("Recertification process")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        linkButton.setOutputMarkupId(true);
        linkButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));

        return linkButton;
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1000;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("Outlier object description");
    }

}