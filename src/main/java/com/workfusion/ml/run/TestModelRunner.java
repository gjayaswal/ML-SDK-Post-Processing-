package com.workfusion.ml.run;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestModelRunner {
	
	public static void main(String[] args) {
		//IeProcessingRunner runner = new ModelRunner(blockFields, constantFields, mainGroupField, groupFields, fieldToComparison);

        Path trainedModelPath = Paths.get("C:\\LossRun\\AutomationTranningDataSet\\TrainedModel\\ocr_result_html_9108cccf-a3a7-4940-b5c6-894ec3e5aabe_HPO_10Hrs_9-Mar_2018\\output\\model");
        //Path inputFolderPath = Paths.get("/Users/pavel/Downloads/ml_sdk_assignment_2_1doc");
        Path outputFolderPath = Paths.get("C:\\LossRun\\metricsCalculation\\Results");
        Path inputCsvFilePath = Paths.get("C:\\Users\\Gjayaswal\\Downloads\\UnseentestSetFrom2_5_and_2_6_109Docs_extractedBYModel.CSV");
        //runner.run
        
	}

}
