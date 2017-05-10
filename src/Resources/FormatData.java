package Resources;

import java.text.DecimalFormat;

/**
 * Created by GW on 10/27/16.
 */
public class FormatData {

    public String RoundTo6Decimals(double value){
        DecimalFormat df6 = new DecimalFormat("#,##0.000000");
        return df6.format(value);
    }

    public String RoundTo5Decimals(double value){
        DecimalFormat df5 = new DecimalFormat("#,##0.00000");
        return df5.format(value);
    }

    public String RoundTo4Decimals(double value){
        DecimalFormat df4 = new DecimalFormat("#,##0.0000");
        return df4.format(value);
    }

    public String RoundTo3Decimals(double value){
        DecimalFormat df3 = new DecimalFormat("#,##0.000");
        return df3.format(value);
    }

    public String RoundTo2Decimals(double value){
        DecimalFormat df2 = new DecimalFormat("#,##0.00");
        return df2.format(value);
    }

    public String RoundTo1Decimals(double value){
        DecimalFormat df1 = new DecimalFormat("#,##0.0");
        return df1.format(value);
    }

    public String PercentFormat(double value){
        DecimalFormat percentFormat = new DecimalFormat("0.00%");
        return percentFormat.format(value);
    }

}
