package Resources;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by GW on 10/14/16.
 */


public class CustomerSpreads {
    private final SimpleStringProperty spotSpread = new SimpleStringProperty("0.00");
    private final SimpleStringProperty spotFxRate = new SimpleStringProperty("0.00");
    private final SimpleStringProperty spotAmount = new SimpleStringProperty("0.00");
    private final SimpleStringProperty spotRevenue = new SimpleStringProperty("0.00");

    public CustomerSpreads(){
        setSpotFxRate("0.00");
        setSpotAmount("0.00");
        setSpotRevenue("0.00");
    }

    public CustomerSpreads(String spotSpread){
        setSpotSpread(spotSpread);
    }

    public CustomerSpreads(String spotFxRate, String spotAmount, String spotRevenue){
        setSpotFxRate(spotFxRate);
        setSpotAmount(spotAmount);
        setSpotRevenue(spotRevenue);
    }

    public CustomerSpreads(String spotSpread, String spotFxRate, String spotAmount, String spotRevenue){
        setSpotSpread(spotSpread);
        setSpotFxRate(spotFxRate);
        setSpotAmount(spotAmount);
        setSpotRevenue(spotRevenue);
    }

    public String getSpotSpread(){return spotSpread.get();}
    public void setSpotSpread(String vName){spotSpread.set(vName);}
    public StringProperty spotSpreadProperty(){
        return spotSpread;
    }

    public String getSpotFxRate(){return spotFxRate.get();}
    public void setSpotFxRate(String vName){spotFxRate.set(vName);}

    public String getSpotAmount(){return spotAmount.get();}
    public void setSpotAmount(String vName){spotAmount.set(vName);}

    public String getSpotRevenue(){return spotRevenue.get();}
    public void setSpotRevenue(String vName){spotRevenue.set(vName);}

}
