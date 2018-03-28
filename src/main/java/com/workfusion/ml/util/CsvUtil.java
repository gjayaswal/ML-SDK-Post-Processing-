package com.workfusion.ml.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CsvUtil {

    public static void main(String[] args) throws IOException {
//        createSnapshot("/Users/pavel/Downloads/Output/");
//        createSnapshot("/Users/pavel/Downloads/output-test/");
        List<Map<String, String>> records = getRecords("C:\\Users\\Gjayaswal\\Downloads\\regexTest.CSV")
                .stream()
                .map(r -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    Document doc = Jsoup.parse(r.get("ocr_result_html_tagged"));
                    map.put("amount", doc.getElementsByTag("total_incurred").text());
                    map.put("plain_text", doc.text());
                    return map;
                })
                .collect(Collectors.toList());
        createCsv(records, "C:\\Users\\Gjayaswal\\Downloads\\regexTestOutput.csv");
    }

    public static List<Map<String, String>> getRecords(String path) throws IOException {
        CSVParser parser = new CSVParser(
                Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8),
                CSVFormat.RFC4180.withHeader().withSkipHeaderRecord()
        );
        return parser.getRecords().stream().map(CSVRecord::toMap).collect(Collectors.toList());
    }

    public static String createCsv(List<Map<String, String>> records, String outputPath) throws IOException {
    	if(records.size() == 0) {
    		System.out.println("no records to print");
    		return null;
    	}
        Set<String> headers = new LinkedHashSet<>(records.get(0).keySet());//.stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
        for(int i = 1; i < records.size(); i++) {
        	headers.addAll(records.get(i).keySet());
;        }
        String[] headersArr = new String[headers.size()];
        headers.toArray(headersArr);
        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.UTF_8),
                CSVFormat.RFC4180.withHeader(headersArr))) {
            for (Map<String, String> record : records) {
                List<String> r = new LinkedList<>();
                for (String key : headersArr) {
                    r.add(record.get(key));
                }
                printer.printRecord(r);
            }
        }
        return outputPath;
    }

//    public static String createSnapshot(String folderPath) throws IOException {
//        List<String> answers = Lists.newArrayList("lob", "claims", "total_incurred", "claim_name",
//                "insured_name", "loss_date", "no_loss_reported", "valuation_date");
//
//        List<Map<String, String>> records = Files.list(Paths.get(folderPath))
//                .filter(f -> !f.getFileName().toString().endsWith(".csv"))
//                .map(f -> {
//                    Map<String, String> map = new LinkedHashMap<>();
//                    try {
//                        String content = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
//                        map.put("filename", f.getFileName().toString());
//                        Document doc = Jsoup.parse(content);
//                        for (String answer : answers) {
//                            doc.getElementsByTag(answer).unwrap();
//                        }
//                        map.put("ocr_result_html", doc.toString());
//
//                        Document doc1 = Jsoup.parse(content);
//                        doc1.getElementsByAttribute(HtmlTagAttr.BLOCK_NUMBER).removeAttr(HtmlTagAttr.BLOCK_NUMBER);
//                        doc1.getElementsByAttribute(HtmlTagAttr.TAB_NUMBER).removeAttr(HtmlTagAttr.TAB_NUMBER);
//                        UimaIeDocument uimaDocument = createDocument(doc1.toString());
//                        List<Processor> blocksProcessors = BlocksConfiguration.getBlocksProcessors();
//                        Processor[] processors = new Processor[blocksProcessors.size()];
//                        blocksProcessors.toArray(processors);
//                        process(uimaDocument, processors);
//                        map.put("ocr_result_html_tagged", getTaggedText(uimaDocument));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    return map;
//                })
//                .collect(Collectors.toList());
//        return createCsv(records, folderPath + "snapshot3.csv");
//    }
}
