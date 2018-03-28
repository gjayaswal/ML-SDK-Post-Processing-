//package com.workfusion.ml;
//
//import com.workfusion.nlp.uima.annotator.ie.BaseIeXmlOutputAnnotator;
//import com.workfusion.nlp.uima.annotator.ie.IeWhitespaceFilter;
//import com.workfusion.nlp.uima.api.component.PipelineType;
//import com.workfusion.nlp.uima.api.constant.View;
//import com.workfusion.nlp.uima.docparser.DocumentParser;
//import com.workfusion.nlp.uima.docparser.DocumentParserConfiguration;
//import com.workfusion.nlp.uima.types.ExtractionTagAnnotation;
//import com.workfusion.vds.api.nlp.model.Field;
//import com.workfusion.vds.api.nlp.model.IeDocument;
//import com.workfusion.vds.api.nlp.processing.IeProcessor;
//import com.workfusion.vds.api.nlp.processing.Processor;
//import com.workfusion.vds.nlp.uima.model.UimaIeDocument;
//import org.apache.uima.analysis_engine.AnalysisEngine;
//import org.apache.uima.analysis_engine.AnalysisEngineDescription;
//import org.apache.uima.fit.factory.AnalysisEngineFactory;
//import org.apache.uima.fit.factory.JCasFactory;
//import org.apache.uima.fit.util.JCasUtil;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.resource.ResourceInitializationException;
//import org.cleartk.util.ViewUriUtil;
//
//import java.math.BigDecimal;
//import java.nio.file.Paths;
//
//public class MLSDKUtils {
//
//    public static UimaIeDocument createDocument(String content) throws Exception {
//        JCas cas = JCasFactory.createJCas();
//        ViewUriUtil.setURI(cas, Paths.get("test.txt").toUri());
//        JCas sourceView = JCasUtil.getView(cas, View.RAW_SOURCE, true);
//        sourceView.setDocumentText(content);
//        AnalysisEngine engine = createDocumentParser();
//        engine.process(cas);
//        return UimaIeDocument.fromCas(cas);
//    }
//
//    public static AnalysisEngine createDocumentParser() throws ResourceInitializationException {
//        DocumentParser documentParser = new DocumentParserConfiguration()
//                .annotateIeGolds()
//                .resetXmlFilters()
//                .addXmlFilter(IeWhitespaceFilter.class)
//                .build();
//        AnalysisEngineDescription parserDescription = documentParser.getParserDescription(PipelineType.PRODUCTION);
//        return AnalysisEngineFactory.createEngine(parserDescription);
//    }
//
//    public static Field.Descriptor addNewField(IeDocument document, String name, String value, BigDecimal score) {
//        Field.Descriptor fieldDescriptor = new Field.Descriptor();
//        fieldDescriptor.setName(name);
//        fieldDescriptor.setScore(score);
//        fieldDescriptor.setValue(value);
//        document.add(fieldDescriptor);
//        return fieldDescriptor;
//    }
//
//    public static Field.Descriptor addNewFieldToPosition(IeDocument document,
//                                                         String name,
//                                                         String value,
//                                                         BigDecimal score,
//                                                         Integer begin,
//                                                         Integer end) {
//        Field.Descriptor fieldDescriptor = new Field.Descriptor();
//        fieldDescriptor.setBegin(begin);
//        fieldDescriptor.setEnd(end);
//        fieldDescriptor.setName(name);
//        fieldDescriptor.setScore(score);
//        fieldDescriptor.setValue(value);
//        document.add(fieldDescriptor);
//        return fieldDescriptor;
//    }
//
//    public static UimaIeDocument createExtractionTag(UimaIeDocument uimaDocument, String tagName, int begin, int end, double score) throws Exception {
//        JCas jCas = getJCas(uimaDocument);
//        createExtractionTag(jCas, tagName, begin, end, score);
//        return UimaIeDocument.fromCas(jCas);
//    }
//
//    public static void createExtractionTag(JCas cas, String tagName, int begin, int end, double score) {
//        final ExtractionTagAnnotation extractionTagAnnotation = new ExtractionTagAnnotation(cas, begin, end);
//        extractionTagAnnotation.setName(tagName);
//        extractionTagAnnotation.setScore(score);
//        extractionTagAnnotation.addToIndexes();
//
//    }
//
//    public static JCas getJCas(UimaIeDocument uimaDocument) throws Exception {
//        UimaIeDocument.toCas(uimaDocument);
//        return uimaDocument.getCas();
//    }
//
//    public static String getTaggedText(UimaIeDocument document) throws Exception {
//        JCas jCas = getJCas(document);
//        AnalysisEngineDescription description = BaseIeXmlOutputAnnotator.getDescription(View.DEFAULT, View.TAGGED_DOCUMENT);
//        AnalysisEngine engine = AnalysisEngineFactory.createEngine(description);
//        engine.process(jCas);
//        return JCasUtil.getView(jCas, View.TAGGED_DOCUMENT, false).getDocumentText();
//    }
//
//    public static void process(UimaIeDocument document, Processor... processors) throws Exception {
//        for (Processor processor : processors) {
//            processor.process(document);
//        }
//    }
//
//    public static UimaIeDocument createFieldInDocument(UimaIeDocument uimaDocument, int begin, int end, String fieldName) throws Exception {
//        UimaIeDocument.toCas(uimaDocument);
//        JCas cas = uimaDocument.getCas();
//        ExtractionTagAnnotation extractionTagAnnotation = new ExtractionTagAnnotation(cas, begin, end);
//        extractionTagAnnotation.setName(fieldName);
//        extractionTagAnnotation.addToIndexes();
//        return UimaIeDocument.fromCas(cas);
//    }
//}