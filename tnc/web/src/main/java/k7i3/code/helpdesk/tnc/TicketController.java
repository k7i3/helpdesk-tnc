package k7i3.code.helpdesk.tnc;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by k7i3 on 24.02.15.
 */
@Named
@RequestScoped
public class TicketController {
    @Inject
    private TransportEJB transportEJB;
    @Inject
    private TicketEJB ticketEJB;
    private Logger logger = Logger.getLogger("k7i3");

    private Transport unitOfTransport;
    private Ticket ticket = new Ticket();
    private List<TicketHeader> ticketHeaders = Arrays.asList(TicketHeader.values());
    private List<TicketResult> ticketResults = Arrays.asList(TicketResult.values());
    TicketResult ticketResult;
    private String didBy;
    private String commentContent;

    //Do ADD

    public void doAddTransportComment(Transport transport, String didBy) {
        logger.info("=>=>=>=>=> TicketController.doAddTransportComment()");
        Comment comment = new Comment(new LifeCycleInfo(new Date(), didBy), new CommentInfo(new LifeCycleInfo(new Date(), didBy), commentContent));
        transport.getComments().add(comment);
        transportEJB.updateTransport(transport);
        commentContent = null;

        FacesMessage msg = new FacesMessage("Сохранено (комментарий к транспорту)", transport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doAddTicketComment(Ticket ticket, String didBy) {
        logger.info("=>=>=>=>=> TicketController.doAddTicketComment()");
        Comment comment = new Comment(new LifeCycleInfo(new Date(), didBy), new CommentInfo(new LifeCycleInfo(new Date(), didBy), commentContent));
        ticket.getComments().add(comment);
        ticketEJB.updateTicket(ticket);
        commentContent = null;

        FacesMessage msg = new FacesMessage("Сохранено (комментарий к заявке)", ticket.getTicketInfo().getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doAddTicket() {
        logger.info("=>=>=>=>=> TicketController.doAddTicket()");

        // TODO (never used) it working automatically by container and i don't now how (but in this case message is not showing)...
        if (!doCheckForPossibilityToAddTicket(unitOfTransport)) { // update unitOfTransport from the database is going on here
            FacesMessage msg = new FacesMessage("Уже есть активная заявка", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        prepareNewTicketInfo(ticket.getTicketInfo(), lifeCycleInfo, TicketStatus.OPENED);

        ticket.setCreation(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference
        unitOfTransport.getTickets().add(ticket); // difference

        transportEJB.updateTransport(unitOfTransport);

        FacesMessage msg = new FacesMessage("Сохранено (заявка)", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
//        doReset(); for PF('addTicketDialog').show() via JS (@SessionScoped) and for Primefaces dialog framework (@SessionScoped)
//        RequestContext.getCurrentInstance().closeDialog(null); for Primefaces dialog framework (@SessionScoped)
    }

    //Do UPDATE

    public void doUpdateTicketInfo() {
        logger.info("=>=>=>=>=> TicketController.doUpdateTicketInfo()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());

        Ticket ticketForUpdates = ticketEJB.findTicketById(ticket.getId()); // difference

        ticket.getTicketInfo().setTicketStatus(ticketForUpdates.getTicketInfo().getTicketStatus()); // it needs for right comparing (equals below) in the case where TicketStatus could be changed in parallel

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);

        Boolean isTicketInfoUpdated = false;
        if (!ticket.getTicketInfo().equals(ticketForUpdates.getTicketInfo())) {
            TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
            prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, ticketForUpdates.getTicketInfo().getTicketStatus()); // last argument repeats operation above it because helper method was implemented that for operation change status
            setNewTicketInfo(newTicketInfo, ticketForUpdates);
            isTicketInfoUpdated = true;
        }

        ticketEJB.updateTicket(ticketForUpdates);

        FacesMessage msg = new FacesMessage(isTicketInfoUpdated? "Обновлено (заявка)" : "Информация не изменена", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    //Do CHANGE STATUS

    public void doAcceptTicket() {
        logger.info("=>=>=>=>=> TicketController.doAcceptTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getAcceptance() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже принята", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.ACCEPTED);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setAcceptance(lifeCycleInfo); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка принята", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doOnServiceTicket() {
        logger.info("=>=>=>=>=> TicketController.doOnServiceTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getService() != null) {
            FacesMessage msg = new FacesMessage("Выезд уже назначен", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getClosing() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже закрыта", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getAcceptance() == null) {
            FacesMessage msg = new FacesMessage("Заявка еще не принята)", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.ON_SERVICE);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setService(lifeCycleInfo); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Выезд назначен", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doCloseTicket() {
        logger.info("=>=>=>=>=> TicketController.doCloseTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getClosing() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже закрыта", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getAcceptance() == null) {
            FacesMessage msg = new FacesMessage("Заявка еще не принята", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.CLOSED);
        newTicketInfo.setTicketResult(ticketResult); // difference
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setClosing(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка закрыта", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doArchiveTicket() {
        logger.info("=>=>=>=>=> TicketController.doArchiveTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getArchiving() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже в архиве", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getClosing() == null) {
            FacesMessage msg = new FacesMessage("Заявка еще не закрыта", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.ARCHIVED);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setArchiving(lifeCycleInfo); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка помещена в архив", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doMarkAsIncorrectTicket() {
        logger.info("=>=>=>=>=> TicketController.doMarkAsIncorrectTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отмечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getArchiving() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже в архиве", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.INCORRECT);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setIncorrectness(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка отмечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doCancelTicket() {
        logger.info("=>=>=>=>=> TicketController.doCancelTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отмечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getArchiving() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже в архиве", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.CANCELED);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.setCancellation(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка отменена", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doRepeatedOnServiceTicket() {
        logger.info("=>=>=>=>=> TicketController.doRepeatedOnServiceTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getService() == null) {
            FacesMessage msg = new FacesMessage("Еще не назначен первичный выезд", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getClosing() == null) {
            FacesMessage msg = new FacesMessage("Заявка еще не закрыта в первый раз", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getRepeatedService().size() != ticket.getRepeatedClosing().size()) {
            FacesMessage msg = new FacesMessage("Повторный выезд уже назначен", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getArchiving() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже в архиве", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.REPEATED_ON_SERVICE);
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.getRepeatedService().add(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Повторный выезд назначен", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doRepeatedCloseTicket() {
        logger.info("=>=>=>=>=> TicketController.doRepeatedCloseTicket()");
        unitOfTransport = transportEJB.findTransportById(unitOfTransport.getId());
        ticket = ticketEJB.findTicketById(ticket.getId());

        if (ticket.getService() == null) {
            FacesMessage msg = new FacesMessage("Еще не назначен первичный выезд", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getClosing() == null) {
            FacesMessage msg = new FacesMessage("Заявка еще не закрыта в первый раз", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getRepeatedService().size() == ticket.getRepeatedClosing().size()) {
            FacesMessage msg = new FacesMessage("заявка уже повторно закрыта", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getArchiving() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже в архиве", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getIncorrectness() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже помечена как невалидная", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        if (ticket.getCancellation() != null) {
            FacesMessage msg = new FacesMessage("Заявка уже отменена", unitOfTransport.getTransportInfo().getStateNumber());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        LifeCycleInfo lifeCycleInfo = new LifeCycleInfo(new Date(), didBy);
        TicketInfo newTicketInfo = new TicketInfo(ticket.getTicketInfo());
        prepareNewTicketInfo(newTicketInfo, lifeCycleInfo, TicketStatus.REPEATED_CLOSED);
        newTicketInfo.setTicketResult(ticketResult); // difference
        setNewTicketInfo(newTicketInfo, ticket);

        ticket.getRepeatedClosing().add(lifeCycleInfo); // difference
        ticket.getComments().add(new Comment(lifeCycleInfo, new CommentInfo(lifeCycleInfo, commentContent))); // difference

        ticketEJB.updateTicket(ticket);

        FacesMessage msg = new FacesMessage("Заявка закрыта повторно", unitOfTransport.getTransportInfo().getStateNumber());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    //Do CHECK

    public Boolean doCheckForPossibilityToAddTicket(Transport transport) {
        logger.info("=>=>=>=>=> TicketController.doCheckForPossibilityToAddTicket()");
        unitOfTransport = transportEJB.findTransportById(transport.getId());
        List<Ticket> tickets = unitOfTransport.getTickets();
        return tickets.isEmpty() || tickets.get(tickets.size() - 1).getArchiving() != null || tickets.get(tickets.size() - 1).getIncorrectness() != null || tickets.get(tickets.size() - 1).getCancellation() != null;
    }

    //HELPER METHODS

    private void prepareNewTicketInfo(TicketInfo newTicketInfo, LifeCycleInfo lifeCycleInfo, TicketStatus ticketStatus) {
        logger.info("=>=>=>=>=> TicketController.prepareNewTicketInfo()");
        newTicketInfo.setModification(lifeCycleInfo);
        newTicketInfo.setTransportInfo(new TransportInfo(unitOfTransport.getTransportInfo()));
        newTicketInfo.setTerminalInfo(new TerminalInfo(unitOfTransport.getTerminal().getTerminalInfo()));
        newTicketInfo.setPointInfo(new PointInfo(unitOfTransport.getPoint().getPointInfo()));
        newTicketInfo.setTicketStatus(ticketStatus);
    }

    private void setNewTicketInfo(TicketInfo newTicketInfo, Ticket ticket) {
        logger.info("=>=>=>=>=> TicketController.setNewTicketInfo()");
        TicketInfo oldTicketInfo = ticket.getTicketInfo(); // = new TicketInfo(ticket.getTicketInfo());
        ticket.getTicketInfoHistory().add(oldTicketInfo);
        ticket.setTicketInfo(newTicketInfo);
    }
//    public void doOpenTicketDialog(Transport transport, String didBy) {
//        this.transport = transport;
//        this.didBy = didBy;
//
//        Map<String,Object> options = new HashMap<>();
//        options.put("modal", true);
//        options.put("draggable", true);
//        options.put("resizable", true);
//
//        RequestContext.getCurrentInstance().openDialog("addTicket", options, null);
//
////        FacesMessage msg = new FacesMessage(didBy, transport.getId().toString());
////        FacesContext.getCurrentInstance().addMessage(null, msg);
//    }

//    public void doReset() {
//        transport = null;
//        didBy = null;
//        ticket = new Ticket();
//        commentContent = null;
//    }
















    public Transport getUnitOfTransport() {
        return unitOfTransport;
    }

    public void setUnitOfTransport(Transport unitOfTransport) {
        this.unitOfTransport = unitOfTransport;
    }

    public String getDidBy() {
        return didBy;
    }

    public void setDidBy(String didBy) {
        this.didBy = didBy;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public List<TicketHeader> getTicketHeaders() {
        return ticketHeaders;
    }

    public void setTicketHeaders(List<TicketHeader> ticketHeaders) {
        this.ticketHeaders = ticketHeaders;
    }

    public List<TicketResult> getTicketResults() {
        return ticketResults;
    }

    public void setTicketResults(List<TicketResult> ticketResults) {
        this.ticketResults = ticketResults;
    }

    public TicketResult getTicketResult() {
        return ticketResult;
    }

    public void setTicketResult(TicketResult ticketResult) {
        this.ticketResult = ticketResult;
    }
}
