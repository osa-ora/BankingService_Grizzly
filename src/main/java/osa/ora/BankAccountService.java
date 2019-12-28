package osa.ora;

import osa.ora.beans.BankAccount;
import osa.ora.beans.Transactions;
import java.util.Calendar;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import osa.ora.beans.TransferRequest;

import osa.ora.customer.exception.InvalidRequestException;
import osa.ora.customer.exception.JsonMessage;
import osa.ora.customer.persistence.TransactionsPersistence;
import osa.ora.customer.persistence.BankAccountPersistence;

@Path("/V1/bankaccounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BankAccountService {

    private final BankAccountPersistence bankAccountsPersistence = new BankAccountPersistence();
    private final TransactionsPersistence transactionsPersistence = new TransactionsPersistence();

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public BankAccount[] getAllBankAccounts() {
        System.out.println("Load all bank accounts..");
        BankAccount[] bankAccounts = bankAccountsPersistence.findAll();
        if (bankAccounts != null && bankAccounts.length > 0) {
            for (BankAccount bankAccount : bankAccounts) {
                Transactions[] transactions = transactionsPersistence.findbyId(bankAccount.getAccount_no());
                System.out.println("Found: " + transactions.length + " transactions for this bank account");
                bankAccount.setTransactions(transactions);
            }
            return bankAccounts;
        } else {
            throw new InvalidRequestException(new JsonMessage("Error", "No Bank Account Found"));
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BankAccount getSpecificBankAccount(@PathParam("id") String account_no) {
        BankAccount account = bankAccountsPersistence.findbyId(account_no);
        if (account != null) {
            System.out.println("Retireve bank account using: " + account_no);
            Transactions[] transactions = transactionsPersistence.findbyId(account.getAccount_no());
            System.out.println("Found: " + transactions.length + " transactions for this bank account");
            account.setTransactions(transactions);
            return account;
        } else {
            throw new InvalidRequestException(new JsonMessage("Error", "Bank Account "
                    + account_no + " not found"));
        }
    }
    @POST
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String transferMoney(TransferRequest transfer) {
        BankAccount account = bankAccountsPersistence.findbyId(transfer.getFromAccount());
        if (account != null && account.getBalance()>transfer.getAmount()) {
            System.out.println("Transfer from bank account: " + account.getAccount_no());
            transfer.setCurrency(account.getCurrency());
            Calendar cal=Calendar.getInstance();
            String swift="SW-"+cal.getTimeInMillis();
            boolean results=sendSwiftUsingCamel("{\"Swift_id\":\""+swift+"\",\"From\":\""+transfer.getFromAccount()
                    +"\",\"To\":\""+transfer.getToAccount()+"\",\"Amount\":"+transfer.getAmount()+
                    ",\"currency\":\""+transfer.getCurrency()+"\",\"Note\":\""+transfer.getNote()+"\"}");
            String message="Transfer Failed, Service Not Availble";
            if(results) {
                Transactions newTransactions=new Transactions(0,transfer.getFromAccount(),transfer.getAmount()
                        , new Date().toString(), transfer.getNote()+",SwiftCode="+swift);
                boolean transactionUpdate=transactionsPersistence.save(newTransactions);
                boolean bankAccountUpdate=false;
                if(transactionUpdate){
                    account.setBalance(account.getBalance()-transfer.getAmount());
                    bankAccountUpdate=bankAccountsPersistence.update(account);
                }
                if(bankAccountUpdate){
                    message="Transfer Succeeded, swift code:"+swift;
                }else{
                    message="Transfer Failed, Failed to Update the account!";
                }
            }
            return "{\"result\":\""+message+"\"}";
        } else {
            String message="Transfer Failed, Insufficient fund in this account!";
            return "{\"result\":\""+message+"\"}";
        }        
    }
    private boolean sendSwiftUsingCamel(String message) {
        CamelContext ctx = new DefaultCamelContext();
        //configure jms component        
        String ip = System.getenv("ACTIVEMQ_HOST");
        if (ip == null) {
            ip = "0.0.0.0";
        }
        String port = System.getenv("ACTIVEMQ_PORT");
        if (port == null) {
            port = "61616";
        }
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://" + ip + ":" + port);
            ctx.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
            System.out.println("will send a message:" + message);
            ProducerTemplate template = ctx.createProducerTemplate();
            template.sendBody("activemq:queue:swift", message);
            System.out.println("Transfer Message sent");
            return true;
        } catch (Exception e) {
            System.out.println("ActiveMQ currently not available! "+e.getLocalizedMessage());
            return false;
        }
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBankAccount(BankAccount account) {
        JsonMessage jsonMessage = bankAccountsPersistence.save(account);
        if (jsonMessage.getType().equals("Success")) {
            System.out.println("Successfully added a new bank account");
            return Response.status(201).build();
        } else {
            throw new InvalidRequestException(jsonMessage);
        }
    }

    @PUT
    @Path("{id}/update")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBankAccount(BankAccount bankAccount, @PathParam("id") long id) {
        bankAccount.setId(id);
        BankAccount cust = bankAccountsPersistence.findbyId(bankAccount.getAccount_no());
        if (cust != null) {
            boolean results = bankAccountsPersistence.update(bankAccount);
            if (results) {
                System.out.println("Successfully updated bank account with id=" + id);
                return Response.status(Response.Status.OK).build();
            } else {
                throw new InvalidRequestException(new JsonMessage("Error", "Failed to Update Bank Account "
                    + bankAccount.getId()));
            }
        } else {
            throw new InvalidRequestException(new JsonMessage("Error", "Bank Account "
                    + bankAccount.getId() + " not found"));
        }
    }

    @DELETE
    @Path("/remove/{id}")
    public Response deleteBankAccount(@PathParam("id") String id) {
        BankAccount cust = bankAccountsPersistence.findbyId(id);
        if (cust != null) {
            JsonMessage jsm = bankAccountsPersistence.delete(id);
            if (jsm.getType().equals("Success")) {
                System.out.println("Successfully deleted bank account with id=" + id);
                return Response.status(Response.Status.OK).build();
            } else {
                throw new InvalidRequestException(jsm);
            }
        } else {
            throw new InvalidRequestException(new JsonMessage("Error", "Bank Account "
                    + id + " not found"));
        }
    }

}
