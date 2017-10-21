package com.example.chief.impl;

import com.amazonaws.services.kinesis.connectors.impl.AllPassFilter;
import com.amazonaws.services.kinesis.connectors.impl.JsonToByteArrayTransformer;
import com.amazonaws.services.kinesis.connectors.interfaces.IBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.ITransformer;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefConnectorPipeline;
import com.example.chief.interfaces.IChiefEmitter;
import com.example.chief.model.JobResult;

public class ChiefJobResultNotifyS3Pipeline implements IChiefConnectorPipeline<JobResult, byte[]>  {
	
    @Override
    public IChiefEmitter<byte[]> getEmitter(ChiefConfiguration configuration) {
        return new ChiefJobResultNotifyS3Emitter(configuration);
    }

    @Override
    public IBuffer<JobResult> getBuffer(ChiefConfiguration configuration) {
        return new ChiefMemoryBuffer<JobResult>(configuration);
    }

    @Override
    public ITransformer<JobResult, byte[]> getTransformer(ChiefConfiguration configuration) {
        return new JsonToByteArrayTransformer<JobResult>(JobResult.class);
    }

    @Override
    public IFilter<JobResult> getFilter(ChiefConfiguration configuration) {
        return new AllPassFilter<JobResult>();
    }
}
