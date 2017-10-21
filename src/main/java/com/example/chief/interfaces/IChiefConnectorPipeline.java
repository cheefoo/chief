package com.example.chief.interfaces;

import com.amazonaws.services.kinesis.connectors.interfaces.IBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.IFilter;
import com.amazonaws.services.kinesis.connectors.interfaces.ITransformerBase;
import com.example.chief.configuration.ChiefConfiguration;

public interface IChiefConnectorPipeline<T, U> {
    /**
     * Return an instance of the users implementation of IEmitter
     * 
     * @param configuration
     * @return a configured instance of the IEmitter implementation.
     */
    IChiefEmitter<U> getEmitter(ChiefConfiguration configuration);

    /**
     * Return an instance of the users implementation of IBuffer
     * 
     * @param configuration
     * @return a configured instance of the IBuffer implementation.
     */
    IBuffer<T> getBuffer(ChiefConfiguration configuration);

    /**
     * Return an instance of the users implementation of ITransformer.
     * 
     * @param configuration
     * @return a configured instance of the ITransformer implementation
     */
    ITransformerBase<T, U> getTransformer(ChiefConfiguration configuration);

    /**
     * Return an instance of the users implementation of IFilter.
     * 
     * @param configuration
     * @return a configured instance of the IFilter implementation.
     */
    IFilter<T> getFilter(ChiefConfiguration configuration);
}
