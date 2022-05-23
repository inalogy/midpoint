/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.prism.query.ReferencedByFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.mapping.CountMappingResolver;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver.ResolutionResult;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Filter processor that resolves {@link ExistsFilter}.
 *
 * @param <Q> query type of the original context
 * @param <R> row type related to {@link Q}
 */
public class ReferencedByFilterProcessor<Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<ReferencedByFilter> {

    private static final ItemPath TARGET = ItemPath.create(new ParentPathSegment());
    private final SqaleQueryContext<?, Q, R> context;
    private final QueryModelMapping<?, Q, R> mapping;

    public ReferencedByFilterProcessor(SqaleQueryContext<?, Q, R> context) {
        this.context = context;
        this.mapping = context.mapping();
    }

    private ReferencedByFilterProcessor(
            SqaleQueryContext<?, Q, R> context, QueryModelMapping<?, Q, R> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public Predicate process(ReferencedByFilter filter) throws RepositoryException {
        return process(filter.getType(), filter.getPath(), filter.getRelation(), filter.getFilter());
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR>  Predicate process(
            @Nullable ComplexTypeDefinition ownerDefinition, ItemPath path, QName relation, ObjectFilter innerFilter) throws RepositoryException {

        var targetMapping = context.repositoryContext().getMappingBySchemaType(ownerDefinition.getCompileTimeClass());
        //var refereePath = path.append(new ObjectReferencePathSegment());
        var targetContext = context.subquery(targetMapping);
        relation = relation != null ? relation : PrismConstants.Q_ANY;
        // We use our package private RefFilter and logic in existing ref filter implementation
        // to sneak OID from parent
        targetContext.processFilter(internalRefFilter(path, relation,context.path(QObject.class).oid));

        // We add nested filter for referencing object
        targetContext.processFilter(innerFilter);
        return targetContext.sqlQuery().exists();

    }

    private ObjectFilter internalRefFilter(ItemPath path, QName relation, UuidPath oid) {
        return new RefFilterWithRepoPath(path, relation, oid);
    }

}