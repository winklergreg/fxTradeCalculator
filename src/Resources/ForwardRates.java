package Resources;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Created by GW on 10/19/16.
 */
public class ForwardRates {
    private final SimpleStringProperty forwardDate = new SimpleStringProperty();
    private final SimpleDoubleProperty forwardPoints = new SimpleDoubleProperty();
    private final SimpleStringProperty forwardRate = new SimpleStringProperty();
    private final SimpleStringProperty forwardAmount = new SimpleStringProperty();
    private final SimpleStringProperty forwardRevenue = new SimpleStringProperty();

    public ForwardRates(){this("NA",0.0, "0.00","0.00","0.00");}

    public ForwardRates(String forwardDate, double forwardPoints, String forwardRate, String forwardAmount, String forwardRevenue){
        setForwardDate(forwardDate);
        setForwardPoints(forwardPoints);
        setForwardRate(forwardRate);
        setForwardAmount(forwardAmount);
        setForwardRevenue(forwardRevenue);
    }

    public String getForwardDate(){return forwardDate.get();}
    public void setForwardDate(String vName){forwardDate.set(vName);}

    public double getForwardPoints(){return forwardPoints.get();}
    public void setForwardPoints(double vName){forwardPoints.set(vName);}

    public String getForwardRate(){return forwardRate.get();}
    public void setForwardRate(String vName){forwardRate.set(vName);}

    public String getForwardAmount(){return forwardAmount.get();}
    public void setForwardAmount(String vName){forwardAmount.set(vName);}

    public String getForwardRevenue(){return forwardRevenue.get();}
    public void setForwardRevenue(String vName){forwardRevenue.set(vName);}

}
