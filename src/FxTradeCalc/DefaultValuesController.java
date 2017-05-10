package FxTradeCalc;

/**
 * Created by GW on 11/4/16.
 */

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class DefaultValuesController {

    @FXML protected Button btnAcceptChanges;
    @FXML protected Button btnCancel;
    @FXML protected TextField txtDefaultHomeCurrency;
    @FXML protected TextField txtDefaultIncrement;
    @FXML protected TextField txtDefaultMaxIncrement;
    @FXML protected ComboBox cbDefaultChartInterval;

    @FXML protected String defaultHomeCurrency;
    @FXML protected String defaultIncrement;
    @FXML protected String defaultMaxIncrement;
    @FXML protected String defaultChartInterval;

    @FXML
    private void initialize() {

        txtDefaultHomeCurrency.setText("USD");
        txtDefaultIncrement.setText("0.0025");
        txtDefaultMaxIncrement.setText("0.01");
        cbDefaultChartInterval.setValue("Daily");

        txtDefaultHomeCurrency.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue){
                setDefaultHomeCurrency(txtDefaultHomeCurrency.getText());
            }
        }));

        txtDefaultIncrement.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue){
                setDefaultIncrement(txtDefaultIncrement.getText());
            }
        }));

        txtDefaultMaxIncrement.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue){
                setDefaultMaxIncrement(txtDefaultMaxIncrement.getText());
            }
        }));
    }

    @FXML
    protected void defaultOptions(){
        setDefaultHomeCurrency("USD");
        setDefaultIncrement("0.0025");
        setDefaultMaxIncrement("0.01");
        setDefaultChartInterval("Daily");

    }


    private void setDefaultHomeCurrency(String homeCurrency){
        defaultHomeCurrency = homeCurrency;
    }

    protected String getDefaultHomeCurrency(){
        return defaultHomeCurrency;
    }

    private void setDefaultIncrement(String increment){
        defaultIncrement = increment;
    }

    protected String getDefaultIncrement(){
        return defaultIncrement;
    }

    private void setDefaultMaxIncrement(String maxIncrement){
        defaultMaxIncrement = maxIncrement;
    }

    protected String getDefaultMaxIncrement(){
        return defaultMaxIncrement;
    }

    private void setDefaultChartInterval(String interval){
        defaultChartInterval = interval;
    }

    protected String getDefaultChartInterval(){
        return defaultChartInterval;
    }


    @FXML
    protected void handleAcceptChanges(ActionEvent event){
        Stage stage = (Stage) btnAcceptChanges.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void handleCancel(){
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

}
