/**
 * Created by GW on 10/12/16.
 */

package FxTradeCalc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TradeCalculator extends Application {

    protected int primaryStageHeight = 635;
    protected int primaryStageWidth = 825;

    @Override
    public void start(Stage primaryStage) throws Exception{

        try {
            Parent root = FXMLLoader.load(getClass().getResource("../Resources/FxCalculator.fxml"));
            //primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("fxLogo.png")));
            primaryStage.setTitle("Greg Winkler's FX Calculator");
            primaryStage.setScene(new Scene(root, primaryStageWidth, primaryStageHeight));
            primaryStage.show();

        }catch (IOException e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
