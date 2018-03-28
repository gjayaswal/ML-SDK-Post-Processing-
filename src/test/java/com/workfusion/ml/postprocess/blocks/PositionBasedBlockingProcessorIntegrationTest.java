//package com.workfusion.ml.postprocess.blocks;
//
//import com.google.common.collect.Sets;
//import com.google.common.io.Resources;
//import com.workfusion.ml.postprocess.Answer;
//import com.workfusion.vds.nlp.processing.grouping.PositionBasedGroupingProcessor;
//import com.workfusion.vds.nlp.uima.model.UimaIeDocument;
//import org.apache.commons.io.IOUtils;
//import org.assertj.core.api.SoftAssertions;
//import org.junit.Test;
//import org.skyscreamer.jsonassert.JSONAssert;
//
//import java.io.File;
//import java.nio.charset.StandardCharsets;
//import java.util.Set;
//
//import static com.workfusion.ml.MLSDKUtils.*;
//
///**
// * Created by Pavel Platonov on 2/28/18.
// */
//public class PositionBasedBlockingProcessorIntegrationTest {
//
//    @Test
//    public void genericTest() throws Exception {
//
//        String text = IOUtils.toString(Resources.getResource("blocks-processing-test-cases/general-case/sample.html").openStream(), StandardCharsets.UTF_8);
//        UimaIeDocument uimaDocument = createDocument(text);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOSS_DATE, 25, 35, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.COMPANY_NAME, 44, 60, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 64, 68, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 78, 79, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 87, 88, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 96, 97, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 105, 106, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 114, 115, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 123, 124, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 128, 132, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 142, 143, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 151, 152, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 160, 161, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 169, 170, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 178, 179, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 187, 188, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 192, 196, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 206, 207, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 215, 216, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 224, 225, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 233, 234, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 242, 243, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 251, 252, 1d);
//
//        processTest(uimaDocument);
//
//        assertResults(uimaDocument,
//                "blocks-processing-test-cases/general-case/expected.html",
//                "blocks-processing-test-cases/general-case/expectedBlocks.json");
//    }
//
//    /*
//    First parent block tag was not extracted. ER: 3 Blocks are created, 1st block does not have parent tag value
//     */
//    @Test
//    public void firstParentBlockTagNotExtractedTest() throws Exception {
//
//        String text = IOUtils.toString(Resources.getResource("blocks-processing-test-cases/first-parent-block-tag-not-extracted/sample.html").openStream(), StandardCharsets.UTF_8);
//        UimaIeDocument uimaDocument = createDocument(text);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOSS_DATE, 25, 35, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.COMPANY_NAME, 44, 60, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 78, 79, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 87, 88, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 96, 97, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 105, 106, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 114, 115, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 123, 124, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 128, 132, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 142, 143, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 151, 152, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 160, 161, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 169, 170, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 178, 179, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 187, 188, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 192, 196, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 206, 207, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 215, 216, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 224, 225, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 233, 234, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 242, 243, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 251, 252, 1d);
//
//        processTest(uimaDocument);
//
//        assertResults(uimaDocument,
//                "blocks-processing-test-cases/first-parent-block-tag-not-extracted/expected.html",
//                "blocks-processing-test-cases/first-parent-block-tag-not-extracted/expectedBlocks.json");
//    }
//
//    /*
//    Second block does not have groups. ER: 2nd group is populated with only constant answers
//     */
//    @Test
//    public void parentBlockTagDoesNotHaveChildsTest() throws Exception {
//
//        String text = IOUtils.toString(Resources.getResource("blocks-processing-test-cases/parent-block-does-not-have-childs/sample.html").openStream(), StandardCharsets.UTF_8);
//        UimaIeDocument uimaDocument = createDocument(text);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOSS_DATE, 25, 35, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.COMPANY_NAME, 44, 60, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 64, 68, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 78, 79, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 87, 88, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 96, 97, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 105, 106, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 114, 115, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 123, 124, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 128, 132, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 192, 196, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 206, 207, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 215, 216, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 224, 225, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 233, 234, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 242, 243, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 251, 252, 1d);
//
//        processTest(uimaDocument);
//
//        assertResults(uimaDocument,
//                "blocks-processing-test-cases/parent-block-does-not-have-childs/expected.html",
//                "blocks-processing-test-cases/parent-block-does-not-have-childs/expectedBlocks.json");
//    }
//
//    @Test
//    public void constantFieldsAreNotExtractedTest() throws Exception {
//
//        String text = IOUtils.toString(Resources.getResource("blocks-processing-test-cases/constant-fields-are-not-extracted/sample.html").openStream(), StandardCharsets.UTF_8);
//        UimaIeDocument uimaDocument = createDocument(text);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 64, 68, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 78, 79, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 87, 88, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 96, 97, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 105, 106, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 114, 115, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 123, 124, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 128, 132, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 142, 143, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 151, 152, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 160, 161, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 169, 170, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 178, 179, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 187, 188, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 192, 196, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 206, 207, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 215, 216, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 224, 225, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 233, 234, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 242, 243, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 251, 252, 1d);
//
//        processTest(uimaDocument);
//
//        assertResults(uimaDocument,
//                "blocks-processing-test-cases/constant-fields-are-not-extracted/expected.html",
//                "blocks-processing-test-cases/constant-fields-are-not-extracted/expectedBlocks.json");
//    }
//
//    /*
//    ER: 2nd+ extracted tags are cleared
//     */
//    @Test
//    public void twoConstantFieldsAreExtractedTest() throws Exception {
//
//        String text = IOUtils.toString(Resources.getResource("blocks-processing-test-cases/two-constant-fields-are-extracted/sample.html").openStream(), StandardCharsets.UTF_8);
//        UimaIeDocument uimaDocument = createDocument(text);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOSS_DATE, 25, 35, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.COMPANY_NAME, 44, 60, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 64, 68, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 78, 79, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 87, 88, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 96, 97, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 105, 106, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 114, 115, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 123, 124, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 128, 132, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 142, 143, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 151, 152, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 160, 161, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 169, 170, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 178, 179, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 187, 188, 1d);
//
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.LOB, 192, 196, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 206, 207, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 215, 216, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 224, 225, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 233, 234, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.CLAIM_NAME, 242, 243, 1d);
//        uimaDocument = createExtractionTag(uimaDocument, Answer.AMOUNT, 251, 252, 1d);
//
//        uimaDocument = createExtractionTag(uimaDocument, Answer.COMPANY_NAME, 256, 274, 1d);
//
//        processTest(uimaDocument);
//
//        assertResults(uimaDocument,
//                "blocks-processing-test-cases/two-constant-fields-are-extracted/expected.html",
//                "blocks-processing-test-cases/two-constant-fields-are-extracted/expectedBlocks.json");
//    }
//
//    /*
//        expected tagged text in expected.html is produced by Manual Task in WF8.4.
//        Extraction tags are cleared from the following attributes:
//        1. data-toggle="tooltip"
//        2. style=".*?"
//        3. class="extraction-tag"
//        4. appendorder=".*?"
//        5. data-original-title=".*?"
//        6. constant=".*?"
//        7. tagorder=".*?"
//        8. confidence="1/1"
//        9. added score="1.0" to each extraction tag
//        10. added meta-manual-answer for blocks answer
//     */
//    private void assertResults(UimaIeDocument uimaDocument, String expectedHtmlFile, String expectedJsonFile) throws Exception {
//        String actualTaggedText = getTaggedText(uimaDocument);
//        SoftAssertions softly = new SoftAssertions();
//        softly.assertThat(actualTaggedText).isXmlEqualToContentOf(new File(Resources.getResource(expectedHtmlFile).toURI()));
//        String actualBlocksJson = uimaDocument.findField(Answer.BLOCKS).getValue().replaceAll("&quot;", "\"");
//
//        String expectedJson = IOUtils.toString(Resources.getResource(expectedJsonFile), StandardCharsets.UTF_8);
//        JSONAssert.assertEquals(expectedJson, actualBlocksJson, false);
//
//        softly.assertAll();
//    }
//
//    private void processTest(UimaIeDocument uimaDocument) throws Exception {
//        String parentBlockAnswer = Answer.LOB;
//        Set<String> childBlockAnswers = Sets.newHashSet(Answer.CLAIM_NAME, Answer.AMOUNT);
//        Set<String> groupAnswers = Sets.newHashSet(Answer.CLAIM_NAME, Answer.AMOUNT);
//        String mainGroupsCode = Answer.CLAIMS;
//        Set<String> constantAnswers = Sets.newHashSet(Answer.COMPANY_NAME, Answer.LOSS_DATE);
//        String blocksJsonCode = Answer.BLOCKS;
//
//        process(uimaDocument, new PositionBasedBlockingProcessor(parentBlockAnswer, childBlockAnswers),
//                new PositionBasedGroupingProcessor(groupAnswers),
//                new AlignGroupToBlockProcessor(groupAnswers),
//                new ConstantProcessor(Answer.COMPANY_NAME),
//                new ConstantProcessor(Answer.LOSS_DATE),
//                new CombineBlocksAnswerProcessor(parentBlockAnswer, childBlockAnswers, groupAnswers, mainGroupsCode,
//                        constantAnswers, blocksJsonCode));
//    }
//}