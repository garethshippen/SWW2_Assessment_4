/*
 * BooksDatabaseService.java
 *
 * The service threads for the books database server.
 * This class implements the database access service, i.e. opens a JDBC connection
 * to the database, makes and retrieves the query, and sends back the result.
 *
 * author: 720547
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
//import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.Socket;

import java.util.StringTokenizer;

import java.sql.*;
import javax.sql.rowset.*;
    //Direct import of the classes CachedRowSet and CachedRowSetImpl will fail becuase
    //these clasess are not exported by the module. Instead, one needs to impor
    //javax.sql.rowset.* as above.



public class BooksDatabaseService extends Thread{

    private Socket serviceSocket = null;
    private String[] requestStr  = new String[2]; //One slot for author's name and one for library's name.
    private ResultSet outcome   = null;

	//JDBC connection
    private String USERNAME = Credentials.USERNAME;
    private String PASSWORD = Credentials.PASSWORD;
    private String URL      = Credentials.URL;



    //Class constructor
    public BooksDatabaseService(Socket aSocket){

        serviceSocket = aSocket;
        this.start();
    }


    //Retrieve the request from the socket
    public String[] retrieveRequest()
    {
        this.requestStr[0] = ""; //For author
        this.requestStr[1] = ""; //For library
		
		String tmp = "";
        try {

            InputStream incomingStream = serviceSocket.getInputStream();
            InputStreamReader streamReader = new InputStreamReader(incomingStream);
            StringBuffer buffer = new StringBuffer();
            final char TERMINATOR = '#';
            final String DELIMITER = ";";
            char currentCharacter;
            boolean execute = true;

            while(execute)
            {
                currentCharacter = (char) streamReader.read();
                if(currentCharacter == TERMINATOR)
                {
                    execute = false;
                    continue;
                }
                buffer.append(currentCharacter);
            }
            requestStr = buffer.toString().split(DELIMITER, 2);
			
         }catch(IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        return this.requestStr;
    }


    //Parse the request command and execute the query
    public boolean attendRequest()
    {
        boolean flagRequestAttended = true;
		
		this.outcome = null;

        String sql = "SELECT title, publisher, genre, rrp, COUNT(*) AS copies FROM bookcopy INNER JOIN book ON bookcopy.bookid = book.bookid INNER JOIN library ON bookcopy.libraryid = library.libraryid INNER JOIN author ON book.authorid = author.authorid WHERE author.familyname ILIKE ? AND library.city ILIKE ? GROUP BY title, publisher, genre, rrp;";


		try {
			//Connet to the database

            ResultSet tempRecords = null;
			Connection bookDB = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			//Make the query

            PreparedStatement preparedQuery = bookDB.prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            preparedQuery.clearParameters();
            preparedQuery.setString(1, requestStr[0]); //author
            preparedQuery.setString(2, requestStr[1]); //city
            tempRecords = preparedQuery.executeQuery();

            RowSetFactory factory = RowSetProvider.newFactory();
            CachedRowSet outcome = factory.createCachedRowSet();
            outcome.populate(tempRecords);
            this.outcome = outcome;



            //Process query

            tempRecords.beforeFirst();
            while (tempRecords.next())
            {
                System.out.println(tempRecords.getString("title") + " | "
                        + tempRecords.getString("publisher") + " | "
                        + tempRecords.getString("genre") + " | "
                        + tempRecords.getDouble("rrp") + " | "
                        + tempRecords.getInt("copies"));
            }
            System.out.println("\n");

            //Clean up

            tempRecords.close();
            preparedQuery.close();
            bookDB.close();

		} catch (Exception e)
		{ System.out.println(e);}

        return flagRequestAttended;
    }



    //Wrap and return service outcome
    public void returnServiceOutcome(){
        try {
			//Return outcome

            ObjectOutputStream outcomeStream = new ObjectOutputStream(serviceSocket.getOutputStream());
			outcomeStream.writeObject(outcome);

            System.out.println("Service thread " + this.getId() + ": Service outcome returned; " + this.outcome);
            outcomeStream.flush();
            outcomeStream.close();
			//Terminating connection of the service socket

			serviceSocket.close();
			
        }catch (IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
    }


    //The service thread run() method
    public void run()
    {
		try {
			System.out.println("\n============================================\n");
            //Retrieve the service request from the socket
            this.retrieveRequest();
            System.out.println("Service thread " + this.getId() + ": Request retrieved: "
						+ "author->" + this.requestStr[0] + "; library->" + this.requestStr[1]);

            //Attend the request
            boolean tmp = this.attendRequest();

            //Send back the outcome of the request
            if (!tmp)
                System.out.println("Service thread " + this.getId() + ": Unable to provide service.");
            this.returnServiceOutcome();

        }catch (Exception e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        //Terminate service thread (by exiting run() method)
        System.out.println("Service thread " + this.getId() + ": Finished service.");
    }

}
