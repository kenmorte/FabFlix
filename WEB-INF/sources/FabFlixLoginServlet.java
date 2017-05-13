import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;


public class FabFlixLoginServlet extends HttpServlet {
	
	public static final String SITE_KEY ="6Leu5iAUAAAAAFdRxiacm59jkVVNCb9nV1wWNZiR";
    public static final String SECRET_KEY ="6Leu5iAUAAAAAKLfQs7_is6cwOwbGANo_-gyHpf4";
	
    public String getServletInfo() {
        return "Servlet connects to MySQL database and authenticates user login";
    }
    
    // HTTP Post
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
    	String email = request.getParameter("email");
    	String password = request.getParameter("password");
    	String type = request.getParameter("type");
    	String recaptchaResponse = request.getParameter("recaptchaResponse");
    	StringWriter out = new StringWriter();
    	JSONObject result = new JSONObject();
    	FabFlixRESTManager restManager;
    	
    	response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    	response.addHeader("Access-Control-Allow-Origin","*");
    	response.addHeader("Access-Control-Allow-Methods","GET,POST");
        response.addHeader("Access-Control-Allow-Headers","Origin, X-Requested-With, Content-Type, Accept");
        
    	// Verify CAPTCHA.
    	if (type != null && type.equals("_user") && !FabFlixVerifyUtils.verify(recaptchaResponse)) {
    	    out.write("{\"error\": \"Recaptcha Error\", \"message\": \"Recaptcha unverified. Please try again.\"}");
    	    response.getWriter().write(out.toString() + "\n");
        	out.close();
    	    return;
    	}
        
    	try {
    		restManager = new FabFlixRESTManager(out);
    		restManager.attemptConnection();
    		
    		if (type.equals("_user")) {
        		JSONObject userJSON = restManager.getUserInfo(email, password);
        		result.put("error", "null");
        		if (userJSON != null) {
            		result.put("user", userJSON);
            		result.put("message", "success");
            		
        		} else {
        			result.remove("user");
        			result.put("message", "Invalid e-mail/password combination.");
        		}
    		} else if (type.equals("_dashboard")) {
    			result = restManager.getDashboardLogin(email, password);
    		}
    		
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
