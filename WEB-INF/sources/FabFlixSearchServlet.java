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
    }
    
    public void doGetBrowseByMovieTitle(HttpServletRequest request, HttpServletResponse response, 
    	String orderColumn, String orderType,
    	String offset, String limit) 
    		throws IOException, ServletException {
    	String character = request.getParameter("starts_with");
    	
    	StringWriter out = new StringWriter();
    	JSONArray result = new JSONArray();
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
}
