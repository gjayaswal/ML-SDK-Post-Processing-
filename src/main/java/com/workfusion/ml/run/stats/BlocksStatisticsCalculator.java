package com.workfusion.ml.run.stats;

import com.google.gson.reflect.TypeToken;
import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import com.workfusion.nlp.uima.util.GsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlocksStatisticsCalculator {

    private static final String MISSED_SIZE = "_missed_size";
    private static final String MATCH = "_match";
    public static final String DUMMY = "(DUMMY)";

    private Set<String> blockFields;
    private Set<String> constantFields;
    private String mainGroupField;
    private Set<String> groupFields;
    private Map<String, BiFunction<String, String, Boolean>> fieldToComparison;

    public BlocksStatisticsCalculator(Set<String> blockFields, Set<String> constantFields,
                                      String mainGroupField,
                                      Set<String> groupFields,
                                      Map<String, BiFunction<String, String, Boolean>> fieldToComparison) {
        this.blockFields = blockFields;
        this.constantFields = constantFields;
        this.mainGroupField = mainGroupField;
        this.groupFields = groupFields;
        this.fieldToComparison = fieldToComparison;
    }

    private Function<String, Boolean> fieldEmptyChecker = value ->
            StringUtils.isBlank(value) || "n/a".equals(value) || DUMMY.equals(value);

    private Function<Map<String, String>, Boolean> groupEmptyChecker = group -> {
        for (String field : group.keySet()) {
            if (!fieldEmptyChecker.apply(group.get(field))) {
                return false;
            }
        }
        return true;
    };

    private Function<Map<String, Object>, Boolean> blockEmptyChecker = block -> {
        for (String blockField : blockFields) {
            if (!fieldEmptyChecker.apply(block.get(blockField).toString())) {
                return false;
            }
        }
        for (Map<String, String> group : getGroupsForBlock(block)) {
            if (!groupEmptyChecker.apply(group)) {
                return false;
            }
        }
        return true;
    };

    private Function<Map<String, Object>, Boolean> blockEmptyCheckerIgnoreGroups = block -> {
        for (String blockField : blockFields) {
            if (!fieldEmptyChecker.apply(block.get(blockField).toString())) {
                return false;
            }
        }
        List<Map<String, String>> groups = getGroupsForBlock(block);
        for (String groupField : groupFields) {
            List<String> valuesFromAllGroups = groups.stream().map(g -> g.get(groupField)).collect(Collectors.toList());
            long countNonEmptyField = valuesFromAllGroups.stream().filter(v -> !fieldEmptyChecker.apply(v)).count();
            if (countNonEmptyField > 0) {
                return false;
            }
        }
        return true;
    };

    private Function<List<Map<String, Object>>, Boolean> documentEmptyChecker = blocks -> {
        if (blocks.size() > 0) {
            for (String constantField : constantFields) {
                if (!fieldEmptyChecker.apply(blocks.get(0).get(constantField).toString())) {
                    return false;
                }
            }
        }

        for (Map<String, Object> block : blocks) {
            if (!blockEmptyChecker.apply(block)) {
                return false;
            }
        }
        return true;
    };

    private Function<List<Map<String, Object>>, Boolean> documentEmptyCheckerIgnoreBlocks = blocks -> {
        if (blocks.size() > 0) {
            for (String constantField : constantFields) {
                if (!fieldEmptyChecker.apply(blocks.get(0).get(constantField).toString())) {
                    return false;
                }
            }

            for (String blockField : blockFields) {
                List<String> blockFieldValues = blocks.stream().map(b -> b.get(blockField).toString()).collect(Collectors.toList());
                for (String blockFieldValue : blockFieldValues) {
                    if (!fieldEmptyChecker.apply(blockFieldValue)) {
                        return false;
                    }
                }
            }
        }

        List<Map<String, String>> groups = blocks.stream().flatMap(b -> getGroupsForBlock(b).stream()).collect(Collectors.toList());
        for (Map<String, String> group : groups) {
            if (!groupEmptyChecker.apply(group)) {
                return false;
            }
        }
        return true;
    };

    private Function<Collection<ComparisonResult>, Map<String, String>> groupsAttrsProcessor = collection -> {
        LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
        for (String groupField : groupFields) {
            long amountOfMissedValues = collection.stream().filter(cr -> "0".equals(cr.getAttribute(groupField + MATCH))).count();
            attrs.put(groupField + MISSED_SIZE, String.valueOf(amountOfMissedValues));
        }
        return attrs;
    };

    private Function<Collection<ComparisonResult>, Map<String, String>> blocksAttrsProcessor = collection -> {
        LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
        for (String blockField : blockFields) {
            long amountOfMissedValues = collection.stream().filter(cr -> "0".equals(cr.getAttribute(blockField + MATCH))).count();
            attrs.put(blockField + MISSED_SIZE, String.valueOf(amountOfMissedValues));
        }
        for (String groupField : groupFields) {
            int amountOfMissedGroupValues = collection.stream().mapToInt(cr -> Integer.valueOf(cr.getAttribute("groups_" + groupField + MISSED_SIZE))).sum();
            attrs.put(groupField + MISSED_SIZE, String.valueOf(amountOfMissedGroupValues));
        }
        return attrs;
    };

    private Function<Collection<ComparisonResult>, Map<String, String>> emptyAttrsProcessor = collection -> new LinkedHashMap<>();

    private BiFunction<Map<String, String>, Map<String, String>, ComparisonResult> groupMatches = (goldGroup, processedGroup) -> {
        ComparisonResult comparisonResult = new ComparisonResult(true);
        for (String field : processedGroup.keySet()) {
            ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, field).apply(goldGroup.get(field),
                    processedGroup.get(field));
            if (!fieldResult.getResult()) {
                comparisonResult.setResult(false);
            }
            comparisonResult.copyAllAttributesWithPrefix(fieldResult, field);
        }
        return comparisonResult;
    };

    private BiFunction<Map<String, String>, Map<String, String>, Double> groupMatchesScore = (goldGroup, processedGroup) -> {
        int totalFields = Math.max(goldGroup.size(), processedGroup.size());
        double score = 0;

        for (String field : processedGroup.keySet()) {
            ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, field).apply(goldGroup.get(field),
                    processedGroup.get(field));
            if (fieldResult.getResult()) {
                score += 1 / totalFields;
            }
        }
        return score;
    };

    private BiFunction<Map<String, Object>, Map<String, Object>, ComparisonResult> blockMatches = (goldBlock, processedBlock) -> {
        ComparisonResult comparisonResult = new ComparisonResult(true);
        for (String blockField : blockFields) {
            ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, blockField).apply(goldBlock.get(blockField).toString(),
                    processedBlock.get(blockField).toString());
            if (!fieldResult.getResult()) {
                comparisonResult.setResult(false);
            }
            comparisonResult.copyAllAttributesWithPrefix(fieldResult, blockField);
        }

        List<Map<String, String>> goldGroups = new LinkedList<>(getGroupsForBlock(goldBlock));
        List<Map<String, String>> processedGroups = new LinkedList<>(getGroupsForBlock(processedBlock));

        levelGroups(goldGroups, processedGroups);

        ComparisonResult groupsMatchResult = allMatch(goldGroups, processedGroups, groupMatches, groupEmptyChecker, groupsAttrsProcessor);
        if (!groupsMatchResult.getResult()) {
            comparisonResult.setResult(false);
        }
        comparisonResult.copyAllAttributesWithPrefix(groupsMatchResult, "groups");

        return comparisonResult;
    };

    private BiFunction<Map<String, Object>, Map<String, Object>, Double> blockMatchesScore = (goldBlock, processedBlock) -> {
        List<Map<String, String>> goldGroups = new LinkedList<>(getGroupsForBlock(goldBlock));
        List<Map<String, String>> processedGroups = new LinkedList<>(getGroupsForBlock(processedBlock));
        levelGroups(goldGroups, processedGroups);
        int totalGroups = Math.max(goldGroups.size(), processedGroups.size()) + blockFields.size();
        double score = 0;

        for (String blockField : blockFields) {
            ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, blockField).apply(goldBlock.get(blockField).toString(),
                    processedBlock.get(blockField).toString());
            if (fieldResult.getResult()) {
                score += 1d / totalGroups;
            }
        }

        levelGroups(goldGroups, processedGroups);

        score += allMatchScore(goldGroups, processedGroups, groupMatchesScore, groupEmptyChecker, emptyAttrsProcessor);

        return score;
    };

    private BiFunction<Map<String, Object>, Map<String, Object>, ComparisonResult> blockMatchesIgnoreGroups = (goldBlock, extractedBlock) -> {
        ComparisonResult comparisonResult = new ComparisonResult(true);
        for (String blockField : blockFields) {
            comparisonResult.putAttribute(blockField + MATCH, "1");
            if (!fieldMatches(blockField, goldBlock.get(blockField).toString(), extractedBlock.get(blockField).toString())) {
                comparisonResult.setResult(false);
                comparisonResult.putAttribute(blockField + MATCH, "0");
            }
        }
        List<Map<String, String>> goldGroups = new LinkedList<>(getGroupsForBlock(goldBlock));
        List<Map<String, String>> extractedGroups = new LinkedList<>(getGroupsForBlock(extractedBlock));
        levelGroups(goldGroups, extractedGroups);

        for (String groupField : groupFields) {
            List<String> goldValues = goldGroups.stream().map(g -> g.get(groupField)).collect(Collectors.toList());
            List<String> extractedValues = extractedGroups.stream().map(g -> g.get(groupField)).collect(Collectors.toList());
            comparisonResult.putAttribute("gold_" + groupField + "_size", String.valueOf(goldValues.size()));
            comparisonResult.putAttribute("extracted_" + groupField + "_size", String.valueOf(extractedValues.size()));
            comparisonResult.putAttribute(groupField + "_" + MISSED_SIZE, "0");
            ComparisonResult allMatchResult = allMatch(goldValues, extractedValues,
                    new FieldMatcher(fieldToComparison, groupField),
                    fieldEmptyChecker,
                    emptyAttrsProcessor);
            if (!allMatchResult.getResult()) {
                comparisonResult.setResult(false);
                comparisonResult.copyAllAttributesWithPrefix(allMatchResult, groupField);
            }
        }

        return comparisonResult;
    };

    private BiFunction<List<Map<String, Object>>, List<Map<String, Object>>, ComparisonResult> documentMatches = (goldBlocks, processedBlocks) -> {
        goldBlocks = new LinkedList<>(goldBlocks);
        processedBlocks = new LinkedList<>(processedBlocks);

        ComparisonResult comparisonResult = new ComparisonResult(true);

        for (String constantField : constantFields) {
            ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, constantField).apply(goldBlocks.get(0).get(constantField).toString(),
                    processedBlocks.get(0).get(constantField).toString());
            if (!fieldResult.getResult()) {
                comparisonResult.setResult(false);
            }
            comparisonResult.copyAllAttributesWithPrefix(fieldResult, constantField);
        }

        ComparisonResult blocksMatchResult = allMatch(goldBlocks, processedBlocks, blockMatches, blockEmptyChecker, blocksAttrsProcessor);
        if (!blocksMatchResult.getResult()) {
            comparisonResult.setResult(false);
        }
        comparisonResult.copyAllAttributesWithPrefix(blocksMatchResult, "blocks");

        return comparisonResult;
    };

    private BiFunction<List<Map<String, Object>>, List<Map<String, Object>>, ComparisonResult> documentMatchesIgnoreBlocks = (goldBlocks, processedBlocks) -> {
        goldBlocks = new LinkedList<>(goldBlocks);
        processedBlocks = new LinkedList<>(processedBlocks);

        ComparisonResult comparisonResult = new ComparisonResult(true);

        if (processedBlocks.size() > 0) {
            for (String constantField : constantFields) {
                ComparisonResult fieldResult = new FieldMatcher(fieldToComparison, constantField).apply(goldBlocks.get(0).get(constantField).toString(),
                        processedBlocks.get(0).get(constantField).toString());
                if (!fieldResult.getResult()) {
                    comparisonResult.setResult(false);
                }
                comparisonResult.copyAllAttributesWithPrefix(fieldResult, constantField);
            }

            for (String blockField : blockFields) {
                List<String> processedBlockFieldValues = processedBlocks.stream().map(b -> b.get(blockField).toString()).collect(Collectors.toList());
                List<String> goldBlockFieldValues = goldBlocks.stream().map(b -> b.get(blockField).toString()).collect(Collectors.toList());

                ComparisonResult allFieldsMatchResult = allMatch(goldBlockFieldValues,
                        processedBlockFieldValues,
                        new FieldMatcher(fieldToComparison, blockField),
                        fieldEmptyChecker,
                        emptyAttrsProcessor);
                if (!allFieldsMatchResult.getResult()) {
                    comparisonResult.setResult(false);
                }
                comparisonResult.copyAllAttributesWithPrefix(allFieldsMatchResult, blockField);
            }
        }

        List<Map<String, String>> goldGroups = goldBlocks.stream().flatMap(b -> getGroupsForBlock(b).stream()).collect(Collectors.toList());
        List<Map<String, String>> processedGroups = processedBlocks.stream().flatMap(b -> getGroupsForBlock(b).stream()).collect(Collectors.toList());

        levelGroups(goldGroups, processedGroups);

        ComparisonResult groupsComparisonResult = allMatch(goldGroups,
                processedGroups,
                groupMatches,
                groupEmptyChecker,
                groupsAttrsProcessor);
        if (!groupsComparisonResult.getResult()) {
            comparisonResult.setResult(false);
        }
        comparisonResult.copyAllAttributesWithPrefix(groupsComparisonResult, "groups");

        return comparisonResult;
    };

    public BiFunction <String, String, List<Map<String, String>>> balancedFieldLevelStats = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("balanced field level stats");
        List<Map<String, Object>> goldBlocks = readBlocksFromJson(goldBlocksJson);
        List<Map<String, Object>> processedBlocks = readBlocksFromJson(postProcessedBlocksJson);

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        for (int i = 0; i < goldBlocks.size(); i++) {
            Map<String, Object> goldBlock = goldBlocks.get(i);
            Map<String, Object> processedBlock = processedBlocks.get(i);

            //System.out.println("gold: " + goldBlock);
            //System.out.println("processed: " + processedBlock);

            if (i == 0) {
                for (String constantField : constantFields) {
                    Map<String, String> result =
                            createResult(constantField, goldBlock.get(constantField).toString(),
                                    processedBlock.get(constantField).toString(),
                                    new FieldMatcher(fieldToComparison, constantField), fieldEmptyChecker);
                    result.put("field", constantField);
                    statRecords.add(result);
                }
            }

            statRecords.addAll(collectBalancedFieldLevelStats(goldBlock, processedBlock));
        }

        return statRecords;
    };

    private List<Map<String, String>> collectBalancedFieldLevelStats(Map<String, Object> goldBlock, Map<String, Object> processedBlock) {
        List<Map<String, String>> statRecords = new LinkedList<>();
        for (String blockField : blockFields) {
            Map<String, String> result =
                    createResult(blockField, goldBlock.get(blockField).toString(),
                            processedBlock.get(blockField).toString(),
                            new FieldMatcher(fieldToComparison, blockField), fieldEmptyChecker);
            result.put("field", blockField);
            statRecords.add(result);
        }

        List<Map<String, String>> goldBlockGroups = getGroupsForBlock(goldBlock);
        List<Map<String, String>> processedBlockGroups = getGroupsForBlock(processedBlock);
        levelGroups(goldBlockGroups, processedBlockGroups);

        for (int j = 0; j < Math.max(goldBlockGroups.size(), processedBlockGroups.size()); j++) {
            Map<String, String> goldGroup = goldBlockGroups.get(j);
            Map<String, String> processedGroup = processedBlockGroups.get(j);
            for (String groupField : groupFields) {
                Map<String, String> result =
                        createResult(groupField, goldGroup.get(groupField), processedGroup.get(groupField),
                                new FieldMatcher(fieldToComparison, groupField), fieldEmptyChecker);
                result.put("field", groupField);
                result.put(HtmlTagAttr.TAB_NUMBER, String.valueOf(j));
                statRecords.add(result);
            }
        }
        return statRecords;
    }

    public BiFunction <String, String, List<Map<String, String>>> balancedFieldLevelStatsWithBlockSearch = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("balanced field level stats with block search");
        List<Map<String, Object>> goldBlocks = readBlocksFromJson(goldBlocksJson);
        List<Map<String, Object>> processedBlocks = readBlocksFromJson(postProcessedBlocksJson);

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        for (int i = 0; i < goldBlocks.size(); i++) {
            Map<String, Object> goldBlock = goldBlocks.get(i);
            Map<String, Object> processedBlock = processedBlocks.get(i);

            if (i == 0) {
                for (String constantField : constantFields) {
                    Map<String, String> result =
                            createResult(constantField, goldBlock.get(constantField).toString(),
                                    processedBlock.get(constantField).toString(),
                                    new FieldMatcher(fieldToComparison, constantField), fieldEmptyChecker);
                    result.put("field", constantField);
                    statRecords.add(result);
                }
            }

        }

        for (Map<String, Object> goldBlock : goldBlocks) {
            double bestScore = -1;
            Map<String, Object> bestProcessedBlock = null;
            for (Map<String, Object> processedBlock : processedBlocks) {
                Double interimScore = blockMatchesScore.apply(goldBlock, processedBlock);
                if (interimScore > bestScore) {
                    bestScore = interimScore;
                    bestProcessedBlock = processedBlock;
                }
            }
            processedBlocks.remove(bestProcessedBlock);

            statRecords.addAll(collectBalancedFieldLevelStats(goldBlock, bestProcessedBlock));
        }

        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> fieldLevelStats = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("field level stats");
        List<Map<String, Object>> goldBlocks = readBlocksFromJson(goldBlocksJson);
        List<Map<String, Object>> processedBlocks = readBlocksFromJson(postProcessedBlocksJson);

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        for (String field : constantFields) {
            LinkedList<String> extractedValues = processedBlocks.stream().limit(1).map(b -> b.get(field).toString()).distinct()
                    .collect(Collectors.toCollection(LinkedList::new));
            LinkedList<String> goldValues = goldBlocks.stream().limit(1).map(b -> b.get(field).toString()).distinct()
                    .collect(Collectors.toCollection(LinkedList::new));
            statRecords.addAll(processField(field, goldValues, extractedValues));
        }

        for (String field : blockFields) {
            LinkedList<String> extractedValues = processedBlocks.stream().map(b -> b.get(field).toString())
                    .collect(Collectors.toCollection(LinkedList::new));
            LinkedList<String> goldValues = goldBlocks.stream().map(b -> b.get(field).toString())
                    .collect(Collectors.toCollection(LinkedList::new));
            statRecords.addAll(processField(field, goldValues, extractedValues));
        }

        List<Map<String, String>> processedGroups = processedBlocks.stream().map(this::getGroupsForBlock)
                .flatMap(Collection::stream).collect(Collectors.toList());
        List<Map<String, String>> goldGroups = goldBlocks.stream().map(this::getGroupsForBlock)
                .flatMap(Collection::stream).collect(Collectors.toList());

        levelGroups(goldGroups, processedGroups);

        for (String field : groupFields) {
            LinkedList<String> extractedValues = processedGroups.stream()
                    .map(m -> m.get(field))
                    .collect(Collectors.toCollection(LinkedList::new));
            LinkedList<String> goldValues = goldGroups.stream()
                    .map(m -> m.get(field))
                    .collect(Collectors.toCollection(LinkedList::new));
            statRecords.addAll(processField(field, goldValues, extractedValues));
        }

        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> groupLevelStats = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("group level stats");
        List<Map<String, Object>> goldBlocks = readBlocksFromJson(goldBlocksJson);
        List<Map<String, Object>> processedBlocks = readBlocksFromJson(postProcessedBlocksJson);

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        LinkedList<Map<String, String>> extractedGroups = processedBlocks.stream().map(this::getGroupsForBlock)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<Map<String, String>> goldGroups = goldBlocks.stream().map(this::getGroupsForBlock)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedList::new));
        levelGroups(goldGroups, extractedGroups);

        List<Map<String, String>> toRemoveExtracted = new LinkedList<>(extractedGroups);

        for (Map<String, String> extractedGroup : extractedGroups) {
            for (Map<String, String> goldGroup : goldGroups) {
                if (!groupMatches.apply(goldGroup, extractedGroup).getResult()) {
                    continue;
                }
                goldGroups.remove(goldGroup);
                toRemoveExtracted.remove(extractedGroup);
                statRecords.add(createResult("group", goldGroup, extractedGroup, groupMatches, groupEmptyChecker));
                break;
            }
        }
        for (int i = 0; i < toRemoveExtracted.size(); i++) {
            statRecords.add(createResult("group", goldGroups.get(i), toRemoveExtracted.get(i), groupMatches, groupEmptyChecker));
        }

        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> blockLevelStats = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("block level stats");
        List<Map<String, Object>> goldBlocks = new LinkedList<>(readBlocksFromJson(goldBlocksJson));
        List<Map<String, Object>> processedBlocks = new LinkedList<>(readBlocksFromJson(postProcessedBlocksJson));

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, Object>> toRemoveProcessedBlocks = new LinkedList<>(processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        for (Map<String, Object> processedBlock : processedBlocks) {
            for (Map<String, Object> goldBlock : goldBlocks) {
                if (!blockMatches.apply(goldBlock, processedBlock).getResult()) {
                    continue;
                }
                toRemoveProcessedBlocks.remove(processedBlock);
                goldBlocks.remove(goldBlock);
                statRecords.add(createResult("block", goldBlock, processedBlock, blockMatches, blockEmptyChecker));
                break;
            }
        }
        for (int i = 0; i < toRemoveProcessedBlocks.size(); i++) {
            statRecords.add(createResult("block", goldBlocks.get(i), toRemoveProcessedBlocks.get(i), blockMatches, blockEmptyChecker));
        }


        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> blockLevelStatsIgnoreGroups = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("block level stats ignore groups");
        List<Map<String, Object>> goldBlocks = new LinkedList<>(readBlocksFromJson(goldBlocksJson));
        List<Map<String, Object>> processedBlocks = new LinkedList<>(readBlocksFromJson(postProcessedBlocksJson));

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        List<Map<String, Object>> toRemoveProcessedBlocks = new LinkedList<>(processedBlocks);

        for (Map<String, Object> processedBlock : processedBlocks) {
            for (Map<String, Object> goldBlock : goldBlocks) {
                if (!blockMatchesIgnoreGroups.apply(goldBlock, processedBlock).getResult()) {
                    continue;
                }
                toRemoveProcessedBlocks.remove(processedBlock);
                goldBlocks.remove(goldBlock);
                statRecords.add(createResult("block", goldBlock, processedBlock, blockMatchesIgnoreGroups, blockEmptyCheckerIgnoreGroups));
                break;
            }
        }
        for (int i = 0; i < toRemoveProcessedBlocks.size(); i++) {
            statRecords.add(createResult("block", goldBlocks.get(i), toRemoveProcessedBlocks.get(i), blockMatchesIgnoreGroups, blockEmptyCheckerIgnoreGroups));
        }


        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> documentLevelStats = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("document level stats");
        List<Map<String, Object>> goldBlocks = new LinkedList<>(readBlocksFromJson(goldBlocksJson));
        List<Map<String, Object>> processedBlocks = new LinkedList<>(readBlocksFromJson(postProcessedBlocksJson));

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        Map<String, String> documentResult = createResult("document", goldBlocks, processedBlocks, documentMatches, documentEmptyChecker);
        statRecords.add(documentResult);

        return statRecords;
    };

    public BiFunction <String, String, List<Map<String, String>>> documentLevelStatsIgnoreBlocking = (goldBlocksJson, postProcessedBlocksJson) -> {
        System.out.println("document level stats ignore blocks");
        List<Map<String, Object>> goldBlocks = new LinkedList<>(readBlocksFromJson(goldBlocksJson));
        List<Map<String, Object>> processedBlocks = new LinkedList<>(readBlocksFromJson(postProcessedBlocksJson));

        levelBlocks(goldBlocks, processedBlocks);

        List<Map<String, String>> statRecords = new LinkedList<>();

        Map<String, String> documentResult = createResult("document", goldBlocks, processedBlocks, documentMatchesIgnoreBlocks, documentEmptyCheckerIgnoreBlocks);

        statRecords.add(documentResult);

        return statRecords;
    };

    private List<Map<String, String>> processField(String field, LinkedList<String> goldValues, LinkedList<String> extractedValues) {
        List<Map<String, String>> statRecords = new LinkedList<>();

        List<String> toRemoveExtracted = new LinkedList<>(extractedValues);

        for (String extractedValue : extractedValues) {
            for (String goldValue : goldValues) {
                if (!fieldMatches(field, goldValue, extractedValue)) {
                    continue;
                }
                toRemoveExtracted.remove(extractedValue);
                goldValues.remove(goldValue);
                Map<String, String> result = createResult(field, goldValue, extractedValue, new FieldMatcher(fieldToComparison, field), fieldEmptyChecker);
                result.put("field", field);
                statRecords.add(result);
                break;
            }
        }
        for (int i = 0; i < toRemoveExtracted.size(); i++) {
            Map<String, String> result = createResult(field, goldValues.get(i), toRemoveExtracted.get(i), new FieldMatcher(fieldToComparison, field), fieldEmptyChecker);
            result.put("field", field);
            statRecords.add(result);
        }
        return statRecords;
    }

    private void levelGroups(List<Map<String, String>> goldBlockGroups, List<Map<String, String>> processedBlockGroups) {
        int maxGroups = Math.max(goldBlockGroups.size(), processedBlockGroups.size());
        for (int j = goldBlockGroups.size(); j < maxGroups; j++) {
            goldBlockGroups.add(createEmptyGroup(groupFields));
        }
        for (int j = processedBlockGroups.size(); j < maxGroups; j++) {
            processedBlockGroups.add(createEmptyGroup(groupFields));
        }
        for (int i = 0; i < processedBlockGroups.size(); i++) {
            for (String groupField : groupFields) {
                processedBlockGroups.get(i).putIfAbsent(groupField, StringUtils.EMPTY);
                goldBlockGroups.get(i).putIfAbsent(groupField, StringUtils.EMPTY);
            }
        }
    }

    private int levelBlocks(List<Map<String, Object>> goldBlocks, List<Map<String, Object>> processedBlocks) {
        int maxBlocks = Math.max(goldBlocks.size(), processedBlocks.size());
        for (int i = goldBlocks.size(); i < maxBlocks; i++) {
            goldBlocks.add(createEmptyBlock(blockFields));
        }
        for (int i = processedBlocks.size(); i < maxBlocks; i++) {
            processedBlocks.add(createEmptyBlock(blockFields));
        }
        for (int i = 0; i < processedBlocks.size(); i++) {
            for (String blockField : blockFields) {
                processedBlocks.get(i).putIfAbsent(blockField, StringUtils.EMPTY);
                goldBlocks.get(i).putIfAbsent(blockField, StringUtils.EMPTY);
            }
            for (String blockField : constantFields) {
                processedBlocks.get(i).putIfAbsent(blockField, StringUtils.EMPTY);
                goldBlocks.get(i).putIfAbsent(blockField, StringUtils.EMPTY);
            }
            processedBlocks.get(i).putIfAbsent(mainGroupField, new ArrayList<Map<String, String>>());
            goldBlocks.get(i).putIfAbsent(mainGroupField, new ArrayList<Map<String, String>>());
        }
        return maxBlocks;
    }

    private List<Map<String, Object>> readBlocksFromJson(String blocksJson) {
        return GsonUtils.GSON.fromJson(blocksJson, new TypeToken<List<Map<String, Object>>>() {
        }.getType());
    }

    private boolean fieldMatches(String field, String gold, String processed) {
        return new FieldMatcher(fieldToComparison, field).apply(gold, processed).getResult();
    }


    private List<Map<String, String>> getGroupsForBlock(Map<String, Object> block) {
        return (List<Map<String, String>>) block.getOrDefault(mainGroupField, new LinkedList<Map<String, String>>());
    }

    private Map<String, Object> createEmptyBlock(Set<String> blockFields) {
        Map<String, Object> block = new LinkedHashMap<>();
        for (String blockField : blockFields) {
            block.put(blockField, DUMMY);
        }
        for (String constantField : constantFields) {
            block.put(constantField, DUMMY);
        }
        block.put(mainGroupField, new ArrayList<Map<String, String>>());
        return block;
    }

    private Map<String, String> createEmptyGroup(Set<String> groupFields) {
        Map<String, String> group = new LinkedHashMap<>();
        for (String groupField : groupFields) {
            group.put(groupField, DUMMY);
        }
        return group;
    }

    private static String naToEmpty(String s) {
        if ("n/a".equalsIgnoreCase(s)) {
            s = "";
        }
        return s;
    }

    private <T extends List<U>, U> ComparisonResult allMatch(T goldList,
                                                             T extractedList,
                                                             BiFunction<U, U, ComparisonResult> compare,
                                                             Function<U, Boolean> emptyChecker,
                                                             Function<Collection<ComparisonResult>, Map<String, String>> attrProcessor) {
        //System.out.println("gold list: " + goldList);
        //System.out.println("extracted list: " + extractedList);

        List<U> goldListCopy = new LinkedList<>(goldList);
        List<U> extractedListCopy = new LinkedList<>(extractedList);

        /*
        a - a
        b - b
        c - e
        d - f
         */
        int matchesFound = 0;

        for (U extracted : extractedList) {
            for (U gold : goldListCopy) {
                ComparisonResult middleResult = compare.apply(gold, extracted);
                if (!middleResult.getResult()) {
                    continue;
                }
                goldListCopy.remove(gold);
                extractedListCopy.remove(extracted);
                matchesFound++;
                break;
            }
        }

        LinkedList<ComparisonResult> missedResults = new LinkedList<>();
        for (int i = 0; i < extractedListCopy.size(); i++) {
            U extractedMissed = extractedListCopy.get(i);
            U goldMissed = goldListCopy.get(i);
            missedResults.add(compare.apply(goldMissed, extractedMissed));
        }
        Map<String, String> attrs = attrProcessor.apply(missedResults);

        boolean result = extractedList.size() == matchesFound;

        ComparisonResult comparisonResult = new ComparisonResult(result);
        comparisonResult.putAttribute("all_match", String.valueOf(result ? "1" : "0"));

        long goldNotEmptySize = goldList.stream().filter(i -> !emptyChecker.apply(i)).count();
        comparisonResult.putAttribute("gold_size", String.valueOf(goldNotEmptySize));

        long extractedNotEmptySize = extractedList.stream().filter(i -> !emptyChecker.apply(i)).count();
        comparisonResult.putAttribute("extracted_size", String.valueOf(extractedNotEmptySize));

        comparisonResult.putAttribute(MISSED_SIZE, String.valueOf(extractedList.size() - matchesFound));

        comparisonResult.getAttributes().putAll(attrs);

        return comparisonResult;
    }

    private <T extends List<U>, U> double allMatchScore(T goldList,
                                                             T extractedList,
                                                             BiFunction<U, U, Double> compareScore,
                                                             Function<U, Boolean> emptyChecker,
                                                             Function<Collection<ComparisonResult>, Map<String, String>> attrProcessor) {
        int total = Math.max(goldList.size(), extractedList.size());
        double score = 0;

        List<U> extractedListCopy = new LinkedList<>(extractedList);

        for (U gold : goldList) {
            double bestScore = -1d;
            U bestExtracted = null;
            for (U extracted : extractedListCopy) {
                Double interimScore = compareScore.apply(gold, extracted);
                if (interimScore > bestScore) {
                    bestScore = interimScore;
                    bestExtracted = gold;
                }
            }
            extractedListCopy.remove(bestExtracted);
            score += (double)bestScore / total;
        }

        return score;
    }

    private <T> Map<String, String> createResult(String field, T gold, T processed, BiFunction<T, T, ComparisonResult> compare,
                                                 Function<T, Boolean> emptyChecker) {
        Map<String, String> resultRecord = new LinkedHashMap<>();
        resultRecord.put("gold", gold.toString());
        resultRecord.put("extracted", processed.toString());
        resultRecord.put("tp", "0");
        resultRecord.put("fp", "0");
        resultRecord.put("fn", "0");
        resultRecord.put("tn", "0");
        resultRecord.put("field", field);

        ComparisonResult result = compare.apply(gold, processed);
        resultRecord.putAll(result.getAttributes());
        boolean matches = result.getResult();
        if (matches) {
            if (emptyChecker.apply(gold)) {
                resultRecord.put("tn", "1");
            } else {
                resultRecord.put("tp", "1");
            }
        } else {
            if (emptyChecker.apply(gold)) {
                resultRecord.put("fp", "1");
            } else if (emptyChecker.apply(processed)) {
                resultRecord.put("fn", "1");
            } else {
                resultRecord.put("fp", "1");
                resultRecord.put("fn", "1");
            }
        }
        return resultRecord;
    }

    private static class FieldMatcher implements BiFunction<String, String, ComparisonResult> {
        private Map<String, BiFunction<String, String, Boolean>> fieldToComparison;
        private String field;

        FieldMatcher(Map<String, BiFunction<String, String, Boolean>> fieldToComparison, String field) {
            this.fieldToComparison = fieldToComparison;
            this.field = field;
        }

        @Override
        public ComparisonResult apply(String s, String s2) {
            s = naToEmpty(s);
            s2 = naToEmpty(s2);
            BiFunction<String, String, Boolean> comparison = fieldToComparison.getOrDefault(field, String::equalsIgnoreCase);
            Boolean result = comparison.apply(s, s2);
            ComparisonResult comparisonResult = new ComparisonResult(result);
            comparisonResult.putAttribute("match", result ? "1" : "0");
            return comparisonResult;
        }
    }

    private static class ComparisonResult {
        private Boolean result;
        private Map<String, String> attributes = new LinkedHashMap<>();

        public ComparisonResult(Boolean result) {
            this.result = result;
        }

        public ComparisonResult(Boolean result, Map<String, String> attributes) {
            this.result = result;
            this.attributes = attributes;
        }

        public Boolean getResult() {
            return result;
        }

        public void setResult(Boolean result) {
            this.result = result;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void putAttribute(String key, String value) {
            attributes.put(key, value);
        }

        public String getAttribute(String key) {
            return attributes.get(key);
        }

        public void copyAllAttributesWithPrefix(ComparisonResult resultB, String prefix) {
            for (String attr : resultB.getAttributes().keySet()) {
                putAttribute(prefix + "_" + attr, resultB.getAttribute(attr));
            }
        }
    }
}
