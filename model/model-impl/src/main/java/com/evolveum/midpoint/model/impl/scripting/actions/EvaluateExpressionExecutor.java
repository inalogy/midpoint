/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.model.impl.scripting.actions.EvaluateExpressionExecutor.ExpressionEvaluationParameters;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.EvaluateExpressionActionExpressionType;

/**
 * Executes "evaluate-expression" (s:evaluateExpression) actions.
 */
@Component
public class EvaluateExpressionExecutor extends AbstractExecuteExecutor<ExpressionEvaluationParameters> {

    private static final String NAME = "evaluate-expression";
    private static final String PARAM_EXPRESSION = "expression";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, EvaluateExpressionActionExpressionType.class, this);
    }

    @Override
    @NotNull String getName() {
        return NAME;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        ExpressionType expressionBean = expressionHelper.getActionArgument(
                ExpressionType.class, action,
                EvaluateExpressionActionExpressionType.F_EXPRESSION, PARAM_EXPRESSION,
                input, context, null,
                PARAM_EXPRESSION, globalResult);

        configCheck(expressionBean != null, "No expression provided");

        ExpressionEvaluationParameters parameters =
                getParameters(action, input, context, globalResult, p -> new ExpressionEvaluationParameters(expressionBean, p));

        return executeInternal(input, parameters, context, globalResult);
    }

    @Override
    <I> Object doSingleExecution(ExpressionEvaluationParameters parameters, TypedValue<I> inputTypedValue,
            VariablesMap externalVariables, ExecutionContext context, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        VariablesMap variables = createVariables(externalVariables);

        variables.put(ExpressionConstants.VAR_INPUT, inputTypedValue);

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment.ExpressionEnvironmentBuilder<>()
                        .lensContext(getLensContext(externalVariables))
                        .currentResult(result)
                        .currentTask(context.getTask())
                        .build());
        try {
            // TEMPORARY: What about multiple values?
            return ExpressionUtil.evaluateExpression(
                    variables,
                    parameters.outputDefinition,
                    parameters.expressionBean,
                    context.getExpressionProfile(),
                    expressionFactory,
                    "in '" + NAME + "' action",
                    context.getTask(), result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Override
    @NotNull String getLegacyActionName() {
        return NAME;
    }

    @Override
    @NotNull String getConfigurationElementName() {
        return SchemaConstantsGenerated.SC_EVALUATE_EXPRESSION.getLocalPart();
    }

    static class ExpressionEvaluationParameters extends Parameters {

        final ExpressionType expressionBean;

        ExpressionEvaluationParameters(ExpressionType expressionBean, Parameters p) {
            super(p.outputDefinition, p.forWholeInput, p.quiet);
            this.expressionBean = expressionBean;
        }
    }
}