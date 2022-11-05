
package issuetrackinglite;

import issuetrackinglite.model.Issue;
import issuetrackinglite.model.Issue.IssueStatus;
import issuetrackinglite.model.ObservableIssue;
import issuetrackinglite.model.TrackingService;
import issuetrackinglite.model.TrackingServiceStub;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class IssueTrackingLiteController implements Initializable {

    @FXML
    Button newIssue;
    @FXML
    Button deleteIssue;
    @FXML
    Button saveIssue;
    @FXML
    TableView<ObservableIssue> table;
    @FXML
    TableColumn<ObservableIssue, String> colName;
    @FXML
    TableColumn<ObservableIssue, IssueStatus> colStatus;
    @FXML
    TableColumn<ObservableIssue, String> colSynopsis;
    @FXML
    ListView<String> list;
    @FXML
    TextField synopsis;

    private String displayedBugId; 
    private String displayedBugProject; 
    @FXML
    Label displayedIssueLabel; 
    @FXML
    AnchorPane details;
    @FXML
    TextArea descriptionValue;
    ObservableList<String> projectsView = FXCollections.observableArrayList();
    TrackingService model = null;
    private TextField statusValue = new TextField();
    final ObservableList<ObservableIssue> tableContent = FXCollections.observableArrayList();
    
    
   
    @Override
    public void initialize(URL url, ResourceBundle rsrcs) {
        assert colName != null : "fx:id=\"colName\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert colStatus != null : "fx:id=\"colStatus\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert colSynopsis != null : "fx:id=\"colSynopsis\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert deleteIssue != null : "fx:id=\"deleteIssue\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert descriptionValue != null : "fx:id=\"descriptionValue\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert details != null : "fx:id=\"details\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert displayedIssueLabel != null : "fx:id=\"displayedIssueLabel\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert newIssue != null : "fx:id=\"newIssue\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert saveIssue != null : "fx:id=\"saveIssue\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert synopsis != null : "fx:id=\"synopsis\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert table != null : "fx:id=\"table\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        assert list != null : "fx:id=\"list\" was not injected: check your FXML file 'IssueTrackingLite.fxml'.";
        
        System.out.println(this.getClass().getSimpleName() + ".initialize");
        configureButtons();
        configureDetails();
        configureTable();
        connectToService();
        if (list != null) {
            list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            list.getSelectionModel().selectedItemProperty().addListener(projectItemSelected);
            displayedProjectNames.addListener(projectNamesListener);
        }
    }


    public void newIssueFired(ActionEvent event) {
        final String selectedProject = getSelectedProject();
        if (model != null && selectedProject != null) {
            ObservableIssue issue = model.createIssueFor(selectedProject);
            if (table != null) {
             
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(issue);
            }
        }
    }


    public void deleteIssueFired(ActionEvent event) {
        final String selectedProject = getSelectedProject();
        if (model != null && selectedProject != null && table != null) {
            // We create a copy of the current selection: we can't delete
            //    issue while looping over the live selection, since
            //    deleting selected issues will modify the selection.
            final List<?> selectedIssue = new ArrayList<Object>(table.getSelectionModel().getSelectedItems());
            for (Object o : selectedIssue) {
                if (o instanceof ObservableIssue) {
                    model.deleteIssue(((ObservableIssue) o).getId());
                }
            }
            table.getSelectionModel().clearSelection();
        }
    }


    public void saveIssueFired(ActionEvent event) {
        final ObservableIssue ref = getSelectedIssue();
        final Issue edited = new DetailsData();
        SaveState saveState = computeSaveState(edited, ref);
        if (saveState == SaveState.UNSAVED) {
            model.saveIssue(ref.getId(), edited.getStatus(),
                    edited.getSynopsis(), edited.getDescription());
        }

        int selectedRowIndex = table.getSelectionModel().getSelectedIndex();
        table.getItems().clear();
        displayedIssues = model.getIssueIds(getSelectedProject());
        for (String id : displayedIssues) {
            final ObservableIssue issue = model.getIssue(id);
            table.getItems().add(issue);
        }
        table.getSelectionModel().select(selectedRowIndex);

        updateSaveIssueButtonState();
    }
    
    private void configureButtons() {
        if (newIssue != null) {
            newIssue.setDisable(true);
        }
        if (saveIssue != null) {
            saveIssue.setDisable(true);
        }
        if (deleteIssue != null) {
            deleteIssue.setDisable(true);
        }
    }

    private ObservableList<String> displayedProjectNames;
    

    private ObservableList<String> displayedIssues;
    

    private final ListChangeListener<String> projectNamesListener = new ListChangeListener<String>() {

        @Override
        public void onChanged(Change<? extends String> c) {
            if (projectsView == null) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasReplaced()) {
                    for (String p : c.getAddedSubList()) {
                        projectsView.add(p);
                    }
                }
                if (c.wasRemoved() || c.wasReplaced()) {
                    for (String p : c.getRemoved()) {
                        projectsView.remove(p);
                    }
                }
            }
            FXCollections.sort(projectsView);
        }
    };
    

    private final ListChangeListener<String> projectIssuesListener = new ListChangeListener<String>() {

        @Override
        public void onChanged(Change<? extends String> c) {
            if (table == null) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasReplaced()) {
                    for (String p : c.getAddedSubList()) {
                        table.getItems().add(model.getIssue(p));
                    }
                }
                if (c.wasRemoved() || c.wasReplaced()) {
                    for (String p : c.getRemoved()) {
                        ObservableIssue removed = null;

                        for (ObservableIssue t : table.getItems()) {
                            if (t.getId().equals(p)) {
                                removed = t;
                                break;
                            }
                        }
                        if (removed != null) {
                            table.getItems().remove(removed);
                        }
                    }
                }
            }
        }
    };


    private void connectToService() {
        if (model == null) {
            model = new TrackingServiceStub();
            displayedProjectNames = model.getProjectNames();
        }
        projectsView.clear();
        List<String> sortedProjects = new ArrayList<String>(displayedProjectNames);
        Collections.sort(sortedProjects);
        projectsView.addAll(sortedProjects);
        list.setItems(projectsView);
    }
    

    private final ListChangeListener<ObservableIssue> tableSelectionChanged =
            new ListChangeListener<ObservableIssue>() {

                @Override
                public void onChanged(Change<? extends ObservableIssue> c) {
                    updateDeleteIssueButtonState();
                    updateBugDetails();
                    updateSaveIssueButtonState();
                }
            };

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private void updateBugDetails() {
        final ObservableIssue selectedIssue = getSelectedIssue();
        if (details != null && selectedIssue != null) {
            if (displayedIssueLabel != null) {
                displayedBugId = selectedIssue.getId();
                displayedBugProject = selectedIssue.getProjectName();
                displayedIssueLabel.setText( displayedBugId + " / " + displayedBugProject );
            }
            if (synopsis != null) {
                synopsis.setText(nonNull(selectedIssue.getSynopsis()));
            }
            if (statusValue != null) {
                statusValue.setText(selectedIssue.getStatus().toString());
            }
            if (descriptionValue != null) {
                descriptionValue.selectAll();
                descriptionValue.cut();
                descriptionValue.setText(selectedIssue.getDescription());
            }
        } else {
            displayedIssueLabel.setText("");
            displayedBugId = null;
            displayedBugProject = null;
        }
        if (details != null) {
            details.setVisible(selectedIssue != null);
        }
    }

    private boolean isVoid(Object o) {
        if (o instanceof String) {
            return isEmpty((String) o);
        } else {
            return o == null;
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean equal(Object o1, Object o2) {
        if (isVoid(o1)) {
            return isVoid(o2);
        }
        return o1.equals(o2);
    }

    private static enum SaveState {

        INVALID, UNSAVED, UNCHANGED
    }

    private final class DetailsData implements Issue {

        @Override
        public String getId() {
            if (displayedBugId == null || isEmpty(displayedIssueLabel.getText())) {
                return null;
            }
            return displayedBugId;
        }

        @Override
        public IssueStatus getStatus() {
            if (statusValue == null || isEmpty(statusValue.getText())) {
                return null;
            }
            return IssueStatus.valueOf(statusValue.getText().trim());
        }
        
        @Override
        public String getProjectName() {
            if (displayedBugProject == null || isEmpty(displayedIssueLabel.getText())) {
                return null;
            }
            return displayedBugProject;
        }

        @Override
        public String getSynopsis() {
            if (synopsis == null || isEmpty(synopsis.getText())) {
                return "";
            }
            return synopsis.getText();
        }

        @Override
        public String getDescription() {
            if (descriptionValue == null || isEmpty(descriptionValue.getText())) {
                return "";
            }
            return descriptionValue.getText();
        }
    }

    private SaveState computeSaveState(Issue edited, Issue issue) {
        try {

            if (!equal(edited.getId(), issue.getId())) {
                return SaveState.INVALID;
            }
            if (!equal(edited.getProjectName(), issue.getProjectName())) {
                return SaveState.INVALID;
            }

            // If these fields differ, the issue needs saving.
            if (!equal(edited.getStatus(), issue.getStatus())) {
                return SaveState.UNSAVED;
            }
            if (!equal(edited.getSynopsis(), issue.getSynopsis())) {
                return SaveState.UNSAVED;
            }
            if (!equal(edited.getDescription(), issue.getDescription())) {
                return SaveState.UNSAVED;
            }
        } catch (Exception x) {

            return SaveState.INVALID;
        }

        return SaveState.UNCHANGED;
    }

    private void updateDeleteIssueButtonState() {
        boolean disable = true;
        if (deleteIssue != null && table != null) {
            final boolean nothingSelected = table.getSelectionModel().getSelectedItems().isEmpty();
            disable = nothingSelected;
        }
        if (deleteIssue != null) {
            deleteIssue.setDisable(disable);
        }
    }

    private void updateSaveIssueButtonState() {
        boolean disable = true;
        if (saveIssue != null && table != null) {
            final boolean nothingSelected = table.getSelectionModel().getSelectedItems().isEmpty();
            disable = nothingSelected;
        }
        if (disable == false) {
            disable = computeSaveState(new DetailsData(), getSelectedIssue()) != SaveState.UNSAVED;
        }
        if (saveIssue != null) {
            saveIssue.setDisable(disable);
        }
    }


    private void configureTable() {
        colName.setCellValueFactory(new PropertyValueFactory<ObservableIssue, String>("id"));
        colSynopsis.setCellValueFactory(new PropertyValueFactory<ObservableIssue, String>("synopsis"));
        colStatus.setCellValueFactory(new PropertyValueFactory<ObservableIssue, IssueStatus>("status"));


        colName.setPrefWidth(75);
        colStatus.setPrefWidth(75);
        colSynopsis.setPrefWidth(443);

        colName.setMinWidth(75);
        colStatus.setMinWidth(75);
        colSynopsis.setMinWidth(443);

        colName.setMaxWidth(750);
        colStatus.setMaxWidth(750);
        colSynopsis.setMaxWidth(4430);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setItems(tableContent);
        assert table.getItems() == tableContent;

        final ObservableList<ObservableIssue> tableSelection = table.getSelectionModel().getSelectedItems();

        tableSelection.addListener(tableSelectionChanged);
    }


    public String getSelectedProject() {
        if (model != null && list != null) {
            final ObservableList<String> selectedProjectItem = list.getSelectionModel().getSelectedItems();
            final String selectedProject = selectedProjectItem.get(0);
            return selectedProject;
        }
        return null;
    }

    public ObservableIssue getSelectedIssue() {
        if (model != null && table != null) {
            List<ObservableIssue> selectedIssues = table.getSelectionModel().getSelectedItems();
            if (selectedIssues.size() == 1) {
                final ObservableIssue selectedIssue = selectedIssues.get(0);
                return selectedIssue;
            }
        }
        return null;
    }
    

    private final ChangeListener<String> projectItemSelected = new ChangeListener<String>() {

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            projectUnselected(oldValue);
            projectSelected(newValue);
        }
    };


    private void projectUnselected(String oldProjectName) {
        if (oldProjectName != null) {
            displayedIssues.removeListener(projectIssuesListener);
            displayedIssues = null;
            table.getSelectionModel().clearSelection();
            table.getItems().clear();
            if (newIssue != null) {
                newIssue.setDisable(true);
            }
            if (deleteIssue != null) {
                deleteIssue.setDisable(true);
            }
        }
    }


    private void projectSelected(String newProjectName) {
        if (newProjectName != null) {
            table.getItems().clear();
            displayedIssues = model.getIssueIds(newProjectName);
            for (String id : displayedIssues) {
                final ObservableIssue issue = model.getIssue(id);
                table.getItems().add(issue);
            }
            displayedIssues.addListener(projectIssuesListener);
            if (newIssue != null) {
                newIssue.setDisable(false);
            }
            updateDeleteIssueButtonState();
            updateSaveIssueButtonState();
        }
    }

    private void configureDetails() {
        if (details != null) {
            details.setVisible(false);
        }

        if (details != null) {
            details.addEventFilter(EventType.ROOT, new EventHandler<Event>() {

                @Override
                public void handle(Event event) {
                    if (event.getEventType() == MouseEvent.MOUSE_RELEASED
                            || event.getEventType() == KeyEvent.KEY_RELEASED) {
                        updateSaveIssueButtonState();
                    }
                }
            });
        }
    }
}
