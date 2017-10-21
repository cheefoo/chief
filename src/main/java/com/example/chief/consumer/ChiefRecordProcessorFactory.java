package com.example.chief.consumer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.connectors.interfaces.IBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.ITransformerBase;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefConnectorPipeline;
import com.example.chief.interfaces.IChiefEmitter;

public class ChiefRecordProcessorFactory<T, U> implements IRecordProcessorFactory {

	private IChiefConnectorPipeline<T, U> pipeline;
    private ChiefConfiguration configuration;

    public ChiefRecordProcessorFactory(IChiefConnectorPipeline<T, U> pipeline,
    		ChiefConfiguration configuration) {
        this.configuration = configuration;
        this.pipeline = pipeline;
    }

    @Override
    public IRecordProcessor createProcessor() {
        try {
            IBuffer<T> buffer = pipeline.getBuffer(configuration);
            IChiefEmitter<U> emitter = pipeline.getEmitter(configuration);
            ITransformerBase<T, U> transformer = pipeline.getTransformer(configuration);
            IFilter<T> filter = pipeline.getFilter(configuration);
            ChiefRecordProcessor<T, U> processor = new ChiefRecordProcessor<T, U>(buffer, filter, emitter, transformer, configuration);
            
            return processor;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
}
