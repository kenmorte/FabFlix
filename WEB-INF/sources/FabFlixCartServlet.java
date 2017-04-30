

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Servlet implementation class FabFlixCartServlet
 */
@WebServlet("/servlet/FabFlixCartServlet")
public class FabFlixCartServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FabFlixCartServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String actionType = request.getParameter("action_type");
		String movieId = request.getParameter("movieId");
		String amount = request.getParameter("amount");
		String sessionId = request.getSession(true).getId();
		
		if (actionType == null)
			return;
		
    	StringWriter out = new StringWriter();
    	JSONObject result = new JSONObject();
    	FabFlixRESTManager restManager;
    	
    	response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		
    		if (actionType.equalsIgnoreCase("UPDATE_CART"))
	    		result = restManager.updateMovieInCart(
	    			sessionId, 
	    			Integer.parseInt(movieId), 
	    			Integer.parseInt(amount)
	    		);
    		else if (actionType.equalsIgnoreCase("GET_CART"))
	    		result = restManager.getCartData(sessionId);

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
