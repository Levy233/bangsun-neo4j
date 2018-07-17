//package org.neo4j.kernel.network;
//
//import org.jboss.marshalling.MarshallerFactory;
//import org.jboss.marshalling.Marshalling;
//import org.jboss.marshalling.MarshallingConfiguration;
//
//import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
//import io.netty.handler.codec.marshalling.DefaultUnmarshallerProvider;
//import io.netty.handler.codec.marshalling.MarshallerProvider;
//import io.netty.handler.codec.marshalling.MarshallingDecoder;
//import io.netty.handler.codec.marshalling.MarshallingEncoder;
//import io.netty.handler.codec.marshalling.UnmarshallerProvider;
//
//public class MarshallingCodeCFactory {
//
//    public static MarshallingDecoder buildMarshallingDecoder() {
//        final MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory("serial");
//        final MarshallingConfiguration configuration = new MarshallingConfiguration();
//        configuration.setVersion(5);
//        UnmarshallerProvider provider = new DefaultUnmarshallerProvider(factory, configuration);
//        MarshallingDecoder decoder = new MarshallingDecoder(provider, 1024);
//        return decoder;
//    }
//
//    public static MarshallingEncoder buildMarshallingEncoder() {
//        final MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory("serial");
//        final MarshallingConfiguration configuration = new MarshallingConfiguration();
//        configuration.setVersion(5);
//        MarshallerProvider provider = new DefaultMarshallerProvider(factory, configuration);
//        MarshallingEncoder encoder = new MarshallingEncoder(provider);
//        return encoder;
//    }
//
//}
