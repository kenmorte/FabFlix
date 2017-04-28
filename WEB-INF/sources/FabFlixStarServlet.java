

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Servlet implementation class FabFlixStarServlet
 */
@WebServlet("/servlet/FabFlixStarServlet")
public class FabFlixStarServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FabFlixStarServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String starIdParameter = request.getParameter("star_id");
		Integer starId = null;
		StringWriter out = new StringWriter();
		JSONObject result = new JSONObject();
		FabFlixRESTManager restManager;
		
		// Return an error message if no ID was passed
		if (starIdParameter == null) {
			out.flush();
			out.write("{ \"error\": \"NoIDPassedError\", \"message\": \"ID not provided, unable to execute query.\" }");
			response.getWriter().write(out.toString() + "\n");
			out.close();
			return;
		}
		
		// Get the Integer value for the parameter, return an error message if an error occurred (not a number)
		try { starId = Integer.parseInt(starIdParameter); }
		catch (NumberFormatException e) { 
			out.flush();
			out.write("{ \"error\": \"" + e.getClass().getName() + "\", \"message\": \"" + e.getMessage() + "\" }");
			response.getWriter().write(out.toString() + "\n");
			out.close();
			return;
		}
		
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		result = restManager.getStarById(starId);
    		
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
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
