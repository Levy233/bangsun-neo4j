package org.neo4j.kernel.network;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Think on 2018/6/27.
 */
public class TransactionStreamList implements TransactionStream,Serializable {
    private static final long nSerialVerUID = 89098415L;
    private LinkedList<CommittedTransactionRepresentation> txList = new LinkedList<>();
    @Override
    public void accept(Visitor<CommittedTransactionRepresentation, Exception> visitor) throws Exception {

    }

    public void add(CommittedTransactionRepresentation tx){
        txList.addLast(tx);
    }

    public List<CommittedTransactionRepresentation> get(){
        return txList;
    }
}
