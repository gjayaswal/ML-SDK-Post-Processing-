package com.workfusion.ml.postprocess.blocks;

import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import com.workfusion.vds.api.nlp.model.Field;
import com.workfusion.vds.api.nlp.model.IeDocument;
import com.workfusion.vds.api.nlp.processing.IeProcessor;
import com.workfusion.vds.api.nlp.processing.ProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConstantProcessor implements IeProcessor {

    public static String HTML_TAG_ATTR_CONSTANT = "constant";//not such attribute in HtmlTagAttr class

    private String code;

    public ConstantProcessor(String code) {
        this.code = code;
    }


    @Override
    public void process(IeDocument ieDocument) throws ProcessingException {
        List<Field> allFields = new ArrayList<>(ieDocument.findFields(code));

        for (int i = 0; i < allFields.size(); i++) {
            if (i == 0) {
                Map<String, String> attributes = allFields.get(i).getAttributes();
                attributes.put(HTML_TAG_ATTR_CONSTANT, "true");
                attributes.put(HtmlTagAttr.BLOCK_NUMBER, "0");
            } else {
                ieDocument.remove(allFields.get(i));
            }
        }
    }
}
