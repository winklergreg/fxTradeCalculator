package FxTradeCalc;

import Resources.CustomerSpreads;
import Resources.FormatData;
import Resources.ForwardRates;
import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.NotFoundException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

public class TradeCalcController {

    @FXML protected MenuBar menuBar;
    @FXML protected MenuItem exitApp;
    @FXML protected MenuItem defaultOptions;
    @FXML protected MenuItem aboutFile;

    @FXML protected TextField txtXCCY;
    @FXML protected TextField txtAmount;
    @FXML protected TextField txtCustomSpotSpread;
    @FXML protected TextField txtCustomFxRate;
    @FXML protected TextField txtForwardSpread;

    @FXML protected DatePicker cdrForwardDate;

    @FXML protected ToggleGroup BuySell;
    @FXML protected ToggleGroup SolveForCurrency;
    @FXML protected ToggleGroup SpotForward;

    @FXML protected RadioButton rbBuy;
    @FXML protected RadioButton rbSell;
    @FXML protected RadioButton rbBaseCCY;
    @FXML protected RadioButton rbRefCCY;
    @FXML protected RadioButton rbSpot;
    @FXML protected RadioButton rbFwd;

    @FXML protected ComboBox cbChartType;

    @FXML private TableView<CustomerSpreads> tblSpreads;
    @FXML protected TableColumn spreadColumn;
    @FXML protected TableColumn spotRateColumn;
    @FXML protected TableColumn spotAmountColumn;
    @FXML protected TableColumn spotRevenueColumn;

    @FXML private TableView<ForwardRates> tblForwards;
    @FXML protected TableColumn forwardDateColumn;
    @FXML protected TableColumn forwardPointsColumn;
    @FXML protected TableColumn forwardRateColumn;
    @FXML protected TableColumn forwardAmountColumn;
    @FXML protected TableColumn forwardRevenueColumn;

    @FXML private Label lblCurrency;
    @FXML private Label spotValueDate;
    @FXML protected Label xccyError;

    @FXML private LineChart<String, Number> chartHistory;
    @FXML private NumberAxis yAxis;
    @FXML private XYChart.Series series = new XYChart.Series();

    private ObservableList<CustomerSpreads> spotData;
    private ObservableList<ForwardRates> fwdData;

    private final double INCREMENTS = 0.0025;
    private final double MAX_INCREMENT = 0.01;
    private double MULTIPLIER = 1;
    private double fxCustomFwdPts;
    private double baseRate;
    private double homeCurrencyRate;
    private double baseAmount;
    private double revenue;
    private double graphMinPrice;
    private double graphMaxPrice;
    private double spreadRate;
    private double adjustedAmount;
    private double dblForwardRates;
    private double dblForwardAmounts;
    private double dblForwardSpread;
    private double pricesHistorical[];
    private double fxRateAsk[];
    private double fxRateBid[];
    private double fxFwdPoints[][][];

    private String fxCurrency;
    private String strForwardRates;
    private String strForwardAmounts;
    private String stringSpreadRate;
    private String stringAmount;
    private String selectedRefBaseCCY;
    private String spotOrFwd;
    private String stringSpread;
    private String stringRevenue;
    private String tableSpotValues[];
    private String tableForwardValues[];

    private Datetime datesHistorical[];
    private Datetime spotSettleDates[];
    private Datetime fwdSettleDates[][];

    private RadioButton chk;

    private int arrayLength;
    private int currencyMaxDecimals;
    private int buySellProperty;
    private int numToDivideFwdPoints;

