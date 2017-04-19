
/* A servlet to display the contents of the MySQL movieDB database */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;


public class FabFlixLoginServlet extends HttpServlet {
    public String getServletInfo() {
        return "Servlet connects to MySQL database and displays result of a SELECT";
    }
    
    // HTTP Post
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
    	String email = request.getParameter("email");
    	String password = request.getParameter("password");
    	StringWriter out = new StringWriter();
    	JSONObject result = new JSONObject();
    	FabFlixRESTManager restManager;
    	
    	response.setContentType("text/json");	// Response mime type
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		JSONObject userJSON = restManager.getUserInfo(email, password);
    		
    		result.put("error", "null");
    		if (userJSON != null) {
        		result.put("user", userJSON.toString());
        		result.put("message", "success");
        		
    		} else {
    			result.put("user", "null");
    			result.put("message", "Invalid e-mail/password combination.");
    		}

        	out.write(result.toString());
    		restManager.closeConnection();
    		
    	} catch (Exception e) {
    		out.flush();
    		out.write("{ error: '" + e.getClass().getName() + "', user: null, message: \"" + e.getMessage() + "' }");
    	}
    	
    	response.getWriter().write(out.toString() + "\n");
    	out.close();
    }

    // Use http GET
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String loginUser = "mytestuser";
        String loginPasswd = "mypassword";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("text/html"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        out.println("<HTML><HEAD><TITLE>MovieDB</TITLE></HEAD>");
        out.println("<BODY><H1>MovieDB</H1>");

        try {
            //Class.forName("org.gjt.mm.mysql.Driver");
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Connection dbcon = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            // Declare our statement
            Statement statement = dbcon.createStatement();

            String query = "SELECT * from stars";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            out.println("<TABLE border>");

            // Iterate through each row of rs
            while (rs.next()) {
                String m_id = rs.getString("id");
                String m_name = rs.getString("first_name") + " " + rs.getString("last_name");
                String m_dob = rs.getString("dob");
                out.println("<tr>" + "<td>" + m_id + "</td>" + "<td>" + m_name + "</td>" + "<td>" + m_dob + "</td>"
                        + "</tr>");
            }

            out.println("</TABLE>");

            rs.close();
            statement.close();
            dbcon.close();
        } catch (SQLException ex) {
            while (ex != null) {
                System.out.println("SQL Exception:  " + ex.getMessage());
                ex = ex.getNextException();
            } // end while
        } // end catch SQLException

        catch (java.lang.Exception ex) {
            out.println("<HTML>" + "<HEAD><TITLE>" + "MovieDB: Error" + "</TITLE></HEAD>\n<BODY>"
                    + "<P>SQL error in doGet: " + ex.getMessage() + "</P></BODY></HTML>");
            return;
        }
        out.close();
    }
}
