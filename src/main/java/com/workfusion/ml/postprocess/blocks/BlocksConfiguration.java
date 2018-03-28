package com.workfusion.ml.postprocess.blocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.workfusion.ml.Answer;
import com.workfusion.vds.api.nlp.processing.Processor;
import com.workfusion.vds.nlp.processing.grouping.PositionBasedGroupingProcessor;

import java.util.List;
import java.util.Set;

public class BlocksConfiguration {

    public static List<Processor> getBlocksProcessors() {
        String parentBlockAnswer = Answer.LOB;
        Set<String> childBlockAnswers = Sets.newHashSet(Answer.NO_LOSS_REPORTED, Answer.LOSS_DATE, Answer.AMOUNT);
        Set<String> groupAnswers = Sets.newHashSet(Answer.LOSS_DATE, Answer.AMOUNT);
        String mainGroupsCode = Answer.CLAIMS;
        Set<String> constantAnswers = Sets.newHashSet(Answer.COMPANY_NAME, Answer.VALUATION_DATE);
        String blocksJsonCode = Answer.BLOCKS;

        return Lists.newArrayList(
                new PositionBasedBlockingProcessor(parentBlockAnswer, childBlockAnswers),
                new PositionBasedGroupingProcessor(groupAnswers),
                new AlignGroupToBlockProcessor(groupAnswers),
                new ConstantProcessor(Answer.COMPANY_NAME),
                new ConstantProcessor(Answer.VALUATION_DATE),
                new CombineBlocksAnswerProcessor(parentBlockAnswer, childBlockAnswers, groupAnswers, mainGroupsCode,
                        constantAnswers, blocksJsonCode)
        );
    }

}
