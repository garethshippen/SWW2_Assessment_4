/*
 * Client.java
 *
 * A client for accessing the books database
 * A naive JavaFX for connecting to the database server and interact
 * with the database.
 *
 * author: 720547
 *
 */


import java.io.*;
import java.util.List;
import java.util.ArrayList;

import java.lang.ClassNotFoundException;
import java.lang.IndexOutOfBoundsException;

import java.net.Socket;
import java.net.UnknownHostException;

import java.sql.*;

import javax.sql.rowset.CachedRowSet;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;



public class BooksDatabaseClient extends Application {

	public static BooksDatabaseClient me; //Get the application instance in javafx
	public static Stage thePrimaryStage;  //Get the application primary scene in javafx
    private Socket clientSocket = null;

    private String userCommand = null; //The user command
    private CachedRowSet serviceOutcome = null; //The service outcome


	//Convenient to populate the TableView
	public class MyTableRecord {
		private StringProperty title;
		private StringProperty publisher;
		private StringProperty genre;
		private StringProperty rrp;
		private StringProperty copyID;
		
		public void setTitle(String value) { titleProperty().set(value); }
		public String getTitle() { return titleProperty().get(); }
		public void setPublisher(String value) { publisherProperty().set(value); }
		public String getPublisher() { return publisherProperty().get(); }
		public void setGenre(String value) { genreProperty().set(value); }
		public String getGenre() { return genreProperty().get(); }
		public void setRrp(String value) { rrpProperty().set(value); }
		public String getRrp() { return rrpProperty().get(); }
		public void setCopyID(String value) { copyIDProperty().set(value); }
		public String getCopyID() { return copyIDProperty().get(); }


		public StringProperty titleProperty() { 
			if (title == null)
				title = new SimpleStringProperty(this, "");
			return title; 
		}
		public StringProperty publisherProperty() { 
			if (publisher == null)
				publisher = new SimpleStringProperty(this, "");
			return publisher; 
		}
		public StringProperty genreProperty() { 
			if (genre == null)
				genre = new SimpleStringProperty(this, "");
			return genre; 
		}
		public StringProperty rrpProperty() { 
			if (rrp == null)
				rrp = new SimpleStringProperty(this, "");
			return rrp; 
		}
		public StringProperty copyIDProperty() { 
			if (copyID == null)
				copyID = new SimpleStringProperty(this, "");
			return copyID; 
		}
				
	}

    //Class Constructor
    public BooksDatabaseClient(){
		
		me=this;
    }

	//Initializes the client socket using the credentials from class Credentials.
	public void initializeSocket(){

		try
		{
			clientSocket = new Socket(Credentials.HOST, Credentials.PORT);
		}
		catch(UnknownHostException e)
		{
			System.out.println("Client: Unknown host. " + e);
		}
		catch(IOException e){
			System.out.println("Client: I/O error. " + e);
		}
	}


    public void requestService() {
        try {
            System.out.println("Client: Requesting books database service for user command\n" + this.userCommand +"\n");

			OutputStreamWriter requestWriter = new OutputStreamWriter(clientSocket.getOutputStream());
			requestWriter.write(userCommand + "#");
			requestWriter.flush();
			//requestWriter.close();

        }catch(IOException e){
            System.out.println("Client: I/O error. " + e);
        }
    }
	


    public void reportServiceOutcome() {
        try {

			ObjectInputStream outcomeReader = new ObjectInputStream(clientSocket.getInputStream());
			serviceOutcome = (CachedRowSet) outcomeReader.readObject();

			while(serviceOutcome.next())
			{
				System.out.println(serviceOutcome.getString("title") + " | "
						+ serviceOutcome.getString("publisher") + " | "
						+ serviceOutcome.getString("genre") + " | "
						+ serviceOutcome.getDouble("rrp") + " | "
						+ serviceOutcome.getInt("copies"));
			}
			serviceOutcome.beforeFirst();
			GridPane grid = (GridPane) thePrimaryStage.getScene().getRoot();
			ObservableList<Node> childrens = grid.getChildren();
			TableView<MyTableRecord> outputBox = (TableView) childrens.get(5);
			ObservableList<MyTableRecord> tmp = outputBox.getItems();
			tmp.clear();

			while(serviceOutcome.next())
			{
				MyTableRecord tempRecord = new MyTableRecord();
				tempRecord.setTitle(serviceOutcome.getString("title"));
				tempRecord.setPublisher(serviceOutcome.getString("publisher"));
				tempRecord.setGenre(serviceOutcome.getString("genre"));
				tempRecord.setRrp((serviceOutcome.getDouble("rrp")) + "");
				tempRecord.setCopyID(serviceOutcome.getInt("copies") + "");
				tmp.add(tempRecord);
			}
			System.out.println("\n");

			System.out.println(tmp +"\n====================================\n");
        }catch(IOException e){
            System.out.println("Client: I/O error. " + e);
        }catch(ClassNotFoundException e){
            System.out.println("Client: Unable to cast read object to CachedRowSet. " + e);
        }catch(SQLException e){
            System.out.println("Client: Can't retrieve requested attribute from result set. " + e);
        }
    }

