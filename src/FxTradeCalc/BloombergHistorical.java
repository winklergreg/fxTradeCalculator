package FxTradeCalc;

/**
 * Created by GW on 10/1/16.
 */

import com.bloomberglp.blpapi.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BloombergHistorical {
    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name SECURITIES = new Name("securities");
    private static final Name SECURITY = new Name("security");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name FIELD_EXCEPTIONS = new Name("fieldExceptions");
    private static final Name FIELD_ID = new Name("fieldId");
    private static final Name ERROR_INFO = new Name("errorInfo");
    private static final Name PX_LAST = new Name("px_last");
    private static final Name DATE = new Name("date");
    protected static double priceLast[];
    protected static String historicalSecurities[];
    protected static Datetime dateField[];
    protected static int numSecurities;
    protected static int numFields;
    protected static double minPrice;
    protected static double maxPrice;
    protected Datetime dates;

    public static void main(String[] args) throws Exception
    {
        BloombergHistorical example = new BloombergHistorical();

        example.run("EURUSD", "DAILY");
        example.showData();
        System.out.println("Press ENTER to quit");
        System.in.read();
    }

    protected void run(String requestedSecurity, String periodType) throws Exception
    {
        String serverHost = "localhost";
        int serverPort = 8194;

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(serverHost);
        sessionOptions.setServerPort(serverPort);

        Session session = new Session(sessionOptions);
        if (!session.start()) {
            System.err.println("Failed to start session.");
            return;
        }
        if (!session.openService("//blp/refdata")) {
            System.err.println("Failed to open //blp/refdata");
            return;
        }
        Service refDataService = session.getService("//blp/refdata");
        Request request = refDataService.createRequest("HistoricalDataRequest");

        Element securities = request.getElement("securities");
        securities.appendValue(requestedSecurity + " Curncy");

        numSecurities = securities.numValues();
        historicalSecurities = new String[numSecurities];

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Calendar calNow = Calendar.getInstance();
        Calendar calPrior = Calendar.getInstance();
        String todayDate = dateFormat.format(calNow.getTime());
        calPrior.setTime(calNow.getTime());
        calPrior.add(Calendar.YEAR, -1);
        String yearPrior = dateFormat.format(calPrior.getTime());

        Element fields = request.getElement("fields");
        fields.appendValue("px_last");

        request.set("periodicityAdjustment", "ACTUAL");
        request.set("periodicitySelection", periodType.toUpperCase());
        request.set("startDate", yearPrior);
        request.set("endDate", todayDate);
        request.set("maxDataPoints", 365);
        request.set("returnEids", true);

        session.sendRequest(request, null);

        boolean continueLoop = true;
        while (continueLoop) {
            Event event = session.nextEvent();
            BloombergHistorical getMSG = new BloombergHistorical();
            switch(event.eventType().intValue())
            {
                case Event.EventType.Constants.RESPONSE:
                    getMSG.processMessage(event, numSecurities);
                    continueLoop = false;
                    break;
                case Event.EventType.Constants.PARTIAL_RESPONSE:
                    getMSG.processMessage(event, numSecurities);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processMessage(Event event, int numSecurities) throws Exception {
        MessageIterator msgIter = event.messageIterator();
        Message msg = msgIter.next();
        //System.out.println(msg);

        Element securityDataArray = msg.getElement("securityData");
        Element securityName = securityDataArray.getElement("security");
        String security = securityName.getValueAsString();

        Element fieldData = securityDataArray.getElement(FIELD_DATA);

        numFields = fieldData.numValues();
        dateField = new Datetime[numFields];
        priceLast = new double[numFields];

        for (int j = 0; j < numFields; ++j) {
            if (securityDataArray.hasElement("securityError")) {
                Element securityError = securityDataArray.getElement("securityError");
                System.out.println("* security =" + security);
                securityError.print(System.out);
                return;
            } else {
                Element fieldValues = fieldData.getValueAsElement(j);
                dateField[j] = fieldValues.getElementAsDatetime(DATE);
                priceLast[j] = fieldValues.getElementAsFloat64(PX_LAST);
                if (j == 0) {
                    minPrice = priceLast[j];
                    maxPrice = priceLast[j];
                }else{
                    if (priceLast[j] < minPrice){minPrice = priceLast[j];}
                    if (priceLast[j] > maxPrice){maxPrice = priceLast[j];}
                }
            }

            Element fieldExceptionArray = securityDataArray.getElement(FIELD_EXCEPTIONS);
            for (int k = 0; k < fieldExceptionArray.numValues(); ++k) {
                Element fieldException = fieldExceptionArray.getValueAsElement(k);
                System.out.println(
                        fieldException.getElement(ERROR_INFO).getElementAsString("category")
                                + ": " + fieldException.getElementAsString(FIELD_ID));
            }
        }
        setMinPrice(minPrice);
        setMaxPrice(maxPrice);

    }

    protected void showData(){

        for (int j = 0; j < numFields; ++j) {
            System.out.println(dateField[j] + ": " + String.format("%,.4f", priceLast[j]));
        }
    }

    protected void setMinPrice(double price){}
    protected double getMinPrice(){return minPrice;}

    protected void setMaxPrice(double price){}
    protected double getMaxPrice(){return maxPrice;}

}