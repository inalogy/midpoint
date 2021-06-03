/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the tree of activities that comprise a logical task.
 */
public class ActivityTree implements DebugDumpable {

    /**
     * The root activity. Children are referenced from it.
     */
    @NotNull private final StandaloneActivity<?, ?> rootActivity;

    @NotNull private final CommonTaskBeans beans;

    private <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> ActivityTree(@NotNull ActivityDefinition<WD> rootDefinition,
            @NotNull CommonTaskBeans beans) {
        AH handler = beans.activityHandlerRegistry.getHandler(rootDefinition);
        this.rootActivity = StandaloneActivity.createRoot(rootDefinition, handler,  this);
        this.beans = beans;
    }

    public static ActivityTree create(Task rootTask, CommonTaskBeans beans) throws SchemaException {
        ActivityDefinition<?> rootDefinition = ActivityDefinition.createRoot(rootTask, beans);
        return new ActivityTree(rootDefinition, beans);
    }

    @NotNull
    public Activity<?, ?> getRootActivity() {
        return rootActivity;
    }

    @NotNull
    public CommonTaskBeans getBeans() {
        return beans;
    }

    @Override
    public String toString() {
        return "ActivityTree{" +
                "rootActivity=" + rootActivity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        return rootActivity.debugDump(indent);
    }

    @NotNull
    public Activity<?, ?> getActivity(ActivityPath path) throws SchemaException {
        Activity<?, ?> current = rootActivity;
        for (String identifier : path.getIdentifiers()) {
            current = current.getChild(identifier);
        }
        return current;
    }
}