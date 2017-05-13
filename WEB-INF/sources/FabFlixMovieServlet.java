

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Servlet implementation class FabFlixMovieServlet
 */
@WebServlet("/servlet/FabFlixMovieServlet")
public class FabFlixMovieServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FabFlixMovieServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String movieIdParameter = request.getParameter("movieId");
		Integer movieId = null;
		StringWriter out = new StringWriter();
		JSONObject result = new JSONObject();
		FabFlixRESTManager restManager;
		
		if (movieIdParameter == null) {
			out.flush();
			out.write("{ \"error\": \"NoIDPassedError\", \"message\": \"ID not provided, unable to execute query.\" }");
			response.getWriter().write(out.toString() + "\n");
			out.close();
			return;
		}
		try { movieId = Integer.parseInt(movieIdParameter); }
		catch (NumberFormatException e) { 
			out.flush();
			out.write("{ \"error\": \"" + e.getClass().getName() + "\", \"message\": \"" + e.getMessage() + "\" }");
			response.getWriter().write(out.toString() + "\n");
			out.close();
			return;
		}
		
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		result = restManager.getMovieById(request.getSession(true).getId(), movieId);
    		
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
		String title = request.getParameter("title");
		String year = request.getParameter("year");
		String director = request.getParameter("director");
		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String genre = request.getParameter("genre");
		String type = request.getParameter("type");
		StringWriter out = new StringWriter();
		JSONObject result = new JSONObject();
		FabFlixRESTManager restManager;
		
		if (!type.equals("_add")) {
			doGet(request, response);
			return;
		}
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		result = restManager.addMovie(title, year, director, firstName, lastName, genre);
    		
    		if (out.toString().isEmpty())
    			out.write(result.toString());
    		restManager.closeConnection();
    		
    	} catch (Exception e) {
    		out.flush();
    		out.write("{ \"success\": false, \"message\": \"" + e.getMessage() + "\" }");
    	}
    	
    	response.getWriter().write(out.toString() + "\n");
    	out.close();
	}

}
