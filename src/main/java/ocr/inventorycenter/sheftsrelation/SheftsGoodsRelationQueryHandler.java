package ocr.inventorycenter.sheftsrelation;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：货位查询根据sku条件
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class SheftsGoodsRelationQueryHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = InvBusiOpenContant.SHEFTSLOCATIONSADDRESS;
	private static final String sku = "sku";

	public SheftsGoodsRelationQueryHandler(AppActivityImpl appActivity) {
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
		
		JsonObject query = msg.body();

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query, result -> {
					if (result.succeeded()) {
/*						JsonArray jsonArray = new JsonArray();
						String fileContent = result.result().toString();
						if (fileContent != null && !fileContent.isEmpty()) {
							JsonArray t = new JsonArray(fileContent);
							JsonObject tt = t.getJsonObject(0);
							jsonArray = tt.getJsonArray("allotLocations");
						}
						msg.reply(jsonArray);*/
						
						List<JsonObject> rets = result.result();
						if (rets != null && rets.size() > 0) {							
							JsonObject tt = rets.get(0);
							msg.reply(tt.getJsonArray("allotLocations"));
						}else{
							msg.fail(100, "无货位关系");
						}						

					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);

					}
				});

	}

/*	private JsonObject getQueryConditon(JsonObject so) {
		JsonObject query = new JsonObject();
		if (so.containsKey(sku) && so.getString(sku) != null && !so.isEmpty()) {
			query.put(sku, so.getString(sku));
		}
		return query;
	}*/

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
