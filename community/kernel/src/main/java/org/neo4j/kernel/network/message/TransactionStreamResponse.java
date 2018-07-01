package org.neo4j.kernel.network.message;

import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.network.TransactionStream;

/**
 * Created by Think on 2018/6/26.
 */
public class TransactionStreamResponse implements Message{
    private final TransactionStream transactions;

    private final StoreId storeId;

    public TransactionStreamResponse(StoreId storeId, TransactionStream transactions)
    {
        this.storeId = storeId;
        this.transactions = transactions;
    }
    @Override
    public void handle(Message.Handler handler) throws Exception{
        transactions.accept(handler.transactions());
    }

    public TransactionStream getTransactions(){
        return transactions;
    }
}
