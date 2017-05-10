/**
 * Created by GW on 10/16/16.
 */

package FxTradeCalc;

import com.bloomberglp.blpapi.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Calendar;

public class BloombergIntraday {

    private static final Name BAR_DATA       = new Name("barData");
    private static final Name BAR_TICK_DATA  = new Name("barTickData");
    private static final Name OPEN           = new Name("open");
    private static final Name HIGH           = new Name("high");
    private static final Name LOW            = new Name("low");
    private static final Name CLOSE          = new Name("close");;
    private static final Name PX_LAST        = new Name("px_last");
    private static final Name TIME           = new Name("time");
    private static final Name RESPONSE_ERROR = new Name("responseError");
    private static final Name CATEGORY       = new Name("category");
    private static final Name MESSAGE        = new Name("message");

    private static String     security;
    private String            d_host;
    private int               d_port;
    private String            d_security;
    private String            d_eventType;
    private int               d_barInterval;
    private boolean           d_gapFillInitialBar;
    private Datetime          d_startDateTime;
    private Datetime          d_endDateTime;
    DateTimeFormatter         d_dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private DecimalFormat     d_decimalFormat;
    protected static double minPrice;
    protected static double maxPrice;
    protected int numBars;
    protected static double dataValues[];
    protected static Datetime dateField[];
    protected String stringDate[];

    public static void main(String[] args) throws Exception {

        BloombergIntraday example = new BloombergIntraday();
        example.run("EURUSD");
    }

    protected void run(String requestedSecurity) throws Exception {

        d_host = "localhost";
        d_port = 8194;
        d_barInterval = 60;
        d_security = requestedSecurity + " Curncy";
        d_eventType = "TRADE";
        d_gapFillInitialBar = false;
        //d_dateFormat = new SimpleDateFormat();
        //d_dateFormat.applyPattern("MM/dd/yyyy k:mm");
        d_decimalFormat = new DecimalFormat();
        d_decimalFormat.setMaximumFractionDigits(4);

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(d_host);
        sessionOptions.setServerPort(d_port);

        Session session = new Session(sessionOptions);
        if (!session.start()) {
            System.err.println("Failed to start session.");
            return;
        }
        if (!session.openService("//blp/refdata")) {
            System.err.println("Failed to open //blp/refdata");
            return;
        }

        sendIntradayBarRequest(session);

        // wait for events from session.
        eventLoop(session);
        session.stop();
    }

    private Calendar getPreviousTradingDate()
    {
        Calendar prevDate = Calendar.getInstance();
        prevDate.roll(Calendar.DAY_OF_MONTH, 0);
        if (prevDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            prevDate.roll(Calendar.DAY_OF_MONTH, -3);
        }
        else if (prevDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            prevDate.roll(Calendar.DAY_OF_MONTH, -2);
        }
        prevDate.set(Calendar.HOUR_OF_DAY, 0);
        prevDate.set(Calendar.MINUTE, 0);
        prevDate.set(Calendar.SECOND, 0);
        return prevDate;
    }

    private void eventLoop(Session session) throws Exception {
        boolean done = false;
        while (!done) {
            Event event = session.nextEvent();
            if (event.eventType() == Event.EventType.PARTIAL_RESPONSE) {
                processResponseEvent(event, session);
            }
            else if (event.eventType() == Event.EventType.RESPONSE) {
                processResponseEvent(event, session);
                done = true;
            } else {
                MessageIterator msgIter = event.messageIterator();
                while (msgIter.hasNext()) {
                    Message msg = msgIter.next();
                    if (event.eventType() == Event.EventType.SESSION_STATUS) {
                        if (msg.messageType().equals("SessionTerminated")) {done = true;}
                    }
                }
            }
        }
    }

    protected void processMessage(Message msg) throws Exception {
        Element data = msg.getElement(BAR_DATA).getElement(BAR_TICK_DATA);
        numBars = data.numValues();

        //System.out.println(msg);

        dateField = new Datetime[numBars];
        dataValues = new double[numBars];
        stringDate = new String[numBars];

        for (int i = 0; i < numBars; ++i) {
            Element bar = data.getValueAsElement(i);
            dateField[i] = bar.getElementAsDate(TIME);
            dataValues[i] = bar.getElementAsFloat64(CLOSE);
            if (i == 0) {
                minPrice = dataValues[i];
                maxPrice = dataValues[i];
            }else{
                if (dataValues[i] < minPrice){minPrice = dataValues[i];}
                if (dataValues[i] > maxPrice){maxPrice = dataValues[i];}
            }
        }
        setMinPrice(minPrice);
        setMaxPrice(maxPrice);
    }

    protected void processResponseEvent(Event event, Session session) throws Exception {
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            if (msg.hasElement(RESPONSE_ERROR)) {
                TradeCalcController error = new TradeCalcController();
                error.setXccyError();
                printErrorInfo("REQUEST FAILED: ", msg.getElement(RESPONSE_ERROR));
                continue;
            }
            processMessage(msg);
        }
    }

    protected void sendIntradayBarRequest(Session session) throws Exception
    {
        Service refDataService = session.getService("//blp/refdata");
        Request request = refDataService.createRequest("IntradayBarRequest");

        // only one security/eventType per request
        request.set("security", d_security);
        request.set("eventType", d_eventType);
        request.set("interval", d_barInterval);

        if (d_startDateTime == null || d_endDateTime == null) {
            Calendar calendar = getPreviousTradingDate();
            Datetime prevTradeDateTime = new Datetime(calendar);

            // set the end date for next day
            calendar.roll(Calendar.DAY_OF_MONTH, +2);
            Datetime endDateTime = new Datetime(calendar);

            request.set("startDateTime", prevTradeDateTime);
            request.set("endDateTime", endDateTime);
        }
        else {
            request.set("startDateTime", d_startDateTime);
            request.set("endDateTime", d_endDateTime);
        }

        if (d_gapFillInitialBar) {request.set("gapFillInitialBar", d_gapFillInitialBar);}

        session.sendRequest(request, null);
    }

    private void printErrorInfo(String leadingStr, Element errorInfo) throws Exception {
        System.out.println(leadingStr + errorInfo.getElementAsString(CATEGORY) + " (" + errorInfo.getElementAsString(MESSAGE) + ")");
    }

    protected void setSecurity(String value){}
    protected String getSecurity() {return security;}

    protected void setMinPrice(double price){}
    protected double getMinPrice(){return minPrice;}

    protected void setMaxPrice(double price){}
    protected double getMaxPrice(){return maxPrice;}

}
