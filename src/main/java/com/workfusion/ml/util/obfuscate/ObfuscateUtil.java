package com.workfusion.ml.util.obfuscate;

import com.workfusion.nlp.uima.api.constant.HtmlTagAttr;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ObfuscateUtil {

    private Map<String, String> regexToDefaultValue;
    private Map<String, String> tagToDefaultValue;

    public ObfuscateUtil(Map<String, String> regexToDefaultValue, Map<String, String> tagToDefaultValue) {
        this.regexToDefaultValue = regexToDefaultValue;
        this.tagToDefaultValue = tagToDefaultValue;
    }

    public static void main(String[] args) throws IOException {
        String inputPath = "C:\\LossRun\\ForPavelWF\\Batch1_104Docs.CSV";
        String outputPath = "C:\\LossRun\\ForPavelWF\\Output";

        Map<String, String> regexMap = new HashMap<>();
        regexMap.put("\\d", "Ⅰ");
        regexMap.put("\\w", "X");
//        regexMap.put("\\d\\d\\d\\.\\d\\d", "{{amount}}");
//        regexMap.put("\\d{3},\\d{2,3}", "{{amount}}");
//        regexMap.put("\\$\\d\\d,\\d\\d", "${{amount}}");
//        regexMap.put("\\$\\d\\d\\d", "${{amount}}");
//        regexMap.put("\\d,\\d\\d\\d", "{{amount}}");
//        regexMap.put("\\d{1,3},\\d{3}\\.\\d{2}", "{{amount}}");
//        regexMap.put("\\d{1,3}\\.\\d{2}", "{{amount}}");
//
//        regexMap.put("\\d{1,2}/\\d{1,2}/\\d{2,4}", "{{date}}");
//        regexMap.put("\\d{1,2}-\\d{1,2}-\\d{2,4}", "{{date}}");
//        regexMap.put("\\d{1,2}/\\d{1,2}", "{{date}}");
//        regexMap.put("201\\d", "{{year}}");
//        regexMap.put("(?i)\\d{1,2}-(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)-\\d{2,4}", "{{date}}");
//        regexMap.put("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s?\\d{1,2}(,|\\.)?\\s{2,4}", "{{date}}");
//
//        regexMap.put("(?i)[0-9a-z]{3}\\d{7}", "{{policy number}}"); //policy number & account number
//        regexMap.put("(?i)\\d{5}[0-9a-z]{3}", "{{policy number}}"); //policy number
//        regexMap.put("(?i)[a-z]\\s\\d{7}", "{{policy number}}"); //policy number
//        regexMap.put("\\d{8}", "{{policy number}}"); //loss file

        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("lob", "Fake LOB");
        tagMap.put("insured_name", "FAKE Insured Name");
        tagMap.put("loss_date", "01/01/2019");
        tagMap.put("total_incured", "$99.99");
        tagMap.put("valuation_date", "01/01/2020");
        tagMap.put("no_loss_reported", "FAKE no loss reported");

        CSVParser parser = new CSVParser(
                Files.newBufferedReader(Paths.get(inputPath), StandardCharsets.UTF_8),
                CSVFormat.RFC4180.withHeader().withSkipHeaderRecord()
        );
        List<Map<String, String>> records = parser.getRecords().stream().map(CSVRecord::toMap).collect(Collectors.toList());

//        if (args.length == 1 && StringUtils.isNotEmpty(args[0])) {
//            String jsonContent = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
//            Map<String, Object> config = GsonUtils.GSON.fromJson(jsonContent, GsonUtils.TYPE_MAP_STRING_OBJECT);
//            inputPath = config.get("input_path").toString();
//            outputPath = config.get("output_path").toString();
//            tagMap = (Map<String, String>) config.get("tag_replacement");
//            regexMap = (Map<String, String>) config.get("regex_replacement");
//        }

        int counter = 0;
        ObfuscateUtil obfuscateUtil = new ObfuscateUtil(regexMap, tagMap);
//        for (Map<String, String> record : records) {
//            String modelContent = record.get("ocr_result_html_tagged");
//            String content = obfuscateUtil.obfuscateContent(modelContent);
//            String outputFilepath = outputPath.endsWith("/") ?
//                    outputPath + counter++ + ".html" :
//                    outputPath + "/" + counter++ + ".html";
//
//            Files.write(Paths.get(outputFilepath), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
//        }
////        obfuscateUtil.obfuscateFolder(inputPath, outputPath);
        obfuscateUtil.obfuscateFile(Paths.get("C:\\LossRun\\new 6.html"), Paths.get("C:\\LossRun"));
    }

    public void obfuscateFolder(String folderPath, String outputFolderPath) throws IOException {
        Path output = Paths.get(outputFolderPath);
        Files.list(Paths.get(folderPath))
                .forEach(f -> {
                    try {
                        obfuscateFile(f, output);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public String obfuscateFile(Path file, Path outputFolder) throws IOException {
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

        content = obfuscateContent(content);

        String outputFilepath = outputFolder.toString().endsWith("/") ?
                outputFolder + file.getFileName().toString() :
                outputFolder.toString() + "/" + file.getFileName() + ".obfuscated";

        Files.write(Paths.get(outputFilepath), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        return outputFilepath;
    }

    protected String obfuscateContent(String content) {
        final Document document = Jsoup.parse(content);

        //obfuscate by tag
        for (Map.Entry<String, String> tagToDefaultValue : tagToDefaultValue.entrySet()) {
            document.getElementsByTag(tagToDefaultValue.getKey())
                    .forEach(t -> {
                        t.text(tagToDefaultValue.getValue());
                        t.attr(HtmlTagAttr.DATA_VALUE, tagToDefaultValue.getValue());
                    });
        }

        Set<String> answers = tagToDefaultValue.keySet();
        List<Element> els = document.body().getAllElements().stream()
                .filter(it -> !answers.contains(it.tagName()))
                .collect(Collectors.toList());
//        Elements els = document.body().getAllElements();
        for (Map.Entry<String, String> regexToDefaultValue : regexToDefaultValue.entrySet()) {
            final String regex = regexToDefaultValue.getKey();
            final String value = regexToDefaultValue.getValue();
            for (Element e : els) {
                for (Node child : e.childNodes()) {
                    if (child instanceof TextNode && !((TextNode) child).isBlank()) {
                        ((TextNode) child).text(((TextNode) child).text().replaceAll(regex, value));
                    }
                }
            }
        }

        List<Element> collected = document.getElementsByAttribute(HtmlTagAttr.DATA_VALUE).stream()
                .filter(e -> !tagToDefaultValue.keySet().contains(e.tagName()))
                .collect(Collectors.toList());
        for (Map.Entry<String, String> regexToDefaultValue : regexToDefaultValue.entrySet()) {
            final String regex = regexToDefaultValue.getKey();
            final String value = regexToDefaultValue.getValue();
            for (Element element : collected) {
                element.attr(HtmlTagAttr.DATA_VALUE, element.attr(HtmlTagAttr.DATA_VALUE).replaceAll(regex, value));
            }
        }

//        document.outputSettings().prettyPrint(true);
//        document.outputSettings().escapeMode(Entities.EscapeMode.base);

        return document.toString();
    }
}
