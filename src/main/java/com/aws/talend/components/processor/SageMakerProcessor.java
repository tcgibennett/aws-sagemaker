package com.aws.talend.components.processor;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.aws.talend.components.service.AwsSageMakerImpl;
import com.aws.talend.components.service.AwsSageMakerInvokeException;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;

import com.aws.talend.components.service.AwsSagemakerService;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

@Version(1) // default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler
@Icon(value = Icon.IconType.CUSTOM, custom = "SageMaker") // you can use a custom one using @Icon(value=CUSTOM, custom="filename") and adding icons/filename.svg in resources
@Processor(name = "SageMaker")
@Documentation("TODO fill the documentation for this processor")
public class SageMakerProcessor implements Serializable {
    private final SageMakerProcessorConfiguration configuration;
    private final AwsSagemakerService service;
    private List<Record> records = null;
    private AwsSageMakerImpl awsSageMaker = null;
    private OutputEmitter<Record> outputEmitter = null;
    private OutputEmitter<Record> rejectEmitter = null;
    private RecordBuilderFactory builderFactory = null;

    public SageMakerProcessor(@Option("configuration") final SageMakerProcessorConfiguration configuration,
                          final AwsSagemakerService service, final RecordBuilderFactory builderFactory) {
        this.configuration = configuration;
        this.service = service;
        this.builderFactory = builderFactory;
    }

    @PostConstruct
    public void init() {
        // this method will be executed once for the whole component execution,
        // this is where you can establish a connection for instance
        // Note: if you don't need it you can delete it
        records = new ArrayList<Record>();
        awsSageMaker = AwsSageMakerImpl.getInstance(this.configuration.getAwsAccessKey(),
                this.configuration.getAwsSecretKey(), this.configuration.getAwsRegion(),
                this.configuration.getContentType(), this.configuration.getAccept(),
                this.configuration.getEndpointName(), this.configuration.getAwsSessionToken());
    }

    @BeforeGroup
    public void beforeGroup() {
        // if the environment supports chunking this method is called at the beginning if a chunk
        // it can be used to start a local transaction specific to the backend you use
        // Note: if you don't need it you can delete it
    }

    @ElementListener
    public void onNext(
            @Input final Record defaultInput,
            @Output final OutputEmitter<Record> defaultOutput,
            @Output("REJECT") final OutputEmitter<Record> REJECTOutput) {
        // this is the method allowing you to handle the input(s) and emit the output(s)
        // after some custom logic you put here, to send a value to next element you can use an
        // output parameter and call emit(value).

        this.records.add(defaultInput);
        this.outputEmitter = defaultOutput;
        this.rejectEmitter = REJECTOutput;
    }

    @AfterGroup
    public void afterGroup() {
        // symmetric method of the beforeGroup() executed after the chunk processing
        // Note: if you don't need it you can delete it
        if (this.records.size() > 0)
        {
            try {
                List<String> responses = this.service.processRecords(this.awsSageMaker, this.records);
                for (String response : responses) {
                    Record.Builder record = builderFactory.newRecordBuilder();
                    record.withString("Response", response);
                    outputEmitter.emit(record.build());
                }

            } catch(AwsSageMakerInvokeException e) {
                Record.Builder reject = builderFactory.newRecordBuilder();
                reject.withString("Response", e.getMessage());
                rejectEmitter.emit(reject.build());
            }
            this.records.clear();
        }
    }

    @PreDestroy
    public void release() {
        // this is the symmetric method of the init() one,
        // release potential connections you created or data you cached
        // Note: if you don't need it you can delete it

    }
}