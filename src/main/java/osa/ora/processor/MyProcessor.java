package osa.ora.processor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@Deprecated
public class MyProcessor implements Processor {
	public void process(Exchange exchange) throws Exception {
        String recipient = exchange.getIn().getBody().toString();
        //String recipientQueue="jms:queue:"+recipient;
        //exchange.getIn().setHeader("queue", recipientQueue);
        //exchange.getIn().setHeader("filename", "file:C:/outputFolder/filename"+recipient.length());
        System.out.println("=="+exchange.getIn().getBody());
	}
}
