package k7i3.code.helpdesk.tnc;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by k7i3 on 27.01.15.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "findAllTransport", query = "SELECT b FROM Transport b ORDER BY b.project DESC"),
        @NamedQuery(name = "findAllProjects", query = "SELECT DISTINCT b.project FROM Transport b ORDER BY b.project DESC"),
        @NamedQuery(name = "findAllBranches", query = "SELECT DISTINCT b.branch FROM Transport b ORDER BY b.branch DESC"),
        @NamedQuery(name = "findAllModels", query = "SELECT DISTINCT b.model FROM Transport b ORDER BY b.model DESC"),
        @NamedQuery(name = "findAllFirmware", query = "SELECT DISTINCT b.terminal.firmware FROM Transport b ORDER BY b.terminal.firmware DESC")
})
public class Transport {
    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    private String project;
    @NotNull
    private String branch;
    @NotNull
    private String stateNumber;
    @NotNull
    private String garageNumber;
    @NotNull
    private String model;
    @NotNull
    @OneToOne (cascade = CascadeType.PERSIST)
    private Terminal terminal;
    @OneToMany (cascade = CascadeType.PERSIST)
    private List<Comment> comments = new ArrayList<>();

    public Transport() {
    }

//    public Transport(String project, String branch, String stateNumber, String garageNumber, String model) {
//        this.project = project;
//        this.branch = branch;
//        this.stateNumber = stateNumber;
//        this.garageNumber = garageNumber;
//        this.model = model;
//    }

        public Transport(String project, String branch, String stateNumber, String garageNumber, String model, Terminal terminal, List<Comment> comments) {
        this.project = project;
        this.branch = branch;
        this.stateNumber = stateNumber;
        this.garageNumber = garageNumber;
        this.model = model;
        this.terminal = terminal;
        this.comments = comments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStateNumber() {
        return stateNumber;
    }

    public void setStateNumber(String stateNumber) {
        this.stateNumber = stateNumber;
    }

    public String getGarageNumber() {
        return garageNumber;
    }

    public void setGarageNumber(String garageNumber) {
        this.garageNumber = garageNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
