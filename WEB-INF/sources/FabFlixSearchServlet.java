import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;

@WebServlet("/servlet/FabFlixSearchServlet")
public class FabFlixSearchServlet extends HttpServlet {
    public String getServletInfo() {
        return "Servlet connects to MySQL database and searches for movie information";
    }
    
    // HTTP Get
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	String queryType = request.getParameter("query_type");
    	String orderColumn = request.getParameter("order_by");
    	String orderType = request.getParameter("order_type");
    	String offset = request.getParameter("offset");
    	String limit = request.getParameter("limit");
    	
    	if (queryType == null)
    		return;
    	if (queryType.equalsIgnoreCase("BROWSE_BY_MOVIE_TITLE"))
    		doGetBrowseByMovieTitle(request, response, orderColumn, orderType, offset, limit);
    	else if (queryType.equalsIgnoreCase("BROWSE_BY_MOVIE_GENRE"))
    		doGetBrowseByMovieGenre(request, response, orderColumn, orderType, offset, limit);
    }
    
    /**
     * Completes the request for browsing movies by first character.
     * 
     * @param request	HTTP request from user
     * @param response	HTTP response back to user
     * @param orderColumn	order column to sort result by
     * @param orderType		order type to sort result by
     * @param offset	offset of results from table
     * @param limit		limit of results to be displayed
     * @throws IOException	if there was an error parsing the response/request
     * @throws ServletException	if there was an error with the server
     */
    public void doGetBrowseByMovieTitle(HttpServletRequest request, HttpServletResponse response, 
    	String orderColumn, String orderType,
    	String offset, String limit) 
    		throws IOException, ServletException {
    	String character = request.getParameter("starts_with");
    	
    	StringWriter out = new StringWriter();
    	JSONObject result = new JSONObject();
    	FabFlixRESTManager restManager;
    	
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		result = restManager.getMoviesByFirstCharacter(character, orderColumn, orderType, offset, limit);
    		
    		if (out.toString().isEmpty())
    			out.write(result.toString());
    		restManager.closeConnection();
    		
    	} catch (Exception e) {
    		out.flush();
    		out.write("{ \"error\": \"" + e.getClass().getName() + "\", \"message\": \"" + e.getMessage() + "\" }");
    	}
    	
    	response.getWriter().write(out.toString() + "\n");
    	out.close();
    }
    
    /**
     * Completes the request for browsing movies by genre.
     * 
     * @param request	HTTP request from user
     * @param response	HTTP response back to user
     * @param orderColumn	order column to sort result by
     * @param orderType		order type to sort result by
     * @param offset	offset of results from table
     * @param limit		limit of results to be displayed
     * @throws IOException	if there was an error parsing the response/request
     * @throws ServletException	if there was an error with the server
     */
    public void doGetBrowseByMovieGenre(HttpServletRequest request, HttpServletResponse response, 
    	String orderColumn, String orderType,
    	String offset, String limit) throws IOException, ServletException {
    	
    	String genre = request.getParameter("genre");
    	
    	StringWriter out = new StringWriter();
    	JSONObject result = new JSONObject();
    	FabFlixRESTManager restManager;
    	
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		result = restManager.getMoviesByGenre(genre, orderColumn, orderType, offset, limit);
    		
    		if (out.toString().isEmpty())
    			out.write(result.toString());
    		restManager.closeConnection();
    		
    	} catch (Exception e) {
    		out.flush();
    		out.write("{ \"error\": \"" + e.getClass().getName() + "\", \"message\": \"" + e.getMessage() + "\" }");
    	}
    	
    	response.getWriter().write(out.toString() + "\n");
    	out.close();
    }
}