    @FXML
    private void initialize() throws Exception {
    /*
    Sets initial values to each component of the user interface. Listeners are included in this section to
    adjust values based on the user's interaction with the interface.
     */

        tblSpreads.setEditable(true);
        tblForwards.setEditable(true);
        addInitialSpotSpreads();
        populateChartOptions();


        cbChartType.setValue("Daily");  // Set default price graph selection

        // Adjusts the chart based on the user's selection
        cbChartType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {

                if(!series.getData().isEmpty()){
                    try {
                        determineGraphRequest(cbChartType.getValue().toString());
                    }catch(Exception e){ e.printStackTrace(); }
                    try {
                        graphData(cbChartType.getValue().toString());
                    }catch (Exception e){ e.printStackTrace(); }

                }
            }
        });

        //Capitalize the currency pair
        txtXCCY.textProperty().addListener((observable, oldValue, newValue) -> {
            txtXCCY.setText(newValue.toUpperCase());
        });

        //Set the label of the amount corresponding to the currency requested
        txtXCCY.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue && !txtXCCY.getText().isEmpty()){
                if(txtXCCY.getText().length() == 6) {
                    xccyError.setText("");

                    try {
                        switch  (rbBaseCCY.selectedProperty().getValue().toString()){
                            case "true":
                                selectedRefBaseCCY = "Base Currency";
                                break;
                            case "false":
                                selectedRefBaseCCY = "Ref Currency";
                                break;
                        }
                        getRefBaseCurrencySelection();
                        try {
                            determineGraphRequest(cbChartType.getValue().toString());
                            graphData(cbChartType.getValue().toString());
                        }catch(Exception e){ e.printStackTrace(); }

                    }catch(NullPointerException e){ e.printStackTrace(); }

                    try {
                        getBloombergSpotData();
                    }catch(Exception e){ e.printStackTrace(); }

                    getSizeOfSpotTableSpreads();
                    getSizeOfFwdTable();
                    try {
                        populateSpotTable();
                        populateForwardTable();
                    }catch(NullPointerException npe){
                        xccyError.setText("Invalid XCCY");
                    }

                }else if (txtXCCY.getText().length() != 6) { setXccyError(); }
            }else{
                if (spotRateColumn.getText().isEmpty()) {
                    tblSpreads.getItems().clear();
                    tblForwards.getItems().clear();
                }

            }
        }));

        // Adjustment to data if user changes to a buy or sell request
        BuySell.selectedToggleProperty().addListener(((observable, oldValue, newValue) -> {
            if(BuySell.getSelectedToggle() != null) {
                if (!tblSpreads.getItems().isEmpty()) {
                    MULTIPLIER = -MULTIPLIER;
                    updateTable();}
            }
        }));

        // Determines which currency to solve for based on the user's selection
        SolveForCurrency.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (SolveForCurrency.getSelectedToggle() != null) {
                chk = (RadioButton) newValue.getToggleGroup().getSelectedToggle();

                // Changes label based on user's selection
                if(chk == rbRefCCY) {
                    selectedRefBaseCCY = "Ref Currency";
                }else {
                    selectedRefBaseCCY = "Base Currency";
                }

                getRefBaseCurrencySelection();

                // Updates table data if not empty
                if (!tblSpreads.getItems().isEmpty()) { updateTable(); }
            }
        });

        // Adjusts data based on the user's input for currency amount
        txtAmount.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue && !txtAmount.getText().isEmpty()) {
                FormatData format = new FormatData();
                double tempValue;

                try {
                    tempValue = Double.parseDouble(txtAmount.getText());
                }catch(NumberFormatException e){
                    tempValue = Double.parseDouble(txtAmount.getText().replaceAll(",",""));
                }

                txtAmount.setText(format.RoundTo2Decimals(tempValue));

                try{
                    // Updates table data if table not empty
                    if (!spotRateColumn.getText().isEmpty()) { updateTable(); }
                }catch (NullPointerException e){ e.printStackTrace(); }
            }
        }));

        txtCustomSpotSpread.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue && !txtCustomSpotSpread.getText().isEmpty()){
                if (!txtCustomSpotSpread.getText().isEmpty()) {
                    addCustomSpread("Spread");
                    txtCustomSpotSpread.clear();
                }
            }
        }));

        txtCustomFxRate.focusedProperty().addListener(((observable, oldValue, newValue) ->  {
            if(!newValue && !txtCustomFxRate.getText().isEmpty()){
                try {
                    addCustomSpread("Rate");
                }catch(NumberFormatException e){ e.printStackTrace(); }
                txtCustomFxRate.clear();
            }
        }));

        txtForwardSpread.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue && !txtForwardSpread.getText().isEmpty()){
                FormatData format = new FormatData();
                double tempFwdSpread;

                try{
                    tempFwdSpread = Double.parseDouble(txtForwardSpread.getText())/100;
                }catch(NumberFormatException e) {
                    tempFwdSpread = Double.parseDouble(txtForwardSpread.getText().replaceAll("%",""))/100;
                }

                txtForwardSpread.setText(format.PercentFormat(tempFwdSpread));
                rbFwd.selectedProperty().setValue(true);
            }


            try {
                if(!tblForwards.getItems().isEmpty()) {
                    //getFxSpotRate();
                    //determineBaseAmountRequest();
                    populateForwardTable();
                }
            }catch(Exception eFwdTable){
                eFwdTable.printStackTrace();
                try {
                    getBloombergSpotData();
                }catch (Exception e){ e.printStackTrace(); }
            }
        }));

        cdrForwardDate.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && (cdrForwardDate.getValue() != null)) {
                addCustomForward();
                cdrForwardDate.setValue(null);
            }
        });

        defaultOptions.setOnAction(EventHandler -> openDefaultWindow());
        aboutFile.setOnAction(EventHandler -> openAboutWindow());
        exitApp.setOnAction(EventHandler -> Platform.exit());

    }

    private void openDefaultWindow(){
        try {
            FXMLLoader loader = new FXMLLoader(TradeCalculator.class.getResource("../Resources/DefaultValues.fxml"));
            AnchorPane defaultPage = (AnchorPane) loader.load();
            Stage defaultOptionsStage = new Stage();
            defaultOptionsStage.setTitle("Default Options");
            defaultOptionsStage.initModality(Modality.WINDOW_MODAL);

            defaultOptionsStage.setScene(new Scene(defaultPage));
            defaultOptionsStage.show();

        }catch(Exception e){ e.printStackTrace(); }
    }

    private void openAboutWindow(){
        try{
            FXMLLoader loader = new FXMLLoader((TradeCalculator.class.getResource("../Resources/AboutFile.fxml")));
            Pane aboutPage = (Pane) loader.load();
            Stage aboutFileStage = new Stage();
            aboutFileStage.setTitle("About");
            aboutFileStage.initModality(Modality.WINDOW_MODAL);

            aboutFileStage.setScene(new Scene(aboutPage));
            aboutFileStage.show();

        }catch (Exception e) { e.printStackTrace(); }
    }

    private void getRefBaseCurrencySelection(){
        if(txtXCCY.getText().length() == 6) {
            switch (selectedRefBaseCCY) {
                case "Ref Currency":
                    lblCurrency.textProperty().setValue(txtXCCY.getText(3, 6));
                    break;
                case "Base Currency":
                    lblCurrency.textProperty().setValue(txtXCCY.getText(0, 3));
                    break;
            }
        }
    }

    private void updateTable(){
        getSizeOfSpotTableSpreads();
        getSizeOfFwdTable();
        getFxSpotRate();
        populateSpotTable();
        populateForwardTable();
    }

    @FXML
    protected void setXccyError(){
        xccyError.setText("Invalid XCCY");
    }

    private void getSizeOfSpotTableSpreads(){
        tableSpotValues = new String[tblSpreads.getItems().size()];
        for(int i = 0; i < (tblSpreads.getItems().size()); ++i) {
            tableSpotValues[i] = String.valueOf(spreadColumn.getCellObservableValue(i).getValue());
        }
    }

    private void getSizeOfFwdTable(){
        tableForwardValues = new String[tblForwards.getItems().size()];
        for(int i = 0; i < (tblForwards.getItems().size()); ++i){
            tableForwardValues[i] = String.valueOf(forwardDateColumn.getCellObservableValue(i).getValue());
        }
    }

    @FXML
    private void handleGetRates(ActionEvent event) throws Exception {
        getSizeOfSpotTableSpreads();
        getSizeOfFwdTable();

        getBloombergSpotData();

        populateSpotTable();
        populateForwardTable();

        /*if(spotOrFwd.equals("Forward")) {
            if (!tblForwards.getItems().isEmpty()) {
                getSizeOfFwdTable();
            }
            populateForwardTable();
        }*/
    }

    @FXML
    private void getBloombergSpotData() throws Exception{
        determineSpotOrForward();

        fxCurrency = txtXCCY.getText();  //Get the requested currency from user

        BloombergSpotFwdRequest bloombergCall = new BloombergSpotFwdRequest();  //Establish call to Bloomberg class

        try {
            bloombergCall.run(fxCurrency);  //Establish an API request to get data from bloomberg

            fxRateAsk = bloombergCall.FxRateAsk; //Get the spot Ask rate
            fxRateBid = bloombergCall.FxRateBid; //Get the spot Bid rate
            spotSettleDates = bloombergCall.settleDate;  //Get the spot settle date
            fwdSettleDates = bloombergCall.fwdSettleDate;  //Get the forward settle dates
            currencyMaxDecimals = bloombergCall.maxDecimals;  //Get the max decimal places for the currency
            fxFwdPoints = bloombergCall.fwdPoints;  //Get the forward points

            spotValueDate.textProperty().setValue("Value Date: " + spotSettleDates[0]);  //Set the spot settle date in the app

            getFxSpotRate(); //Call to determine the appropriate rate to use

        }catch (StringIndexOutOfBoundsException e){
            txtXCCY.requestFocus();
            xccyError.setText("Input XCCY");
        }catch (ArrayIndexOutOfBoundsException eArray){
            eArray.printStackTrace();
        }catch(NotFoundException nfe){
            xccyError.setText("Invalid XCCY");
        }
    }

    @FXML
    private void determineSpotOrForward(){
        if(rbSpot.selectedProperty().getValue()){
            spotOrFwd = "Spot";
        }else spotOrFwd = "Forward";
    }

    @FXML
    protected void graphData(String value){
        LocalDateTime intradayDates[] = new LocalDateTime[arrayLength];
        LocalDate dates[] = new LocalDate[arrayLength];

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSS");

        //clearChartData();
        series.getData().clear();
        series = new XYChart.Series();

        //Set the yAxis to adjust for data
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(graphMinPrice);
        yAxis.setUpperBound(graphMaxPrice);
        yAxis.setTickUnit((yAxis.getUpperBound() - yAxis.getLowerBound())/4);

        //Set series of data points to a chart array
        for (int i = 0; i < arrayLength; ++i) {
            if (value.equals("Hourly")) {
                intradayDates[i] = (LocalDateTime.parse(datesHistorical[i].toString(), dtf)).minusHours(6);
                series.getData().add(new XYChart.Data(intradayDates[i].toString(), pricesHistorical[i]));
            }else {
                dates[i] = (LocalDate.parse(datesHistorical[i].toString(), df));
                series.getData().add(new XYChart.Data(dates[i].toString(), pricesHistorical[i]));
            }


        }

        //Add the array of data points to the chart
        try {
            chartHistory.getData().add(series);
        }catch(IllegalArgumentException e){ e.printStackTrace(); }
    }

    @FXML
    private void determineGraphRequest(String value) throws Exception{
        arrayLength = 0;
        datesHistorical = new Datetime[arrayLength];
        pricesHistorical = new double[arrayLength];
        graphMinPrice = 0;
        graphMaxPrice = 0;

        switch (value){
            case "Weekly":
                getBloombergHistorical();
                break;
            case "Daily":
                getBloombergHistorical();
                break;
            case "Hourly":
                getBloombergHistoricalIntraday();
                break;
        }
    }

    @FXML
    private void setDatesHistorical(Datetime[] dateArray){ datesHistorical = dateArray; }

    @FXML
    private void setPricesHistorical(double[] pricesArray){
        pricesHistorical = pricesArray;
    }

    @FXML
    private void setArrayLength(int graphLength){
        arrayLength = graphLength;
    }

    @FXML
    private void setGraphMinPrice(double minPrice){
        graphMinPrice = minPrice;
    }

    @FXML
    private void setGraphMaxPrice(double maxPrice){
        graphMaxPrice = maxPrice;
    }

    @FXML
    private void getBloombergHistorical() throws Exception{
        int arrayLengthDaily;
        Datetime datesDaily[];
        double pricesDaily[];
        double graphMinPriceDaily;
        double graphMaxPriceDaily;

        BloombergHistorical historical = new BloombergHistorical();

        try {
            historical.run(txtXCCY.getText(), cbChartType.getValue().toString());
            //System.out.println(cbChartType.getValue().toString());
        }catch (StringIndexOutOfBoundsException e){ e.printStackTrace(); }

        arrayLengthDaily = historical.numFields;
        datesDaily = historical.dateField;
        pricesDaily = historical.priceLast;
        graphMinPriceDaily = historical.getMinPrice() * 0.995;
        graphMaxPriceDaily = historical.getMaxPrice() * 1.005;

        setDatesHistorical(datesDaily);
        setPricesHistorical(pricesDaily);
        setArrayLength(arrayLengthDaily);
        setGraphMinPrice(graphMinPriceDaily);
        setGraphMaxPrice(graphMaxPriceDaily);
    }

    @FXML
    private void getBloombergHistoricalIntraday(){
        int arrayLengthIntraday;
        Datetime datesIntraday[];
        double pricesIntraday[];
        double graphMinPriceIntraday;
        double graphMaxPriceIntraday;

        BloombergIntraday historicalid = new BloombergIntraday();

        try{
            historicalid.run(txtXCCY.getText());
        }catch  (Exception e){ e.printStackTrace(); }

        arrayLengthIntraday = historicalid.numBars;
        datesIntraday = historicalid.dateField;
        pricesIntraday = historicalid.dataValues;
        graphMinPriceIntraday = historicalid.getMinPrice() * 0.999;
        graphMaxPriceIntraday = historicalid.getMaxPrice() * 1.001;

        setDatesHistorical(datesIntraday);
        setPricesHistorical(pricesIntraday);
        setArrayLength(arrayLengthIntraday);
        setGraphMinPrice(graphMinPriceIntraday);
        setGraphMaxPrice(graphMaxPriceIntraday);
    }

    @FXML
    private void getFxSpotRate(){
        if(rbBuy.selectedProperty().getValue()) {
            baseRate = fxRateAsk[0];
            //if(spotOrFwd.equals("Forward")) {fxFwdPoints = fwdPoints;}
        }else{
            /*if(tblSpreads.getItems().isEmpty()) {
                System.out.println(MULTIPLIER);
                MULTIPLIER = -MULTIPLIER;
            }*/
            baseRate = fxRateBid[0];
            //if(spotOrFwd.equals("Forward")) {fxFwdPoints = fxRateBid;}
        }

        DefaultValuesController defaultOptions = new DefaultValuesController();
        defaultOptions.defaultOptions();

        if(!txtXCCY.getText().contains(defaultOptions.getDefaultHomeCurrency())){
            if(rbBaseCCY.selectedProperty().getValue()) {
                homeCurrencyRate = 1 / fxRateAsk[1];
            }else{ homeCurrencyRate = fxRateAsk[0] / fxRateAsk[1]; }
        }else if(lblCurrency.getText().equals(defaultOptions.getDefaultHomeCurrency())){
            homeCurrencyRate = 1 / fxRateAsk[0];
        } else homeCurrencyRate = 1;

    }

    @FXML
    private void populateChartOptions(){
        cbChartType.getItems().add("Weekly");
        cbChartType.getItems().add("Daily");
        cbChartType.getItems().add("Hourly");
    }

    private void determineBaseAmountRequest(){
        //Get user input for the amount requested

        try {
            baseAmount = Double.valueOf(txtAmount.getText().replaceAll(",", ""));
        }catch (Exception e){
            baseAmount = 0.0;
        }

        if(rbBaseCCY.selectedProperty().getValue()){
            baseAmount = baseAmount * baseRate;
        }else {
            baseAmount = baseAmount / baseRate;
        }
    }

    private String calculateSpotRevenue(double adjustedAmount, double amount, double homeRate){
        FormatData format = new FormatData();

        revenue = (adjustedAmount - amount) * homeRate;
        stringRevenue = format.RoundTo2Decimals(Math.abs(revenue));

        return stringRevenue;
    }

    private String calculateFwdRevenue(double adjustedAmount, double amount, double homeRate, double spread){
        FormatData format = new FormatData();
        revenue = (adjustedAmount - amount / baseRate * (baseRate + spread / numToDivideFwdPoints)) * homeRate;
        stringRevenue = format.RoundTo2Decimals(Math.abs(revenue));

        return stringRevenue;
    }

    @FXML
    private void populateSpotTable(){
        FormatData format = new FormatData();
        tblSpreads.getItems().clear(); //Clear table of any existing data

        determineBaseAmountRequest();  //Get base amount requested by user

        spotData = tblSpreads.getItems();
        //for (double i = 0; i <= (MAX_INCREMENT); i = i + INCREMENTS) {
        for(int i = 0; i < tableSpotValues.length; ++i){
            stringSpread = tableSpotValues[i];

            spreadRate = calculateSpotRate(Double.valueOf(tableSpotValues[i].replaceAll("%",""))/100);
            stringSpreadRate = determineDecimalRounding(spreadRate, "Spot");    //Round rate to appropriate decimal place

            if(rbRefCCY.selectedProperty().getValue()){
                spreadRate = 1 / spreadRate;
            }

            try {
                adjustedAmount = Double.parseDouble(txtAmount.getText().replaceAll(",", "")) * spreadRate;
            }catch(NullPointerException e){
                adjustedAmount = 0.0;
            }catch(NumberFormatException nf){
                adjustedAmount = 0.0;
            }

            stringAmount = format.RoundTo2Decimals(adjustedAmount);

            stringRevenue = calculateSpotRevenue(adjustedAmount, baseAmount, homeCurrencyRate);

            spotData.add(new CustomerSpreads(stringSpread, stringSpreadRate, stringAmount, stringRevenue));

        }
    }

    @FXML
    private void addCustomSpread(String customRequest) {
        FormatData format = new FormatData();

        determineBaseAmountRequest();

        if (customRequest.equals("Spread")) {
            stringSpread = txtCustomSpotSpread.getText().replaceAll("%", "");
            spreadRate = calculateSpotRate(Double.parseDouble(stringSpread) / 100);
            stringSpreadRate = determineDecimalRounding(spreadRate, "Spot");
        } else if (customRequest.equals("Rate")) {
            spreadRate = Double.valueOf(txtCustomFxRate.getText());
            stringSpreadRate = determineDecimalRounding(spreadRate, "Spot");
            stringSpread = format.PercentFormat(spreadRate / baseRate - 1);
        }

        if (rbRefCCY.selectedProperty().getValue()) {
            spreadRate = 1 / spreadRate;
        }

        adjustedAmount = Double.parseDouble(txtAmount.getText().replaceAll(",", "")) * spreadRate;
        stringAmount = format.RoundTo2Decimals(adjustedAmount);

        stringRevenue = calculateSpotRevenue(adjustedAmount, baseAmount, homeCurrencyRate);

        if (customRequest.equals("Spread")) {
            stringSpread = format.PercentFormat(Double.parseDouble(stringSpread) / 100);
        }

        spotData = tblSpreads.getItems();
        spotData.add(new CustomerSpreads(stringSpread, stringSpreadRate, stringAmount, stringRevenue));

        tblSpreads.sort();
    }

    @FXML
    private void populateForwardTable(){
        FormatData format = new FormatData();
        int maxIncrement = 18;
        int additionalForwards = 0;
        int newFwdsCounter = 0;
        double forwardPoints = 0.0;

        tblForwards.getItems().clear();

        if(currencyMaxDecimals == 2){
            numToDivideFwdPoints = 100;
        }else numToDivideFwdPoints = 10000;

        if(txtForwardSpread.getText().isEmpty()){
            dblForwardSpread = 0.0;
        }else {
            dblForwardSpread = Double.parseDouble(txtForwardSpread.getText().replaceAll("%", "")) / 100;
        }

        if(rbSell.selectedProperty().getValue()) {
            dblForwardSpread = -dblForwardSpread;
            buySellProperty = 0;  //Set to get the Bid
        }else{
            dblForwardSpread = Math.abs(dblForwardSpread);
            buySellProperty = 2;  //Set to get the Ask
        }

        determineBaseAmountRequest();

        if(rbRefCCY.selectedProperty().getValue()){
            baseRate = 1 / baseRate;
        }

        fwdData = tblForwards.getItems();

        if (tableForwardValues.length == 0) {
            additionalForwards = 0;
        }else{
            additionalForwards = tableForwardValues.length - maxIncrement;
        }

        for(int i = 0; i < (maxIncrement + additionalForwards); ++i){

            if(tableForwardValues.length == 0){
                forwardPoints = fxFwdPoints[0][i][buySellProperty];
            }else {
                if (tableForwardValues[i].equals(fwdSettleDates[0][i - newFwdsCounter].toString())) {
                    forwardPoints = fxFwdPoints[0][i - newFwdsCounter][buySellProperty];
                } else {
                    forwardPoints = calculateCustomForward(i + newFwdsCounter);
                    newFwdsCounter = newFwdsCounter + 1;
                }
            }

            BigDecimal roundedRate = new BigDecimal((baseRate + forwardPoints / numToDivideFwdPoints)
                    * (1 + dblForwardSpread));
            dblForwardRates = roundedRate.setScale(currencyMaxDecimals + 2, BigDecimal.ROUND_HALF_UP).doubleValue();

            strForwardRates = determineDecimalRounding(dblForwardRates, "Forward");    //Round to appropriate decimal place

            /*if(rbRefCCY.selectedProperty().getValue()){
                dblForwardRates = 1 / dblForwardRates;
            }*/

            try {
                dblForwardAmounts = Double.parseDouble(txtAmount.getText().replaceAll(",", "")) * dblForwardRates;
            }catch(NumberFormatException e){
                dblForwardAmounts = 0.0;
            }

            strForwardAmounts = format.RoundTo2Decimals(dblForwardAmounts);

            stringRevenue = calculateFwdRevenue(dblForwardAmounts, baseAmount, homeCurrencyRate, forwardPoints);

            if(tableForwardValues.length > 0){
                fwdData.add(new ForwardRates(tableForwardValues[i], forwardPoints, strForwardRates,
                        strForwardAmounts, stringRevenue));
            }else {
                fwdData.add(new ForwardRates(fwdSettleDates[0][i].toString(), forwardPoints, strForwardRates,
                        strForwardAmounts, stringRevenue));
            }
        }
    }

    @FXML
    private void addCustomForward(){
        FormatData format = new FormatData();
        double fwdSpread;

        fwdSpread = Double.valueOf(format.RoundTo2Decimals(getCustomForward()));

        fwdData = tblForwards.getItems();

        BigDecimal roundedRate = new BigDecimal((baseRate + fwdSpread / numToDivideFwdPoints) * (1 + dblForwardSpread));
        dblForwardRates = roundedRate.setScale(currencyMaxDecimals + 2, BigDecimal.ROUND_HALF_UP).doubleValue();

        strForwardRates = determineDecimalRounding(dblForwardRates, "Forward");    //Round to appropriate decimal place

        if(rbRefCCY.selectedProperty().getValue()){
            dblForwardRates = 1 / dblForwardRates;
        }

        try {
            dblForwardAmounts = Double.parseDouble(txtAmount.getText().replaceAll(",", "")) * dblForwardRates;
        }catch (NumberFormatException e){ }

        strForwardAmounts = format.RoundTo2Decimals(dblForwardAmounts);

        stringRevenue = calculateFwdRevenue(dblForwardAmounts, baseAmount, homeCurrencyRate, fwdSpread);

        fwdData.add(new ForwardRates(cdrForwardDate.getValue().toString(), fwdSpread, strForwardRates,
                strForwardAmounts, stringRevenue));

        tblForwards.sort();
    }

    private String determineDecimalRounding(double dblRate, String spotFwd){
        String strRate;
        int forwardMaxDecimals;
        FormatData format = new FormatData();

        if (spotFwd.equals("Forward")) { forwardMaxDecimals = currencyMaxDecimals + 2; }
        else forwardMaxDecimals = currencyMaxDecimals;

        switch (forwardMaxDecimals) {
            case 1:
                strRate = format.RoundTo1Decimals(dblRate);
                break;
            case 2:
                strRate = format.RoundTo2Decimals(dblRate);
                break;
            case 3:
                strRate = format.RoundTo3Decimals(dblRate);
                break;
            case 4:
                strRate = format.RoundTo4Decimals(dblRate);
                break;
            case 5:
                strRate = format.RoundTo5Decimals(dblRate);
                break;
            case 6:
                strRate = format.RoundTo6Decimals(dblRate);
                break;
            default:
                strRate = format.RoundTo4Decimals(dblRate);
                break;
        }

        return strRate;
    }

    @FXML
    protected void clearChartData(){
        chartHistory.getData().clear();
        series.getData().clear();
    }

    private void resetBuySellSpotFwd(){
        rbSpot.selectedProperty().setValue(true);
        rbBuy.selectedProperty().setValue(true);
    }

    private void resetData(){
        txtXCCY.clear();
        txtAmount.clear();
        rbBaseCCY.selectedProperty().setValue(true);
        cdrForwardDate.setValue(null);
        tblSpreads.getItems().clear();
        tblForwards.getItems().clear();
        lblCurrency.setText("");
        chartHistory.getData().clear();
        series.getData().clear();
        spotValueDate.textProperty().setValue("Spot Value Date: ");
        txtForwardSpread.clear();
        MULTIPLIER = Math.abs(MULTIPLIER);

        addInitialSpotSpreads();
    }

    @FXML
    protected void handleReset(ActionEvent event){
        resetBuySellSpotFwd();
        resetData();
    }

    private double calculateSpotRate(double spread){
        BigDecimal roundedRate = new BigDecimal(baseRate * (1 + spread * MULTIPLIER));
        spreadRate = roundedRate.setScale(currencyMaxDecimals, BigDecimal.ROUND_HALF_UP).doubleValue();

        return spreadRate;
    }

    private void addInitialSpotSpreads(){

        FormatData format = new FormatData();

        ObservableList<CustomerSpreads> spotData = tblSpreads.getItems();
        for (double i = 0.0; i <= (MAX_INCREMENT); i = i + INCREMENTS) {
            stringSpread = format.PercentFormat(i);
            spotData.add(new CustomerSpreads(stringSpread));
        }
    }

    private double calculateCustomForward(int i){
        InterpolationMethods interpolate = new InterpolationMethods();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        setBuySellProperty();

        fxCustomFwdPts = interpolate.linearInterpolation(LocalDate.parse(tableForwardValues[i-1].toString(), dtf),
                LocalDate.parse(tableForwardValues[i+1].toString(), dtf), LocalDate.parse(tableForwardValues[i].toString(), dtf),
                fxFwdPoints[0][i-1][buySellProperty], fxFwdPoints[0][i][buySellProperty]);

        return fxCustomFwdPts;
    }


    private double getCustomForward(){
        InterpolationMethods interpolate = new InterpolationMethods();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        setBuySellProperty();

        for(int i = 1; i < fwdSettleDates[0].length; ++i){

            if (cdrForwardDate.getValue().compareTo(LocalDate.parse(fwdSettleDates[0][i].toString(), dtf)) == 0){
                //fwd points = fxForwardPoints
                fxCustomFwdPts = fxFwdPoints[0][i][buySellProperty];

            }else if (cdrForwardDate.getValue().compareTo(LocalDate.parse(fwdSettleDates[0][i].toString(), dtf)) < 0){

                fxCustomFwdPts = interpolate.linearInterpolation(LocalDate.parse(fwdSettleDates[0][i-1].toString(), dtf),
                        LocalDate.parse(fwdSettleDates[0][i].toString(), dtf), cdrForwardDate.getValue(),
                        fxFwdPoints[0][i-1][buySellProperty], fxFwdPoints[0][i][buySellProperty]);

                i = fwdSettleDates[0].length;
            }
        }

        return fxCustomFwdPts;
    }

    private void setBuySellProperty(){
        if(rbSell.selectedProperty().getValue()) {
            buySellProperty = 0;  //Set to get the Bid
        }else{
            buySellProperty = 2;  //Set to get the Ask
        }
    }
}
