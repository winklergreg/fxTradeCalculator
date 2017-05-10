/**
 * Created by GW on 10/12/16.
 */

package FxTradeCalc;

import com.bloomberglp.blpapi.*;

public class BloombergSpotFwdRequest {

    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name SECURITY = new Name("security");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name FIELD_EXCEPTIONS = new Name("fieldExceptions");
    private static final Name FIELD_ID = new Name("fieldId");
    private static final Name ERROR_INFO = new Name("errorInfo");
    private static final Name MAX_DECIMALS = new Name("PX_DISP_FORMAT_MAX_NUM_DEC");
    protected double FxRateBid[];
    protected double FxRateAsk[];
    protected Datetime settleDate[];
    protected Datetime fwdSettleDate[][];
    protected int maxDecimals;
    protected int numSecurities;
    protected String securityID;
    protected String fwdSecurityName[][];
    protected double fwdPoints[][][];

    public static void main(String[] args) throws Exception {
        BloombergSpotFwdRequest example = new BloombergSpotFwdRequest();
        example.run("EURUSD");
        System.out.println("Press ENTER to quit");
        System.in.read();
    }

    protected void run(String requestedCurrency) throws Exception {
        CorrelationID d_cid;

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
        Request request = refDataService.createRequest("ReferenceDataRequest");

        // append securities to request
        Element securities = request.getElement("securities");

        securities.appendValue(requestedCurrency + " Curncy");
        numSecurities = 1;

        DefaultValuesController defaultOptions = new DefaultValuesController();
        defaultOptions.defaultOptions();
        if(!requestedCurrency.contains(defaultOptions.getDefaultHomeCurrency())){
            securities.appendValue(defaultOptions.getDefaultHomeCurrency() + requestedCurrency.substring(3) + " Curncy");
            numSecurities = 2;
        }

        FxRateBid = new double[numSecurities];
        FxRateAsk = new double[numSecurities];
        settleDate = new Datetime[numSecurities];
        fwdSecurityName = new String[numSecurities][18];
        fwdSettleDate = new Datetime[numSecurities][18];
        fwdPoints = new double[numSecurities][18][3];

        // append fields to request
        Element fields = request.getElement("fields");
        fields.appendValue("PX_BID");
        fields.appendValue("PX_ASK");
        fields.appendValue("SETTLE_DT");
        fields.appendValue("PX_DISP_FORMAT_MAX_NUM_DEC");
        fields.appendValue("FWD_CURVE");

        d_cid = session.sendRequest(request, null);

        int k = 0;
        while (true) {
            Event event = session.nextEvent();
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                if (msg.correlationID() == d_cid) {
                    processMessage(msg, k);
                    ++k;
                }
            }
            if (event.eventType() == Event.EventType.RESPONSE) {
                break;
            }
        }
    }

    protected void processMessage(Message msg, int k) throws Exception {
        Element securityDataArray = msg.getElement(SECURITY_DATA);

        int numItems = securityDataArray.numValues();

        for (int i = 0; i < numItems; ++i) {

            Element securityData = securityDataArray.getValueAsElement(i);
            Element fieldData = securityData.getElement(FIELD_DATA);
            Element fwdCurve = fieldData.getElement("FWD_CURVE");

            FxRateBid[k] = fieldData.getElementAsFloat64("PX_BID");
            FxRateAsk[k] = fieldData.getElementAsFloat64("PX_ASK");
            settleDate[k] = fieldData.getElementAsDate("SETTLE_DT");

            for (int j = 0; j < fwdCurve.numValues(); ++j) {
                Element forwardData = fwdCurve.getValueAsElement(j);
                fwdSecurityName[k][j] = forwardData.getElementAsString("Security Description");
                fwdSettleDate[k][j] = forwardData.getElementAsDatetime("Settlement Date");
                fwdPoints[k][j][0] = forwardData.getElementAsFloat64("Bid");
                fwdPoints[k][j][1] = forwardData.getElementAsFloat64("Mid");
                fwdPoints[k][j][2] = forwardData.getElementAsFloat64("Ask");
            }

            if (k == 0) {
                maxDecimals = fieldData.getElementAsInt32("PX_DISP_FORMAT_MAX_NUM_DEC");
            }

        }

/*       for(int i = 0; i< 18; ++i){
            System.out.println(securityName[i] + " Settle: " + settleDate[i] + " Bid: " + fwdPoints[i][0]);
       }*/

    }

    protected void setSecurityName(String value) {securityID = value.substring(0,6);}
    protected String getSecurityName(){return securityID;}
}
