package com.workfusion.ml.run.stats;

import com.workfusion.ml.util.CsvUtil;
import com.workfusion.vds.nlp.uima.statistics.FieldStatistics;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlocksStatisticsPrinter {

    Path outputFolder;
    String dateTime;

    public BlocksStatisticsPrinter(Path output) throws IOException {
        dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy_HH_mm_ss"));
        outputFolder = output.resolve(dateTime);
        Files.createDirectories(outputFolder);
    }

    public String print(List<Map<String, String>> records, String postfix) throws IOException {
        Path statisticsFile = outputFolder.resolve("statistics_" + postfix + "_" + dateTime + ".csv");
        Files.deleteIfExists(statisticsFile);
        return CsvUtil.createCsv(records, statisticsFile.toString());
    }

    public String printMetrics(List<Map<String, String>> records, String postfix) throws IOException {
        Path statisticsFile = outputFolder.resolve("metrics_" + postfix + "_" + dateTime + ".csv");
//        Path statisticsFile = Paths.get(output.toString(), "metrics_" + postfix + "_" + dateTime + ".csv");
        Files.deleteIfExists(statisticsFile);

        if (records.size() == 0) {
            System.out.println("No records to calculate metrics");
            return statisticsFile.toString();
        }

        List<Map<String, String>> recordsToPrint = new LinkedList<>();

        Map<String, List<Map<String, String>>> collectedByField = records.stream().collect(Collectors.groupingBy(r -> r.get("field")));
        for (String field : collectedByField.keySet()) {
            List<Map<String, String>> recordsForField = collectedByField.get(field);
            Map<String, String> metricsRecord = new LinkedHashMap<>();
            metricsRecord.put("field", field);
            long tp = recordsForField.stream().filter(r -> "1".equals(r.get("tp"))).count();
            metricsRecord.put("tp", String.valueOf(tp));
            long fp = recordsForField.stream().filter(r -> "1".equals(r.get("fp"))).count();
            metricsRecord.put("fp", String.valueOf(fp));
            long fn = recordsForField.stream().filter(r -> "1".equals(r.get("fn"))).count();
            metricsRecord.put("fn", String.valueOf(fn));
            long tn = recordsForField.stream().filter(r -> "1".equals(r.get("tn"))).count();
            metricsRecord.put("tn", String.valueOf(tn));

            metricsRecord.put("p", String.valueOf((double)tp/(tp + fp)));
            metricsRecord.put("r", String.valueOf((double)tp/(tp + fn)));
            metricsRecord.put("a", String.valueOf((double)(tp + tn)/(tp + fp + fn + tn)));
            metricsRecord.put("f1", String.valueOf(2.0 * tp / (2.0 * tp + fp + fn )));
            recordsToPrint.add(metricsRecord);
        }
        CsvUtil.createCsv(recordsToPrint, statisticsFile.toString());

        //System.out.println();
        //System.out.println(postfix);
        if (recordsToPrint.size() > 0) {
            for (String key : recordsToPrint.get(0).keySet()) {
                //System.out.print(key + "\t");
            }
            //System.out.println();
            for (Map<String, String> record : recordsToPrint) {
                for (String key : record.keySet()) {
                    //System.out.print(record.get(key) + "\t");
                }
                //System.out.println();
            }
        }
        return statisticsFile.toString();
    }
}
