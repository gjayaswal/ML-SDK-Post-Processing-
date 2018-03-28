package com.workfusion.ml.postprocess.blocks;

import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import com.workfusion.vds.api.nlp.model.Field;
import com.workfusion.vds.api.nlp.model.IeDocument;
import com.workfusion.vds.api.nlp.processing.ProcessingException;
import com.workfusion.vds.nlp.processing.grouping.PositionBasedGroupingProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class PositionBasedBlockingProcessor extends PositionBasedGroupingProcessor {

    private String parentBlockCode;
    private Set<String> childBlockCodes;

    public PositionBasedBlockingProcessor(String parentBlockCode, Set<String> childBlockCodes) {
        this.parentBlockCode = parentBlockCode;
        this.childBlockCodes = childBlockCodes;
    }

    @Override
    public void process(IeDocument document) throws ProcessingException {
        final Map<String, Integer> tagToPriority = new HashMap<>();
        tagToPriority.put(parentBlockCode, 1);
        for (String child : childBlockCodes) {
            tagToPriority.put(child, 2);
        }

        List<Field> fields = document.findFields().stream()
                .sorted(Comparator.comparingInt(Field::getBegin)
                        .thenComparing(Comparator.comparingInt(f -> tagToPriority.getOrDefault(f.getName(), 0))))
                .collect(Collectors.toList());

        Map<String, List<Field>> block = new HashMap<>();

        int blockNumber = 0;

        for (Field field : fields) {
            if (isChildBlockField(field)) {
                addFieldToBlock(block, field);
            } else if (isParentBlockField(field)) {
                List<Field> allFieldsInBlock = getAllFieldsInBlock(block);
                if (allFieldsInBlock.isEmpty()) {
                    addFieldToBlock(block, field);
                    continue;
                }

                addFieldToBlock(block, field);
                insertGroupIndices(allFieldsInBlock, blockNumber);
                block.clear();
                blockNumber++;
                addFieldToBlock(block, field);
            }
        }

        if (!block.isEmpty()) {
            insertGroupIndices(getAllFieldsInBlock(block), blockNumber);
        }
    }

    @Override
    protected void insertGroupIndices(Collection<Field> fields, int... indices) {
        fields.forEach(field -> insertGroupIndices(field, indices));
    }

    @Override
    protected void insertGroupIndices(Field field, int... indices) {
        field.getAttributes().put(
                HtmlTagAttr.BLOCK_NUMBER,
                Arrays.stream(indices)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining("|")));
    }

    protected boolean isChildBlockField(Field field) {
        return childBlockCodes.contains(field.getName());
    }

    protected boolean isParentBlockField(Field field) {
        return parentBlockCode.equals(field.getName());
    }

    private void addFieldToBlock(Map<String, List<Field>> block, Field field) {
        if (!block.containsKey(field.getName())) {
            block.put(field.getName(), new LinkedList<>());
        }
        block.get(field.getName()).add(field);
    }

    private List<Field> getAllFieldsInBlock(Map<String, List<Field>> block) {
        return block.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

}
