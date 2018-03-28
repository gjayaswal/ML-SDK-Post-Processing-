package com.workfusion.ml.util.datautils;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.workfusion.ml.Answer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Created by nelsonc on 3/19/18.
 */
public class InjectColorIntoHtmlUtil {

    private Map<String, String> tagToColor;

    public InjectColorIntoHtmlUtil() {
        this.tagToColor = ImmutableMap.<String, String>builder()
                .put(Answer.VALUATION_DATE, "164, 232, 4")
                .put(Answer.COMPANY_NAME, "255, 145, 63")
                .put(Answer.LOB, "28, 230, 255")
                .put(Answer.NO_LOSS_REPORTED, "255, 52, 255")
                .put(Answer.LOSS_DATE, "255, 74, 70")
                .put(Answer.AMOUNT, "99, 255, 172")
                .build();
    }

    public InjectColorIntoHtmlUtil(Map<String, String> tagToColor) {
        this.tagToColor = tagToColor;
    }

    public static void main(String[] args) throws Exception {
        String input = "/Users/pavel/IdeaProjects/ml-use-cases/aig-loss-run/src/test/resources/blocks-processing-test-cases/general-case";
        String output = "/Users/pavel/Downloads/inject-clor-test";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i")) {
                input = args[i + 1];
            } else if (args[i].equals("-o")) {
                output = args[i + 1];
            }
        }

        new InjectColorIntoHtmlUtil().run(input, output);
    }

    public void run(String input, String output) throws Exception {
        Collection<File> files = FileUtils.listFiles(new File(input), new SuffixFileFilter(".html"), FalseFileFilter.INSTANCE);

        for (File file : files) {
            System.out.println(file.getName());

            String xml = FileUtils.readFileToString(file);
            xml = injectColor(xml, "valuation_date", "164, 232, 4");
            xml = injectColor(xml, "insured_name", "255, 145, 63");
            xml = injectColor(xml, "no_loss_reported", "255, 52, 255");
            xml = injectColor(xml, "lob", "28, 230, 255");
            xml = injectColor(xml, "loss_date", "255, 74, 70");
            xml = injectColor(xml, "total_incurred", "99, 255, 172");

            FileUtils.writeStringToFile(new File(output + "color." + file.getName()), xml);
        }

    }

    private String injectColor(String html, String tag, String color) {
        Pattern pattern = Pattern.compile("(?si)(<" + tag + ".*)(>.*?</" + tag + ">)");
        return pattern.matcher(html).replaceAll("$1 style=\"background-color:rgb(" + color + ")\"; $2");
        //        return html.replaceAll(regex, "$1 style=\"background-color:rgb(" + color + ") $2");
    }

    public String injectColor(String html, String tag) {
        return injectColor(html, tag, tagToColor.getOrDefault(tag, "255, 255, 0"));
    }

    public String injectColorForAllTags(String html) {
        for (String tag : tagToColor.keySet()) {
            html = injectColor(html, tag, tagToColor.getOrDefault(tag, "255, 255, 0"));
        }
        return html;
    }
}
