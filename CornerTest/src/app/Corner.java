package app;
import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Corner extends Application {

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
		
	}

	@Override
	public void start(Stage arg0) throws Exception {
		// TODO Auto-generated method stub
		FXMLLoader loader = new FXMLLoader(getClass().getResource("View.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root, 950, 500);
		
		arg0.setScene(scene);
		arg0.show();
	}

}
