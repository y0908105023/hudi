/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.helper;

import org.apache.hudi.connect.transaction.TransactionParticipant;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that emulates the Kafka Connect f/w and additionally
 * implements {@link SinkTaskContext} for testing purposes.
 */
public class TestKafkaConnect implements SinkTaskContext {

  private static final int NUM_RECORDS_BATCH = 5;
  private final TopicPartition testPartition;

  private TransactionParticipant participant;
  private long currentKafkaOffset;
  private boolean isPaused;

  public TestKafkaConnect(TopicPartition testPartition) {
    this.testPartition = testPartition;
    isPaused = false;
    currentKafkaOffset = 0L;
  }

  public void setParticipant(TransactionParticipant participant) {
    this.participant = participant;
  }

  public boolean isPaused() {
    return isPaused;
  }

  public boolean isResumed() {
    return !isPaused;
  }

  public int putRecordsToParticipant() {
    for (int i = 1; i <= NUM_RECORDS_BATCH; i++) {
      participant.buffer(getNextKafkaRecord());
    }
    participant.processRecords();
    return NUM_RECORDS_BATCH;
  }

  public SinkRecord getNextKafkaRecord() {
    return new SinkRecord(testPartition.topic(),
        testPartition.partition(),
        Schema.OPTIONAL_BYTES_SCHEMA,
        ("key-" + currentKafkaOffset).getBytes(),
        Schema.OPTIONAL_BYTES_SCHEMA,
        "value".getBytes(), currentKafkaOffset++);
  }

  public long getCurrentKafkaOffset() {
    return currentKafkaOffset;
  }

  @Override
  public void pause(TopicPartition... partitions) {
    if (Arrays.stream(partitions).allMatch(testPartition::equals)) {
      isPaused = true;
    }
  }

  @Override
  public void resume(TopicPartition... partitions) {
    if (Arrays.stream(partitions).allMatch(testPartition::equals)) {
      isPaused = false;
    }
  }

  @Override
  public void offset(Map<TopicPartition, Long> offsets) {
    for (TopicPartition tp : offsets.keySet()) {
      if (tp.equals(testPartition)) {
        currentKafkaOffset = offsets.get(tp);
      }
    }
  }

  @Override
  public void offset(TopicPartition tp, long offset) {
    if (tp.equals(testPartition)) {
      currentKafkaOffset = offset;
    }
  }

  @Override
  public Map<String, String> configs() {
    return null;
  }

  @Override
  public void timeout(long timeoutMs) {

  }

  @Override
  public Set<TopicPartition> assignment() {
    return null;
  }

  @Override
  public void requestCommit() {

  }
}
