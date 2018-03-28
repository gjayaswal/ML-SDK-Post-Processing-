package com.workfusion.ml.run;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Sets;
import com.workfusion.ml.Answer;
import com.workfusion.ml.run.stats.BlocksStatisticsCalculator;
import com.workfusion.ml.run.stats.BlocksStatisticsPrinter;
import com.workfusion.ml.util.CsvUtil;
import com.workfusion.ml.util.S3Manager;
import com.workfusion.ml.util.datautils.InjectColorIntoHtmlUtil;
import com.workfusion.nlp.uima.api.constant.View;
import com.workfusion.nlp.uima.workflow.model.Hypermodel;
import com.workfusion.nlp.uima.workflow.task.extract.IeCompositeAnnotatorResult;
import com.workfusion.vds.nlp.processing.normalization.OcrAmountNormalizer;
import com.workfusion.vds.nlp.uima.processing.run.IeProcessingRunner;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.FileUtils;
import org.cleartk.util.ViewUriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ModelRunner extends IeProcessingRunner {

    private static final Logger logger = LoggerFactory.getLogger(ModelRunner.class);
    private static final String JSON = ".json";

    private static final String GOLD_FILE = "gold-file";

    private static final String COLUMN_FILENAME = "system_id";
    private static final String COLUMN_GOLD_HTML = "tagged_text";
    private static final String COLUMN_GOLD_BLOCKS = "ocr_result_html_blocks_gold";
    private static final String COLUMN_MODEL_BLOCKS = "ocr_result_html_blocks";
    private static final String COLUMN_GOLD_TAGGED_DOC = "ocr_result_html_tagged_gold";
    private static final String COLUMN_MODEL_TAGGED_DOC = "ocr_result_html_tagged";
    private static final String COLUMN_DOCUMENT_LINK = "document_link";
    private static final String COLUMN_OCRED_DOC = "ocr_result_html";

    private BlocksStatisticsCalculator calculator;
    private boolean uploadToS3;

    public ModelRunner(Set<String> blockFields, Set<String> constantFields,
                       String mainGroupField,
                       Set<String> groupFields,
                       Map<String, BiFunction<String, String, Boolean>> fieldToComparison,
                       boolean uploadToS3) {
        this.calculator = new BlocksStatisticsCalculator(blockFields, constantFields, mainGroupField, groupFields, fieldToComparison);
        this.uploadToS3 = uploadToS3;
    }

    public static void main(String[] args) throws Exception {
        Set<String> blockFields = Sets.newHashSet(Answer.LOB, Answer.NO_LOSS_REPORTED);
        Set<String> constantFields = Sets.newHashSet(Answer.COMPANY_NAME, Answer.VALUATION_DATE);
        String mainGroupField = Answer.CLAIMS;
        Set<String> groupFields = Sets.newHashSet(Answer.LOSS_DATE, Answer.AMOUNT);

        Map<String, BiFunction<String, String, Boolean>> fieldToComparison = new HashMap<>();
        fieldToComparison.put(Answer.VALUATION_DATE, getDateComparison());
        fieldToComparison.put(Answer.LOSS_DATE, getDateComparison());
        fieldToComparison.put(Answer.AMOUNT, getNumericComparison());
        fieldToComparison.put(Answer.COMPANY_NAME, getTextNumericComparison());
        fieldToComparison.put(Answer.LOB, getLobComparison());
        fieldToComparison.put(Answer.NO_LOSS_REPORTED, getNoLossReportedComparison());

        ModelRunner runner = new ModelRunner(blockFields, constantFields, mainGroupField,
                groupFields, fieldToComparison, false);

        Path trainedModelPath = Paths.get("C:\\LossRun\\AutomationTranningDataSet\\TrainedModel\\ocr_result_html_9108cccf-a3a7-4940-b5c6-894ec3e5aabe_HPO_10Hrs_9-Mar_2018\\output\\model");
        //Path inputFolderPath = Paths.get("/Users/pavel/Downloads/ml_sdk_assignment_2_1doc");
        Path inputCsvFilePath = Paths.get("C:\\Users\\Gjayaswal\\Downloads\\Loss_Run_-_Extraction_161Docs_with_modelTrainedon_299_5c8ad_Mar_28,_2018.CSV");
        Path outputFolderPath = Paths.get("C:\\LossRun\\metricsCalculation\\Results");

        //input csv 2 columns are required: id (file_name), gold html document content, gold blocks json
        runner.calculateStatistics(inputCsvFilePath, outputFolderPath);
    }

    @Override
    protected void extractAndPostProcess(Hypermodel model, Path input, Path output, AnalysisEngine pipeline) throws Exception {
        CsvUtil.getRecords(input.toString())
                .parallelStream()
                .forEach(record -> {
                            try {
                                Path rawExtractionResult = Paths.get(output.toString(), MODEL_RESULT, record.get(COLUMN_FILENAME) + JSON);
                                if (Files.exists(rawExtractionResult)) {
                                    processRawResults(rawExtractionResult, model, output);
                                } else {
                                    Path rawFile = Paths.get(output.toString(), GOLD_FILE, record.get(COLUMN_FILENAME) + JSON);
                                    Files.createDirectories(rawFile.getParent());
                                    Files.createFile(rawFile);
                                    FileUtils.saveString2File(record.get(COLUMN_GOLD_HTML), rawFile.toFile());
                                    JCas jCas = extract(pipeline, rawFile);
                                    JCas processingCas = postProcess(jCas, model);
                                    processResults(processingCas, output);
                                }
                            } catch (Exception e) {
                                logger.error("Error processing document " + record.get(COLUMN_FILENAME), e);
                            }
                        }
                );
    }

    private void processRawResults(Path rawExtractionResult, Hypermodel model, Path output) throws Exception {
        String extractionResults = new String(Files.readAllBytes(rawExtractionResult), StandardCharsets.UTF_8);
        IeCompositeAnnotatorResult result = IeCompositeAnnotatorResult.fromJson(extractionResults);

        AnalysisEngine pipeline = getPostProcessingPipeline(model);
        JCas jCas = pipeline.newJCas();
        JCasUtil.getView(jCas, View.RAW_SOURCE, true).setDocumentText(result.getTaggedText());
        JCasUtil.getView(jCas, View.RAW_RESULTS, true).setDocumentText(extractionResults);
        ViewUriUtil.setURI(jCas, rawExtractionResult.toUri());
        pipeline.process(jCas);

        processResults(jCas, output);
    }

    private void processResults(JCas jCas, Path output) throws Exception {
        String rawExtraction = JCasUtil.getView(jCas, View.RAW_RESULTS, true).getDocumentText();

        String finalResult = jCas.getView(View.FINAL_RESULTS).getDocumentText();

        Path rawExtractFolder = Paths.get(output.toString(), MODEL_RESULT);
        Path finalResultFolder = Paths.get(output.toString(), PROCESSING_RESULT);


        String fileName = getFileName(jCas);
        String originalFileName = fileName;
        if (!fileName.endsWith(JSON)) {
            fileName = fileName + JSON;
        } else {
            originalFileName = fileName.replace(JSON, "");
        }
        if (!StringUtils.isBlank(rawExtraction)) {
            rawExtractFolder.toFile().mkdirs();
            Files.write(Paths.get(rawExtractFolder.toString(), fileName), rawExtraction.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            IeCompositeAnnotatorResult modelResult = IeCompositeAnnotatorResult.fromJson(rawExtraction);
            Files.write(Paths.get(rawExtractFolder.toString(), originalFileName), modelResult.getTaggedText().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }

        finalResultFolder.toFile().mkdirs();
        Path finalResultFile = Paths.get(finalResultFolder.toString(), fileName);
        Files.deleteIfExists(finalResultFile);
        Files.write(finalResultFile, finalResult.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        Path finalHtmlResultFile = Paths.get(finalResultFolder.toString(), originalFileName);
        Files.deleteIfExists(finalHtmlResultFile);
        IeCompositeAnnotatorResult processingResult = IeCompositeAnnotatorResult.fromJson(finalResult);
        Files.write(finalHtmlResultFile, processingResult.getTaggedText().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }

    private JCas postProcess(JCas rawCas, Hypermodel model) throws Exception {
        AnalysisEngine pipeline = getPostProcessingPipeline(model);
        JCas processingCas = pipeline.newJCas();
        ViewUriUtil.setURI(processingCas, ViewUriUtil.getURI(rawCas));
        JCasUtil.getView(processingCas, View.RAW_SOURCE, true).setDocumentText(JCasUtil.getView(rawCas, View.RAW_SOURCE, false).getDocumentText());
        JCasUtil.getView(processingCas, View.RAW_RESULTS, true).setDocumentText(JCasUtil.getView(rawCas, View.FINAL_RESULTS, false).getDocumentText());
        pipeline.process(processingCas);
        return processingCas;
    }

    protected void calculateStatistics(Path input, Path output) throws Exception {
        BlocksStatisticsPrinter blocksStatisticsPrinter = new BlocksStatisticsPrinter(output);
        S3Manager s3Manager = new S3Manager();

        List<Map<String, String>> records = CsvUtil.getRecords(input.toString());

        for (Map<String, String> stringStringMap : records) {
            try {
                if (uploadToS3) {
                    InjectColorIntoHtmlUtil injectColorIntoHtmlUtil = new InjectColorIntoHtmlUtil();
                    String key = stringStringMap.get(COLUMN_FILENAME) + ".html";
                    String goldContent = stringStringMap.get(COLUMN_GOLD_TAGGED_DOC);
                    stringStringMap.put(COLUMN_GOLD_TAGGED_DOC, s3Manager.put("aig-data", "TaggedData/gold-tagged/" + key, new StringInputStream(goldContent), "text/html"));
                    String extractedContent = stringStringMap.get(COLUMN_MODEL_TAGGED_DOC);
                    extractedContent = injectColorIntoHtmlUtil.injectColorForAllTags(extractedContent);
                    stringStringMap.put(COLUMN_MODEL_TAGGED_DOC, s3Manager.put("aig-data", "TaggedData/model-tagged/" + key, new StringInputStream(extractedContent), "text/html"));
                }
            } catch (Exception e) {
                logger.error("Error calculating stats for file " + stringStringMap.get(COLUMN_FILENAME), e);
            }
        }

        calculateMetrics(records, calculator.fieldLevelStats, blocksStatisticsPrinter,"field");
        calculateMetrics(records, calculator.balancedFieldLevelStats, blocksStatisticsPrinter,"balanced_field");
        calculateMetrics(records, calculator.balancedFieldLevelStatsWithBlockSearch, blocksStatisticsPrinter,"balanced_field_with_block_search");
        calculateMetrics(records, calculator.groupLevelStats, blocksStatisticsPrinter,"group");
        calculateMetrics(records, calculator.blockLevelStats, blocksStatisticsPrinter,"block");
        calculateMetrics(records, calculator.blockLevelStatsIgnoreGroups, blocksStatisticsPrinter,"block_ignore_groups");
        calculateMetrics(records, calculator.documentLevelStats, blocksStatisticsPrinter,"document");
        calculateMetrics(records, calculator.documentLevelStatsIgnoreBlocking, blocksStatisticsPrinter,"document_ignore_blocks");
    }

    private void calculateMetrics(List<Map<String, String>> records, BiFunction<String, String, List<Map<String, String>>> calc,
                                  BlocksStatisticsPrinter blocksStatisticsPrinter,
                                  String prefix) throws IOException {
        List<Map<String, String>> allStats = new LinkedList<>();
        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);
            try {
//                        String goldContent = record.get(COLUMN_GOLD_HTML);
//                        String goldFileName = record.get(COLUMN_FILENAME);

                String goldBlocks = record.get(COLUMN_GOLD_BLOCKS);

                //Path extractedResultPath = Paths.get(extract.toString(), goldFileName + JSON);
                //String extractedResult = new String(Files.readAllBytes(extractedResultPath), StandardCharsets.UTF_8);
                //String extractedDocument = IeCompositeAnnotatorResult.fromJson(extractedResult).getTaggedText();

//                        Path processedResultPath = Paths.get(processed.toString(), goldFileName + JSON);
//                        String processedResult = new String(Files.readAllBytes(processedResultPath), StandardCharsets.UTF_8);
//                        System.out.println("json: " + processedResult);
                String processedBlocks = record.get(COLUMN_MODEL_BLOCKS);

                List<Map<String, String>> stats = calc.apply(goldBlocks, processedBlocks);
                enrichStats(record, stats);
                allStats.addAll(stats);

                System.out.println("processed documents: " + (i + 1));

            } catch (Exception e) {
                logger.error("Error calculating stats for file " + record.get(COLUMN_FILENAME), e);
            }
        }

        blocksStatisticsPrinter.print(allStats, prefix);
        blocksStatisticsPrinter.printMetrics(allStats, prefix);

    }

    private List<Map<String, String>> enrichStats(Map<String, String> record, List<Map<String, String>> stats) {
        for (Map<String, String> stat : stats) {
            stat.put(COLUMN_GOLD_BLOCKS, record.get(COLUMN_GOLD_BLOCKS));
            stat.put(COLUMN_MODEL_BLOCKS, record.get(COLUMN_MODEL_BLOCKS));
            stat.put(COLUMN_FILENAME, record.get(COLUMN_FILENAME));
            stat.put(COLUMN_DOCUMENT_LINK, record.get(COLUMN_DOCUMENT_LINK));
            stat.put(COLUMN_OCRED_DOC, record.get(COLUMN_OCRED_DOC));
           // stat.put(COLUMN_GOLD_TAGGED_DOC, record.get(COLUMN_GOLD_TAGGED_DOC));
           // stat.put(COLUMN_MODEL_TAGGED_DOC, record.get(COLUMN_MODEL_TAGGED_DOC));
        }
        return stats;
    }

    private static BiFunction<String, String, Boolean> getNumericComparison() {
        return (s1, s2) -> {
            s1 = naToEmpty(s1);
            s2 = naToEmpty(s2);
            s1 = dummyToEmpty(s1);
            s2 = dummyToEmpty(s2);
            if (s1.isEmpty() && s2.isEmpty()) {
                return true;
            }
            OcrAmountNormalizer n = new OcrAmountNormalizer();
            BigDecimal bd1 = n.parseNumber(s1);
            BigDecimal bd2 = n.parseNumber(s2);
            if (bd1 != null && bd2 != null && bd1.compareTo(bd2) == 0) {
                return true;
            }
            return false;

        };
    }

    private static BiFunction<String, String, Boolean> getDateComparison() {
        return (s1, s2) -> {
            s1 = naToEmpty(s1);
            s2 = naToEmpty(s2);
            s1 = dummyToEmpty(s1);
            s2 = dummyToEmpty(s2);
            if (s1.isEmpty() && s2.isEmpty()) {
                return true;
            }
            if (s1.matches("(\\d{1,2}/)?\\d{1,2}/\\d{4}") && s2.matches("(\\d{1,2}/)?\\d{1,2}/\\d{4}")) {
                LocalDate ld1 = LocalDate.parse(s1, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                LocalDate ld2 = LocalDate.parse(s2, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                if (ld1.compareTo(ld2) == 0) {
                    return true;
                }
            }
            return false;
        };
    }

    private static BiFunction<String, String, Boolean> getTextNumericComparison() {
        return (s1, s2) -> {
            s1 = naToEmpty(s1);
            s2 = naToEmpty(s2);
            s1 = dummyToEmpty(s1);
            s2 = dummyToEmpty(s2);
            s1 = s1.replaceAll("&#039;", "");
            s2 = s2.replaceAll("&#039;", "");
            s1 = s1.replaceAll("[^0-9A-Za-z]", "");
            s2 = s2.replaceAll("[^0-9A-Za-z]", "");
            return s1.equalsIgnoreCase(s2);

        };
    }

    private static BiFunction<String, String, Boolean> getLobComparison() {
        return (s1, s2) -> {
            s1 = naToEmpty(s1);
            s2 = naToEmpty(s2);
            s1 = dummyToEmpty(s1);
            s2 = dummyToEmpty(s2);
            s1 = s1.replaceAll("[^0-9A-Za-z]", "");
            s2 = s2.replaceAll("[^0-9A-Za-z]", "");
            return s1.equalsIgnoreCase(s2);

        };
    }

    private static String naToEmpty(String s) {
        if ("n/a".equalsIgnoreCase(s)) {
            s = "";
        }
        return s;
    }

    private static String dummyToEmpty(String s) {
        if (BlocksStatisticsCalculator.DUMMY.equalsIgnoreCase(s)) {
            s = "";
        }
        return s;
    }

    public static BiFunction<String, String, Boolean> getNoLossReportedComparison() {
        return (s1, s2) -> {
            s1 = naToEmpty(s1);
            s2 = naToEmpty(s2);
            s1 = dummyToEmpty(s1);
            s2 = dummyToEmpty(s2);

            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();

            s1 = s1.replaceAll("&#039;", "");
            s2 = s2.replaceAll("&#039;", "");
            s1 = s1.replaceAll("[^0-9A-Za-z]", "");
            s2 = s2.replaceAll("[^0-9A-Za-z]", "");
            return s1.equalsIgnoreCase(s2) ||
                    (s1.contains("noloss") && s2.contains("noloss") ||
                            (s1.contains("noclaim") && s2.contains("noclaim")));
        };
    }
}
