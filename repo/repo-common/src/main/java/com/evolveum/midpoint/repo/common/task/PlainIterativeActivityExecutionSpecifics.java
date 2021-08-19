/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Provides execution logic and/or execution state related to a plain iterative activity execution.
 *
 * Main responsibilities:
 *
 * 1. Starting the item source and submitting the items to processing.
 * 2. "Pure" processing of an item found, free from threading, reporting, error handling, tracing, etc.
 * (Inherited from {@link ItemProcessor}.)
 * 3. Specifying default error action if no error handling policy is defined or matching.
 *
 * @param <I> Kind of items that are processed.
 */
public interface PlainIterativeActivityExecutionSpecifics<I>
        extends IterativeActivityExecutionSpecifics, ItemProcessor<I> {

    /**
     * Starts the item source for the current bucket (e.g. by issuing the `synchronize` call) and begins processing items
     * generated by it. Returns when the source finishes.
     *
     * See {@link IterativeActivityExecution#iterateOverItemsInBucket(OperationResult)}.
     *
     * Majority of plain iterative activities are not bucketed. However, some of them are, e.g. audit report export.
     *
     * Note that the bucket can be obtained from the activity execution. It is included here simply for convenience.
     */
    void iterateOverItemsInBucket(@NotNull WorkBucketType bucket, OperationResult opResult) throws CommonException;

    /**
     * @return Default error action if no policy is specified or if no policy entry matches.
     */
    @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction();
}