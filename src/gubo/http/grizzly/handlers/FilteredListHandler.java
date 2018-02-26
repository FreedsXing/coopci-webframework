package gubo.http.grizzly.handlers;

import gubo.db.ISimplePoJo;
import gubo.http.grizzly.ApiHttpHandler;
import gubo.http.querystring.QueryStringBinder;
import gubo.http.querystring.QueryStringField;
import gubo.jdbc.mapping.ResultSetMapper;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * 列出被@Entity的类对应的表中的数据。 带有筛选功能，筛选功能用 {@link QueryStringBinder } 的 genJDBCWhere
 * 实现。 作为筛选的字段需要用 {@link QueryStringField} 标注才行。
 **/
public class FilteredListHandler extends ApiHttpHandler {

	Class<? extends ISimplePoJo> clazz;

	public FilteredListHandler(Class<? extends ISimplePoJo> clazz) {
		this.clazz = clazz;
	}

	protected final Object doFilter(Map<String, String> params)
			throws Exception {

		Connection dbconn = this.getConnection();
		try {
			dbconn.setAutoCommit(true);

			List<?> data = ResultSetMapper.loadPoJoList(dbconn, clazz, params);
			HashMap<String, Object> ret = this.getOKResponse();
			ret.put("data", data);
			return ret;

		} finally {
			dbconn.close();
		}
	}

	/**
	 * Subclasses can override this method to do custom check, add extra
	 * conditions, then call super.doGet
	 * 
	 **/
	@Override
	public Object doGet(Request request, Response response) throws Exception {
		this.authCheck(request);
		Map<String, String> conditions = QueryStringBinder
				.extractParameters(request);
		Object ret = this.doFilter(conditions);
		return ret;
	}

}
