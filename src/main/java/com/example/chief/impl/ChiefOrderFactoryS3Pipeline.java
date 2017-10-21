package com.example.chief.impl;

import com.amazonaws.services.kinesis.connectors.impl.AllPassFilter;
import com.amazonaws.services.kinesis.connectors.impl.JsonToByteArrayTransformer;
import com.amazonaws.services.kinesis.connectors.interfaces.IBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.ITransformer;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefConnectorPipeline;
import com.example.chief.interfaces.IChiefEmitter;
import com.example.chief.model.Order;

public class ChiefOrderFactoryS3Pipeline implements IChiefConnectorPipeline<Order, byte[]>  {
	
    @Override
    public IChiefEmitter<byte[]> getEmitter(ChiefConfiguration configuration) {
        return new ChiefOrderFactoryS3Emitter(configuration);
    }

    @Override
    public IBuffer<Order> getBuffer(ChiefConfiguration configuration) {
        return new ChiefMemoryBuffer<Order>(configuration);
    }

    @Override
    public ITransformer<Order, byte[]> getTransformer(ChiefConfiguration configuration) {
        return new JsonToByteArrayTransformer<Order>(Order.class);
    }

    @Override
    public IFilter<Order> getFilter(ChiefConfiguration configuration) {
        return new AllPassFilter<Order>();
    }
}
