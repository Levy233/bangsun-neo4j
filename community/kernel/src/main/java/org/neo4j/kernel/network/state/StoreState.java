package org.neo4j.kernel.network.state;

/**
 * Created by Think on 2018/6/21.
 */
public enum StoreState implements State{

    newer{
        @Override
        public void handle() {

        }
    },
    older{
        @Override
        public void handle() {

        }
    },
    same{
        @Override
        public void handle() {

        }
    },
    pulling{
        @Override
        public void handle() {

        }
    },
    start{
        @Override
        public void handle() {

        }
    }
}
