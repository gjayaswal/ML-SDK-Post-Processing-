package com.workfusion.ml.model;

import com.workfusion.ml.Answer;
import com.workfusion.nlp.uima.api.component.ConfiguredPipelineStep;
import com.workfusion.nlp.uima.pipeline.constants.ConfigurationConstants;
import com.workfusion.nlp.uima.util.TokenizationUtil;
import com.workfusion.nlp.uima.util.answer.AnswerInfo;
import com.workfusion.vds.nlp.hypermodel.ie.invoice.generic.GenericInvoiceIeConfiguration;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Map;

import static com.workfusion.nlp.uima.api.parameter.sweeping.Dimensions.selectOne;



public class AIGLossRunConfiguration extends GenericInvoiceIeConfiguration {

    public AIGLossRunConfiguration() throws Exception {
        super();
        initGlobalParameters();
    }

    @SuppressWarnings("deprecation")
	protected void initTokenizers(@SuppressWarnings("deprecation") AnswerInfo answerInfo, Map<String, Object> params) throws ResourceInitializationException {
        final ConfiguredPipelineStep defaultTokenizer = new ConfiguredPipelineStep("DefaultTokenizer", TokenizationUtil.getSplitTokenizer(
                (String) params.getOrDefault(ConfigurationConstants.TOKENIZER_DEFAULT_REGEX,
                        "(([\\s:#_;'\\ǀ\"\\|\\[\\]\\(\\)\\*•■])|(\\s,)|(\\s-)|(\\.\\s))")));

        final ConfiguredPipelineStep tokenizer = new ConfiguredPipelineStep("Tokenizer10", TokenizationUtil.getSplitTokenizer(
                "(([\\s:#_;'\\ǀ\\\"\\|\\[\\]\\(\\)\\*•■“’$])|(\\s,)|(,\\s)|(\\s-)|(\\.\\s)|( +))"));
    
        
        if (Answer.LOSS_DATE.equals(answerInfo.getAnswerCode()) || Answer.VALUATION_DATE.equals(answerInfo.getAnswerCode())) {
            addAnnotators("Date Tokenizer", selectOne(
                    //getGeneratedAnnotators(params, defaultTokenizer),
                    tokenizer // default for price
                    // put any custom tokenizers, let machine choose or remove previous defaults
            ).byDefault(0));
        } else if (Answer.AMOUNT.equals(answerInfo.getAnswerCode())) {
            addAnnotators("Price Tokenizer", selectOne(
                    //getGeneratedAnnotators(params, defaultTokenizer),
                    tokenizer // default for price
                    // put any custom tokenizers, let machine choose or remove previous defaults
            ).byDefault(0));
        } else {
            super.initTokenizers(answerInfo, params);
        }
    }
}
