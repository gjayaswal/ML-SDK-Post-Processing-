package com.workfusion.ml.postprocess.blocks;

import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import com.workfusion.vds.api.nlp.model.Field;
import com.workfusion.vds.api.nlp.model.IeDocument;
import com.workfusion.vds.api.nlp.processing.IeProcessor;
import com.workfusion.vds.api.nlp.processing.ProcessingException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlignGroupToBlockProcessor implements IeProcessor {

    private Collection<String> groupCodes;

    public AlignGroupToBlockProcessor(Collection<String> groupCodes) {
        this.groupCodes = groupCodes;
    }

    @Override
    public void process(IeDocument ieDocument) throws ProcessingException {
        Map<String, List<Field>> fieldsByBlock = ieDocument.findFields().stream()
                .filter(f -> groupCodes.contains(f.getName()))
                .collect(Collectors.groupingBy(f -> f.getAttributes().get(HtmlTagAttr.BLOCK_NUMBER)));


        for (List<Field> fieldsInBlock : fieldsByBlock.values()) {
            if (fieldsByBlock.size() == 0) {
                continue;
            }

            fieldsInBlock.sort((f1, f2) -> {
                Integer t1 = Integer.valueOf(f1.getAttributes().get(HtmlTagAttr.TAB_NUMBER));
                Integer t2 = Integer.valueOf(f2.getAttributes().get(HtmlTagAttr.TAB_NUMBER));
                return t1 - t2;
            });
            Integer minTabNumber = Integer.valueOf(fieldsInBlock.get(0).getAttributes().get(HtmlTagAttr.TAB_NUMBER));

            for (Field field : fieldsInBlock) {
                Map<String, String> attributes = field.getAttributes();
                Integer tabNumber = Integer.valueOf(attributes.get(HtmlTagAttr.TAB_NUMBER));
                attributes.put(HtmlTagAttr.TAB_NUMBER, String.valueOf(tabNumber - minTabNumber));
            }
        }
    }
}
