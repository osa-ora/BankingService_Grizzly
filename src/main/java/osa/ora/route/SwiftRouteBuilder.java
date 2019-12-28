package osa.ora.route;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

@Deprecated
public class SwiftRouteBuilder extends RouteBuilder {
    private String message;
    private CamelContext ctx;
    public SwiftRouteBuilder(String message,CamelContext ctx){
        super();
        this.message=message;   
        this.ctx=ctx;
    }
    @Override
    public void configure() throws Exception {
        System.out.println("will send a message:"+message);
        ProducerTemplate template = ctx.createProducerTemplate();
        template.sendBody("activemq:queue:swift", message);
        System.out.println("Transfer Message sent");
    }

}