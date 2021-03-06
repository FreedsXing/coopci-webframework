package gubo.http.grizzly;

import gubo.db.ISimplePoJo;
import gubo.exceptions.ApiException;
import gubo.exceptions.BadParameterException;
import gubo.exceptions.QueryStringParseException;
import gubo.exceptions.RequiredParameterException;
import gubo.exceptions.SessionNotFoundException;
import gubo.http.querystring.QueryStringBinder;
import gubo.jdbc.mapping.InsertStatementGenerator;
import gubo.session.SessonManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.jtwig.JtwigTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class NannyHttpHandler extends HttpHandler {
	SessonManager sessonManager = new SessonManager();

	public SessonManager getNannySessionManager() {
		return this.sessonManager;
	}

	public void send(Object ret, Request req, Response res) throws Exception {
		if (ret instanceof ModelAndTemplate) {
			send((ModelAndTemplate) ret, req, res);
		} else if (ret instanceof String) {
			String s = (String) ret;
			send(s, req, res);
		} else if (ret instanceof ISentAsJson) {
			send((ISentAsJson) ret, req, res);
		} else {
			this.sendJson(ret, req, res);
		}
	}

	public void serveHead(Request req, Response res) throws Exception {
		Object ret = this.doHead(req, res);
		this.send(ret, req, res);
	}

	public void serveGet(Request req, Response res) throws Exception {
		Object ret = this.doGet(req, res);
		this.send(ret, req, res);
	}

	public void servePost(Request req, Response res) throws Exception {
		Object ret = this.doPost(req, res);
		this.send(ret, req, res);
	}

	public void servePut(Request req, Response res) throws Exception {
		Object ret = this.doPut(req, res);
		this.send(ret, req, res);
	}

	public void serveDelete(Request req, Response res) throws Exception {
		Object ret = this.doDelete(req, res);
		this.send(ret, req, res);
	}

	@Override
	public void service(Request req, Response res) throws Exception {
		req.setCharacterEncoding("utf-8");
		Method method = req.getMethod();

		try {
			if (method.getMethodString().equals("GET")) {
				this.serveGet(req, res);
			} else if (method.getMethodString().equals("POST")) {
				this.servePost(req, res);
			} else if (method.getMethodString().equals("HEAD")) {
				this.serveHead(req, res);
			} else if (method.getMethodString().equals("PUT")) {
				this.servePut(req, res);
			} else if (method.getMethodString().equals("DELETE")) {
				this.serveDelete(req, res);
			}

		} catch (Exception ex) {
			this.handleException(ex, req, res);
		}
	}

	public long getRequiredLongParameter(Request request, String pname)
			throws RequiredParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.length() == 0)
			throw new RequiredParameterException(pname);
		try {
			return Long.parseLong(ret);
		} catch (Exception e) {
			throw new RequiredParameterException(pname);
		}
	}

	public double getRequiredDoubleParameter(Request request, String pname)
			throws RequiredParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.length() == 0)
			throw new RequiredParameterException(pname);
		try {
			return Double.parseDouble(ret);
		} catch (Exception e) {
			throw new RequiredParameterException(pname);
		}
	}

	/**
	 * 获取double类型的参数，如果参数不存在返回defaultValue。如果参数错误，
	 * throwExceptionWhenError为true时抛出异常，
	 * throwExceptionWhenError为false时返回defaultValue。
	 * 
	 * @param request
	 * @param pname
	 * @param defaultValue
	 * @param throwExceptionWhenError
	 * @return
	 * @throws BadParameterException
	 */
	public double getDoubleParameter(Request request, String pname,
			double defaultValue, boolean throwExceptionWhenError)
			throws BadParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.isEmpty()) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(ret);
		} catch (Exception e) {
			if (throwExceptionWhenError) {
				throw new BadParameterException("Bad parameter '" + pname + "'");
			}
		}
		return 0.0;
	}

	/**
	 * 获取long类型的参数，如果参数不存在返回defaultValue。如果参数错误，
	 * throwExceptionWhenError为true时抛出异常，
	 * throwExceptionWhenError为false时返回defaultValue。
	 * 
	 * @param request
	 * @param pname
	 * @param defaultValue
	 * @param throwExceptionWhenError
	 * @return
	 * @throws BadParameterException
	 */
	public long getLongParameter(Request request, String pname,
			long defaultValue, boolean throwExceptionWhenError)
			throws BadParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(ret);
		} catch (Exception e) {
			if (throwExceptionWhenError) {
				throw new BadParameterException("Bad parameter '" + pname + "'");
			}
		}
		return 0L;
	}

	public long getLongParameter(Request request, String pname)
			throws BadParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.isEmpty()) {
			throw new BadParameterException("Bad parameter '" + pname + "'");
		}
		try {
			return Long.parseLong(ret);
		} catch (Exception e) {
			throw new BadParameterException("Bad parameter '" + pname + "'");
		}
	}

	public String getRequiredStringParameter(Request request, String pname)
			throws RequiredParameterException {
		return getRequiredStringParameter(request, pname, false);
	}

	public String getRequiredStringParameter(Request request, String pname,
			boolean trim) throws RequiredParameterException {
		String ret = request.getParameter(pname);
		if (ret == null || ret.length() == 0)
			throw new RequiredParameterException(pname);
		return ret.trim();
	}

	public String getTrimedStringParameter(Request request, String pname)
			throws RequiredParameterException {
		String ret = request.getParameter(pname);
		if (ret != null)
			ret = ret.trim();
		return ret;
	}

	/**
	 * 获取string类型的参数值，如果为null或空字符串，则返回缺省值。
	 * 
	 * @param request
	 * @param pname
	 * @param defaultValue
	 * @return
	 */
	public String getStringParameter(Request request, String pname,
			String defaultValue) {
		String ret = request.getParameter(pname);
		if (ret == null || ret.isEmpty()) {
			return defaultValue;
		}
		return ret;
	}

	public ObjectMapper getJsonObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		return objectMapper;
	}

	public void send(ModelAndTemplate mat, Request req, Response response)
			throws IOException {
		JtwigTemplate template = mat.getTemplate();
		response.setContentType("text/html; charset=UTF-8");
		String content = template.render(mat.getModel());
		response.getWriter().write(content);
	}

	public void send(String s, Request req, Response response)
			throws IOException {
		response.setContentType("text/html; charset=UTF-8");
		response.getWriter().write(s);
	}

	public void send(ISentAsJson obj, Request req, Response response)
			throws Exception {
		this.setCrossDomain(response);
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json");
		response.getWriter().write(obj.toJson());
	}

	public void sendJson(Object o, Request req, Response response)
			throws IOException {
		if (o == null) {
			response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
		}

		ObjectMapper mapper = getJsonObjectMapper();
		String content = mapper.writeValueAsString(o);
		this.setCrossDomain(response);
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json");
		response.getWriter().write(content);
	}

	/*
	 * IConnectionProvider connectionProvider = null; public IConnectionProvider
	 * getConnectionProvider() { return connectionProvider; } public void
	 * setConnectionProvider(IConnectionProvider connectionProvider) {
	 * this.connectionProvider = connectionProvider; }
	 */
	public Connection getConnection() throws SQLException {
		return null;
	}

	// 从req里找sessid，并返回关联的 user id。
	// 如果找不到就抛异常。
	public Long requireLogin(Request req) throws RequiredParameterException,
			NoSuchAlgorithmException, SQLException, SessionNotFoundException {
		String sess_id = this.getRequiredStringParameter(req, "sess_id");
		Connection dbconn = this.getConnection();
		dbconn.setAutoCommit(true);
		try {
			return this.getNannySessionManager().get(dbconn, sess_id, true);
		} finally {
			dbconn.close();
		}
	}

	// subclasses should implement these doXXX methods. The returned Object is
	// serialized by this class and the status code will always be 200 upon
	// successful doXXX.
	public Object doHead(Request req, Response res) throws Exception {
		return null;
	}

	public Object doGet(Request req, Response res) throws Exception {
		return null;
	}

	public Object doPost(Request req, Response res) throws Exception {
		return null;
	}

	public Object doPut(Request req, Response res) throws Exception {
		return null;
	}

	public Object doDelete(Request req, Response res) throws Exception {
		return null;
	}

	public void setCrossDomain(Response response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	public HashMap<String, Object> getOKResponse() {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		// ret.put("code", "200");
		ret.put("status", 200);
		ret.put("message", "OK");
		HashMap<String, Object> data = new HashMap<String, Object>();
		ret.put("data", data);
		return ret;
	}

	public HashMap<String, Object> getErrorResponse(int error, String message) {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("code", error);
		ret.put("message", message);
		return ret;
	}

	public void handleException(Exception ex, Request req, Response res)
			throws Exception {
		if (ex instanceof MySQLIntegrityConstraintViolationException) {
			this.handleException(
					(MySQLIntegrityConstraintViolationException) ex, req, res);
		} else if (ex instanceof SessionNotFoundException) {
			this.handleException((SessionNotFoundException) ex, req, res);
		} else if (ex instanceof QueryStringParseException) {
			this.handleException((QueryStringParseException) ex, req, res);
		} else if (ex instanceof ApiException) {
			this.handleException((ApiException) ex, req, res);
		} else {
			throw ex;
		}

		return;
	}

	public void handleException(QueryStringParseException ex, Request req,
			Response res) throws Exception {
		String msg = ex.getMessage();
		HashMap<String, Object> ret = getErrorResponse(400, msg);
		ret.put("message", msg);
		ret.put("exception-handler", NannyHttpHandler.class);
		ret.put("handler", this.getClass().toString());
		res.setStatus(200);
		this.sendJson(ret, req, res);
		return;
	}

	public void handleException(ApiException ex, Request req, Response res)
			throws Exception {
		String msg = ex.getMessage();
		HashMap<String, Object> ret = getErrorResponse(ex.getCode(), msg);
		ret.put("message", msg);
		ret.put("exception-handler", NannyHttpHandler.class);
		ret.put("handler", this.getClass().toString());
		res.setStatus(ex.getHttpStatus());
		this.sendJson(ret, req, res);
		return;
	}

	public void handleException(MySQLIntegrityConstraintViolationException ex,
			Request req, Response res) throws Exception {
		if (ex.getSQLState().equals("23000")) {
			String msg = ex.getMessage();
			HashMap<String, Object> ret = getErrorResponse(500, msg);
			ret.put("handler", NannyHttpHandler.class);
			this.sendJson(ret, req, res);
			return;
		}
		throw ex;
	}

	public void handleException(SessionNotFoundException ex, Request req,
			Response res) throws Exception {

		String msg = ex.getMessage();
		HashMap<String, Object> ret = getErrorResponse(500, msg);
		ret.put("message", "SessionNotFound");
		ret.put("sess_id", ex.getSessid());
		ret.put("handler", NannyHttpHandler.class);
		this.sendJson(ret, req, res);
		return;
	}

	public HashMap<String, Object> createSimplePojo(Request request,
			Class<? extends ISimplePoJo> clazz) throws Exception {

		ISimplePoJo newPojo = clazz.newInstance();

		QueryStringBinder binder = new QueryStringBinder();
		binder.bind(request, newPojo);

		Connection dbconn = this.getConnection();
		try {
			dbconn.setAutoCommit(false);

			Long newid = InsertStatementGenerator.insertNew(dbconn, newPojo);
			newPojo.setId(newid);
			dbconn.commit();

			HashMap<String, Object> ret = this.getOKResponse();
			ret.put("data", newPojo);
			return ret;

		} catch (Exception ex) {
			dbconn.rollback();
			throw ex;
		} finally {
			dbconn.close();
		}
	}

	public boolean needLogin = true;

	public void checkPermission(Long uid) throws Exception {

	}

	public Long authCheck(Request request) throws Exception {
		if (this.needLogin) {
			Long uid = this.requireLogin(request);
			this.checkPermission(uid);
			return uid;
		}
		return null;
	}

	public void bindParameter(Request req, Object p) throws Exception {

		// TODO test content-type to call the right binder.
		final QueryStringBinder binder = new QueryStringBinder();
		binder.bind(req, p);
		return;
	}

}