    //Execute client
    public void execute(){
 		GridPane grid = (GridPane) thePrimaryStage.getScene().getRoot();
		ObservableList<Node> childrens = grid.getChildren();
		TextField authorInputBox = (TextField) childrens.get(1);
		TextField libraryInputBox = (TextField) childrens.get(3);


		//Build user message command
		userCommand = authorInputBox.getText().strip() + ";" + libraryInputBox.getText().strip();


        //Request service
        try{

			//Initializes the socket
			this.initializeSocket();

            //Request service
            this.requestService();

            //Report user outcome of service
            this.reportServiceOutcome();

            //Close the connection with the server
            this.clientSocket.close();

        }catch(Exception e)
        {// Raised if connection is refused or other technical issue
            System.out.println("Client: Exception " + e);
        }
    }




    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Books Database Client");
		
		//Create a GridPane container
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(5);
		grid.setHgap(5);

		
		//Add the input boxes
		Label authorLabel = new Label("Author's Surname:");
		GridPane.setConstraints(authorLabel, 0, 0);
		grid.getChildren().add(authorLabel);		
		
		TextField authorInputBox = new TextField ();
		authorInputBox.setPromptText("Author's Surname:");
		authorInputBox.setPrefColumnCount(30);
		GridPane.setConstraints(authorInputBox, 1, 0);
		grid.getChildren().add(authorInputBox);		
		
		Label libraryLabel = new Label("Library's city:");
		GridPane.setConstraints(libraryLabel, 0, 1);
		grid.getChildren().add(libraryLabel);		
		
		TextField libraryInputBox = new TextField ();
		libraryInputBox.setPromptText("Library's city:");
		libraryInputBox.setPrefColumnCount(30);
		GridPane.setConstraints(libraryInputBox, 1, 1);
		grid.getChildren().add(libraryInputBox);		
		
		//Add the service request button
        Button btn = new Button();
        btn.setText("Request Books Database Service");
        btn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
				me.execute();
            }
        });        
		GridPane.setConstraints(btn, 0, 2, 2, 1);
		grid.getChildren().add(btn);
		
		//Add the output box
		TableView<MyTableRecord> outputBox = new TableView<MyTableRecord>();
		TableColumn<MyTableRecord,String> titleCol     = new TableColumn<MyTableRecord,String>("Title");
		TableColumn<MyTableRecord,String> publisherCol = new TableColumn<MyTableRecord,String>("Publisher");
		TableColumn<MyTableRecord,String> genreCol     = new TableColumn<MyTableRecord,String>("Genre");
		TableColumn<MyTableRecord,String> rrpCol       = new TableColumn<MyTableRecord,String>("RRP");
		TableColumn<MyTableRecord,String> copyIDCol    = new TableColumn<MyTableRecord,String>("Num. Copies");
		titleCol.setCellValueFactory(new PropertyValueFactory("title"));
		publisherCol.setCellValueFactory(new PropertyValueFactory("publisher"));
		genreCol.setCellValueFactory(new PropertyValueFactory("genre"));
		rrpCol.setCellValueFactory(new PropertyValueFactory("rrp"));
		copyIDCol.setCellValueFactory(new PropertyValueFactory("copyID"));


		@SuppressWarnings("unchecked") ObservableList<TableColumn<MyTableRecord,?>> tmp = outputBox.getColumns();
		tmp.addAll(titleCol, publisherCol, genreCol, rrpCol, copyIDCol);
			//Leaving this type unchecked by now... It may be convenient to compile with -Xlint:unchecked for details.

		GridPane.setConstraints(outputBox, 0, 3, 2, 1);
		grid.getChildren().add(outputBox);		

		//Adjust gridPane's columns width
		ColumnConstraints col1 = new ColumnConstraints();
		col1.setPercentWidth(25);
	    ColumnConstraints col2 = new ColumnConstraints();
		col2.setPercentWidth(75);
	    grid.getColumnConstraints().addAll( col1, col2);

        primaryStage.setScene(new Scene(grid, 505, 505));
        primaryStage.show();
		
		
		thePrimaryStage = primaryStage;

		
    }




    public static void main (String[] args) {
		launch(args);
        System.out.println("Client: Finished.");
        System.exit(0);
    }
}
