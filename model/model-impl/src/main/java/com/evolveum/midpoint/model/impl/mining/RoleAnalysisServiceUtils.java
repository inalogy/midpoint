package com.evolveum.midpoint.model.impl.mining;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.prism.PrismConstants.Q_ANY;
import static com.evolveum.midpoint.prism.PrismConstants.T_SELF;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

public class RoleAnalysisServiceUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisServiceUtils.class);

    private RoleAnalysisServiceUtils() {
    }

    @NotNull
    protected static RoleAnalysisSessionStatisticType prepareRoleAnalysisSessionStatistic(
            @NotNull AnalysisClusterStatisticType clusterStatistics,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            int deletedClusterMembersCount) {
        Double membershipDensity = clusterStatistics.getMembershipDensity();
        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
        Double meanDensity = sessionStatistic.getMeanDensity();
        Integer clusterCount = sessionStatistic.getClusterCount();

        int newClusterCount = clusterCount - 1;

        RoleAnalysisSessionStatisticType recomputeSessionStatistic = new RoleAnalysisSessionStatisticType();

        if (newClusterCount == 0) {
            recomputeSessionStatistic.setMeanDensity(0.0);
            recomputeSessionStatistic.setProcessedObjectCount(0);
        } else {
            double recomputeMeanDensity = ((meanDensity * clusterCount) - (membershipDensity)) / newClusterCount;
            int recomputeProcessedObjectCount = processedObjectCount - deletedClusterMembersCount;
            recomputeSessionStatistic.setMeanDensity(recomputeMeanDensity);
            recomputeSessionStatistic.setProcessedObjectCount(recomputeProcessedObjectCount);
        }
        recomputeSessionStatistic.setClusterCount(newClusterCount);
        return recomputeSessionStatistic;
    }

    protected static void executeBusinessRoleMigrationTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull TaskType taskObject) {
        try {

            modelInteractionService.submit(
                    activityDefinition,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(taskObject)
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);
        } catch (CommonException e) {
            throw new SystemException("Couldn't execute business role migration activity: ", e);
        }
    }

    protected static void switchRoleToActiveLifeState(
            @NotNull ModelService modelService,
            @NotNull PrismObject<RoleType> roleObject,
            @NotNull Trace logger,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleType.class)
                    .item(ObjectType.F_LIFECYCLE_STATE).replace("active")
                    .asObjectDelta(roleObject.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

            modelService.executeChanges(deltas, null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException |
                SecurityViolationException e) {
            logger.error("Couldn't update lifecycle state of object RoleType {}", roleObject, e);
        }
    }

    protected static @Nullable ActivityDefinitionType createMigrationActivity(
            @NotNull List<FocusType> roleMembersOid,
            @NotNull String roleOid,
            @NotNull Trace logger,
            @NotNull OperationResult result) {
        if (roleMembersOid.isEmpty()) {
            result.recordWarning("Couldn't start migration. There are no members to migrate.");
            logger.warn("Couldn't start migration. There are no members to migrate.");
            return null;
        }

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleType.COMPLEX_TYPE);
        objectReferenceType.setOid(roleOid);

        RoleMembershipManagementWorkDefinitionType roleMembershipManagementWorkDefinitionType = new RoleMembershipManagementWorkDefinitionType();
        roleMembershipManagementWorkDefinitionType.setRoleRef(objectReferenceType);

        ObjectSetType members = new ObjectSetType();
        for (FocusType member : roleMembersOid) {
            ObjectReferenceType memberRef = new ObjectReferenceType();
            memberRef.setOid(member.getOid());
            memberRef.setType(FocusType.COMPLEX_TYPE);
            members.getObjectRef().add(memberRef);
        }
        roleMembershipManagementWorkDefinitionType.setMembers(members);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleMembershipManagement(roleMembershipManagementWorkDefinitionType));
    }

    protected static void cleanClusterDetectedPatterns(
            @NotNull RepositoryService repositoryService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull Trace logger,
            @NotNull OperationResult result) {
        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(Collections.emptyList())
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC)
                    .replace(0.0)
                    .asItemDelta());

            repositoryService.modifyObject(RoleAnalysisClusterType.class, cluster.getOid(), modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            logger.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", cluster.getOid(), e);
        }
    }

    @NotNull
    protected static AssignmentType getAssignmentTo(String unassignedRole) {
        return createAssignmentTo(unassignedRole, ObjectTypes.ROLE);
    }

    protected static @NotNull List<AssignmentPathMetadataType> computeAssignmentPaths(
            @NotNull ObjectReferenceType roleMembershipRef) {
        List<AssignmentPathMetadataType> assignmentPaths = new ArrayList<>();
        List<ProvenanceMetadataType> metadataValues = collectProvenanceMetadata(roleMembershipRef.asReferenceValue());
        if (metadataValues == null) {
            return assignmentPaths;
        }
        for (ProvenanceMetadataType metadataType : metadataValues) {
            assignmentPaths.add(metadataType.getAssignmentPath());
        }
        return assignmentPaths;
    }

    protected static <PV extends PrismValue> List<ProvenanceMetadataType> collectProvenanceMetadata(PV rowValue) {
        List<ValueMetadataType> valueMetadataValues = collectValueMetadata(rowValue);
        return valueMetadataValues.stream()
                .map(ValueMetadataType::getProvenance)
                .collect(Collectors.toList());

    }

    protected static <PV extends PrismValue> @NotNull List<ValueMetadataType> collectValueMetadata(@NotNull PV rowValue) {
        PrismContainer<ValueMetadataType> valueMetadataContainer = rowValue.getValueMetadataAsContainer();
        return (List<ValueMetadataType>) valueMetadataContainer.getRealValues();
    }

    /**
     * Builds an ObjectQuery for assignment search based on the provided user, role, and assignment filters.
     *
     * @param userObjectFiler An optional filter to apply to the user objects.
     * @param roleObjectFilter An optional filter to apply to the role objects.
     * @param assignmentFilter An optional filter to apply to the assignment objects.
     * @return The constructed ObjectQuery for assignment search.
     */
    public static ObjectQuery buildAssignmentSearchObjectQuery(
            @Nullable ObjectFilter userObjectFiler,
            @Nullable ObjectFilter roleObjectFilter,
            @Nullable ObjectFilter assignmentFilter) {
        S_FilterExit filter;

        if (userObjectFiler != null) {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class, AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .filter(userObjectFiler)
                    .endBlock();
        } else {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class);
        }

        if (roleObjectFilter != null) {
            filter = filter
                    .and()
                    .exists(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE)
                    .type(RoleType.class)
                    .block().filter(roleObjectFilter)
                    .endBlock();
        } else {
            filter = filter
                    .and()
                    .block()
                    .ref(AssignmentType.F_TARGET_REF)
                    .type(RoleType.class)
                    .endBlock();
        }

        if (assignmentFilter != null) {
            filter = filter
                    .and()
                    .filter(assignmentFilter);
        }

        return filter.build();
    }

    /**
     * Builds an ObjectQuery for membership search based on the provided user and role filters.
     *
     * @param userObjectFiler An optional filter to apply to the user objects.
     * @param roleObjectFilter An optional filter to apply to the role objects.
     * @return The constructed ObjectQuery for membership search.
     */
    public static ObjectQuery buildMembershipSearchObjectQuery(@Nullable ObjectFilter userObjectFiler, @Nullable ObjectFilter roleObjectFilter) {
        S_FilterExit filter;

        if (userObjectFiler != null) {
            filter = PrismContext.get().queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                    .block()
                    .filter(userObjectFiler)
                    .endBlock();
        } else {
            filter = PrismContext.get().queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF);
        }

        if (roleObjectFilter != null) {
            filter = filter
                    .and()
                    .ref(ItemPath.SELF_PATH, RoleType.COMPLEX_TYPE, null)
                    .block()
                    .filter(roleObjectFilter)
                    .endBlock();

        } else {
            filter = filter
                    .and()
                    .item(T_SELF)
                    .ref(null, RoleType.COMPLEX_TYPE, Q_ANY);
        }

        return filter.build();
    }

    /**
     * Builds an ObjectQuery for assignment role member search based on the provided role, user, and assignment filters.
     *
     * @param roles A set of role OIDs to be included in the search.
     * @param userObjectFiler An optional filter to apply to the user objects.
     * @param roleObjectFilter An optional filter to apply to the role objects.
     * @param assignmentFilter An optional filter to apply to the assignment objects.
     * @return The constructed ObjectQuery for assignment role member search.
     */
    public static ObjectQuery buildAssignmentRoleMemberSearchObjectQuery(
            @NotNull Set<String> roles,
            @Nullable ObjectFilter userObjectFiler,
            @Nullable ObjectFilter roleObjectFilter,
            @Nullable ObjectFilter assignmentFilter) {
        S_FilterExit filter;

        if (userObjectFiler != null) {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class, AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .filter(userObjectFiler)
                    .endBlock();
        } else {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class);
        }

        filter = filter
                .and()
                .item(AssignmentType.F_TARGET_REF)
                .ref(roles.toArray(new String[0]));

        if (roleObjectFilter != null) {
            filter = filter
                    .and()
                    .exists(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE)
                    .type(RoleType.class)
                    .block().filter(roleObjectFilter)
                    .endBlock();
        } else {
            filter = filter
                    .and()
                    .block()
                    .ref(AssignmentType.F_TARGET_REF)
                    .type(RoleType.class)
                    .endBlock();
        }

        if (assignmentFilter != null) {
            filter = filter
                    .and()
                    .filter(assignmentFilter);
        }

        return filter.build();
    }

    /**
     * Builds an ObjectQuery for assignment user access search based on the provided user, role, and assignment filters.
     *
     * @param users A set of user OIDs to be included in the search.
     * @param userObjectFiler An optional filter to apply to the user objects.
     * @param roleObjectFilter An optional filter to apply to the role objects.
     * @param assignmentFilter An optional filter to apply to the assignment objects.
     * @return The constructed ObjectQuery for assignment user access search.
     */
    public static ObjectQuery buildAssignmentUserAccessSearchObjectQuery(
            @NotNull Set<String> users,
            @Nullable ObjectFilter userObjectFiler,
            @Nullable ObjectFilter roleObjectFilter,
            @Nullable ObjectFilter assignmentFilter) {
        S_FilterExit filter;
        if (userObjectFiler != null) {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class, AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .id(users.toArray(new String[0]))
                    .and()
                    .filter(userObjectFiler)
                    .endBlock();
        } else {
            filter = PrismContext.get().queryFor(AssignmentType.class)
                    .ownedBy(UserType.class).block().id(users.toArray(new String[0])).endBlock();
        }

        if (roleObjectFilter != null) {
            filter = filter
                    .and()
                    .exists(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE)
                    .type(RoleType.class)
                    .block().filter(roleObjectFilter)
                    .endBlock();
        } else {
            filter = filter
                    .and()
                    .block()
                    .ref(AssignmentType.F_TARGET_REF)
                    .type(RoleType.class)
                    .endBlock();
        }

        if (assignmentFilter != null) {
            filter = filter
                    .and()
                    .filter(assignmentFilter);
        }

        return filter.build();
    }

    public static void loadDetectedPattern(
            @NotNull RepositoryService repositoryService,
            @NotNull RoleAnalysisDetectionPatternType pattern,
            @NotNull Map<String, RoleAnalysisClusterType> mappedClusters,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull List<DetectedPattern> detectedPatterns,
            @NotNull OperationResult result) {
        PrismContainerValue<?> prismContainerValue = pattern.asPrismContainerValue();
        Map<String, Object> userData = prismContainerValue.getUserData();
        @SuppressWarnings("unchecked") List<Object> containerIdPath = (List<Object>) userData.get("containerIdPath");
        String patternClusterOid = containerIdPath.get(0).toString();
        Long patternId = (Long) containerIdPath.get(1);

        if (!mappedClusters.containsKey(patternClusterOid)) {
            RoleAnalysisClusterType cluster = null;
            try {
                cluster = repositoryService.getObject(RoleAnalysisClusterType.class, patternClusterOid, options, result)
                        .asObjectable();
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Couldn't get cluster object with oid: " + patternClusterOid, e);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't get cluster object with oid: " + patternClusterOid, e);
            }

            if (cluster == null) {
                return;
            }

            mappedClusters.put(patternClusterOid, cluster);
        }

        RoleAnalysisClusterType cluster = mappedClusters.get(patternClusterOid);
        ObjectReferenceType clusterRef = createObjectRef(cluster);
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        DetectedPattern detectedPattern = transformDefaultPattern(pattern, clusterRef, roleAnalysisSessionRef.clone(), patternId);
        detectedPatterns.add(detectedPattern);
    }

    public static void prepareOutlierPartitionMap(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> partitionOutlierMap,
            @NotNull Trace logger) {
        PrismContainerValue<?> prismContainerValue = partition.asPrismContainerValue();
        if (prismContainerValue == null) {
            logger.error("Couldn't get prism container value during outlier partition search");
            return;
        }

        Map<String, Object> userData = prismContainerValue.getUserData();
        @SuppressWarnings("unchecked") List<Object> containerIdPath = (List<Object>) userData.get("containerIdPath");
        String patternClusterOid = containerIdPath.get(0).toString();

        PrismObject<RoleAnalysisOutlierType> outlierPrisObject = roleAnalysisService.getObject(
                RoleAnalysisOutlierType.class, patternClusterOid, task, result);

        if (outlierPrisObject == null) {
            logger.error("Couldn't get outlier object during outlier partition search");
            return;
        }

        partitionOutlierMap.put(partition, outlierPrisObject.asObjectable());
    }

    protected static void loadOutlierDeleteProcessModifications(
            Map<String, ObjectDelta<RoleAnalysisOutlierType>> modifyDeltas,
            @NotNull RoleAnalysisClusterType cluster,
            List<RoleAnalysisOutlierPartitionType> outlierPartitions,
            RoleAnalysisOutlierType outlierObject) throws SchemaException, ObjectNotFoundException {

        ObjectDelta<RoleAnalysisOutlierType> deleteDelta;

        if (outlierPartitions == null || (outlierPartitions.size() == 1
                && outlierPartitions.get(0).getClusterRef().getOid().equals(cluster.getOid()))) {
            deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(RoleAnalysisOutlierType.class, outlierObject.getOid());
            modifyDeltas.put(outlierObject.getOid(), deleteDelta);
        } else {
            RoleAnalysisOutlierPartitionType partitionToDelete = null;

            double overallConfidence = 0;
            double anomalyObjectsConfidence = 0;

            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                if (outlierPartition.getClusterRef().getOid().equals(cluster.getOid())) {
                    partitionToDelete = outlierPartition;
                } else {
                    overallConfidence += outlierPartition.getPartitionAnalysis().getOverallConfidence();
                    anomalyObjectsConfidence += outlierPartition.getPartitionAnalysis().getAnomalyObjectsConfidence();
                }
            }

            int partitionCount = outlierPartitions.size();
            if (partitionToDelete != null) {
                partitionCount--;
            }

            overallConfidence = overallConfidence / partitionCount;
            anomalyObjectsConfidence = anomalyObjectsConfidence / partitionCount;

            if (partitionToDelete == null) {
                throw new ObjectNotFoundException("Couldn't find partition to delete");
            }

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            var finalPartitionToDelete = new RoleAnalysisOutlierPartitionType()
                    .id(partitionToDelete.getId());
            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_PARTITION).delete(
                            finalPartitionToDelete)
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE)
                    .replace(overallConfidence).asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_ANOMALY_OBJECTS_CONFIDENCE)
                    .replace(anomalyObjectsConfidence).asItemDelta());

            deleteDelta = PrismContext.get().deltaFactory().object().createModifyDelta(
                    outlierObject.getOid(), modifications, RoleAnalysisOutlierType.class);
            modifyDeltas.put(outlierObject.getOid(), deleteDelta);
        }

    }

}
