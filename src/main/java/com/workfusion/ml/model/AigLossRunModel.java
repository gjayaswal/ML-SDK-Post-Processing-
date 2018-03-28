package com.workfusion.ml.model;

import com.workfusion.vds.api.nlp.hypermodel.ModelType;
import com.workfusion.vds.api.nlp.hypermodel.annotation.FixedModelConfiguration;
import com.workfusion.vds.api.nlp.hypermodel.annotation.ModelDescription;
import com.workfusion.vds.api.nlp.hypermodel.annotation.PostProcessorsConfiguration;
import com.workfusion.vds.nlp.hypermodel.ie.invoice.generic.GenericInvoiceIeHyperModel;
import com.workfusion.vds.nlp.model.description.ConfigurationProvider;
import com.workfusion.ml.postprocess.AigLossRunModelPostProcessingConfiguration;

@ModelDescription(
        code = "aig-loss-run",
        title = "AIG loss run",
        description = "AIG loss run",
        version = "1.2.3",
        type = ModelType.IE
)
@PostProcessorsConfiguration(AigLossRunModelPostProcessingConfiguration.class)
@ConfigurationProvider(AIGLossRunConfiguration.class)
@FixedModelConfiguration(value = "")
public class AigLossRunModel extends GenericInvoiceIeHyperModel {
	
    public AigLossRunModel() throws Exception {
    }
}
