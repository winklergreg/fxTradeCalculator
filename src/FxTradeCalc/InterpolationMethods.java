package FxTradeCalc;

/**
 * Created by GW on 10/24/16.
 */

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class InterpolationMethods {
    double interpolatedPoint;

    public double linearInterpolation(LocalDate t0, LocalDate t1, LocalDate ti, double fwdPt0, double fwdPt1){

        interpolatedPoint = (fwdPt0 + (ChronoUnit.DAYS.between(t0, ti)) * (fwdPt1 - fwdPt0) / (ChronoUnit.DAYS.between(t0, t1)));

        /*interpolatedPoint = (fwdPt0 * (1 - ((double) ChronoUnit.DAYS.between(t0, ti) / ChronoUnit.DAYS.between(t0, t1))))
                            + (fwdPt1  * ((double) ChronoUnit.DAYS.between(t0, ti) / ChronoUnit.DAYS.between(t0, t1)));
        */

        return interpolatedPoint;
    }

    public double exponentialInterpolation(LocalDate t0, LocalDate t1, LocalDate ti, double fwdPt0, double fwdPt1){

        interpolatedPoint = (fwdPt1 * ((double) ChronoUnit.DAYS.between(t0, ti) / ChronoUnit.DAYS.between(t0, t1))
                * ((double) t1.toEpochDay() / ti.toEpochDay())) + (fwdPt0 * ((double) ChronoUnit.DAYS.between(ti, t1)
                / ChronoUnit.DAYS.between(t0, t1)) * ((double) t0.toEpochDay() / ti.toEpochDay()));

        return interpolatedPoint;
    }

    public double splineInterpolation(double value){
        int i = 0;
        if (i==0){
            //calc endpoint
        }else {

        }

        return value;
    }

    private void splineEndPoint(){

    }

}
