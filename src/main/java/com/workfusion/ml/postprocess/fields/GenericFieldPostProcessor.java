package com.workfusion.ml.postprocess.fields;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.workfusion.vds.api.nlp.model.Field;
import com.workfusion.vds.api.nlp.model.IeDocument;
import com.workfusion.vds.api.nlp.processing.IeProcessor;
import com.workfusion.vds.api.nlp.processing.ProcessingException;
import com.workfusion.vds.nlp.processing.normalization.OcrAmountNormalizer;
import com.workfusion.vds.nlp.processing.normalization.OcrDateNormalizer;

public class GenericFieldPostProcessor implements IeProcessor {

	OcrDateNormalizer dateNormalizer = new OcrDateNormalizer("MM/dd/yyyy");
	OcrAmountNormalizer numberNormalizer = new OcrAmountNormalizer();

	@Override
	public void process(IeDocument document) throws ProcessingException {

		Collection<Field> currentFields = document.findFields();
		currentFields.forEach(this::preformPostProcessingForField);
	}

	private void preformPostProcessingForField(Field field) {

		if (isValidField(field)) {

			normalizeLossDate(field);
			normalizeInsuredName(field);
			normalizeValuationDate(field);
			normalizeIncurredValue(field);

		}

	}

	private void normalizeLossDate(Field targetField) {

		if ("loss_date".equals(targetField.getName())) {

			targetField.setValue(performGenericDateNormalization(targetField.getValue().trim()));
		}
	}

	private void normalizeInsuredName(Field targetField) {

		if ("insured_name".equals(targetField.getName()) && !"".equals(targetField.getValue())) {

			String insuredName = getOutputValue(targetField, new String[] { "Inc", "Ltd", "Corp", "LLC" });
			if (!insuredName.isEmpty()) {
				targetField.setValue(insuredName);
			}
		}
	}

	private String getOutputValue(Field targetField, String[] postFixes) {

		return isAnyStringContainsList(targetField.getValue(), postFixes)
				? checkForAnyValue(targetField.getValue(), postFixes) : "";
	}

	@SuppressWarnings("unchecked")
	private boolean isAnyStringContainsList(String st, String[] arr) {
		return Arrays.asList(arr).stream().anyMatch(s -> StringUtils.containsIgnoreCase(st, (String) s));
	}

	private String checkForAnyValue(String extractedString, String[] postFixValues) {

		for (String postFix : postFixValues) {

			if (!StringUtils.containsIgnoreCase(extractedString, postFix)) {
				continue;
			} else {
				if (!StringUtils.endsWithIgnoreCase(extractedString, postFix)) {

					String orgValue = extractedString.split(String.format("(?i)%s", postFix))[0];
					int orgValueLength = orgValue.length();
					String orgTitleString = extractedString.substring(orgValueLength,
							orgValueLength + postFix.length());
					extractedString = orgValue + orgTitleString;

				} else if (!postFix.equalsIgnoreCase(extractedString)) {

					extractedString += ".";
				}
				break;
			}
		}
		return extractedString;
	}

	private void normalizeValuationDate(Field targetField) {

		if ("valuation_date".equalsIgnoreCase(targetField.getName())) {
			targetField.setValue(performGenericDateNormalization(targetField.getValue().trim()));
		}
	}

	private void normalizeIncurredValue(Field inputField) {

		if ("total_incurred".equalsIgnoreCase(inputField.getName())) {

			String updatedTotalInccured = inputField.getValue();

			updatedTotalInccured = updatedTotalInccured.startsWith("$") ? updatedTotalInccured.substring(1)
					: updatedTotalInccured;

			if ((updatedTotalInccured.startsWith("(") && updatedTotalInccured.endsWith(")"))) {

				updatedTotalInccured = updatedTotalInccured.replaceAll("\\(", "").replaceAll("\\)", "").trim();

				updatedTotalInccured = "-".concat(updatedTotalInccured);
				inputField.setValue(updatedTotalInccured);
			}

			inputField.setValue(performGenericNumberNormalization(inputField.getValue().trim()));
		}
	}

	private boolean isValidField(Field field) {
		return null != field && null != field.getValue();
	}

	private String performGenericDateNormalization(String dateString) {
		return dateNormalizer.normalize(dateString.trim());
	}

	private String performGenericNumberNormalization(String numberString) {
		return numberNormalizer.normalize(numberString.trim());
	}
}