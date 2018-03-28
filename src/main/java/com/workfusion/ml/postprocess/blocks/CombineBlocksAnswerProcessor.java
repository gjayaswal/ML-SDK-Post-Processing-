package com.workfusion.ml.postprocess.blocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.workfusion.ml.Answer;
import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import com.workfusion.nlp.uima.util.GsonUtils;
import com.workfusion.vds.api.nlp.model.Field;
import com.workfusion.vds.api.nlp.model.IeDocument;
import com.workfusion.vds.api.nlp.processing.IeProcessor;
import com.workfusion.vds.api.nlp.processing.ProcessingException;
import java.math.BigDecimal;

import java.util.*;
import java.util.stream.Collectors;

public class CombineBlocksAnswerProcessor implements IeProcessor {

    private String parentBlockCode;
    private Set<String> childBlockCodes;
    private Set<String> groupCodes;
    private String mainGroupCode;
    private Set<String> constantCodes;
    private String blockJsonCode;

    public CombineBlocksAnswerProcessor(String parentBlockCode, Set<String> childBlockCodes, Set<String> groupCodes,
                                        String mainGroupCode, Set<String> constantCodes, String blockJsonCode) {
        this.parentBlockCode = parentBlockCode;
        this.childBlockCodes = childBlockCodes;
        this.groupCodes = groupCodes;
        this.mainGroupCode = mainGroupCode;
        this.constantCodes = constantCodes;
        this.blockJsonCode = blockJsonCode;
    }

    @Override
    public void process(IeDocument ieDocument) throws ProcessingException {
        List<List<Field>> fieldLists = ieDocument.findFields().stream()
                .filter(this::isValidTag)
                .collect(Collectors.groupingBy(f -> f.getAttributes().get(HtmlTagAttr.BLOCK_NUMBER)))
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        List<Map<String, Object>> blocks = new LinkedList<>();

        for (List<Field> fields : fieldLists) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Field field : fields) {
                if (!groupCodes.contains(field.getName())) {
                    map.put(field.getName(), field.getValue());
                } else {
                    List<Map<String, String>> groups;
                    if (map.containsKey(mainGroupCode)) {
                        groups = (List<Map<String, String>>) map.get(mainGroupCode);
                    } else {
                        groups = new LinkedList<>();
                        map.put(mainGroupCode, groups);
                    }
                    Integer index = Integer.valueOf(field.getAttributes().get(HtmlTagAttr.TAB_NUMBER));
                    if (groups.size() < index + 1) {
                        for (int i = 0; i < index + 1 - groups.size(); i++) {
                            groups.add(new HashMap<>());
                        }
                    }
                    groups.get(index).put(field.getName(), field.getValue());
                }
            }
            blocks.add(map);
        }

        // deal with constant fields. Decided to take first one and remove the rest.
        for (String constantCode : constantCodes) {
            List<Field> fields = new ArrayList<>(ieDocument.findFields(constantCode));
            if (fields.size() > 0) {
                for (int i = 0; i < fields.size(); i++) {
                    if (i == 0) {
                        for (Map<String, Object> block : blocks) {
                            block.put(fields.get(0).getName(), fields.get(0).getValue());
                        }
                    } else {
                        ieDocument.remove(fields.get(i));
                    }
                }
            }
        }

        Gson gson = GsonUtils.GSON;
        String jsonString = gson.toJson(blocks);
        jsonString = jsonString.replaceAll("\"", "&quot;");
        Field.Descriptor blocksJsonField =
                new Field.Descriptor().setName(blockJsonCode).setScore(new BigDecimal(1d)).setValue(jsonString);

        ieDocument.add(blocksJsonField);
    }

    private boolean isValidTag(Field field) {
        String answerCode = field.getName();
        return parentBlockCode.equals(answerCode) || childBlockCodes.contains(answerCode) || groupCodes.contains(answerCode);
    }

}
