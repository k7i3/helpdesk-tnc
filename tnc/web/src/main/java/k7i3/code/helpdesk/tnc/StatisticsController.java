package k7i3.code.helpdesk.tnc;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.MeterGaugeChartModel;
import org.primefaces.model.chart.PieChartModel;
import org.primefaces.model.chart.BarChartModel;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by k7i3 on 07.04.15.
 */
@Named
@ViewScoped
public class StatisticsController implements Serializable{
    @Inject
    private TransportEJB transportEJB;
    @Inject
    private UserEJB userEJB;
    @Inject
    private TicketEJB ticketEJB;
    @Inject
    private StatisticsEJB statisticsEJB;

    private Logger logger = Logger.getLogger("k7i3");

    private PieChartModel pieModel;
    private PieChartModel pieModelTicket;
    private PieChartModel pieModelHeader;
    private BarChartModel barModel;
    private MeterGaugeChartModel meterGaugeModel;

    private Set<String> projects;
    private Set<String> branches;
    private Set<String> selectedProjects;
    private Set<String> selectedBranches;

    private Set<TicketHeader> ticketHeaders;
    private Set<TicketResult> ticketResults;
    private Set<TicketHeader> selectedTicketHeaders;
    private Set<TicketResult> selectedTicketResults;

    private Set<TicketStatus> ticketStatuses;
    private Set<TicketStatus> selectedTicketStatuses;



    private int countOfAllTickets;
    private int countOfOpenedTickets;
    private int countOfAcceptedTickets;
    private int countOfOnServiceTickets;
    private int countOfClosedTickets;
    private int countOfArchivedTickets;
    private int countOfIncorrectTickets;
    private int countOfCanceledTickets;
    private int countOfRepeatedOnServiceTickets;
    private int countOfRepeatedClosedTickets;

    private List <Object[]> countOfTicketsByResults;
    private List <Object[]> countOfTicketsByStatuses;
    private List <Object[]> countOfTicketsByHeaders;
    private List <Object[]> countOfTicketsByProjects;
    private List <Object[]> countOfTicketsByBranches;

    private List <Object[]> countOfTodaysTicketsByResults;
    private List <Object[]> countOfTodaysTicketsByStatuses;
    private List <Object[]> countOfTodaysTicketsByHeaders;
    private List <Object[]> countOfTodaysTicketsByProjects;
    private List <Object[]> countOfTodaysTicketsByBranches;

    private List <Object[]> countOfFilteredTicketsByResults;
    private List <Object[]> countOfFilteredTicketsByStatuses;
    private List <Object[]> countOfFilteredTicketsByHeaders;
    private List <Object[]> countOfFilteredTicketsByProjects;
    private List <Object[]> countOfFilteredTicketsByBranches;

    Date todayStartDate;
    Date todayEndDate;
    Date startDate;
    Date endDate;

    //Init

    @PostConstruct
    public void init() {
        logger.info("=>=>=>=>=> StatisticsController.init()");


//        createBarModel();
        User user = userEJB.initUser();
        projects = user.getProjects();
        if (projects.isEmpty()) projects = new HashSet<>(transportEJB.findAllProjects());
        selectedProjects = projects;
        branches = user.getBranches();
        if (branches.isEmpty()) branches = new HashSet<>(transportEJB.findAllBranches());
        selectedBranches = branches;

        ticketHeaders = new HashSet<>(ticketEJB.findAllTicketHeaders());
        selectedTicketHeaders = ticketHeaders;
        ticketResults = new HashSet<>(ticketEJB.findAllTicketResults());
        selectedTicketResults = ticketResults;

        ticketStatuses = new HashSet<>(Arrays.asList(TicketStatus.values()));
        selectedTicketStatuses = ticketStatuses;

        initDates();
        doCountTicketsByResults();
        doCountTicketsByStatuses();
        doCountTodaysTickets();
        doCountFilteredTickets();
        doCountTickets();

        createMeterGaugeModel();
        initMeterGaugeModel();


        createPieModel();
        initPieModel();

        createPieModelTicket();
        initPieModelTicket();

        createPieModelHeader();
        initPieModelHeader();

        createBarModel();
        initBarModel();
        //FOR RATINGS



    }

