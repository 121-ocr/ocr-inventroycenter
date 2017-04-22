package ocr.inventorycenter.pharseinv;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 根据来源id,是否存在采购入库单id，要求方：采购订单
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class QueryPharseInvByResHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "queryByRes";

	public QueryPharseInvByResHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject query = new JsonObject();
		query.put("bo.detail.resid", msg.body().getString("bo_id"));

		FindOptions findOptions = new FindOptions();
		findOptions.setFields(new JsonObject().put("_id", true));
		JsonObject rs = new JsonObject();
		this.queryLatestFactDataList(null, appActivity.getBizObjectType(), "created", null, query, null, findRet -> {
			if (findRet.succeeded()) {
				List<JsonObject> retObj = findRet.result();
				if (retObj != null && retObj.size() > 0) {
					rs.put("result", true);
					msg.reply(rs);
				} else {
					rs.put("result", false);
					msg.reply(rs);
				}
			} else {
				Throwable err = findRet.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				msg.fail(500, errMsg);
			}

		});

	}

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
