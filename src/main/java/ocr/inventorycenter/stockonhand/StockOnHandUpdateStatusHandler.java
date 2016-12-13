package ocr.inventorycenter.stockonhand;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：现存量-更新存量状态
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandUpdateStatusHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "update_status";

	public StockOnHandUpdateStatusHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject so = msg.body();
		
		JsonObject query = new JsonObject().put("biz_data_type", so.getString("biz_data_type"))
				.put("bo_id", so.getString("bo_id")).put("status", so.getString("from_status"));
		
		JsonObject updateObj = new JsonObject()
				.put("$set", new JsonObject().put("status", so.getString("to_status")));

		// 设置存在更新，不存在添加
		appActivity.getAppDatasource().getMongoClient().updateCollection(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query, updateObj, res -> {
					if (res.succeeded()) {
						MongoClientUpdateResult updateRet = res.result();						
						msg.reply(updateRet.toJson());
					} else {
						Throwable errThrowable = res.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
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