    private void initDates() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(59);
        todayStartDate = Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());
        todayEndDate = Date.from(todayEnd.atZone(ZoneId.systemDefault()).toInstant());
        startDate = Date.from(todayStart.minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        endDate = Date.from(todayEnd.minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
    }

    private BarChartModel initBarModel() {
        logger.info("=>=>=>=>=> StatisticsController.initBarModel()");
        BarChartModel model = new BarChartModel();


        for (Object[] countOfTicketsByProject : countOfTicketsByProjects) {
            ChartSeries branch = new ChartSeries();
            branch.setLabel((String) countOfTicketsByProject[0]);
            for (Object[] countOfFilteredTicketsByStatus : countOfFilteredTicketsByStatuses) {
                if (countOfFilteredTicketsByStatus[0].equals(countOfTicketsByProject[0])){
                    branch.set((TicketStatus) countOfFilteredTicketsByStatus[0], (Long) countOfFilteredTicketsByStatus[1]);
                }
            }
            model.addSeries(branch);
        }
        return model;
    }

    private void initMeterGaugeModel() {
        logger.info("=>=>=>=>=> StatisticsController.initMeterGaugeModel()");

        int countOfAllActiveTickets = countOfOpenedTickets + countOfAcceptedTickets + countOfOnServiceTickets + countOfClosedTickets + countOfRepeatedOnServiceTickets + countOfRepeatedClosedTickets;

        meterGaugeModel.setTitle("Количество активных заявок: " + countOfAllActiveTickets);
        meterGaugeModel.setValue(countOfAllActiveTickets);
    }

    private void initPieModel() {
        logger.info("=>=>=>=>=> StatisticsController.initPieModel()");

        for (Object[] countOfTicketsByProject : countOfTicketsByProjects) {
            pieModel.set((String) countOfTicketsByProject[0], (Long) countOfTicketsByProject[1]);
        }
        pieModel.setTitle("Количество проектов, имеющих заявки: " + countOfTicketsByProjects.size());
        pieModel.setLegendPosition("e");
        pieModel.setShowDataLabels(true);
    }
    private void initPieModelTicket() {
        logger.info("=>=>=>=>=> StatisticsController.initPieModelTicket()");

        for (Object[] countOfTicketsByResult : countOfTicketsByResults) {
            pieModelTicket.set(String.valueOf((TicketResult) countOfTicketsByResult[0]), (Integer) countOfTicketsByResult[1]);
        }
        pieModelTicket.setTitle("Закрытых заявок: " + countOfTicketsByResults.size());
        pieModelTicket.setLegendPosition("e");
        pieModelTicket.setShowDataLabels(true);
    }

    private void initPieModelHeader() {
        logger.info("=>=>=>=>=> StatisticsController.initPieModelHeader()");

        for (Object[] countOfTicketsByHeader : countOfTicketsByHeaders) {
            pieModelHeader.set(String.valueOf((TicketHeader) countOfTicketsByHeader[0]), (Long) countOfTicketsByHeader[1]);
        }
        pieModelHeader.setTitle("Причины неисправностей");
        pieModelHeader.setLegendPosition("e");
        pieModelHeader.setShowDataLabels(true);
    }
    //Do COUNT

    public void doCountTicketsByStatuses() {
        countOfAllTickets = statisticsEJB.countAllTickets();

        countOfOpenedTickets = statisticsEJB.countTicketsByStatus(TicketStatus.OPENED);
        countOfAcceptedTickets = statisticsEJB.countTicketsByStatus(TicketStatus.ACCEPTED);
        countOfOnServiceTickets = statisticsEJB.countTicketsByStatus(TicketStatus.ON_SERVICE);
        countOfClosedTickets = statisticsEJB.countTicketsByStatus(TicketStatus.CLOSED);
        countOfArchivedTickets = statisticsEJB.countTicketsByStatus(TicketStatus.ARCHIVED);
        countOfIncorrectTickets = statisticsEJB.countTicketsByStatus(TicketStatus.INCORRECT);
        countOfCanceledTickets = statisticsEJB.countTicketsByStatus(TicketStatus.CANCELED);
        countOfRepeatedOnServiceTickets = statisticsEJB.countTicketsByStatus(TicketStatus.REPEATED_ON_SERVICE);
        countOfRepeatedClosedTickets = statisticsEJB.countTicketsByStatus(TicketStatus.REPEATED_CLOSED);


    }

    public void doCountTickets() {
       // doCountTicketsByResults();
        countOfTicketsByStatuses = statisticsEJB.countTicketsByStatuses(new Date(0), todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTicketsByHeaders = statisticsEJB.countTicketsByHeaders(new Date(0), todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTicketsByProjects = statisticsEJB.countTicketsByProjects(new Date(0), todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTicketsByBranches = statisticsEJB.countTicketsByBranches(new Date(0), todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
    }

    public void doCountTodaysTickets() {
        doCountTodaysTicketsByResults();
        countOfTodaysTicketsByStatuses = statisticsEJB.countTicketsByStatuses(todayStartDate, todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTodaysTicketsByHeaders = statisticsEJB.countTicketsByHeaders(todayStartDate, todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTodaysTicketsByProjects = statisticsEJB.countTicketsByProjects(todayStartDate, todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
        countOfTodaysTicketsByBranches = statisticsEJB.countTicketsByBranches(todayStartDate, todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
    }

    public void doCountFilteredTickets() {
        logger.info("=>=>=>=>=> StatisticsController.doCountFilteredTickets()");
        doCountFilteredTicketsByResults();
        countOfFilteredTicketsByStatuses = statisticsEJB.countTicketsByStatuses(startDate, endDate, selectedProjects, selectedBranches, selectedTicketHeaders, selectedTicketStatuses);
        countOfFilteredTicketsByHeaders = statisticsEJB.countTicketsByHeaders(startDate, endDate, selectedProjects, selectedBranches, selectedTicketHeaders, selectedTicketStatuses);
        countOfFilteredTicketsByProjects = statisticsEJB.countTicketsByProjects(startDate, endDate, selectedProjects, selectedBranches, selectedTicketHeaders, selectedTicketStatuses);
        countOfFilteredTicketsByBranches = statisticsEJB.countTicketsByBranches(startDate, endDate, selectedProjects, selectedBranches, selectedTicketHeaders, selectedTicketStatuses);

        FacesMessage msg = new FacesMessage("Фильтр применен", "таблицы обновлены");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doCountTicketsByResults() {
        logger.info("=>=>=>=>=> StatisticsController.doCountTicketsByResults()");
        int count;
        countOfTicketsByResults = new ArrayList<>();
        List<TicketResult> ticketResults = ticketEJB.findAllTicketResults();
        for (TicketResult result: ticketResults) {
            Object[] resultCount = new Object[2];
            resultCount[0] = result;
            count = statisticsEJB.countTicketsByResult(result, new Date(0), todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
            if (count == 0) continue;
            resultCount[1] = count;
            countOfTicketsByResults.add(resultCount);
        }

        countOfTicketsByResults.sort((o1, o2) -> (int) o2[1] - (int) o1[1]);
    }


    public void doCountTodaysTicketsByResults() {
        logger.info("=>=>=>=>=> StatisticsController.doCountTodaysTicketsByResults()");
        int count;
        countOfTodaysTicketsByResults = new ArrayList<>();
        List<TicketResult> ticketResults = ticketEJB.findAllTicketResults();
        for (TicketResult result: ticketResults) {
            Object[] resultCount = new Object[2];
            resultCount[0] = result;
            count = statisticsEJB.countTicketsByResult(result, todayStartDate, todayEndDate, projects, branches, ticketHeaders, ticketStatuses);
            if (count == 0) continue;
            resultCount[1] = count;
            countOfTodaysTicketsByResults.add(resultCount);
        }

        countOfTicketsByResults.sort((o1, o2) -> (int) o2[1] - (int) o1[1]);
    }

    public void doCountFilteredTicketsByResults() {
        logger.info("=>=>=>=>=> StatisticsController.doCountFilteredTicketsByResults()");
        int count;
        countOfFilteredTicketsByResults = new ArrayList<>();
        List<TicketResult> ticketResults = ticketEJB.findAllTicketResults();
        for (TicketResult result: ticketResults) {
            Object[] resultCount = new Object[2];
            resultCount[0] = result;
            count = statisticsEJB.countTicketsByResult(result, startDate, endDate, selectedProjects, selectedBranches, selectedTicketHeaders, selectedTicketStatuses);
            if (count == 0) continue;
            resultCount[1] = count;
            countOfFilteredTicketsByResults.add(resultCount);
        }

        countOfTicketsByResults.sort((o1,o2) -> (int) o2[1] - (int) o1[1]);
    }



    //Create

    private void createMeterGaugeModel() {
        logger.info("=>=>=>=>=> StatisticsController.createMeterGaugeModel()");

        List<Number> intervals = new ArrayList<Number>(){{
            add(10);
            add(20);
            add(30);
            add(40);
            add(50);
        }};
        meterGaugeModel = new MeterGaugeChartModel(0, intervals);
        meterGaugeModel.setSeriesColors("66cc66,93b75f,E7E658,cc6666,F50000");
        meterGaugeModel.setGaugeLabel("заявки");
    }
    private void createPieModel() {
        logger.info("=>=>=>=>=> StatisticsController.createPieModel()");
        pieModel = new PieChartModel();
    }

    private void createPieModelTicket() {
        logger.info("=>=>=>=>=> StatisticsController.createPieModelTicket()");
        pieModelTicket = new PieChartModel();
    }

    private void createPieModelHeader() {
        logger.info("=>=>=>=>=> StatisticsController.createPieModelHeader()");
        pieModelHeader = new PieChartModel();
    }

    private void createBarModel() {
        logger.info("=>=>=>=>=> StatisticsController.createBarModel()");
        barModel = initBarModel();
        barModel.setTitle("Количество заявок по проектам:");
        barModel.setAnimate(true);
        barModel.setLegendPosition("ne");
        Axis yAxis = barModel.getAxis(AxisType.Y);
        yAxis.setMin(0);
        yAxis.setMax(10);
    }
//
//    GETTERS AND SETTERS

    public BarChartModel getBarModel() {
        return barModel;
    }

    public void setBarModel(BarChartModel barModel) {
        this.barModel = barModel;
    }
//
    public PieChartModel getPieModel() {
        initPieModel();
        return pieModel;
    }

    public void setPieModel(PieChartModel pieModel) {
        this.pieModel = pieModel;
    }

    public PieChartModel getPieModelTicket() {
        initPieModelTicket();
        return pieModelTicket;
    }

    public void setPieModelTicket(PieChartModel pieModelTicket) {
        this.pieModelTicket = pieModelTicket;
    }

    public PieChartModel getPieModelHeader() {
        initPieModelHeader();
        return pieModelHeader;
    }

    public void setPieModelHeader(PieChartModel pieModelHeader) {
        this.pieModelHeader = pieModelHeader;
    }

    public MeterGaugeChartModel getMeterGaugeModel() {
        initMeterGaugeModel();
        return meterGaugeModel;
    }

    public void setMeterGaugeModel(MeterGaugeChartModel meterGaugeModel) {
        this.meterGaugeModel = meterGaugeModel;
    }

    public int getCountOfAllTickets() {
        return countOfAllTickets;
    }

    public void setCountOfAllTickets(int countOfAllTickets) {
        this.countOfAllTickets = countOfAllTickets;
    }

    public int getCountOfOpenedTickets() {
        return countOfOpenedTickets;
    }

    public void setCountOfOpenedTickets(int countOfOpenedTickets) {
        this.countOfOpenedTickets = countOfOpenedTickets;
    }

    public int getCountOfAcceptedTickets() {
        return countOfAcceptedTickets;
    }

    public void setCountOfAcceptedTickets(int countOfAcceptedTickets) {
        this.countOfAcceptedTickets = countOfAcceptedTickets;
    }

    public int getCountOfOnServiceTickets() {
        return countOfOnServiceTickets;
    }

    public void setCountOfOnServiceTickets(int countOfOnServiceTickets) {
        this.countOfOnServiceTickets = countOfOnServiceTickets;
    }

    public int getCountOfClosedTickets() {
        return countOfClosedTickets;
    }

    public void setCountOfClosedTickets(int countOfClosedTickets) {
        this.countOfClosedTickets = countOfClosedTickets;
    }

    public int getCountOfArchivedTickets() {
        return countOfArchivedTickets;
    }

    public void setCountOfArchivedTickets(int countOfArchivedTickets) {
        this.countOfArchivedTickets = countOfArchivedTickets;
    }

    public int getCountOfIncorrectTickets() {
        return countOfIncorrectTickets;
    }

    public void setCountOfIncorrectTickets(int countOfIncorrectTickets) {
        this.countOfIncorrectTickets = countOfIncorrectTickets;
    }

    public int getCountOfCanceledTickets() {
        return countOfCanceledTickets;
    }

    public void setCountOfCanceledTickets(int countOfCanceledTickets) {
        this.countOfCanceledTickets = countOfCanceledTickets;
    }

    public int getCountOfRepeatedOnServiceTickets() {
        return countOfRepeatedOnServiceTickets;
    }

    public void setCountOfRepeatedOnServiceTickets(int countOfRepeatedOnServiceTickets) {
        this.countOfRepeatedOnServiceTickets = countOfRepeatedOnServiceTickets;
    }

    public int getCountOfRepeatedClosedTickets() {
        return countOfRepeatedClosedTickets;
    }

    public void setCountOfRepeatedClosedTickets(int countOfRepeatedClosedTickets) {
        this.countOfRepeatedClosedTickets = countOfRepeatedClosedTickets;
    }

    public List<Object[]> getCountOfTicketsByStatuses() {
        return countOfTicketsByStatuses;
    }

    public void setCountOfTicketsByStatuses(List<Object[]> countOfTicketsByStatuses) {
        this.countOfTicketsByStatuses = countOfTicketsByStatuses;
    }

    public List<Object[]> getCountOfTicketsByHeaders() {
        return countOfTicketsByHeaders;
    }

    public void setCountOfTicketsByHeaders(List<Object[]> countOfTicketsByHeaders) {
        this.countOfTicketsByHeaders = countOfTicketsByHeaders;
    }

    public List<Object[]> getCountOfTicketsByResults() {
        return countOfTicketsByResults;
    }

    public void setCountOfTicketsByResults(List<Object[]> countOfTicketsByResults) {
        this.countOfTicketsByResults = countOfTicketsByResults;
    }

    public List<Object[]> getCountOfTicketsByProjects() {
        return countOfTicketsByProjects;
    }

    public void setCountOfTicketsByProjects(List<Object[]> countOfTicketsByProjects) {
        this.countOfTicketsByProjects = countOfTicketsByProjects;
    }

    public List<Object[]> getCountOfTicketsByBranches() {
        return countOfTicketsByBranches;
    }

    public void setCountOfTicketsByBranches(List<Object[]> countOfTicketsByBranches) {
        this.countOfTicketsByBranches = countOfTicketsByBranches;
    }

    public List<Object[]> getCountOfTodaysTicketsByResults() {
        return countOfTodaysTicketsByResults;
    }

    public void setCountOfTodaysTicketsByResults(List<Object[]> countOfTodaysTicketsByResults) {
        this.countOfTodaysTicketsByResults = countOfTodaysTicketsByResults;
    }

    public List<Object[]> getCountOfTodaysTicketsByStatuses() {
        return countOfTodaysTicketsByStatuses;
    }

    public void setCountOfTodaysTicketsByStatuses(List<Object[]> countOfTodaysTicketsByStatuses) {
        this.countOfTodaysTicketsByStatuses = countOfTodaysTicketsByStatuses;
    }

    public List<Object[]> getCountOfTodaysTicketsByHeaders() {
        return countOfTodaysTicketsByHeaders;
    }

    public void setCountOfTodaysTicketsByHeaders(List<Object[]> countOfTodaysTicketsByHeaders) {
        this.countOfTodaysTicketsByHeaders = countOfTodaysTicketsByHeaders;
    }

    public List<Object[]> getCountOfTodaysTicketsByProjects() {
        return countOfTodaysTicketsByProjects;
    }

    public void setCountOfTodaysTicketsByProjects(List<Object[]> countOfTodaysTicketsByProjects) {
        this.countOfTodaysTicketsByProjects = countOfTodaysTicketsByProjects;
    }

    public List<Object[]> getCountOfTodaysTicketsByBranches() {
        return countOfTodaysTicketsByBranches;
    }

    public void setCountOfTodaysTicketsByBranches(List<Object[]> countOfTodaysTicketsByBranches) {
        this.countOfTodaysTicketsByBranches = countOfTodaysTicketsByBranches;
    }

    public List<Object[]> getCountOfFilteredTicketsByResults() {
        return countOfFilteredTicketsByResults;
    }

    public void setCountOfFilteredTicketsByResults(List<Object[]> countOfFilteredTicketsByResults) {
        this.countOfFilteredTicketsByResults = countOfFilteredTicketsByResults;
    }

    public List<Object[]> getCountOfFilteredTicketsByStatuses() {
        return countOfFilteredTicketsByStatuses;
    }

    public void setCountOfFilteredTicketsByStatuses(List<Object[]> countOfFilteredTicketsByStatuses) {
        this.countOfFilteredTicketsByStatuses = countOfFilteredTicketsByStatuses;
    }

    public List<Object[]> getCountOfFilteredTicketsByHeaders() {
        return countOfFilteredTicketsByHeaders;
    }

    public void setCountOfFilteredTicketsByHeaders(List<Object[]> countOfFilteredTicketsByHeaders) {
        this.countOfFilteredTicketsByHeaders = countOfFilteredTicketsByHeaders;
    }

    public List<Object[]> getCountOfFilteredTicketsByProjects() {
        return countOfFilteredTicketsByProjects;
    }

    public void setCountOfFilteredTicketsByProjects(List<Object[]> countOfFilteredTicketsByProjects) {
        this.countOfFilteredTicketsByProjects = countOfFilteredTicketsByProjects;
    }

    public List<Object[]> getCountOfFilteredTicketsByBranches() {
        return countOfFilteredTicketsByBranches;
    }

    public void setCountOfFilteredTicketsByBranches(List<Object[]> countOfFilteredTicketsByBranches) {
        this.countOfFilteredTicketsByBranches = countOfFilteredTicketsByBranches;
    }

    public Date getTodayStartDate() {
        return todayStartDate;
    }

    public void setTodayStartDate(Date todayStartDate) {
        this.todayStartDate = todayStartDate;
    }

    public Date getTodayEndDate() {
        return todayEndDate;
    }

    public void setTodayEndDate(Date todayEndDate) {
        this.todayEndDate = todayEndDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Set<String> getProjects() {
        return projects;
    }

    public void setProjects(Set<String> projects) {
        this.projects = projects;
    }

    public Set<String> getBranches() {
        return branches;
    }

    public void setBranches(Set<String> branches) {
        this.branches = branches;
    }

    public Set<String> getSelectedProjects() {
        return selectedProjects;
    }

    public void setSelectedProjects(Set<String> selectedProjects) {
        this.selectedProjects = selectedProjects;
    }

    public Set<String> getSelectedBranches() {
        return selectedBranches;
    }

    public void setSelectedBranches(Set<String> selectedBranches) {
        this.selectedBranches = selectedBranches;
    }

    public Set<TicketHeader> getTicketHeaders() {
        return ticketHeaders;
    }

    public void setTicketHeaders(Set<TicketHeader> ticketHeaders) {
        this.ticketHeaders = ticketHeaders;
    }

    public Set<TicketResult> getTicketResults() {
        return ticketResults;
    }

    public void setTicketResults(Set<TicketResult> ticketResults) {
        this.ticketResults = ticketResults;
    }

    public Set<TicketHeader> getSelectedTicketHeaders() {
        return selectedTicketHeaders;
    }

    public void setSelectedTicketHeaders(Set<TicketHeader> selectedTicketHeaders) {
        this.selectedTicketHeaders = selectedTicketHeaders;
    }

    public Set<TicketResult> getSelectedTicketResults() {
        return selectedTicketResults;
    }

    public void setSelectedTicketResults(Set<TicketResult> selectedTicketResults) {
        this.selectedTicketResults = selectedTicketResults;
    }

    public Set<TicketStatus> getTicketStatuses() {
        return ticketStatuses;
    }

    public void setTicketStatuses(Set<TicketStatus> ticketStatuses) {
        this.ticketStatuses = ticketStatuses;
    }

    public Set<TicketStatus> getSelectedTicketStatuses() {
        return selectedTicketStatuses;
    }

    public void setSelectedTicketStatuses(Set<TicketStatus> selectedTicketStatuses) {
        this.selectedTicketStatuses = selectedTicketStatuses;
    }
}
