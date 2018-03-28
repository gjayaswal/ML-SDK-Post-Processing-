package com.workfusion.ml.postprocess;

import java.util.ArrayList;
import java.util.List;

import com.workfusion.ml.postprocess.blocks.BlocksConfiguration;
import com.workfusion.ml.postprocess.fields.GenericFieldPostProcessor;
import com.workfusion.vds.api.nlp.processing.Processor;
import com.workfusion.vds.api.nlp.processing.ProcessorsConfiguration;

public class AigLossRunModelPostProcessingConfiguration implements ProcessorsConfiguration {

    @Override
    public List<Processor> getProcessors() {
    	final List<Processor> processorsList = new ArrayList<Processor>();
    	processorsList.add(new GenericFieldPostProcessor());
    	processorsList.addAll(BlocksConfiguration.getBlocksProcessors());
        return processorsList;
    }

}
