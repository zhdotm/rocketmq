/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.broker.transaction;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.header.EndTransactionRequestHeader;
import org.apache.rocketmq.store.MessageExtBrokerInner;
import org.apache.rocketmq.store.PutMessageResult;

import java.util.concurrent.CompletableFuture;

/**
 * 事务消息处理实现类
 */
public interface TransactionalMessageService {

    /**
     * 用于保存Half事务消息，用户可以对其进行Commit或Rollback。
     * Process prepare message, in common, we should put this message to storage service.
     *
     * @param messageInner Prepare(Half) message.
     * @return Prepare message storage result.
     */
    PutMessageResult prepareMessage(MessageExtBrokerInner messageInner);

    /**
     * Process prepare message in async manner, we should put this message to storage service
     *
     * @param messageInner Prepare(Half) message.
     * @return CompletableFuture of put result, will be completed at put success(flush and replica done)
     */
    CompletableFuture<PutMessageResult> asyncPrepareMessage(MessageExtBrokerInner messageInner);

    /**
     * 用于删除事务消息，一般用于Broker回查失败的Half消息。
     * Delete prepare message when this message has been committed or rolled back.
     *
     * @param messageExt
     */
    boolean deletePrepareMessage(MessageExt messageExt);

    /**
     * 用于提交事务消息，使消费者可以正常地消费事务消息。
     * Invoked to process commit prepare message.
     *
     * @param requestHeader Commit message request header.
     * @return Operate result contains prepare message and relative error code.
     */
    OperationResult commitMessage(EndTransactionRequestHeader requestHeader);

    /**
     * 用于回滚事务消息，回滚后消费者将不能够消费该消息。通常用于生产者主动进行Rollback时，以及Broker回查的生产者本地事务失败时。
     * Invoked to roll back prepare message.
     *
     * @param requestHeader Prepare message request header.
     * @return Operate result contains prepare message and relative error code.
     */
    OperationResult rollbackMessage(EndTransactionRequestHeader requestHeader);

    /**
     * Traverse uncommitted/unroll back half message and send check back request to producer to obtain transaction
     * status.
     *
     * @param transactionTimeout  The minimum time of the transactional message to be checked firstly, one message only
     *                            exceed this time interval that can be checked.
     * @param transactionCheckMax The maximum number of times the message was checked, if exceed this value, this
     *                            message will be discarded.
     * @param listener            When the message is considered to be checked or discarded, the relative method of this class will
     *                            be invoked.
     */
    void check(long transactionTimeout, int transactionCheckMax, AbstractTransactionalMessageCheckListener listener);

    /**
     * 用于打开事务服务。
     * Open transaction service.
     *
     * @return If open success, return true.
     */
    boolean open();

    /**
     * 用于关闭事务服务。
     * Close transaction service.
     */
    void close();
}
